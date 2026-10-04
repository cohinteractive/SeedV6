package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.nnue.NnueNetworkCodec;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.validation.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

/** Explicit, bounded paired games. No Best-store promotion and no administrative draws. */
public final class BrnSuccessorMatch {
    record Game(String termination,Double score,String failure,int plies,long candidateNodes,long opponentNodes,
                double candidateSeconds,double opponentSeconds,String finalFen,List<String> moves) {}
    record Pair(int index,String identity,String fen,Game white,Game black) {
        boolean complete(){return white.score()!=null&&black.score()!=null;}
        double score(){return (white.score()+black.score())/2;}
    }
    static Supplier<SearchDriver> factory(Path path,double gain)throws Exception {
        if(path.toString().endsWith(".nnue")) {
            var network=NnueNetworkCodec.read(new BufferedInputStream(new ByteArrayInputStream(Files.readAllBytes(path))));
            return ()->new SearchDriver(new ExactSearchAdapter(SearchEvaluation.incremental(network),new TTable(4)));
        }
        if(path.toString().endsWith(".brn3")) {
            var model=BrnLearningProbe.model(path);
            return ()->new SearchDriver(new ExactSearchAdapter(SearchEvaluation.brn3Research(model,gain),new TTable(4)));
        }
        var model=BrnSuccessorCandidate.decode(Files.readAllBytes(path));
        if(model.pool==BrnSuccessorCandidate.Pool.SUM) {
            var compatible=new BrnSuccessorInference(model).compatibleModel();
            return ()->new SearchDriver(new ExactSearchAdapter(SearchEvaluation.brn3Research(compatible,gain),new TTable(4)));
        }
        return ()->{var work=new BrnSuccessorInference(model);return new SearchDriver(new ExactSearchAdapter((b,ply)->Brn3Model.score(work.evaluatePawns(b,gain)),new TTable(4)));};
    }
    static Game play(ValidationArena.Opening opening,Supplier<SearchDriver> candidate,Supplier<SearchDriver> opponent,
                     int color,int depth,int millis,long deadline) {
        var game=opening.newGame(2048);var moves=new ArrayList<String>();long[] nodes=new long[2],times=new long[2];String failure=null;
        try(var first=candidate.get();var second=opponent.get()) {
            for(var driver:List.of(first,second))driver.search(new SearchRequest(opening.board(),opening.history(),1));
            while(game.active()) {
                if(System.nanoTime()>=deadline){failure="Batch wall-clock budget";game.abort(GameTermination.CANCELLED,failure);break;}
                int actor=game.sideToMove()==color?0:1;long start=System.nanoTime();
                var control=SearchControl.controlled(depth==0?-1:2_000_000,start,depth==0?millis*1_000_000L:5_000_000_000L,TimeSource.SYSTEM);
                var outcome=(actor==0?first:second).search(new SearchRequest(game.boardSnapshot(),game.historySnapshot(),depth==0?64:depth,SearchObserver.NONE,control,false));
                times[actor]+=System.nanoTime()-start;nodes[actor]+=control.nodes();var result=outcome.lastCompletedResult();
                if(result==null||!result.completed()||!result.hasMove()||depth!=0&&!outcome.targetDepthCompleted()) {
                    failure="No completed required search";game.abort(GameTermination.SEARCH_FAILURE,failure);break;
                }
                moves.add(Long.toUnsignedString(result.bestMove(),16));game.play(result.bestMove());
            }
        }catch(RuntimeException e){failure=e.toString();game.abort(GameTermination.INFRASTRUCTURE_FAILURE,failure);}
        Double score=failure==null&&game.termination().completed()?(game.termination().result().orElseThrow().target(color)+1)/2:null;
        return new Game(game.termination().name(),score,failure,game.playedPlies(),nodes[0],nodes[1],times[0]/1e9,times[1]/1e9,Fen.fromBoard(game.boardSnapshot()),moves);
    }
    static Map<String,Object> summary(List<Pair> pairs) {
        var complete=pairs.stream().filter(Pair::complete).toList();var out=new LinkedHashMap<String,Object>();
        out.put("completePairs",complete.size());out.put("incompletePairs",pairs.size()-complete.size());
        var terminations=new TreeMap<String,Integer>();int w=0,d=0,l=0;
        for(var pair:pairs)for(var g:List.of(pair.white(),pair.black())) {
            terminations.merge(g.termination(),1,Integer::sum);
            if(pair.complete()){if(g.score()==1)w++;else if(g.score()==0)l++;else d++;}
        }
        out.put("wins",w);out.put("draws",d);out.put("losses",l);out.put("terminations",terminations);
        if(!complete.isEmpty()) {
            double mean=complete.stream().mapToDouble(Pair::score).average().orElseThrow();out.put("pointScore",mean);
            double[] boots=new double[10000];var rng=new SplittableRandom(728140);
            for(int n=0;n<boots.length;n++){for(int i=0;i<complete.size();i++)boots[n]+=complete.get(rng.nextInt(complete.size())).score();boots[n]/=complete.size();}
            Arrays.sort(boots);out.put("pairedBootstrap95",List.of(boots[249],boots[9749]));out.put("oneSidedBootstrap95Lower",boots[499]);
            out.put("hoeffdingOneSided95Lower",mean-Math.sqrt(Math.log(20)/(2*complete.size())));
        }
        out.put("scope","Completed pairs only; incomplete games are not draws; exploratory unless canon predeclares confirmation");return out;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=10)throw new IllegalArgumentException("CANDIDATE OPPONENT NEW_OUT PAIRS DEPTH MILLIS FIRST OPENING_SEED GAIN OPPONENT_GAIN");
        Path candidatePath=Path.of(args[0]),opponentPath=Path.of(args[1]),out=Path.of(args[2]);if(Files.exists(out))throw new IOException("Use new output");
        int pairs=Integer.parseInt(args[3]),depth=Integer.parseInt(args[4]),millis=Integer.parseInt(args[5]),first=Integer.parseInt(args[6]);long seed=Long.parseLong(args[7]);
        double gain=Double.parseDouble(args[8]),otherGain=Double.parseDouble(args[9]);
        if(pairs<1||pairs>128||depth<0||depth>6||millis<1||millis>100||first<0||!Double.isFinite(gain)||!Double.isFinite(otherGain)||gain<0||gain>1||otherGain<0||otherGain>1)throw new IllegalArgumentException("Experiment bounds");
        var candidate=factory(candidatePath,gain);var opponent=factory(opponentPath,otherGain);
        var root=Board.startingPosition();var history=GameHistory.initial(root);var config=new ValidationConfig(pairs,seed,6,10,Math.max(1,depth),1,NnueScoreMapping.V1,2048);
        var results=new ArrayList<Pair>();var identities=new HashSet<String>();var report=new LinkedHashMap<String,Object>();Files.createDirectories(out);
        report.put("arguments",List.of(args));report.put("candidateSha256",BrnResearchComparison.digest(candidatePath));report.put("opponentSha256",BrnResearchComparison.digest(opponentPath));
        report.put("openingPolicy","legal-uniform 6..10 plies; distinct active balanced-material openings; fixed seed/index; same opening with colors reversed");
        report.put("searchPolicy","production SearchDriver/ExactSearch explicit evaluator,4MiB TT,one worker; equal untimed depth1 warmup;2048 ply cap;240s batch budget");
        long deadline=System.nanoTime()+240_000_000_000L;
        for(int index=first;results.size()<pairs&&index<first+100*pairs;index++) {
            var opening=ValidationArena.opening(root,history,config,index);
            if(!opening.newGame(2048).active()||Math.abs(RelationalCandidate.material(opening.board()))>=.5||!identities.add(opening.identity()))continue;
            Game white,black;
            if(results.size()%2==0){white=play(opening,candidate,opponent,0,depth,millis,deadline);black=play(opening,candidate,opponent,1,depth,millis,deadline);}
            else{black=play(opening,candidate,opponent,1,depth,millis,deadline);white=play(opening,candidate,opponent,0,depth,millis,deadline);}
            results.add(new Pair(index,opening.identity(),Fen.fromBoard(opening.board()),white,black));report.put("pairs",results);report.put("summary",summary(results));
            report.put("nextOpeningIndex",index+1);DataFiles.write(out.resolve("match.json"),report);System.out.println(DataFiles.JSON.toJson(report.get("summary")));
            if(System.nanoTime()>=deadline)break;
        }
    }
    private BrnSuccessorMatch(){}
}
