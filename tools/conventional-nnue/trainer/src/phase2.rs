//! Isolated Phase 2 research trainer. Existing conventional executable is unchanged.
use bullet_lib::{
    game::{inputs::{ChessBucketsMirrored, SparseInputType}, outputs::MaterialCount},
    nn::{InitSettings, Shape, optimiser::{AdamW, AdamWParams}},
    trainer::{save::SavedFormat, schedule::{TrainingSchedule, TrainingSteps, lr, wdl}, settings::LocalSettings},
    value::{ValueTrainerBuilder, loader::DirectSequentialDataLoader},
};
use bullet_lib::trainer::schedule::lr::LrScheduler;
use bulletformat::ChessBoard;
use serde::Deserialize;
use sha2::{Digest, Sha256};
use std::{env, fs, io::{BufRead, BufReader, BufWriter, Write}, path::Path};

const H:usize=256;
const BUCKETS:[usize;32]=[0,1,2,3,4,4,5,5,6,6,6,6,7,7,7,7,8,8,8,8,8,8,8,8,9,9,9,9,9,9,9,9];
const PAIRS:usize=96*95/2;

#[derive(Clone)]
struct Inputs { pairs:bool }
impl SparseInputType for Inputs {
    type RequiredDataType=ChessBoard;
    fn num_inputs(&self)->usize {7680+if self.pairs {PAIRS}else{0}}
    fn max_active(&self)->usize {32+if self.pairs {120}else{0}}
    fn shorthand(&self)->String {format!("phase2-{}",self.num_inputs())}
    fn description(&self)->String {"Conventional king buckets plus optional adjacent-file unordered pawn pairs".into()}
    fn map_features<F:FnMut(usize,usize)>(&self,pos:&ChessBoard,mut f:F) {
        ChessBucketsMirrored::new(BUCKETS).map_features(pos,&mut f);
        if !self.pairs {return;}
        let mut pawns=[(0usize,0usize);16];let mut n=0;
        for (pc,sq) in *pos {
            if pc&7==0 {
                assert!((8..56).contains(&sq) && n<16,"Invalid pawn position");
                pawns[n]=((pc>>3) as usize,sq as usize);n+=1;
            }
        }
        let flips=[if pos.our_ksq()%8>3 {7}else{0},56^if pos.opp_ksq()%8>3 {7}else{0}];
        let index=|i:usize,j:usize,side:usize| {
            let id=|(colour,sq):(usize,usize)| (colour^side)*48+(sq^flips[side])-8;
            let a=id(pawns[i]);let b=id(pawns[j]);let hi=a.max(b);let lo=a.min(b);
            7680+hi*(hi-1)/2+lo
        };
        for i in 0..n {for j in i+1..n {
            if (pawns[i].1%8).abs_diff(pawns[j].1%8)<=1 {f(index(i,j,0),index(i,j,1));}
        }}
    }
}

