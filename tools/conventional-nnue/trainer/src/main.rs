use bullet_lib::{
    game::{inputs::{ChessBucketsMirrored, SparseInputType}, outputs::MaterialCount},
    nn::{InitSettings, Shape, optimiser::{AdamW, AdamWParams}},
    trainer::{save::SavedFormat, schedule::{TrainingSchedule, TrainingSteps, lr, wdl}, settings::LocalSettings},
    value::{ValueTrainerBuilder, loader::DirectSequentialDataLoader},
};
use bulletformat::ChessBoard;
use bullet_lib::trainer::schedule::lr::LrScheduler;
use serde::Deserialize;
use sha2::{Digest, Sha256};
use std::{env, fs, io::{BufRead, BufReader, BufWriter, Write}, path::Path};

const HIDDEN: usize = 256;
const BUCKETS: [usize; 32] = [
    0,1,2,3, 4,4,5,5, 6,6,6,6, 7,7,7,7,
    8,8,8,8, 8,8,8,8, 9,9,9,9, 9,9,9,9,
];

#[derive(Deserialize)]
#[serde(deny_unknown_fields)]
struct Config {
    data: String, output: String, probes: String, seed: u64,
    batch_size: usize, batches_per_superbatch: usize,
    start_superbatch: usize, end_superbatch: usize, learning_rate: f32,
    wdl: f32, resume: Option<String>, data_sha256: String, probes_sha256: String,
    #[serde(default = "one")]
    save_rate: usize,
    data_fork_reason: Option<String>,
    #[serde(default)]
    lr_drop_every: usize,
    #[serde(default = "unit_gamma")]
    lr_gamma: f32,
    #[serde(default = "one")]
    output_buckets: usize,
}
fn one()->usize {1}
fn unit_gamma()->f32 {1.0}
fn digest(path:impl AsRef<Path>)->String {format!("{:x}",Sha256::digest(fs::read(path).unwrap()))}

fn compute_permitted() -> bool {
    // No implicit permission for direct invocations, missing state, or expired windows.
    let read = || -> Option<bool> {
        let path=env::var("SEED_NNUE_CONTROL").ok()?;
        let operation=env::var("SEED_NNUE_OPERATION").ok()?;
        let state:serde_json::Value=serde_json::from_slice(&fs::read(path).ok()?).ok()?;
        let now=std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).ok()?.as_secs_f64();
        Some(state["schema"]==1 && state["mode"]=="NORMAL" && state["active"]["id"]==operation
            && state["active"]["stop_requested_at"].is_null()
            && (state["expires_at"].is_null() || now<state["expires_at"].as_f64()?))
    };
    read().unwrap_or(false)
}

fn main() {
    let args: Vec<_> = env::args().collect();
    assert!(args.len() >= 3, "features <fen-file> | convert <text> <data> | train <config.json>");
    match args[1].as_str() {
        "features" => {
            let inputs = ChessBucketsMirrored::new(BUCKETS);
            for line in BufReader::new(fs::File::open(&args[2]).unwrap()).lines() {
                let fen = line.unwrap();
                let board: ChessBoard = format!("{fen} | 0 | 0.5").parse().unwrap();
                let (mut stm, mut ntm) = (Vec::new(), Vec::new());
                inputs.map_features(&board, |a,b| { stm.push(a); ntm.push(b); });
                println!("{}", serde_json::json!({"fen":fen,"stm":stm,"ntm":ntm}));
            }
        }
        "convert" => {
            assert!(compute_permitted(),"Launch expensive work through nnue-control.cmd run/train");
            assert_eq!(args.len(), 4);
            let mut out = BufWriter::new(fs::OpenOptions::new().write(true).create_new(true).open(&args[3]).unwrap());
            let mut count = 0;
            for line in BufReader::new(fs::File::open(&args[2]).unwrap()).lines() {
                if count%4096==0 {assert!(compute_permitted(),"Compute hold; incomplete conversion preserved");}
                let board: ChessBoard = line.unwrap().parse().unwrap();
                // bulletformat explicitly defines a packed 32-byte native record.
                let bytes = unsafe { std::slice::from_raw_parts((&board as *const ChessBoard).cast::<u8>(), std::mem::size_of::<ChessBoard>()) };
                out.write_all(bytes).unwrap(); count += 1;
            }
            out.flush().unwrap(); println!("Converted {count} records");
        }
        "train" => {
            let c:Config=serde_json::from_slice(&fs::read(&args[2]).unwrap()).unwrap();
            match c.output_buckets {
                1=>train::<1>(&args[2]),8=>train::<8>(&args[2]),
                _=>panic!("Only one or eight output buckets are supported"),
            }
        },
        _ => panic!("Unknown operation"),
    }
}

