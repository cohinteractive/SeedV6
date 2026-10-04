package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Trainer;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.validation.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Small, prospectively configured screens; no checkpoint promotion or seed selection. */
public final class ArchitectureScreen {
    private static final Gson JSON=new GsonBuilder().serializeNulls().create();
    public static IncrementalModel model(String variant,long seed) {
        if (variant.endsWith("-material"))
            return new MaterialBootstrapModel(model(variant.substring(0, variant.length() - "-material".length()), seed));
        return switch(variant) {
            case "baseline" -> new HalfKpHead(seed,false,false);
            case "baseline-hires" -> new HalfKpHead(seed,false,true);
            case "odd" -> new HalfKpHead(seed,true,false);
            case "odd-hires" -> new HalfKpHead(seed,true,true);
            case "piece-square" -> new SharedFeatures(seed,SharedFeatures.Kind.PIECE_SQUARE);
            case "smooth" -> new SharedFeatures(seed,SharedFeatures.Kind.SMOOTH);
            case "mirror-smooth" -> new SharedFeatures(seed,SharedFeatures.Kind.MIRROR_SMOOTH);
            case "counts" -> new SharedFeatures(seed,SharedFeatures.Kind.COUNTS);
            case "zero-head" -> new HalfKpHead(seed,false,false,true);
            case "local3" -> new LocalPatchModel(seed,1);
            case "local5" -> new LocalPatchModel(seed,2);
            case "global-linear" -> new GlobalPairModel(seed,false);
            case "global-pair" -> new GlobalPairModel(seed,true);
            default -> throw new IllegalArgumentException("Unknown candidate "+variant);
        };
    }
    public static Map<String,Object> diagnostics(IncrementalModel model,List<BootstrapAudit.Edge> edges) {
        var work=model.worker(3); double maxError=0,maxNullError=0;
        double xx=0,yy=0,xy=0,sx=0,sy=0;long n=0;
        var mapped=new TreeSet<Integer>();
        for(var e:edges) {
            var p=e.parent();var c=e.child();work.refresh(p,0);work.transition(p,c,0,1);work.refresh(c,2);
            double a=work.raw(p,0),b=work.raw(c,1),oracle=work.raw(c,2);
            maxError=Math.max(maxError,Math.abs(b-oracle));mapped.add(work.evaluate(p,0));
            var flipped=p.clone();flipped[4]^=1;
            maxNullError=Math.max(maxNullError,Math.abs(a+work.raw(flipped,0)));
            if(e.kind().equals("quiet")) {
                double x=Board.player((int)p[4])==0?a:-a,y=Board.player((int)c[4])==0?b:-b;
                n++;sx+=x;sy+=y;xx+=x*x;yy+=y*y;xy+=x*y;
            }
        }
        if(maxError>1e-6)throw new AssertionError("Incremental/full mismatch: "+maxError);
        var out=new LinkedHashMap<String,Object>();out.put("parameters",model.parameters());out.put("incrementalMaxRawError",maxError);
        out.put("samePlacementAntisymmetryMaxError",maxNullError);out.put("mappedScores",mapped);
        double vx=xx-sx*sx/n,vy=yy-sy*sy/n;
        out.put("quietWhiteCorrelation",vx>1e-24&&vy>1e-24?(xy-sx*sy/n)/Math.sqrt(vx*vy):null);
        int count=Math.min(256,edges.size());var micro=model.worker(count+1);
        for(int i=0;i<count;i++)micro.refresh(edges.get(i).parent(),i);
        out.put("prepared",BootstrapAudit.measure(count,i->micro.raw(edges.get(i).parent(),i)));
        out.put("transitionAndInference",BootstrapAudit.measure(count,i->{var e=edges.get(i);micro.transition(e.parent(),e.child(),i,count);return micro.raw(e.child(),count);}));
        out.put("fullRefreshAndInference",BootstrapAudit.measure(count,i->{micro.refresh(edges.get(i).child(),count);return micro.raw(edges.get(i).child(),count);}));
        out.put("transitionAndScore",BootstrapAudit.measure(count,i->{var e=edges.get(i);micro.transition(e.parent(),e.child(),i,count);return micro.evaluate(e.child(),count);}));
        out.put("fullRefreshAndScore",BootstrapAudit.measure(count,i->{micro.refresh(edges.get(i).child(),count);return micro.evaluate(edges.get(i).child(),count);}));
        return out;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=7&&args.length!=8)throw new IllegalArgumentException("NEW_OUT MODE[audit|material|match|confirm|depthcheck|time] VARIANTS_CSV SEEDS_CSV_OR_@FILE PAIRS DEPTH CAP [MILLIS]");
        var path=Path.of(args[0]);String mode=args[1];
        boolean timed=mode.equals("time");
        if(!mode.equals("audit")&&!mode.equals("material")&&!mode.equals("match")&&!mode.equals("confirm")&&!mode.equals("depthcheck")&&!timed)throw new IllegalArgumentException("Mode");
        if((args.length==8)!=timed)throw new IllegalArgumentException("Millis required only for time mode");
        int millis=timed?Integer.parseInt(args[7]):-1;
        if(timed&&(millis<1||millis>1000))throw new IllegalArgumentException("Time budget");
        boolean independentBlocks=mode.equals("confirm")||mode.equals("depthcheck")||timed;
        int openingOffset=mode.equals("depthcheck")?64:0;
        String seedText=args[3].startsWith("@")?Files.readString(Path.of(args[3].substring(1))).trim():args[3];
        String[] variants=args[2].split(",");long[] seeds=Arrays.stream(seedText.split(",")).mapToLong(Long::parseLong).toArray();
        int pairs=Integer.parseInt(args[4]),depth=Integer.parseInt(args[5]),cap=Integer.parseInt(args[6]);
        if(pairs<1||depth<1||depth>(timed?64:6)||cap<1)throw new IllegalArgumentException("Bounds");
        Files.createDirectory(path);
        var meta=new LinkedHashMap<String,Object>();meta.put("args",args);meta.put("jvmArgs",ManagementFactory.getRuntimeMXBean().getInputArguments());
        meta.put("java",System.getProperty("java.runtime.version"));meta.put("os",System.getProperty("os.name"));meta.put("arch",System.getProperty("os.arch"));
        meta.put("searchPolicy","production BRN-3 reference; fresh seed-matched models; per-game4MiBTT; one thread; "+(timed?"equal milliseconds, last completed iteration, configured depth ceiling; untimed common eight-root depth4 warmup per model; isolated from other research jobs":"equal fixed depth")+"; no qsearch/book/tablebase; caps unscored");
        long openingSeed=timed?2026100505L:independentBlocks?2026100503L:BootstrapAudit.OPENING_SEED;
        meta.put("seeds",seeds);meta.put("openingOffset",openingOffset);meta.put("openingSeed",openingSeed);meta.put("corpusSeed",BootstrapAudit.CORPUS_SEED);
        meta.put("openingPolicy",independentBlocks?"Independent opening-index block per initialization; same block across variants; paired colours; legal-uniform6..10; no filtering":"Common opening indices across initialization seeds; paired colours; legal-uniform6..10; no filtering");
        var hashes=new TreeMap<String,String>();
        try(var files=Files.list(Path.of("app/src/verification/java/com/ohinteractive/seedv6/tools/nnue/cglhw"))) {
            for(var f:files.filter(f->f.toString().endsWith(".java")).toList())hashes.put(f.toString(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f))));
        }
        for(String name:List.of("core/nnue/NnueMaterialBootstrap.java", "search/evaluation/SearchEvaluation.java",
                "core/brn3/Brn3Features.java", "core/brn3/Brn3Model.java", "core/brn3/Brn3Workspace.java", "core/brn3/Brn3Trainer.java", "core/brn3/Brn3SearchCalibration.java")) {
            var f=Path.of("app/src/main/java/com/ohinteractive/seedv6").resolve(name);
            hashes.put(f.toString(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f))));
        }
        meta.put("trackPolicy","-material suffix: Track B BRN3 V1 additive material, unchanged neural mapping; all other arms: Track A knowledge-free controls");
        meta.put("sources",hashes);Files.writeString(path.resolve("metadata.json"),JSON.toJson(meta),StandardOpenOption.CREATE_NEW);
        var edges=BootstrapAudit.corpus(16,80);var root=Board.startingPosition();
        var config=new ValidationConfig(pairs,openingSeed,6,10,depth,1,NnueScoreMapping.V1,cap);
        try(var writer=Files.newBufferedWriter(path.resolve("results.jsonl"),StandardOpenOption.CREATE_NEW)) {
            for(int seedOrdinal=0;seedOrdinal<seeds.length;seedOrdinal++) {
                long seed=seeds[seedOrdinal];
                var brn=new Brn3Trainer(seed).snapshot();
                if(mode.equals("audit")) {
                    var ref=brn.newWorkspace();
                    int count=Math.min(256,edges.size());
                    write(writer,Map.of("type","referenceCost","seed",seed,"data",
                            BootstrapAudit.measure(count,i->com.ohinteractive.seedv6.core.brn3.Brn3Model.score(
                                    ref.evaluatePawns(edges.get(i).child(),com.ohinteractive.seedv6.core.brn3.Brn3SearchCalibration.RESIDUAL_GAIN)))));
                    int[] material=new int[count];for(int i=0;i<count;i++)material[i]=com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.whiteScore(edges.get(i).parent());
                    write(writer,Map.of("type","materialCost","seed",seed,"data",BootstrapAudit.measure(count,i->
                            com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap.update(edges.get(i).parent(),edges.get(i).child(),material[i]))));
                }
                for(String variant:variants) {
                    var candidate=model(variant,seed);
                    if(timed)warm(candidate,brn);
                    if(mode.equals("audit"))write(writer,Map.of("type","candidateAudit","seed",seed,"variant",variant,"data",diagnostics(candidate,edges)));
                    else if(mode.equals("material"))write(writer,Map.of("type","materialProbe","seed",seed,"variant",variant,"data",MaterialRemovalProbe.report(candidate)));
                    else for(int i=0;i<pairs;i++) {
                        int openingIndex=openingOffset+openingIndex(independentBlocks,seedOrdinal,pairs,i);
                        var opening=ValidationArena.opening(root,GameHistory.initial(root),config,openingIndex);
                        if(!opening.newGame(cap).active())throw new IllegalStateException("Inactive opening");
                        var first=BootstrapAudit.game(opening,()->new SearchDriver(new ExactSearchAdapter(candidate.worker(257),new TTable(4))),
                                ()->new SearchDriver(new ExactSearchAdapter(SearchEvaluation.brn3(brn),new TTable(4))),i%2,depth,cap,millis);
                        var second=BootstrapAudit.game(opening,()->new SearchDriver(new ExactSearchAdapter(candidate.worker(257),new TTable(4))),
                                ()->new SearchDriver(new ExactSearchAdapter(SearchEvaluation.brn3(brn),new TTable(4))),1-i%2,depth,cap,millis);
                        write(writer,Map.of("type","pair","variant",variant,"seed",seed,"index",openingIndex,"openingHash",opening.identity(),"first",first,"second",second));
                    }
                }
            }
        }
    }
    static int openingIndex(boolean independentBlocks,int seedOrdinal,int pairs,int pair) {
        return independentBlocks?Math.addExact(Math.multiplyExact(seedOrdinal,pairs),pair):pair;
    }
    private static void warm(IncrementalModel model,com.ohinteractive.seedv6.core.brn3.Brn3Model brn) {
        var config=new ValidationConfig(8,2026100506L,6,10,4,1,NnueScoreMapping.V1,2048);
        var root=Board.startingPosition();
        try(var a=new SearchDriver(new ExactSearchAdapter(model.worker(257),new TTable(4)));
            var b=new SearchDriver(new ExactSearchAdapter(SearchEvaluation.brn3(brn),new TTable(4)))) {
            for(int i=0;i<8;i++) {
                var opening=ValidationArena.opening(root,GameHistory.initial(root),config,i);
                for(var driver:i%2==0?List.of(a,b):List.of(b,a)) {
                    driver.newGame();var result=driver.search(new SearchRequest(opening.board(),opening.history(),4));
                    if(!result.targetDepthCompleted())throw new IllegalStateException("Untimed warmup incomplete");
                }
            }
        }
    }
    private static void write(BufferedWriter out,Object row)throws IOException {
        var text=JSON.toJson(row);out.write(text);out.newLine();out.flush();System.out.println(text);
    }
}
