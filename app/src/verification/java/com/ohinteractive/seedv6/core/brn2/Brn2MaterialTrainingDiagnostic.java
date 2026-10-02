package com.ohinteractive.seedv6.core.brn2;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import static com.ohinteractive.seedv6.core.brn2.Brn2Model.*;

/** Explicit-path, read-only lineage inspection and isolated production-trainer experiments.
 * Developer source set only. Never opens a checkpoint writer or publishes a model. */
public final class Brn2MaterialTrainingDiagnostic {
    static final String[] NAMES = {"equal", "+pawn", "-pawn", "+knight", "+bishop", "+rook", "+queen", "rook-knight", "queen-rook", "two-rooks-queen"};
    static final String[] US = {"P7","P7","8","N7","B7","R7","Q7","R7","Q7","RR6"};
    static final String[] THEM = {"p7","8","p7","8","8","8","8","n7","r7","q7"};
    static final Set<Long> MILESTONES = Set.of(0L,1L,2L,10L,100L,1000L,4096L);
    static Brn2Model g0, g1;
    static Path actualCp1;
    static PartialGeneration partial;
    static List<TrajectorySampler.Sample> samples;
    static void out(Object... x) { System.out.println(String.join("\t", Arrays.stream(x).map(String::valueOf).toList())); }
    static long[] position(int i) { return Board.fromFen("7k/"+THEM[i]+"/8/8/8/8/"+US[i]+"/7K w - - 0 1"); }
    static double rawPrior(long[] b) { return Brn2MaterialPrior.BASIC_V1.combine(b,0); }
    static void eval(String label, String name, Brn2Model m, long[] b) {
        var w = new Brn2Workspace(); double y=m.evaluate(b,w), prior=m.materialPrior().combine(b,0);
        out("eval",label,name,Fen.fromBoard(b),Brn2MaterialPrior.BASIC_V1.score(b),prior,w.raw()-prior,w.raw(),y,BrnScoreMapping.map(y));
    }
    static String stats(double[] a, int lo, int hi) {
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY,sum=0,sq=0; int nonzero=0;
        for(int i=lo;i<hi;i++){ min=Math.min(min,a[i]);max=Math.max(max,a[i]);sum+=a[i];sq+=a[i]*a[i];if(a[i]!=0)nonzero++; }
        return "min="+min+",max="+max+",mean="+sum/(hi-lo)+",rms="+Math.sqrt(sq/(hi-lo))+",nonzero="+nonzero;
    }
    static void modelStats(String label, Brn2Model m) {
        double[] a=m.copyWeights();out("model",label,m.materialPrior(),stats(a,OUTPUT_WEIGHT_OFFSET,OUTPUT_BIAS),"bias="+a[OUTPUT_BIAS],"upstream="+stats(a,0,OUTPUT_WEIGHT_OFFSET));
    }
    static Brn2Trainer loadTrainer(Path cp) throws IOException { try(var in=new BufferedInputStream(Files.newInputStream(cp.resolve("training.state")))){return Brn2Codec.readTraining(in);} }
    static Brn2Model loadModel(Path cp) throws IOException { try(var in=new BufferedInputStream(Files.newInputStream(cp.resolve("network.brn2")))){return Brn2Codec.readModel(in);} }
    static void inspect(Path root) throws Exception {
        String best=CheckpointInspection.reference(root,"best"),latest=CheckpointInspection.reference(root,"latest-training");
        out("refs",best,latest);
        Path cp0=root.resolve("checkpoints").resolve(best),cp1=root.resolve("checkpoints").resolve(latest);
        actualCp1=cp1;
        out("manifest0",CheckpointInspection.manifest(cp0));out("manifest1",CheckpointInspection.manifest(cp1));
        g0=loadModel(cp0);g1=loadModel(cp1);
        for(var entry:List.of(cp0,cp1)) {
            var trainer=loadTrainer(entry);var model=entry.equals(cp0)?g0:g1;
            out("persistence",entry.getFileName(),trainer.materialPrior(),trainer.optimizer().step(),trainer.config(),"model_equal="+Arrays.equals(trainer.snapshot().copyWeights(),model.copyWeights()));
            moments(entry.equals(cp0)?"actual-g0":"actual-g1",trainer);
        }
        out("source",CheckpointStore.readTrainingSource(root));out("supervision",CheckpointStore.readBrnSupervision(root));out("capture",CheckpointStore.readBrnCaptureConsistency(root));
        partial=PartialGeneration.inspect(root).orElseThrow();samples=partial.games().samples();
        out("attempt",partial.attempt());out("batch",partial.statistics());out("training",partial.training());
        var terminations=new TreeMap<String,Integer>();
        for(var pair:partial.pairs()) for(var game:List.of(pair.candidateWhite(),pair.candidateBlack())) terminations.merge(game.termination().name(),1,Integer::sum);
        out("pairs",terminations);
        modelStats("actual-g0",g0);modelStats("actual-g1",g1);
        for(int i=0;i<NAMES.length;i++)for(int p=0;p<2;p++){
            long[] b=position(i);b[Board.STATUS]=(b[Board.STATUS]&~Board.PLAYER_BIT)|p;
            eval("actual-g0",NAMES[i],g0,b);eval("actual-g1",NAMES[i],g1,b);
        }
        eval("actual-g0","start",g0,Board.startingPosition());eval("actual-g1","start",g1,Board.startingPosition());
        int[] counts=new int[3];double[] residual=new double[samples.size()];
        for(int i=0;i<samples.size();i++){
            var s=samples.get(i);counts[(int)s.target()+1]++;var w=new Brn2Workspace();g1.evaluate(s.board(),w);residual[i]=w.raw()-rawPrior(s.board());
            if(i<6 || i%512==0)out("sample",i,Fen.fromBoard(s.board()),"wdl="+s.target(),"public_equivalent="+s.target()*32511,"material_cp="+Brn2MaterialPrior.BASIC_V1.score(s.board()),"prior_raw="+rawPrior(s.board()));
        }
        out("sample_counts",Arrays.toString(counts),"retained="+samples.size());
    }
    static void generate(Path file,int games,int workers) throws Exception {
        var config=new SelfPlayConfig(games,4,workers,7921502845091313457L,0,8,32,1024,NnueScoreMapping.V1,-1,-1);
        long start=System.nanoTime();
        var batch=SelfPlayBatch.generateHandcrafted(config,Board.startingPosition(),new SelfPlayControl(),p->out("generated",p.lastGame()));
        out("generated_stats",batch.statistics(),"seconds="+(System.nanoTime()-start)/1e9);
        try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))){
            out.writeInt(batch.samples().size());for(var s:batch.samples()){for(long x:s.board())out.writeLong(x);out.writeDouble(s.target());}
        }
    }
    static void readSamples(Path file) throws Exception {
        var list=new ArrayList<TrajectorySampler.Sample>();
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))){
            for(int n=in.readInt();n>0;n--){long[] b=new long[6];for(int i=0;i<6;i++)b[i]=in.readLong();list.add(new TrajectorySampler.Sample(b,in.readDouble()));}
        }
        samples=List.copyOf(list);
    }
    static int[] order(int n,long seed) {
        int[] order=new int[n];for(int i=0;i<n;i++)order[i]=i;
        var random=new SplittableRandom(seed);for(int i=n-1;i>0;i--){int j=random.nextInt(i+1),t=order[i];order[i]=order[j];order[j]=t;}
        return order;
    }
    static void moments(String label,Brn2Trainer t) {
        double[] first=new double[33],second=new double[33];
        for(int i=0;i<33;i++){first[i]=t.optimizer().firstMoment(OUTPUT_WEIGHT_OFFSET+i);second[i]=t.optimizer().secondMoment(OUTPUT_WEIGHT_OFFSET+i);}
        out("moments",label,t.optimizer().step(),"first="+stats(first,0,32),"second="+stats(second,0,32),"bias_m="+first[32],"bias_v="+second[32]);
    }
    static void progress(String label,Brn2Trainer t,int[] order,double targetScale) throws Exception {
        out("progress_columns","run,step,index,fen,target,prediction,loss,material_cp,material_raw,residual_raw,total_raw,output,public_score");
        for(int step=0;step<order.length;step++){
            int index=order[step];var sample=samples.get(index);long[] b=sample.board();double target=sample.target()*targetScale;
            if(MILESTONES.contains((long)step)){
                var w=new Brn2Workspace();double y=t.snapshot().evaluate(b,w),prior=t.materialPrior().combine(b,0);
                out("progress",label,step,index,Fen.fromBoard(b),target,y,.5*(y-target)*(y-target),Brn2MaterialPrior.BASIC_V1.score(b),prior,w.raw()-prior,w.raw(),y,BrnScoreMapping.map(y));
                modelStats(label+"@"+step,t.snapshot());moments(label,t);
                eval(label+"@"+step,"start",t.snapshot(),Board.startingPosition());eval(label+"@"+step,"+rook",t.snapshot(),position(5));
            }
            double y=t.predict(b),d=(y-target)*(1-y*y);var w=new Brn2Workspace();w.evaluate(b,t.weights,t.materialPrior(),false);
            if(MILESTONES.contains((long)step)){
                double[] grad=new double[32];double sum=0;for(int h=0;h<32;h++){sum+=Math.max(0,w.boardPre[h]);grad[h]=d*Math.max(0,w.boardPre[h]);}
                out("grad",label,step,"dL_dz="+d,"hidden_sum="+sum,"head="+stats(grad,0,32),"first_step_expected_delta="+(-t.config().learningRate()*Math.signum(d)*(sum+1)));
            }
            t.train(b,target);
            if(step<2){
                var gf=Brn2Trainer.class.getDeclaredField("gradients");gf.setAccessible(true);double[] grad=(double[])gf.get(t);
                out("sparse_grad",label,step+1,stats(grad,0,grad.length));
                var wf=new Brn2Workspace();double after=wf.evaluate(b,t.weights,t.materialPrior(),false);out("post_update",label,step+1,index,after,wf.raw()-t.materialPrior().combine(b,0),BrnScoreMapping.map(after));
            }
        }
        modelStats(label+"@final",t.snapshot());moments(label,t);
        var last=samples.get(order[order.length-1]);var lastWorkspace=new Brn2Workspace();double finalValue=lastWorkspace.evaluate(last.board(),t.weights,t.materialPrior(),false),lastTarget=last.target()*targetScale,prior=t.materialPrior().combine(last.board(),0);
        out("progress",label,order.length,order[order.length-1],Fen.fromBoard(last.board()),lastTarget,finalValue,.5*(finalValue-lastTarget)*(finalValue-lastTarget),Brn2MaterialPrior.BASIC_V1.score(last.board()),prior,lastWorkspace.raw()-prior,lastWorkspace.raw(),finalValue,BrnScoreMapping.map(finalValue));
        eval(label+"@final","start",t.snapshot(),Board.startingPosition());
        for(int i=0;i<NAMES.length;i++)eval(label+"@final",NAMES[i],t.snapshot(),position(i));
    }
    static Brn2Model scaledResidual(Brn2Model m,double scale) {
        double[] a=m.copyWeights();for(int i=OUTPUT_WEIGHT_OFFSET;i<PARAMETER_COUNT;i++)a[i]*=scale;
        return new Brn2Model(a,m.materialPrior());
    }
    static long[] child(long[] b,long move) { long[] c=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],move,c);return c; }
    static void line(String label,String name,Brn2Model m,long[] root,SearchResult result) {
        long[] b=root;StringBuilder moves=new StringBuilder();int plies=0;
        for(long move:result.principalVariation()){b=child(b,move);moves.append(Move.coordinate(move)).append(' ');plies++;}
        var w=new Brn2Workspace();double y=m.evaluate(b,w),prior=m.materialPrior().combine(b,0),sign=plies%2==0?1:-1;
        out("search",label,name,result.depth(),Move.coordinate(result.bestMove()),result.score(),result.nodes(),moves,"leaf="+Fen.fromBoard(b),"material_root="+sign*Brn2MaterialPrior.BASIC_V1.score(b),"prior_root="+sign*prior,"residual_root="+sign*(w.raw()-prior),"static_root="+sign*BrnScoreMapping.map(y));
    }
    static void searchCase(String name,long[] b,Brn2Model m,String label,int depth) {
        try(var driver=new SearchDriver(SearchEvaluation.brn2(m))){var result=driver.search(new SearchRequest(b,depth)).lastCompletedResult();line(label,name,m,b,result);}
    }
    static void choices() {
        String[] fens={"6k1/3r4/8/8/3Q4/8/8/6K1 w - - 0 1","6k1/6b1/8/8/3R4/8/8/6K1 w - - 0 1","6k1/6b1/8/8/3N4/8/8/6K1 w - - 0 1","6k1/6b1/8/8/3B4/8/8/6K1 w - - 0 1"};
        for(int i=0;i<fens.length;i++)for(int d: new int[]{2,4}){
            long[] b=Board.fromFen(fens[i]);searchCase("hanging-"+i,b,g0,"actual-g0",d);searchCase("hanging-"+i,b,g1,"actual-g1",d);searchCase("hanging-"+i,b,scaledResidual(g1,.01),"actual-g1-residual-0.01",d);
        }
    }
    static void forced(String name,long[] root,String coordinate,Brn2Model model,String label) {
        long move=Arrays.stream(new HeadlessGame(root,64).legalMoves()).filter(x->Move.coordinate(x).equals(coordinate)).findFirst().orElseThrow();
        long[] c=child(root,move);
        try(var driver=new SearchDriver(SearchEvaluation.brn2(model))){
            var response=driver.search(new SearchRequest(c,1)).lastCompletedResult();long[] b=c;int plies=1;
            for(long reply:response.principalVariation()){b=child(b,reply);plies++;}
            double sign=plies%2==0?1:-1;var w=new Brn2Workspace();double y=model.evaluate(b,w),prior=model.materialPrior().combine(b,0);
            out("forced",label,name,coordinate,"reply="+Arrays.stream(response.principalVariation()).mapToObj(Move::coordinate).toList(),"score="+(-response.score()),"material="+sign*Brn2MaterialPrior.BASIC_V1.score(b),"prior_raw="+sign*prior,"residual_raw="+sign*(w.raw()-prior),"static="+sign*BrnScoreMapping.map(y),"leaf="+Fen.fromBoard(b));
        }
    }
    static void failures() {
        // First root is independently captured before the actual bishop giveaway.
        long[] b=Board.fromFen("r1bqkbnr/pppppppp/n7/8/P6P/3P4/1PP1PPP1/RNBQKBNR w KQk - 1 4");
        for(var entry:Map.of("actual-g0",g0,"actual-g1",g1,"actual-g1-residual-0.01",scaledResidual(g1,.01)).entrySet()){
            for(String move:new String[]{"b1c3","c1h6"})forced("bishop-giveaway",b,move,entry.getValue(),entry.getKey());
            searchCase("bishop-giveaway",b,entry.getValue(),entry.getKey(),2);
            searchCase("bishop-giveaway",b,entry.getValue(),entry.getKey(),4);
        }
        try(var driver=new SearchDriver(ProductionSearch.create(12,SearchEvaluation.brn2(g1)))){
            line("actual-g1-workers12","bishop-giveaway",g1,b,driver.search(new SearchRequest(b,2)).lastCompletedResult());
        }
        long[] safe=Board.fromFen("r1bqkbnr/pppppppp/8/8/Pn5P/2NP4/1PP1PPP1/R1BQKBNR w KQk - 3 5");
        long[] lost=Board.fromFen("r1bqkb1r/pppppppp/n6n/8/P6P/3P4/1PP1PPP1/RN1QKBNR w KQk - 0 5");
        for(int clock:new int[]{0,1,2,3,4,8,16})for(int i=0;i<2;i++){
            long[] c=(i==0?safe:lost).clone();c[4]=(c[4]&~((long)Board.HALF_MOVE_CLOCK_BITS<<Board.HALF_MOVE_CLOCK_SHIFT))|((long)clock<<Board.HALF_MOVE_CLOCK_SHIFT);
            eval("clock-ablation",(i==0?"safe":"bishop-lost")+"@"+clock,g1,c);
        }
        long[] queenRoot=Board.fromFen("1rbq1b1r/pppp1ppp/n2k3n/8/P4p1P/3P4/1PP1Q1P1/1R2KBNR w K - 2 15");
        long[] rookRoot=Board.fromFen("1rbq1b1r/pppp1ppp/n6n/8/P3kp1P/2P4R/1P4P1/1R2K1N1 w - - 0 21");
        for(var e:Map.of("actual-g0",g0,"actual-g1",g1,"actual-g1-residual-0.01",scaledResidual(g1,.01)).entrySet()){
            for(String move:new String[]{"e2d2","e2e5"})forced("queen-giveaway",queenRoot,move,e.getValue(),e.getKey());
            for(String move:new String[]{"h3h1","h3e3"})forced("rook-giveaway",rookRoot,move,e.getValue(),e.getKey());
        }
        double r=1.787165041163943;
        out("saturation", "fixed_residual="+r,"material_zero_score="+32511*StrictMath.tanh(r),"material_minus_bishop_score="+32511*StrictMath.tanh(r+Brn2MaterialPrior.BASIC_V1.combine(lost,0)),"cost_cp="+32511*(StrictMath.tanh(r)-StrictMath.tanh(r+Brn2MaterialPrior.BASIC_V1.combine(lost,0))));
    }
    static long[] reversed(long[] b) {
        long[] c=b.clone();long occupied=Brn2Features.occupied(b);
        for(int i=0;i<3;i++)c[i]=Long.reverseBytes(b[i]);c[3]=Long.reverseBytes(b[3]^occupied);
        int rights=(int)(b[4]>>>1)&15,ep=Board.enPassantSquare((int)b[4]);
        c[4]=(b[4]&~(31L | (63L<<5)))|((b[4]&1)^1)|(((rights&3)<<2 | rights>>>2)<<1);
        if(ep>=0)c[4]|=(long)(ep^56)<<5;
        return Board.fromFen(Fen.fromBoard(c));
    }
    static void boundaries(Path file) throws Exception {
        readSamples(file);double maxRaw=0;int mismatch=0,count=0;var trainer=loadTrainer(actualCp1);
        var definition=SearchEvaluation.brn2(g1);var main=definition.newState(4);var q=definition.newState(4);var full=SearchEvaluation.brn2FullRecompute(g1).newState(4);
        for(int i=0;i<samples.size();i+=31){
            long[] b=samples.get(i).board();var game=new HeadlessGame(b,8);long[] moves=game.legalMoves();
            main.initialize(b,0);full.initialize(b,0);var workspace=new Brn2Workspace();double y=g1.evaluate(b,workspace),z=workspace.raw();int score=BrnScoreMapping.map(y);
            if(main.evaluate(b,0)!=score || full.evaluate(b,0)!=score || trainer.predict(b)!=y)mismatch++;
            maxRaw=Math.max(maxRaw,Math.abs(workspace.raw()-z));
            long[] reverse=reversed(b);var ws=new Brn2Workspace();double other=g1.evaluate(reverse,ws);
            if(other!=y || rawPrior(reverse)!=rawPrior(b))mismatch++;
            if(moves.length>0){long[] c=child(b,moves[0]);main.child(b,c,0);q.initializeFrom(c,1,main);full.initialize(c,1);double cy=g1.evaluateReference(c,ws);
                if(main.evaluate(c,1)!=BrnScoreMapping.map(cy)||q.evaluate(c,1)!=BrnScoreMapping.map(cy)||full.evaluate(c,1)!=BrnScoreMapping.map(cy))mismatch++;
                var accumulator=new Brn2Accumulator(g1);accumulator.rebuild(b);var ca=new Brn2Accumulator(g1);ca.update(b,c,accumulator);double ay=ca.evaluate(c);
                maxRaw=Math.max(maxRaw,Math.abs(ca.raw()-ws.raw()));if(Math.abs(ay-cy)>1e-14)mismatch++;
            }count++;
        }
        out("boundaries",count,"mismatches="+mismatch,"max_raw_delta="+maxRaw,"mode="+trainer.materialPrior());
        if(mismatch!=0 || maxRaw>1e-13)throw new AssertionError("Inference boundary mismatch");
        var resumed=Brn2Codec.decodeTraining(Brn2Codec.encodeTraining(trainer));int[] order=order(samples.size(),-6540313355536843707L);
        for(int i=0;i<10;i++){var s=samples.get(order[i]);trainer.train(s.board(),s.target());resumed.train(s.board(),s.target());}
        out("continuation","identical_bytes="+Arrays.equals(Brn2Codec.encodeTraining(trainer),Brn2Codec.encodeTraining(resumed)),"mode="+resumed.materialPrior(),"step="+resumed.optimizer().step());
        if(!Arrays.equals(Brn2Codec.encodeTraining(trainer),Brn2Codec.encodeTraining(resumed)))throw new AssertionError("Continuation mismatch");
        gradientChecks(order);
    }
    static Object field(Object o,String name) throws Exception {var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static void gradientChecks(int[] order) throws Exception {
        var t=new Brn2Trainer(g0,new BrnAdamConfig(.001));
        for(int step=0;step<2;step++){
            var s=samples.get(order[step]);long[] b=s.board();double[] before=t.weights.clone();t.train(b,s.target());
            var og=(double[])field(t,"outputGradient");var gradient=(double[])field(t,"gradients");var touched=(int[])field(t,"touched");int n=(int)field(t,"touchedCount"),best=0;
            for(int i=1;i<n*32;i++)if(Math.abs(gradient[i])>Math.abs(gradient[best]))best=i;
            int[] indexes={OUTPUT_BIAS,OUTPUT_WEIGHT_OFFSET,touched[best/32]*32+best%32};
            var w=new Brn2Workspace();double y=w.evaluate(b,before,t.materialPrior(),false),d=(y-s.target())*(1-y*y);
            double[] expected={d,og[0],gradient[best]};double epsilon=1e-6;
            for(int j=0;j<indexes.length;j++){
                int index=indexes[j];double value=before[index];before[index]=value+epsilon;double p=w.evaluate(b,before,t.materialPrior(),false);before[index]=value-epsilon;double m=w.evaluate(b,before,t.materialPrior(),false);before[index]=value;
                double numeric=(.5*(p-s.target())*(p-s.target())-.5*(m-s.target())*(m-s.target()))/(2*epsilon);
                out("finite_difference",step+1,index,"analytic="+expected[j],"numeric="+numeric,"error="+Math.abs(expected[j]-numeric));
                if(Math.abs(expected[j]-numeric)>1e-8)throw new AssertionError("Gradient mismatch");
            }
        }
    }
    static void targetAudit(Path file) throws Exception {
        readSamples(file);int[] counts=new int[3];var groups=new HashMap<String,int[]>();
        for(var s:samples){counts[(int)s.target()+1]++;var f=new Brn2Features();f.extract(s.board());int[] ids=new int[f.size()];for(int i=0;i<ids.length;i++)ids[i]=f.indexAt(i);
            groups.computeIfAbsent(Arrays.toString(ids),k->new int[3])[(int)s.target()+1]++;}
        int conflicting=0,conflictedSamples=0;for(var c:groups.values()){int nonzero=0;for(int n:c)if(n>0)nonzero++;if(nonzero>1){conflicting++;conflictedSamples+=Arrays.stream(c).sum();}}
        out("target_audit","counts="+Arrays.toString(counts),"unique_feature_positions="+groups.size(),"conflicting_groups="+conflicting,"conflicted_samples="+conflictedSamples);
        for(int index:new int[]{3507,1386,1223,2418}){
            var s=samples.get(index);var w=new Brn2Workspace();double y=g0.evaluate(s.board(),w);
            out("target_sample",index,Fen.fromBoard(s.board()),"source_terminal_wdl="+s.target(),"target="+s.target(),"target_public_equivalent="+BrnScoreMapping.map(s.target()),"material_cp="+Brn2MaterialPrior.BASIC_V1.score(s.board()),"prior_raw="+w.raw(),"target_residual_raw="+(Math.abs(s.target())==1?(s.target()>0?"+infinity":"-infinity"):String.valueOf(-w.raw())));
        }
        long[] board=position(6);var t=new Brn2Trainer(g0,new BrnAdamConfig(.001));
        for(int i=0;i<=1000;i++){if(MILESTONES.contains((long)i)){var w=new Brn2Workspace();double y=w.evaluate(board,t.weights,t.materialPrior(),false);out("constant_win",i,"target=1","material=900","material_raw="+rawPrior(board),"residual_raw="+(w.raw()-rawPrior(board)),"prediction="+y,"public_score="+BrnScoreMapping.map(y),"loss="+.5*(y-1)*(y-1));}if(i<1000)t.train(board,1);}
        for(double target:new double[]{1,.01,.001}){
            var initial=new Brn2Trainer(g0,new BrnAdamConfig(.001));var opening=Board.startingPosition();
            double[][] firstWeights={null};
            var list=List.of(new TrajectorySampler.Sample(opening,1),new TrajectorySampler.Sample(opening,1));
            Brn2SelfPlayTraining.trainSamples(initial,list,new SelfPlayTraining.Config(1,1,false,0),new SelfPlayControl(),p->{
                var w=new Brn2Workspace();double y=w.evaluate(opening,initial.weights,initial.materialPrior(),false);
                double hiddenSum=0;for(double h:w.boardPre)hiddenSum+=Math.max(0,h);
                out("adam_scale",target,p.optimizerStep(),"raw="+w.raw(),"public="+BrnScoreMapping.map(y),"head="+stats(initial.weights,OUTPUT_WEIGHT_OFFSET,OUTPUT_BIAS),"bias="+initial.weights[OUTPUT_BIAS],"hidden_sum="+hiddenSum);
                if(p.optimizerStep()==1)firstWeights[0]=initial.weights.clone();
                else{
                    double[] headOnly=firstWeights[0].clone(),upstreamOnly=initial.weights.clone();
                    System.arraycopy(initial.weights,OUTPUT_WEIGHT_OFFSET,headOnly,OUTPUT_WEIGHT_OFFSET,33);
                    System.arraycopy(firstWeights[0],OUTPUT_WEIGHT_OFFSET,upstreamOnly,OUTPUT_WEIGHT_OFFSET,33);
                    eval("step2-head-only","target="+target,new Brn2Model(headOnly,Brn2MaterialPrior.BASIC_V1),opening);
                    eval("step2-upstream-only","target="+target,new Brn2Model(upstreamOnly,Brn2MaterialPrior.BASIC_V1),opening);
                }
            },s->target);
        }
    }
    static void fragment(String label,Brn2Model candidate,int depth,int cap) {
        var game=new HeadlessGame(Board.startingPosition(),cap);
        try(var white=new SearchDriver(SearchEvaluation.brn2(candidate));var black=new SearchDriver(SearchEvaluation.brn2(g0))){
            while(game.active()) {
                long[] b=game.boardSnapshot();var m=game.sideToMove()==0?candidate:g0;
                var result=(game.sideToMove()==0?white:black).search(new SearchRequest(b,game.historySnapshot(),depth)).lastCompletedResult();
                line(label,"ply-"+game.playedPlies(),m,b,result);
                game.play(result.bestMove());
                long[] c=game.boardSnapshot();out("fragment_board",label,game.playedPlies(),Fen.fromBoard(c),"white_material="+(game.sideToMove()==0?1:-1)*Brn2MaterialPrior.BASIC_V1.score(c));
            }
            out("fragment_end",label,game.termination(),game.playedPlies(),Fen.fromBoard(game.boardSnapshot()));
        }
    }
    static void ablations(Path file) throws Exception {
        readSamples(file);var config=new SelfPlayTraining.Config(1,1,true,-6540313355536843707L);
        String[] labels={"basic","no-prior-zero-head","basic-legacy-head","legacy-none","basic-lr-1e-5","basic-target-0.01","basic-material-target","basic-exact-initial-target"};
        for(String label:labels){
            Brn2Model m=switch(label){case "no-prior-zero-head"->new Brn2Model(g0.copyWeights(),Brn2MaterialPrior.NONE);case "basic-legacy-head"->new Brn2Model(new Brn2Model(INITIALIZATION_SEED,Brn2MaterialPrior.NONE).copyWeights(),Brn2MaterialPrior.BASIC_V1);case "legacy-none"->new Brn2Model(INITIALIZATION_SEED,Brn2MaterialPrior.NONE);default->g0;};
            var t=new Brn2Trainer(m,new BrnAdamConfig(label.equals("basic-lr-1e-5")?1e-5:.001));
            var targetWorkspace=new Brn2Workspace();
            var metrics=Brn2SelfPlayTraining.trainSamples(t,samples,config,new SelfPlayControl(),p->{},s->label.equals("basic-target-0.01")?s.target()*.01:label.equals("basic-material-target")?Brn2MaterialPrior.BASIC_V1.score(s.board())/32511.0:label.equals("basic-exact-initial-target")?g0.evaluate(s.board(),targetWorkspace):s.target());
            out("ablation",label,metrics);modelStats(label,t.snapshot());
            double[] r=new double[samples.size()];for(int i=0;i<r.length;i++){var w=new Brn2Workspace();w.evaluate(samples.get(i).board(),t.weights,t.materialPrior(),false);r[i]=w.raw()-t.materialPrior().combine(samples.get(i).board(),0);}
            out("ablation_residual",label,stats(r,0,r.length));
            eval(label,"start",t.snapshot(),Board.startingPosition());for(int i=0;i<NAMES.length;i++)eval(label,NAMES[i],t.snapshot(),position(i));
            for(int i: new int[]{0,1})searchCase("ablation-hanging-"+i,Board.fromFen(i==0?"6k1/3r4/8/8/3Q4/8/8/6K1 w - - 0 1":"6k1/6b1/8/8/3R4/8/8/6K1 w - - 0 1"),t.snapshot(),label,2);
            forced("bishop-giveaway",Board.fromFen("r1bqkbnr/pppppppp/n7/8/P6P/3P4/1PP1PPP1/RNBQKBNR w KQk - 1 4"),"c1h6",t.snapshot(),label);
            fragment(label,t.snapshot(),2,64);
        }
    }
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        if(args.length==0)throw new IllegalArgumentException("Usage: <lineage-root> [generate <sample-file> <games> <workers>|progress <sample-file>|ablations <sample-file>|boundaries <sample-file>|targets <4096-sample-file>|choices|failures|fragment <label> <residual-scale> <depth> <plies>]");
        inspect(Path.of(args[0]));
        if(args.length>1 && args[1].equals("generate"))generate(Path.of(args[2]),Integer.parseInt(args[3]),Integer.parseInt(args[4]));
        if(args.length>1 && args[1].equals("progress")){
            readSamples(Path.of(args[2]));int[] order=order(samples.size(),-6540313355536843707L);
            var t=new Brn2Trainer(g0,new BrnAdamConfig(.001));progress("basic",t,order,1);
            out("replay_actual",Arrays.equals(t.weights,g1.copyWeights()));
            var production=new Brn2Trainer(g0,new BrnAdamConfig(.001));
            var metrics=Brn2SelfPlayTraining.trainSamples(production,samples,new SelfPlayTraining.Config(1,1,true,-6540313355536843707L),new SelfPlayControl(),p->{});
            out("production_replay",metrics,"same_weights="+Arrays.equals(t.weights,production.weights),"actual_weights="+Arrays.equals(production.weights,g1.copyWeights()));
            if(!Arrays.equals(t.weights,production.weights))throw new AssertionError("Production trainer replay mismatch");
        }
        if(args.length>1 && args[1].equals("choices"))choices();
        if(args.length>1 && args[1].equals("failures"))failures();
        if(args.length>1 && args[1].equals("boundaries"))boundaries(Path.of(args[2]));
        if(args.length>1 && args[1].equals("targets"))targetAudit(Path.of(args[2]));
        if(args.length>1 && args[1].equals("fragment"))fragment(args[2],scaledResidual(g1,Double.parseDouble(args[3])),Integer.parseInt(args[4]),Integer.parseInt(args[5]));
        if(args.length>1 && args[1].equals("ablations"))ablations(Path.of(args[2]));
    }
}