fn train<const OUTPUTS:usize>(config_path: &str) {
    assert!(compute_permitted(),"Launch training through nnue-control.cmd train");
    let config_bytes = fs::read(config_path).unwrap();
    let c: Config = serde_json::from_slice(&config_bytes).unwrap();
    assert_eq!(c.output_buckets,OUTPUTS);
    assert_eq!(format!("{:x}",Sha256::digest(fs::read(&c.data).unwrap())),c.data_sha256,"Dataset identity mismatch");
    assert_eq!(format!("{:x}",Sha256::digest(fs::read(&c.probes).unwrap())),c.probes_sha256,"Probe identity mismatch");
    assert!(c.batch_size > 0 && c.batch_size <= 4096);
    assert!(c.batches_per_superbatch > 0 && c.start_superbatch > 0 && c.end_superbatch >= c.start_superbatch);
    assert!(c.save_rate>0);
    assert!(c.learning_rate.is_finite() && c.learning_rate > 0.0 && c.learning_rate <= 0.01);
    assert!(c.lr_gamma.is_finite() && c.lr_gamma > 0.0 && c.lr_gamma <= 1.0);
    assert!(c.lr_drop_every > 0 || c.lr_gamma == 1.0,"Decay needs a global superbatch interval");
    assert!(c.wdl.is_finite() && (0.0..=1.0).contains(&c.wdl));
    assert!(!Path::new(&c.output).exists(), "Output directory must be new; preserve completed/failed runs");
    assert!(fs::metadata(&c.data).unwrap().len() >= (c.batch_size * 32) as u64);
    assert_eq!(fs::metadata(&c.data).unwrap().len()%32,0,"Truncated data record");
    if let Some(resume)=&c.resume {
        let path=Path::new(resume);
        let step:usize=path.file_name().unwrap().to_str().unwrap().strip_prefix("seed-cnnue-v1-").expect("Checkpoint name").parse().unwrap();
        let previous:Config=serde_json::from_slice(&fs::read(path.parent().unwrap().join("config.json")).unwrap()).unwrap();
        let manifest:serde_json::Value=serde_json::from_slice(&fs::read(path.join("seed-manifest.json")).expect("Resume needs a completed checkpoint receipt")).unwrap();
        assert_eq!(manifest["schema"],"seed-bullet-checkpoint-v1");
        assert_eq!(manifest["completed_superbatch"].as_u64().unwrap(),step as u64);
        assert_eq!(manifest["config_sha256"].as_str().unwrap(),digest(path.parent().unwrap().join("config.json")));
        for name in ["quantised.bin","raw.bin","optimiser_state/weights.bin","optimiser_state/momentum.bin","optimiser_state/velocity.bin"] {
            assert_eq!(manifest["files"][name].as_str().unwrap(),digest(path.join(name)),"Checkpoint integrity mismatch");
        }
        assert_eq!(manifest["files"].as_object().unwrap().len(),5,"Incomplete checkpoint receipt");
        assert_eq!(c.start_superbatch,step+1,"Resume must continue the checkpoint's next superbatch");
        assert_eq!((c.seed,c.batch_size,c.batches_per_superbatch,c.learning_rate,c.wdl),
            (previous.seed,previous.batch_size,previous.batches_per_superbatch,previous.learning_rate,previous.wdl),
            "Resume cannot change optimizer/schedule recipe");
        assert_eq!((c.lr_drop_every,c.lr_gamma),(previous.lr_drop_every,previous.lr_gamma),
            "Resume cannot change the persisted global learning-rate schedule");
        assert_eq!(c.output_buckets,previous.output_buckets,"Resume cannot change model geometry");
        if (c.data_sha256.as_str(),c.probes_sha256.as_str())!=(previous.data_sha256.as_str(),previous.probes_sha256.as_str()) {
            assert!(c.data_fork_reason.as_ref().is_some_and(|s|s.trim().len()>=12),
                "Dataset/probe change requires an explicit data_fork_reason; this is not exact continuation of the old stream");
        }
    }
    let mut trainer = ValueTrainerBuilder::default()
        .dual_perspective().seed(c.seed).optimiser(AdamW)
        .inputs(ChessBucketsMirrored::new(BUCKETS))
        .output_buckets(MaterialCount::<OUTPUTS>)
        .save_format(&[
            SavedFormat::id("l0w").transform(|store, weights| {
                let factoriser = store.get("l0f").values.f32().repeat(10);
                weights.into_iter().zip(factoriser).map(|(a,b)| a+b).collect()
            }).round().quantise::<i16>(255),
            SavedFormat::id("l0b").round().quantise::<i16>(255),
            SavedFormat::id("l1w").round().quantise::<i16>(64).transpose(),
            SavedFormat::id("l1b").round().quantise::<i16>(255*64),
        ])
        .loss_fn(|output, target| output.sigmoid().squared_error(target))
        .build(|builder, stm, ntm, buckets| {
            let factor = builder.new_weights("l0f", Shape::new(HIDDEN,768), InitSettings::Zeroed);
            let mut l0 = builder.new_affine("l0", 7680, HIDDEN);
            l0.weights = l0.weights + factor.repeat(10);
            let l1 = builder.new_affine("l1", HIDDEN*2, OUTPUTS);
            l1.forward(l0.forward(stm).screlu().concat(l0.forward(ntm).screlu())).select(buckets)
        });
    let clipping = AdamWParams { max_weight: 0.99, min_weight: -0.99, ..Default::default() };
    trainer.optimiser.set_params_for_weight("l0w", clipping);
    trainer.optimiser.set_params_for_weight("l0f", clipping);
    if let Some(resume) = &c.resume {
        trainer.optimiser.load_from_checkpoint(&format!("{resume}/optimiser_state")).unwrap();
    } else { assert_eq!(c.start_superbatch, 1, "Fresh training starts at 1"); }
    fs::create_dir_all(&c.output).unwrap();
    fs::write(format!("{}/config.json",c.output), config_bytes).unwrap();
    // Persist initializer + optimizer before any training. All probes use fresh
    // GPU evaluators after training because upstream caches evaluator weights.
    fs::create_dir(format!("{}/initial",c.output)).unwrap();
    trainer.optimiser.write_to_checkpoint(&format!("{}/initial",c.output)).unwrap();
    let schedule = TrainingSchedule {
        net_id: "seed-cnnue-v1".into(), eval_scale: 400.0,
        steps: TrainingSteps {batch_size:c.batch_size,batches_per_superbatch:c.batches_per_superbatch,
            start_superbatch:c.start_superbatch,end_superbatch:c.end_superbatch},
        wdl_scheduler:wdl::ConstantWDL{value:c.wdl},
        lr_scheduler:lr::StepLR{start:c.learning_rate,gamma:c.lr_gamma,
            step:if c.lr_drop_every==0 {usize::MAX} else {c.lr_drop_every}}, save_rate:c.save_rate,
    };
    let settings = LocalSettings {threads:1,test_set:None,output_directory:&c.output,batch_queue_size:2};
    trainer.run(&schedule,&settings,&DirectSequentialDataLoader::new(&[&c.data]));
    for step in c.start_superbatch..=c.end_superbatch {
        if step%c.save_rate!=0 && step!=c.end_superbatch {continue;}
        let path=Path::new(&c.output).join(format!("seed-cnnue-v1-{step}"));
        let mut files=serde_json::Map::new();
        for name in ["quantised.bin","raw.bin","optimiser_state/weights.bin","optimiser_state/momentum.bin","optimiser_state/velocity.bin"] {
            files.insert(name.into(),digest(path.join(name)).into());
        }
        // Publish the receipt last. A missing/partial receipt is never a resume point.
        for name in ["quantised.bin","raw.bin","optimiser_state/weights.bin","optimiser_state/momentum.bin","optimiser_state/velocity.bin"] {
            fs::OpenOptions::new().write(true).open(path.join(name)).unwrap().sync_all().unwrap();
        }
        let mut receipt=fs::OpenOptions::new().write(true).create_new(true).open(path.join("seed-manifest.json.tmp")).unwrap();
        receipt.write_all(&serde_json::to_vec_pretty(&serde_json::json!({
            "schema":"seed-bullet-checkpoint-v1","bullet":"579b78b37a694cacd6e9ede4706386c15c34837c",
            "completed_superbatch":step,"config_sha256":digest(config_path),"files":files,
            "effective_learning_rate":schedule.lr_scheduler.lr(0,step),
            "reproducibility":"fixed recipe/data/seed; GPU float reductions are not bitwise deterministic"
        })).unwrap()).unwrap();
        receipt.sync_all().unwrap(); drop(receipt);
        fs::rename(path.join("seed-manifest.json.tmp"),path.join("seed-manifest.json")).unwrap();
    }
    let mut out = BufWriter::new(fs::File::create(format!("{}/gpu-probes.jsonl",c.output)).unwrap());
    for line in BufReader::new(fs::File::open(&c.probes).unwrap()).lines() {
        let fen=line.unwrap(); let raw=trainer.eval(&fen);
        writeln!(out,"{}",serde_json::json!({"fen":fen,"raw":raw,"cp":raw*400.0})).unwrap();
    }
}