#[derive(Deserialize)]
#[serde(deny_unknown_fields)]
struct Config {
    architecture:String,data:String,output:String,probes:String,seed:u64,
    batch_size:usize,batches_per_superbatch:usize,start_superbatch:usize,end_superbatch:usize,
    learning_rate:f32,wdl:f32,resume:Option<String>,data_sha256:String,probes_sha256:String,
    lr_drop_every:usize,lr_gamma:f32,
}
fn digest(path:impl AsRef<Path>)->String {format!("{:x}",Sha256::digest(fs::read(path).unwrap()))}
fn permitted()->bool {
    let read=||->Option<bool>{
        let s:serde_json::Value=serde_json::from_slice(&fs::read(env::var("SEED_NNUE_CONTROL").ok()?).ok()?).ok()?;
        let op=env::var("SEED_NNUE_OPERATION").ok()?;
        let now=std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).ok()?.as_secs_f64();
        Some(s["schema"]==1 && s["mode"]=="NORMAL" && s["active"]["id"]==op
             && s["active"]["stop_requested_at"].is_null()
             && (s["expires_at"].is_null()||now<s["expires_at"].as_f64()?))
    };read().unwrap_or(false)
}
fn main() {
    let a:Vec<_>=env::args().collect();
    assert!(a.len()>=3,"features FENS ARCHITECTURE | train CONFIG");
    if a[1]=="features" {
        assert_eq!(a.len(),4);assert!(["base","deep16","pawnpairs"].contains(&a[3].as_str()));
        let input=Inputs{pairs:a[3]=="pawnpairs"};
        for line in BufReader::new(fs::File::open(&a[2]).unwrap()).lines() {
            let fen=line.unwrap();let board:ChessBoard=format!("{fen} | 0 | 0.5").parse().unwrap();
            let(mut stm,mut ntm)=(vec![],vec![]);input.map_features(&board,|x,y|{stm.push(x);ntm.push(y);});
            println!("{}",serde_json::json!({"fen":fen,"stm":stm,"ntm":ntm}));
        }return;
    }
    assert_eq!(a[1],"train");assert_eq!(a.len(),3);assert!(permitted(),"Owner supervised operation required");
    let bytes=fs::read(&a[2]).unwrap();let c:Config=serde_json::from_slice(&bytes).unwrap();
    let deep=c.architecture=="deep16";let pairs=c.architecture=="pawnpairs";
    assert!(deep||pairs||c.architecture=="base");
    assert_eq!(digest(&c.data),c.data_sha256);assert_eq!(digest(&c.probes),c.probes_sha256);
    assert!(c.batch_size>0&&c.batch_size<=4096&&c.batches_per_superbatch>0&&c.batches_per_superbatch<=1024);
    assert_eq!(c.start_superbatch,c.end_superbatch,"One checkpoint per supervised segment");
    assert!(c.start_superbatch>0&&c.learning_rate.is_finite()&&c.learning_rate>0.&&c.learning_rate<=0.01);
    assert!(c.wdl.is_finite()&&(0.0..=1.0).contains(&c.wdl));
    assert!(c.lr_drop_every>0&&c.lr_gamma.is_finite()&&c.lr_gamma>0.&&c.lr_gamma<=1.);
    assert!(!Path::new(&c.output).exists(),"Never overwrite research output");
    assert_eq!(fs::metadata(&c.data).unwrap().len()%32,0);
    let mut formats=vec![
        SavedFormat::id("l0w").transform(move|store,weights|{
            let factor=store.get("l0f").values.f32().repeat(10);
            let mut result:Vec<_>=weights.into_iter().zip(factor).map(|(a,b)|a+b).collect();
            if pairs {result.extend(store.get("ppw").values.f32());}result
        }).round().quantise::<i16>(255),
        SavedFormat::id("l0b").round().quantise::<i16>(255),
        SavedFormat::id("l1w").transpose().round().quantise::<i16>(64),
        SavedFormat::id("l1b").round().quantise::<i16>(255*64),
    ];
    if deep {formats.extend([
        SavedFormat::id("l2w").transpose().round().quantise::<i16>(64),
        SavedFormat::id("l2b").round().quantise::<i16>(255*64),
        SavedFormat::id("l3w").transpose().round().quantise::<i16>(64),
        SavedFormat::id("l3b").round().quantise::<i16>(255*64),
    ]);}
    let mut trainer=ValueTrainerBuilder::default().dual_perspective().seed(c.seed).optimiser(AdamW)
        .inputs(Inputs{pairs}).output_buckets(MaterialCount::<8>).save_format(&formats)
        .loss_fn(|output,target|output.sigmoid().squared_error(target))
        .build(|b,stm,ntm,buckets| {
            let factor=b.new_weights("l0f",Shape::new(H,768),InitSettings::Zeroed);
            let mut l0=b.new_affine("l0",7680,H);l0.weights=l0.weights+factor.repeat(10);
            if pairs {
                let pp=b.new_weights("ppw",Shape::new(H,PAIRS),InitSettings::Zeroed);
                l0.weights=l0.weights.reshape((H*7680,1)).concat(pp.reshape((H*PAIRS,1))).reshape((H,7680+PAIRS));
            }
            let hidden=l0.forward(stm).screlu().concat(l0.forward(ntm).screlu());
            let l1=b.new_affine("l1",2*H,8);let direct=l1.forward(hidden).select(buckets);
            if deep {
                let l2=b.new_affine("l2",2*H,16);
                let l3w=b.new_weights("l3w",Shape::new(8,16),InitSettings::Zeroed);
                let l3b=b.new_weights("l3b",Shape::new(8,1),InitSettings::Zeroed);
                direct+(l3w.matmul(l2.forward(hidden).screlu())+l3b).select(buckets)
            }else{direct}
        });
    let clip=AdamWParams{max_weight:0.99,min_weight:-0.99,..Default::default()};
    trainer.optimiser.set_params_for_weight("l0w",clip);trainer.optimiser.set_params_for_weight("l0f",clip);
    if pairs {trainer.optimiser.set_params_for_weight("ppw",clip);}
    if let Some(path)=&c.resume {
        let path=Path::new(path);let previous:Config=serde_json::from_slice(&fs::read(path.parent().unwrap().join("config.json")).unwrap()).unwrap();
        assert_eq!((&c.architecture,&c.data_sha256,&c.probes_sha256,c.seed,c.batch_size,c.batches_per_superbatch),
                   (&previous.architecture,&previous.data_sha256,&previous.probes_sha256,previous.seed,previous.batch_size,previous.batches_per_superbatch));
        assert_eq!((c.learning_rate,c.wdl,c.lr_drop_every,c.lr_gamma),(previous.learning_rate,previous.wdl,previous.lr_drop_every,previous.lr_gamma));
        assert_eq!(c.start_superbatch,previous.end_superbatch+1);
        let receipt:serde_json::Value=serde_json::from_slice(&fs::read(path.join("receipt.json")).unwrap()).unwrap();
        assert_eq!(receipt["config_sha256"],digest(path.parent().unwrap().join("config.json")));
        for name in ["quantised.bin","raw.bin","optimiser_state/weights.bin","optimiser_state/momentum.bin","optimiser_state/velocity.bin"] {
            assert_eq!(receipt["files"][name],digest(path.join(name)));
        }
        trainer.optimiser.load_from_checkpoint(&format!("{}/optimiser_state",path.display())).unwrap();
    }else{assert_eq!(c.start_superbatch,1);}
    fs::create_dir_all(&c.output).unwrap();fs::write(format!("{}/config.json",c.output),bytes).unwrap();
    fs::create_dir(format!("{}/initial",c.output)).unwrap();
    trainer.optimiser.write_to_checkpoint(&format!("{}/initial",c.output)).unwrap();
    let schedule=TrainingSchedule{net_id:"seed-phase2".into(),eval_scale:400.0,
        steps:TrainingSteps{batch_size:c.batch_size,batches_per_superbatch:c.batches_per_superbatch,start_superbatch:c.start_superbatch,end_superbatch:c.end_superbatch},
        wdl_scheduler:wdl::ConstantWDL{value:c.wdl},
        lr_scheduler:lr::StepLR{start:c.learning_rate,gamma:c.lr_gamma,step:c.lr_drop_every},save_rate:1};
    let settings=LocalSettings{threads:1,test_set:None,output_directory:&c.output,batch_queue_size:2};
    trainer.run(&schedule,&settings,&DirectSequentialDataLoader::new(&[&c.data]));
    let path=Path::new(&c.output).join(format!("seed-phase2-{}",c.end_superbatch));
    let mut out=BufWriter::new(fs::OpenOptions::new().write(true).create_new(true).open(path.join("gpu-probes.jsonl")).unwrap());
    for line in BufReader::new(fs::File::open(&c.probes).unwrap()).lines() {
        let fen=line.unwrap();let raw=trainer.eval(&fen);writeln!(out,"{}",serde_json::json!({"fen":fen,"raw":raw})).unwrap();
    }out.flush().unwrap();drop(out);
    let mut files=serde_json::Map::new();
    for name in ["quantised.bin","raw.bin","optimiser_state/weights.bin","optimiser_state/momentum.bin","optimiser_state/velocity.bin","gpu-probes.jsonl"] {
        fs::OpenOptions::new().write(true).open(path.join(name)).unwrap().sync_all().unwrap();files.insert(name.into(),digest(path.join(name)).into());
    }
    let receipt=serde_json::json!({"schema":"seed-phase2-checkpoint-v1","architecture":c.architecture,"completed_superbatch":c.end_superbatch,
        "config_sha256":digest(&a[2]),"files":files,"effective_learning_rate":schedule.lr_scheduler.lr(0,c.end_superbatch)});
    let mut out=fs::OpenOptions::new().write(true).create_new(true).open(path.join("receipt.json.tmp")).unwrap();
    out.write_all(&serde_json::to_vec_pretty(&receipt).unwrap()).unwrap();out.sync_all().unwrap();drop(out);
    fs::rename(path.join("receipt.json.tmp"),path.join("receipt.json")).unwrap();
}
