package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;

/** Bounded successor baseline/coverage experiment. Uses validation, never sealed tests. */
public final class BrnSuccessorProbe {
    static volatile double sink;
    static int channel(int code) { return (code&7)-1+((code>>>3)&1)*6; }
    static int row(int a,int b) { int large=Math.max(a,b),small=Math.min(a,b);return large*(large-1)/2+small; }
    static int[] entities(long[] b,int perspective) {
        int[] result=new int[Long.bitCount(b[0]|b[1]|b[2])];int i=0;
        for(long bits=b[0]|b[1]|b[2];bits!=0;bits&=bits-1) {
            int sq=Long.numberOfTrailingZeros(bits),code=Board.getSquare(b[0],b[1],b[2],b[3],sq);
            result[i++]=channel(code^(perspective<<3))*64+(sq^(perspective*56));
        }
        return result;
    }
    static Map<String,Object> coverage(BrnResearchData.Dataset data) {
        int[] counts=new int[Brn3Layout.EDGE_ROWS];long incidences=0,unseen=0,validationIncidences=0;
        for(var e:data.training())for(int p=0;p<2;p++) {
            var es=entities(e.board(),p);
            for(int i=0;i<es.length;i++)for(int j=i+1;j<es.length;j++){counts[row(es[i],es[j])]++;incidences++;}
        }
        for(var e:data.validation())for(int p=0;p<2;p++) {
            var es=entities(e.board(),p);
            for(int i=0;i<es.length;i++)for(int j=i+1;j<es.length;j++) {validationIncidences++;if(counts[row(es[i],es[j])]==0)unseen++;}
        }
        int sameSquare=0,illegalPawnRank=0,sameSideKings=0,zero=0,rare=0;
        for(int large=1;large<768;large++)for(int small=0;small<large;small++) {
            if(large%64==small%64)sameSquare++;
            else if((large/64%6==5&&(large%64<8||large%64>=56))||(small/64%6==5&&(small%64<8||small%64>=56)))illegalPawnRank++;
            else if(large/64==small/64&&large/64%6==0)sameSideKings++;
        }
        for(int n:counts){if(n==0)zero++;if(n>0&&n<10)rare++;}
        int[] sorted=counts.clone();Arrays.sort(sorted);
        return Map.ofEntries(Map.entry("trainingPositions",data.training().size()),Map.entry("validationPositions",data.validation().size()),
                Map.entry("rows",counts.length),Map.entry("zeroExposureRows",zero),Map.entry("oneToNineExposureRows",rare),
                Map.entry("medianRowExposure",sorted[sorted.length/2]),Map.entry("p90RowExposure",sorted[sorted.length*9/10]),
                Map.entry("trainingPairIncidencesBothPerspectives",incidences),Map.entry("validationUnseenFraction",unseen/(double)validationIncidences),
                Map.entry("impossibleSameSquare",sameSquare),Map.entry("additionalIllegalPawnRank",illegalPawnRank),
                Map.entry("additionalSameColorKings",sameSideKings),Map.entry("note","Exposure includes both perspectives, not independent examples; impossible counts are disjoint sufficient conditions, not a full legality proof."));
    }
    static List<long[]> sequence(BrnResearchData.Dataset data,String kind) {
        var out=new ArrayList<long[]>();
        for(var e:data.validation().subList(0,256)) {
            var b=e.board();var moves=new long[512];int n=BrnResearchDiagnostics.legal(b,moves);
            if(kind.equals("unrelated")){out.add(b);continue;}
            for(int j=0;j<n;j++) {
                var c=BrnResearchDiagnostics.child(b,moves[j]);
                boolean capture=BrnResearchDiagnostics.pieces(c)<BrnResearchDiagnostics.pieces(b);
                if(kind.equals("capture")&&!capture||kind.equals("quiet")&&capture)continue;
                if(!kind.equals("siblings"))out.add(b);
                out.add(c);
            }
        }
        return out;
    }
    static double timed(Brn3Workspace work,List<long[]> boards,int loops) {
        double sum=0;long start=System.nanoTime();
        for(int r=0;r<loops;r++)for(var b:boards)sum+=work.evaluatePawns(b,.25);
        sink=sum;return (System.nanoTime()-start)/(double)(boards.size()*loops);
    }
    static Map<String,Object> runtime(Brn3Model model,BrnResearchData.Dataset data) {
        var result=new LinkedHashMap<String,Object>();
        for(String kind:List.of("unrelated","quiet","capture","siblings")) {
            var boards=sequence(data,kind);var work=model.newWorkspace();
            double max=0;int scoreChanges=0;long checks=0;
            for(var b:boards) {
                double actual=work.evaluatePawns(b,.25),expected=model.newWorkspace().evaluatePawns(b,.25);
                max=Math.max(max,Math.abs(actual-expected));if(Brn3Model.score(actual)!=Brn3Model.score(expected))scoreChanges++;checks++;
            }
            if(max>1e-4)throw new AssertionError("cache drift "+max);
            timed(work,boards,12);double[] trials=new double[7];
            work.rebuilds=work.updates=work.repeats=0;
            for(int r=0;r<trials.length;r++)trials[r]=timed(work,boards,12);
            result.put(kind,Map.of("positions",boards.size(),"nanoseconds",trials,"medianNs",Brn3RuntimeMeasurement.median(trials),
                    "rebuilds",work.rebuilds,"updates",work.updates,"repeats",work.repeats,"checked",checks,"maxErrorPawns",max,"scoreDifferences",scoreChanges));
        }
        var searches=new ArrayList<Map<String,Object>>();
        for(int i=0;i<32;i++) {
            var b=data.validation().get(i).board();long start=System.nanoTime();
            var control=SearchControl.controlled(2_000_000,start,3_000_000_000L,TimeSource.SYSTEM);
            try(var driver=new SearchDriver(new ExactSearchAdapter(SearchEvaluation.brn3(model),new TTable(4)))) {
                var outcome=driver.search(new SearchRequest(b,GameHistory.initial(b),4,SearchObserver.NONE,control,true));
                var last=outcome.lastCompletedResult();
                searches.add(Map.of("index",i,"complete",outcome.targetDepthCompleted(),"nodes",control.nodes(),
                        "seconds",(System.nanoTime()-start)/1e9,"score",last.score(),"move",Long.toUnsignedString(last.bestMove())));
            }
        }
        result.put("searchDepth4",searches);return result;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("coverage|runtime DATA MODEL NEW_JSON");
        Path dataPath=Path.of(args[1]),modelPath=Path.of(args[2]),out=Path.of(args[3]);
        if(Files.exists(out))throw new IllegalArgumentException("Output must be new");
        var data=BrnResearchData.read(dataPath,false);var report=new LinkedHashMap<String,Object>();
        report.put("mode",args[0]);report.put("datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class));
        report.put("java",System.getProperty("java.version"));report.put("availableProcessors",Runtime.getRuntime().availableProcessors());
        if(args[0].equals("coverage"))report.put("coverage",coverage(data));
        else if(args[0].equals("runtime")) {
            var model=BrnLearningProbe.model(modelPath);report.put("modelSha256",BrnResearchComparison.digest(modelPath));
            report.put("runtime",runtime(model,data));
        } else throw new IllegalArgumentException("Unknown mode");
        DataFiles.write(out,report);System.out.println(DataFiles.JSON.toJson(report));
    }
    private BrnSuccessorProbe(){}
}
