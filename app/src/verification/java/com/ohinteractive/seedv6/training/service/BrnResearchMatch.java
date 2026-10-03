package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Isolated equal-time games through the production driver and rule adjudication.
 * Administrative caps/failures are never draws and cannot satisfy acceptance. */
public final class BrnResearchMatch {
    record Game(String termination,String failure,int plies,Double brnScore,String finalFen,List<String> moves,
                long brnNodes,long nnueNodes,double brnSeconds,double nnueSeconds,double brnMeanDepth,double nnueMeanDepth) {}
    record Pair(int openingIndex,String openingHash,String openingFen,Game brnWhite,Game brnBlack) {
        boolean complete(){return brnWhite.brnScore()!=null && brnBlack.brnScore()!=null;}
        double score(){return (brnWhite.brnScore()+brnBlack.brnScore())/2;}
    }
    static Game play(ValidationArena.Opening opening,int brnColor,ToDoubleFunction<long[]> brn,com.ohinteractive.seedv6.core.brn3.Brn3Model productionModel,NnueNetwork nnue,long nanos,int cap) {
        var game=opening.newGame(cap);var moves=new ArrayList<String>();long[] nodes=new long[2],times=new long[2],depths=new long[2],turns=new long[2];
        String failure=null;
        var brnAdapter=productionModel==null?new ExactSearchAdapter((b,ply)->BrnResearchDiagnostics.score(brn.applyAsDouble(b)),new TTable(4))
                :new ExactSearchAdapter(SearchEvaluation.brn3(productionModel),new TTable(4));
        try(var brnDriver=new SearchDriver(brnAdapter);
            var nnueDriver=new SearchDriver(new ExactSearchAdapter(SearchEvaluation.incremental(nnue),new TTable(4)))) {
            // Equal untimed depth-1 warmup; no model updates, fresh per-game tables.
            for(var driver:List.of(brnDriver,nnueDriver))driver.search(new SearchRequest(opening.board(),opening.history(),1));
            while(game.active()) {
                int actor=game.sideToMove()==brnColor?0:1;var driver=actor==0?brnDriver:nnueDriver;
                long[] board=game.boardSnapshot();var history=game.historySnapshot();long start=System.nanoTime();
                var control=SearchControl.controlled(-1,start,nanos,TimeSource.SYSTEM);
                var outcome=driver.search(new SearchRequest(board,history,64,SearchObserver.NONE,control,false));
                var result=outcome.lastCompletedResult();times[actor]+=System.nanoTime()-start;nodes[actor]+=control.nodes();
                if(result==null || !result.completed() || !result.hasMove()) {game.abort(GameTermination.SEARCH_FAILURE,"No completed time-bounded iteration");break;}
                turns[actor]++;depths[actor]+=result.depth();moves.add(Long.toUnsignedString(result.bestMove(),16));game.play(result.bestMove());
            }
        } catch(RuntimeException problem){failure=problem.toString();game.abort(GameTermination.INFRASTRUCTURE_FAILURE,failure);}
        Double score=failure==null && game.termination().completed()?(game.termination().result().orElseThrow().target(brnColor)+1)/2:null;
        return new Game(failure==null?game.termination().name():GameTermination.INFRASTRUCTURE_FAILURE.name(),failure,game.playedPlies(),score,Fen.fromBoard(game.boardSnapshot()),List.copyOf(moves),nodes[0],nodes[1],times[0]/1e9,times[1]/1e9,
                turns[0]==0?0:depths[0]/(double)turns[0],turns[1]==0?0:depths[1]/(double)turns[1]);
    }
    static Map<String,Object> summary(List<Pair> pairs) {
        var completed=pairs.stream().filter(Pair::complete).toList();var report=new LinkedHashMap<String,Object>();
        report.put("completedPairs",completed.size());report.put("incompletePairs",pairs.size()-completed.size());
        report.put("acceptanceSampleSizeMet",completed.size()>=100 && completed.size()==pairs.size());
        var terminations=new TreeMap<String,Integer>();int wins=0,draws=0,losses=0;
        for(var pair:pairs)for(var game:List.of(pair.brnWhite(),pair.brnBlack())){
            terminations.merge(game.termination(),1,Integer::sum);
            if(pair.complete()){if(game.brnScore()==1)wins++;else if(game.brnScore()==0)losses++;else draws++;}
        }
        report.put("terminations",terminations);report.put("brnWins",wins);report.put("draws",draws);report.put("brnLosses",losses);
        if(!completed.isEmpty()) {
            report.put("brnPointScore",completed.stream().mapToDouble(Pair::score).average().orElseThrow());
            var rng=new SplittableRandom(90217);double[] bootstrap=new double[20000];
            for(int i=0;i<bootstrap.length;i++){double sum=0;for(int j=0;j<completed.size();j++)sum+=completed.get(rng.nextInt(completed.size())).score();bootstrap[i]=sum/completed.size();}
            Arrays.sort(bootstrap);report.put("pairedBootstrapOneSided95Lower",bootstrap[999]);
            report.put("uncertaintyNote","Opening-pair bootstrap, conditional on this opening generator and time budget; incomplete pairs excluded and explicitly counted");
        }
        return report;
    }
    public static void main(String[] args)throws Exception {
        if(args.length<5)throw new IllegalArgumentException("BRN_RUN NNUE_RUN OUT PAIRS MILLISECONDS [MAX_PLIES=512] [compact|production] [FIRST_OPENING=0]");
        Path brnRun=Path.of(args[0]),nnueRun=Path.of(args[1]),out=Path.of(args[2]);if(Files.exists(out))throw new IOException("Use a new match output directory");
        int pairs=Integer.parseInt(args[3]),millis=Integer.parseInt(args[4]),cap=args.length>5?Integer.parseInt(args[5]):512;
        if(pairs<1 || pairs>1000 || millis<1 || millis>1000 || cap<1)throw new IllegalArgumentException("Match budget");
        NnueNetwork nnue;try(var in=new BufferedInputStream(Files.newInputStream(nnueRun.resolve("selected.nnue")))){nnue=NnueNetworkCodec.read(in);}
        var brnMeta=BrnResearchComparison.metadata(brnRun);var nnueMeta=BrnResearchComparison.metadata(nnueRun);
        if(!Objects.equals(((Map<?,?>)brnMeta.get("datasetManifest")).get("payloadSha256"),((Map<?,?>)nnueMeta.get("datasetManifest")).get("payloadSha256")))throw new IOException("Unmatched training population");
        boolean compact=args.length>6 && args[6].equals("compact"),production=args.length>6&&args[6].equals("production");
        int firstOpening=args.length>7?Integer.parseInt(args[7]):0;if(firstOpening<0)throw new IllegalArgumentException("Negative opening index");
        var productionModel=production?BrnResearchComparison.productionModel(brnRun):null;
        ToDoubleFunction<long[]> inference;
        if(production){var workspace=productionModel.newWorkspace();inference=workspace::evaluatePawns;}
        else {AbsoluteRelationCandidate model;try(var in=new BufferedInputStream(Files.newInputStream(brnRun.resolve("selected.state")))){model=AbsoluteRelationCandidate.read(in).foldedInference();}
            var cache=new AbsoluteRelationInference(model,compact);inference=cache::predict;}
        var config=new ValidationConfig(pairs,620391,6,10,4,1,NnueScoreMapping.V1,cap);var root=Board.startingPosition();var history=GameHistory.initial(root);
        var results=new ArrayList<Pair>();var identities=new HashSet<String>();var report=new LinkedHashMap<String,Object>();Files.createDirectories(out);
        report.put("brnRun",brnRun.toString());report.put("nnueRun",nnueRun.toString());report.put("requestedPairs",pairs);report.put("millisecondsPerMove",millis);report.put("maximumPlies",cap);
        report.put("brnStateSha256",BrnResearchComparison.digest(brnRun.resolve("selected.state")));report.put("nnueModelSha256",BrnResearchComparison.digest(nnueRun.resolve("selected.nnue")));
        report.put("inference",production?"BRN-3 production model/workspace/search state":compact?"rolling cache, float32 weights":"rolling cache, double weights");
        if(production)report.put("brnProductionModelSha256",BrnResearchComparison.digest(brnRun.resolve("production-preview/network.brn3")));
        report.put("firstOpeningIndex",firstOpening);
        report.put("openingPolicy","SeedV6 legal-uniform 6..10 plies, seed620391; distinct active openings with |material|<.5 pawn; no evaluator-based filtering");
        for(int index=firstOpening;results.size()<pairs;index++) {
            if(index-firstOpening>=pairs*100)throw new IOException("Opening admission bound");
            var opening=ValidationArena.opening(root,history,config,index);
            if(!opening.newGame(cap).active() || Math.abs(RelationalCandidate.material(opening.board()))>=.5 || !identities.add(opening.identity()))continue;
            Game white,black;
            if(results.size()%2==0){white=play(opening,0,inference,productionModel,nnue,millis*1_000_000L,cap);black=play(opening,1,inference,productionModel,nnue,millis*1_000_000L,cap);}
            else{black=play(opening,1,inference,productionModel,nnue,millis*1_000_000L,cap);white=play(opening,0,inference,productionModel,nnue,millis*1_000_000L,cap);}
            results.add(new Pair(index,opening.identity(),Fen.fromBoard(opening.board()),white,black));report.put("pairs",results);report.put("summary",summary(results));
            DataFiles.write(out.resolve("match.json"),report);System.out.println("PAIR "+results.size()+" "+DataFiles.JSON.toJson(report.get("summary")));
        }
    }
}
