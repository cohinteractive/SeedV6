package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Isolated learning-strength observations; no production settings or stores are changed. */
public final class BrnLearningProbe {
    static Brn3Model model(Path path) throws IOException {
        try (var in = new BufferedInputStream(Files.newInputStream(path))) { return Brn3Codec.readModel(in); }
    }
    static ToDoubleFunction<long[]> evaluator(Brn3Model model, double scale) {
        var workspace = model.newWorkspace();
        return board -> {
            double material = Brn3Features.material(board);
            return scale == 0 ? material : material + scale * (workspace.evaluatePawns(board) - material);
        };
    }
    static SearchDriver driver(ToDoubleFunction<long[]> evaluate) {
        return new SearchDriver(new ExactSearchAdapter((board, ply) -> Brn3Model.score(evaluate.applyAsDouble(board)), new TTable(4)));
    }
    static Map<String, Object> distribution(List<Double> source) {
        var values = source.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        if (values.length == 0) return Map.of("count", 0);
        var result = new LinkedHashMap<String, Object>();
        result.put("count", values.length); result.put("mean", Arrays.stream(values).average().orElseThrow());
        for (double p : new double[]{0, .01, .1, .5, .9, .99, 1}) result.put("p" + (int)(100*p), values[(int)((values.length-1)*p)]);
        return result;
    }
    static long[] child(long[] board, long move) {
        var child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],move,child);
        return child;
    }
    static Map<String, Object> diagnose(Brn3Model model, BrnResearchData.Dataset data, int roots) {
        var evaluate = evaluator(model, 1); var residuals = new ArrayList<Double>();
        var checkedResiduals = new ArrayList<Double>(); var deltas = new ArrayList<Double>();
        var quietResidualDeltas = new ArrayList<Double>(); var captureResidualDeltas = new ArrayList<Double>();
        double materialLoss=0, trainedLoss=0, materialCe=0, trainedCe=0;
        int checked=0; var examples=data.validation();
        for (int i=0;i<examples.size();i++) {
            var e=examples.get(i); var b=e.board(); double m=Brn3Features.material(b), v=evaluate.applyAsDouble(b);
            double target=e.outcome();
            materialLoss += .5*Math.pow(Brn3Objective.outcome(m,b)-target,2);
            trainedLoss += .5*Math.pow(Brn3Objective.outcome(v,b)-target,2);
            materialCe += Brn3Objective.crossEntropy(m,e.sfMaterial(),target)[0];
            trainedCe += Brn3Objective.crossEntropy(v,e.sfMaterial(),target)[0];
            residuals.add(v-m);
            if(Board.isPlayerInCheck(b[0],b[1],b[2],b[3],Board.player((int)b[4]))) { checked++; checkedResiduals.add(v-m); }
            if(i<256) {
                var game=new HeadlessGame(b,1);
                for(long move:game.legalMoves()) {
                    var c=child(b,move); double cv=-evaluate.applyAsDouble(c), cm=-Brn3Features.material(c);
                    deltas.add(cv-v); (Math.abs(cm-m)<1e-6?quietResidualDeltas:captureResidualDeltas).add((cv-cm)-(v-m));
                }
            }
        }
        var report=new LinkedHashMap<String,Object>();int n=examples.size();
        report.put("validationCount",n);report.put("checked",checked);
        report.put("materialHalfMse",materialLoss/n);report.put("trainedHalfMse",trainedLoss/n);
        report.put("materialCe",materialCe/n);report.put("trainedCe",trainedCe/n);
        report.put("residual",distribution(residuals));report.put("checkedResidual",distribution(checkedResiduals));
        report.put("commonPerspectiveChildMinusParent",distribution(deltas));
        report.put("unchangedMaterialResidualDelta",distribution(quietResidualDeltas));
        report.put("changedMaterialResidualDelta",distribution(captureResidualDeltas));
        var searches=new ArrayList<Object>();
        for(int i=0;i<Math.min(roots,n);i++) {
            var e=examples.get(i*n/Math.min(roots,n));var row=new LinkedHashMap<String,Object>();row.put("fen",Fen.fromBoard(e.board()));row.put("cp",e.cp());
            for(double scale:new double[]{0,1,.25}) {
                var fn=evaluator(model,scale);
                try(var search=driver(fn)) {
                    long start=System.nanoTime();var control=SearchControl.controlled(2_000_000,start,10_000_000_000L,TimeSource.SYSTEM);
                    var result=search.search(new SearchRequest(e.board(),GameHistory.initial(e.board()),4,SearchObserver.NONE,control,false));
                    var r=result.lastCompletedResult();var detail=new LinkedHashMap<String,Object>();
                    detail.put("complete",result.targetDepthCompleted());detail.put("nodes",control.nodes());detail.put("seconds",(System.nanoTime()-start)/1e9);
                    if(r!=null) {detail.put("score",r.score());detail.put("depth",r.depth());detail.put("pv",Arrays.stream(r.principalVariation()).mapToObj(x->Long.toUnsignedString(x,16)).toList());
                        var leaf=e.board();for(long move:r.principalVariation())leaf=child(leaf,move);
                        detail.put("leafFen",Fen.fromBoard(leaf));detail.put("leafMaterial",Brn3Features.material(leaf));detail.put("leafTrained",evaluate.applyAsDouble(leaf));}
                    row.put(scale==0?"material":scale==1?"trained":"quarter",detail);
                }
            }
            searches.add(row);
        }
        report.put("searchRoots",searches);return report;
    }

    record Ply(String fen, String move, int actor, int score, int depth, long nodes,
               double material, double trained, double afterMaterial, double afterTrained) {}
    record Game(String termination, String failure, Double candidateScore, List<Ply> plies, String finalFen) {}
    record Pair(int index, String openingHash, String openingFen, Game white, Game black) {
        boolean complete(){return white.candidateScore()!=null && black.candidateScore()!=null;}
        double score(){return (white.candidateScore()+black.candidateScore())/2;}
    }
    static Game play(ValidationArena.Opening opening, Brn3Model model, double scale, int color, int depth, int cap, int workers) {
        var game=opening.newGame(cap);var traces=new ArrayList<Ply>();String failure=null;
        var trained=evaluator(model,1);
        try(var candidate=workers==0?driver(evaluator(model,scale)):modelDriver(model,scale,workers);
            var incumbent=workers==0?driver(evaluator(model,0)):modelDriver(model,0,workers)) {
            while(game.active()) {
                int actor=game.sideToMove()==color?0:1;var b=game.boardSnapshot();
                var control=SearchControl.controlled(10_000_000,System.nanoTime(),10_000_000_000L,TimeSource.SYSTEM);
                var outcome=(actor==0?candidate:incumbent).search(new SearchRequest(b,game.historySnapshot(),depth,SearchObserver.NONE,control,false));
                var r=outcome.lastCompletedResult();
                if(!outcome.targetDepthCompleted()||r==null||!r.completed()||!r.hasMove()) {
                    failure="Requested depth incomplete";game.abort(GameTermination.SEARCH_FAILURE,failure);break;
                }
                game.play(r.bestMove());var c=game.boardSnapshot();
                traces.add(new Ply(Fen.fromBoard(b),Long.toUnsignedString(r.bestMove(),16),actor,r.score(),r.depth(),control.nodes(),
                        Brn3Features.material(b),trained.applyAsDouble(b),-Brn3Features.material(c),-trained.applyAsDouble(c)));
            }
        } catch(RuntimeException e) {failure=e.toString();game.abort(GameTermination.INFRASTRUCTURE_FAILURE,failure);}
        Double score=failure==null&&game.termination().completed()?(game.termination().result().orElseThrow().target(color)+1)/2:null;
        return new Game(game.termination().name(),failure,score,List.copyOf(traces),Fen.fromBoard(game.boardSnapshot()));
    }
    static SearchDriver modelDriver(Brn3Model model,double scale,int workers) {
        var definition=scale==Brn3SearchCalibration.RESIDUAL_GAIN
                ?com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.brn3(model)
                :com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.brn3Research(model,scale);
        return new SearchDriver(ProductionSearch.create(workers,definition));
    }
    static Map<String,Object> summary(List<Pair> pairs) {
        var complete=pairs.stream().filter(Pair::complete).toList();var result=new LinkedHashMap<String,Object>();
        result.put("completedPairs",complete.size());result.put("incompletePairs",pairs.size()-complete.size());
        int w=0,d=0,l=0,completedGames=0;double completedPoints=0;var terminations=new TreeMap<String,Integer>();
        for(var pair:pairs)for(var g:List.of(pair.white(),pair.black())) {
            terminations.merge(g.termination(),1,Integer::sum);
            if(g.candidateScore()!=null){completedGames++;completedPoints+=g.candidateScore();}
            if(pair.complete()) {if(g.candidateScore()==1)w++;else if(g.candidateScore()==0)l++;else d++;}
        }
        result.put("wins",w);result.put("draws",d);result.put("losses",l);result.put("terminations",terminations);
        result.put("wdlScope","Complete pairs only");result.put("completedGamesIncludingPartialPairs",completedGames);
        if(!pairs.isEmpty())result.put("allGameScoreBounds",List.of(completedPoints/(2*pairs.size()),(completedPoints+2*pairs.size()-completedGames)/(2*pairs.size())));
        if(!complete.isEmpty()) {
            result.put("pointScore",complete.stream().mapToDouble(Pair::score).average().orElseThrow());
            result.put("hoeffdingOneSided95Lower",complete.stream().mapToDouble(Pair::score).average().orElseThrow()-Math.sqrt(Math.log(20)/(2*complete.size())));
            var rng=new SplittableRandom(83473);double[] boots=new double[10000];
            for(int i=0;i<boots.length;i++){for(int j=0;j<complete.size();j++)boots[i]+=complete.get(rng.nextInt(complete.size())).score();boots[i]/=complete.size();}
            Arrays.sort(boots);result.put("pairBootstrap95",List.of(boots[249],boots[9749]));
        }
        return result;
    }
    public static void main(String[] args) throws Exception {
        if(args.length==3 && args[0].equals("archive")) { archive(Path.of(args[1]),Path.of(args[2]));return; }
        if(args.length==6 && args[0].equals("mechanism")) { mechanism(Path.of(args[1]),Path.of(args[2]),Path.of(args[3]),Path.of(args[4]),Path.of(args[5]));return; }
        if(args.length>=5 && args[0].equals("qsearch")) { qsearch(Path.of(args[1]),Path.of(args[2]),Path.of(args[3]),Integer.parseInt(args[4]),args.length>5?Integer.parseInt(args[5]):0,args.length>6?Long.parseLong(args[6]):5000);return; }
        if(args.length==4 && args[0].equals("continue")) { continuation(Path.of(args[1]),Path.of(args[2]),Path.of(args[3]));return; }
        if(args.length==4 && args[0].equals("threads")) { threads(Path.of(args[1]),Path.of(args[2]),Path.of(args[3]));return; }
        if(args.length<4)throw new IllegalArgumentException("diagnose MODEL OUT DATA [ROOTS=32] | match MODEL OUT PAIRS DEPTH SCALE OPENING_SEED FIRST_INDEX [CAP=512] [WORKERS=0] | qsearch MODEL DATA OUT COUNT [FIRST=0] [MILLIS=5000] | threads MODEL DATA OUT | continue STATE DATA OUT | mechanism MODEL STATE DATA MATCH OUT | archive STORE OUT");
        Path path=Path.of(args[1]),out=Path.of(args[2]);if(Files.exists(out))throw new IOException("Use a new output directory");
        var model=model(path);var report=new LinkedHashMap<String,Object>();
        report.put("model",path.toString());report.put("modelSha256",BrnResearchComparison.digest(path));
        report.put("arguments",List.of(args));report.put("purpose","Learning-strength measurement; programme canon determines exploratory or confirmatory use");
        Files.createDirectories(out);
        if(args[0].equals("diagnose")) {
            var data=BrnResearchData.read(Path.of(args[3]),false);
            report.put("dataManifest",DataFiles.read(Path.of(args[3]).resolve("manifest.json"),Map.class));
            report.put("diagnostics",diagnose(model,data,args.length>4?Integer.parseInt(args[4]):32));
            DataFiles.write(out.resolve("diagnostics.json"),report);System.out.println(DataFiles.JSON.toJson(report));return;
        }
        if(!args[0].equals("match")||args.length<8)throw new IllegalArgumentException("Invalid command");
        int pairs=Integer.parseInt(args[3]),depth=Integer.parseInt(args[4]),first=Integer.parseInt(args[7]),cap=args.length>8?Integer.parseInt(args[8]):512;
        double scale=Double.parseDouble(args[5]);long seed=Long.parseLong(args[6]);int workers=args.length>9?Integer.parseInt(args[9]):0;
        if(pairs<1||pairs>512||depth<1||depth>8||first<0||cap<1||workers<0||workers>12||!Double.isFinite(scale))throw new IllegalArgumentException("Budget");
        report.put("depth",depth);report.put("scale",scale);report.put("openingSeed",seed);
        report.put("openingPolicy","Legal-uniform6..10, active and abs(material)<.5, paired colors, no evaluation filtering");
        report.put("threads",workers==0?1:workers);report.put("ttMiB",workers==0?4:192);report.put("guard","10s and10million nodes per move; cap is not draw");
        report.put("evaluatorMode",workers==0?"Explicit reference evaluator":"ProductionSearch and SearchEvaluation; production calibration for quarter residual");
        report.put("productionCalibrationId",Brn3SearchCalibration.ID);
        var config=new ValidationConfig(pairs,seed,6,10,depth,1,NnueScoreMapping.V1,cap);
        var root=Board.startingPosition();var history=GameHistory.initial(root);var results=new ArrayList<Pair>();var identities=new HashSet<String>();
        for(int index=first;results.size()<pairs;index++) {
            if(index-first>=pairs*100)throw new IOException("Opening admission budget exhausted");
            var opening=ValidationArena.opening(root,history,config,index);
            if(!opening.newGame(cap).active()||Math.abs(Brn3Features.material(opening.board()))>=.5||!identities.add(opening.identity()))continue;
            Game white,black;
            if(results.size()%2==0){white=play(opening,model,scale,0,depth,cap,workers);black=play(opening,model,scale,1,depth,cap,workers);}
            else{black=play(opening,model,scale,1,depth,cap,workers);white=play(opening,model,scale,0,depth,cap,workers);}
            results.add(new Pair(index,opening.identity(),Fen.fromBoard(opening.board()),white,black));
            report.put("pairs",results);report.put("summary",summary(results));DataFiles.write(out.resolve("match.json"),report);
            System.out.println("PAIR "+results.size()+" "+DataFiles.JSON.toJson(report.get("summary")));
        }
    }
    static void threads(Path modelPath,Path dataPath,Path out) throws Exception {
        if(Files.exists(out))throw new IOException("New thread-check output required");
        var model=model(modelPath);var data=BrnResearchData.read(dataPath,false);var results=new ArrayList<Object>();
        Files.createDirectories(out);int mismatches=0;
        for(double scale:new double[]{0,1,.25}) {
            var definition=com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.brn3Research(model,scale);
            try(var one=new SearchDriver(ProductionSearch.create(1,definition));var twelve=new SearchDriver(ProductionSearch.create(12,definition))) {
                for(int i=0;i<32;i++) {
                    var b=data.validation().get(i*data.validation().size()/32).board();var h=GameHistory.initial(b);
                    one.newGame();twelve.newGame();var a=one.search(new SearchRequest(b,h,4)).lastCompletedResult();var z=twelve.search(new SearchRequest(b,h,4)).lastCompletedResult();
                    if(a.score()!=z.score())mismatches++;
                    results.add(Map.of("index",i,"scale",scale,"score1",a.score(),"score12",z.score(),"move1",Long.toUnsignedString(a.bestMove(),16),"move12",Long.toUnsignedString(z.bestMove(),16)));
                }
            }
        }
        DataFiles.write(out.resolve("threads.json"),Map.of("modelSha256",BrnResearchComparison.digest(modelPath),"scoreMismatches",mismatches,"results",results,
                "note","Production1vs12workers, default192MiB TT,depth4, fresh game per root; ties may choose different moves"));
        if(mismatches!=0)throw new IllegalStateException("Thread score discrepancy requires diagnosis");
        System.out.println("Thread parity "+results.size()+" roots");
    }
    static void continuation(Path state,Path dataset,Path out) throws Exception {
        if(Files.exists(out))throw new IOException("New continuation output required");
        Brn3Trainer trainer;try(var in=new BufferedInputStream(Files.newInputStream(state))){trainer=Brn3Codec.readTraining(in);}
        var data=BrnResearchData.read(dataset,false);int count=131072;
        if(data.training().size()!=2*count)throw new IOException("Expected two disjoint131072-position slices");
        var report=new LinkedHashMap<String,Object>();report.put("initialTrainingSha256",BrnResearchComparison.digest(state));report.put("initialStep",trainer.step());
        report.put("dataset",DataFiles.read(dataset.resolve("manifest.json"),Map.class));report.put("epochsPerSlice",8);report.put("batch",128);
        report.put("shuffle","SplittableRandom seed791103+100*generation+epoch, independent Fisher-Yates each epoch");
        report.put("optimizer",trainer.config().toString());report.put("deploymentResidualScale",.25);
        var events=new ArrayList<Object>();report.put("generations",events);Files.createDirectories(out);
        for(int generation=1;generation<=2;generation++) {
            long start=System.nanoTime();var samples=data.training().subList((generation-1)*count,generation*count);long stepBefore=trainer.step();
            for(int epoch=0;epoch<8;epoch++) {
                int[] order=BrnResearchMain.order(count,791103L+100*generation+epoch);
                for(int offset=0;offset<count;offset+=128) {
                    long[][] boards=new long[128][];double[] targets=new double[128];
                    for(int i=0;i<128;i++){var e=samples.get(order[offset+i]);boards[i]=e.board();targets[i]=e.outcome();}
                    trainer.trainBatch(boards,targets,128);
                }
                System.out.println("CONTINUATION "+generation+" EPOCH "+(epoch+1)+" STEP "+trainer.step());
            }
            Path dest=out.resolve("g"+generation);Files.createDirectories(dest);
            try(var stream=new BufferedOutputStream(Files.newOutputStream(dest.resolve("network.brn3")))){Brn3Codec.writeModel(trainer.snapshot(),stream);}
            try(var stream=new BufferedOutputStream(Files.newOutputStream(dest.resolve("training.state")))){Brn3Codec.writeTraining(trainer,stream);}
            var model=trainer.snapshot();var full=evaluator(model,1);var quarter=evaluator(model,.25);
            var row=new LinkedHashMap<String,Object>();row.put("generation",generation);row.put("stepBefore",stepBefore);row.put("stepAfter",trainer.step());row.put("exposure",8L*count);
            row.put("seconds",(System.nanoTime()-start)/1e9);row.put("modelSha256",BrnResearchComparison.digest(dest.resolve("network.brn3")));row.put("trainingSha256",BrnResearchComparison.digest(dest.resolve("training.state")));
            row.put("rawValidation",BrnResearchMain.metrics(data.validation(),e->full.applyAsDouble(e.board()),true));
            row.put("quarterValidation",BrnResearchMain.metrics(data.validation(),e->quarter.applyAsDouble(e.board()),true));events.add(row);
            DataFiles.write(out.resolve("continuation.json"),report);
            // Explicit codec resume between the two prospective endpoints, preserving all moments.
            try(var in=new BufferedInputStream(Files.newInputStream(dest.resolve("training.state")))){trainer=Brn3Codec.readTraining(in);}
        }
    }
    static void qsearch(Path modelPath,Path dataPath,Path out,int count,int first,long millis) throws Exception {
        if(Files.exists(out)||count<1||count>128||first<0||millis<1||millis>60000)throw new IOException("New output, 1..128 roots, nonnegative first and 1..60000 ms required");
        var model=model(modelPath);var data=BrnResearchData.read(dataPath,false);var rows=new ArrayList<Object>();
        if(first>data.validation().size()-count)throw new IllegalArgumentException("Root range exceeds validation population");
        var report=new LinkedHashMap<String,Object>();report.put("modelSha256",BrnResearchComparison.digest(modelPath));
        report.put("guard","Separate existing opt-in TT-free unpruned qsearch, depth4,2million nodes/"+millis+"ms per root; production unchanged");
        report.put("data",dataPath.toString());report.put("results",rows);Files.createDirectories(out);
        for(int i=first;i<first+count;i++) {
            var b=data.validation().get(i).board();var row=new LinkedHashMap<String,Object>();row.put("index",i);row.put("fen",Fen.fromBoard(b));
            for(double scale:new double[]{0,1,.25}) {
                var fn=evaluator(model,scale);var search=com.ohinteractive.seedv6.search.exact.ExactSearch.quiescenceResearch((board,ply)->Brn3Model.score(fn.applyAsDouble(board)));
                long start=System.nanoTime();var r=search.search(b,GameHistory.initial(b),4,()->search.visitedNodes()>=2_000_000||System.nanoTime()-start>=millis*1_000_000L);
                row.put("scale"+scale,Map.of("completed",r.completed(),"score",r.score(),"nodes",r.nodes(),"qnodes",r.qnodes(),"qply",r.maximumQply(),"seconds",(System.nanoTime()-start)/1e9));
            }
            rows.add(row);DataFiles.write(out.resolve("qsearch.json"),report);System.out.println("QROOT "+i);
        }
    }
    static void mechanism(Path modelPath,Path trainingPath,Path dataPath,Path matchPath,Path out) throws Exception {
        if(Files.exists(out))throw new IOException("Use a new output directory");
        var model=model(modelPath);Brn3Trainer trainer;
        try(var in=new BufferedInputStream(Files.newInputStream(trainingPath))){trainer=Brn3Codec.readTraining(in);}
        var evaluate=evaluator(model,1);var data=BrnResearchData.read(dataPath,false);
        var report=new LinkedHashMap<String,Object>();
        report.put("modelSha256",BrnResearchComparison.digest(modelPath));report.put("trainingSha256",BrnResearchComparison.digest(trainingPath));
        report.put("matchSha256",BrnResearchComparison.digest(matchPath));report.put("optimizerStep",trainer.step());
        report.put("populationNote","Fixed old reference population; may overlap historical training, not held-out evidence");
        var material=new ArrayList<Double>();var values=new ArrayList<Double>();var targets=new ArrayList<Double>();
        var captureGains=new ArrayList<Double>();var fullResponse=new ArrayList<Double>();var quarterResponse=new ArrayList<Double>();
        double ceFull=0,ceQuarter=0,mseFull=0,mseQuarter=0;
        for(int i=0;i<data.validation().size();i++) {
            var e=data.validation().get(i);var b=e.board();double m=Brn3Features.material(b),v=evaluate.applyAsDouble(b),q=m+.25*(v-m);
            material.add(m);values.add(v);targets.add(e.cp()/100.);
            ceFull+=Brn3Objective.crossEntropy(v,e.sfMaterial(),e.outcome())[0];ceQuarter+=Brn3Objective.crossEntropy(q,e.sfMaterial(),e.outcome())[0];
            mseFull+=.5*Math.pow(Brn3Objective.outcome(v,b)-e.outcome(),2);mseQuarter+=.5*Math.pow(Brn3Objective.outcome(q,b)-e.outcome(),2);
            if(i<256)for(long move:new HeadlessGame(b,1).legalMoves()) {
                var c=child(b,move);double cm=-Brn3Features.material(c),cv=-evaluate.applyAsDouble(c);
                if(cm-m>.5) {captureGains.add(cm-m);fullResponse.add(cv-v);quarterResponse.add(.75*(cm-m)+.25*(cv-v));}
            }
        }
        report.put("referenceMaterial",distribution(material));report.put("referencePrediction",distribution(values));report.put("referenceTarget",distribution(targets));
        report.put("predictionVsMaterialSlope",slope(material,values));report.put("targetVsMaterialSlope",slope(material,targets));
        int n=material.size();report.put("referenceLoss",Map.of("fullCE",ceFull/n,"quarterCE",ceQuarter/n,"fullMse",mseFull/n,"quarterMse",mseQuarter/n));
        report.put("captureMaterialGain",distribution(captureGains));report.put("captureFullResponse",distribution(fullResponse));report.put("captureQuarterResponse",distribution(quarterResponse));
        var match=DataFiles.read(matchPath,com.google.gson.JsonObject.class);
        var gameMaterial=new ArrayList<Double>();var gameValues=new ArrayList<Double>();
        double maxParityError=0;int parityCount=0,materialDisagreementsFull=0,materialDisagreementsQuarter=0,largeImbalance=0;
        var roots=new ArrayList<Object>();var starting=Board.startingPosition();
        var config=new ValidationConfig(1,match.get("openingSeed").getAsLong(),6,10,4,1,NnueScoreMapping.V1,1024);
        for(var pe:match.getAsJsonArray("pairs")) {
            var p=pe.getAsJsonObject();
            for(String color:List.of("white","black")) {
                var g=p.getAsJsonObject(color);var opening=ValidationArena.opening(starting,GameHistory.initial(starting),config,p.get("index").getAsInt());
                var game=opening.newGame(1024);int ordinal=0;boolean rootTaken=false;
                for(var ply:g.getAsJsonArray("plies")) {
                    var trace=ply.getAsJsonObject();var b=game.boardSnapshot();
                    if(!Fen.fromBoard(b).equals(trace.get("fen").getAsString()))throw new IOException("Trace replay mismatch");
                    double m=Brn3Features.material(b),v=evaluate.applyAsDouble(b),q=m+.25*(v-m),oracle=trainer.predictPawns(b);
                    maxParityError=Math.max(maxParityError,Math.abs(v-oracle));parityCount++;
                    if(ordinal<80) {
                        gameMaterial.add(m);gameValues.add(v);
                        if(Math.abs(m)>=5) {largeImbalance++;if(m*v<0)materialDisagreementsFull++;if(m*q<0)materialDisagreementsQuarter++;}
                    }
                    if(!rootTaken&&roots.size()<12&&ordinal<60&&trace.get("actor").getAsInt()==0&&m<=-3) {
                        rootTaken=true;var row=new LinkedHashMap<String,Object>();row.put("openingIndex",p.get("index").getAsInt());row.put("color",color);row.put("ply",ordinal);
                        row.put("fen",Fen.fromBoard(b));row.put("material",m);row.put("fullStatic",v);row.put("quarterStatic",q);
                        for(double scale:new double[]{0,1,.25})try(var search=driver(evaluator(model,scale))) {
                            var outcome=search.search(new SearchRequest(b,game.historySnapshot(),4));var r=outcome.lastCompletedResult();
                            var line=new ArrayList<Object>();var leaf=b;int sign=1;
                            for(long move:r.principalVariation()){leaf=child(leaf,move);sign=-sign;line.add(Map.of("move",Long.toUnsignedString(move,16),"material",sign*Brn3Features.material(leaf),"full",sign*evaluate.applyAsDouble(leaf)));}
                            row.put("scale"+scale,Map.of("score",r.score(),"bestMove",Long.toUnsignedString(r.bestMove(),16),"pv",line));
                        }
                        roots.add(row);
                    }
                    game.play(Long.parseUnsignedLong(trace.get("move").getAsString(),16));ordinal++;
                }
            }
        }
        report.put("gameFirst80Material",distribution(gameMaterial));report.put("gameFirst80Prediction",distribution(gameValues));
        report.put("gameFirst80PredictionVsMaterialSlope",slope(gameMaterial,gameValues));
        report.put("largeMaterialImbalancePositions",largeImbalance);report.put("materialSignDisagreementFull",materialDisagreementsFull);report.put("materialSignDisagreementQuarter",materialDisagreementsQuarter);
        report.put("signNote","Disagreement with material is diagnostic, not an oracle of chess correctness");
        report.put("scalarParityCount",parityCount);report.put("maxScalarParityErrorPawns",maxParityError);report.put("criticalRoots",roots);
        Files.createDirectories(out);DataFiles.write(out.resolve("mechanism.json"),report);
        System.out.println("Mechanism: "+parityCount+" scalar checks, max error="+maxParityError+", roots="+roots.size());
    }
    static double slope(List<Double> x,List<Double> y) {
        double mx=x.stream().mapToDouble(Double::doubleValue).average().orElseThrow(),my=y.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double xx=0,xy=0;for(int i=0;i<x.size();i++){xx+=Math.pow(x.get(i)-mx,2);xy+=(x.get(i)-mx)*(y.get(i)-my);}return xy/xx;
    }
    /** Read immutable payloads and a bounded history snapshot without opening a store writer. */
    static void archive(Path root,Path out) throws Exception {
        if(Files.exists(out))throw new IOException("Use a new archive destination");
        Files.createDirectories(out);var records=new ArrayList<Object>();
        try(var paths=Files.list(root.resolve("checkpoints"))) {
            for(var path:paths.sorted().toList()) {
                var manifest=com.ohinteractive.seedv6.training.checkpoint.CheckpointInspection.manifest(path);
                if(manifest.architecture()!=com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN3)throw new IOException("Not BRN-3");
                var destination=out.resolve("g"+manifest.generation());Files.createDirectories(destination);
                for(String name:List.of("network.brn3","manifest.bin")) {
                    var source=path.resolve(name);String before=BrnResearchComparison.digest(source);
                    if(name.equals("network.brn3")&&!before.equals(manifest.networkSha256()))throw new IOException("Manifest model hash mismatch");
                    Files.copy(source,destination.resolve(name));
                    if(!before.equals(BrnResearchComparison.digest(source))||!before.equals(BrnResearchComparison.digest(destination.resolve(name))))throw new IOException("Unstable source");
                }
                // Only the latest completed anchors need optimizer payloads for later controlled continuation.
                if(Set.of(0L,1L,5L,9L).contains(manifest.generation())) {
                    var source=path.resolve("training.state");String before=BrnResearchComparison.digest(source);
                    if(!before.equals(manifest.trainingSha256()))throw new IOException("Manifest training hash mismatch");
                    Files.copy(source,destination.resolve("training.state"));
                    if(!before.equals(BrnResearchComparison.digest(source))||!before.equals(BrnResearchComparison.digest(destination.resolve("training.state"))))throw new IOException("Unstable training source");
                }
                records.add(manifest);
            }
        }
        var historyFile=root.resolve(com.ohinteractive.seedv6.training.history.HistoryRepository.FILE);
        String before=BrnResearchComparison.digest(historyFile);
        var history=new com.ohinteractive.seedv6.training.history.HistoryRepository(root).refresh();
        Files.copy(historyFile,out.resolve("generations-v1.tsv"));
        if(!before.equals(BrnResearchComparison.digest(historyFile))||!before.equals(BrnResearchComparison.digest(out.resolve("generations-v1.tsv"))))throw new IOException("History changed during capture");
        DataFiles.write(out.resolve("provenance.json"),Map.of("source",root.toString(),"checkpoints",records,
                "historyRecords",history.records().stream().map(Object::toString).toList(),"historyWarnings",history.warnings(),"historySha256",before,
                "scope","Read-only immutable Windows checkpoint capture; no store writer, references, locks or Mac state accessed"));
        System.out.println("Archived "+records.size()+" checkpoint models and "+history.records().size()+" history rows");
    }
    private BrnLearningProbe(){}
}
