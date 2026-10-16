package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.security.DigestOutputStream;
import java.util.*;
import java.util.function.Supplier;

/** Common legal child+score requests, with native initialize/child hooks and an untimed oracle. */
public final class BrnArchitectureTransitions {
    static final int KING=1,CAPTURE=2,PROMOTION=4,CASTLE=8,EN_PASSANT=16;
    record Request(long[] parent,long[] child,long move,int flags) {}
    static int flags(long[] parent,long[] child,long move) {
        int from=(int)move&63,to=(int)(move>>>Board.TARGET_SQUARE_SHIFT)&63;
        int type=Board.getSquare(parent[0],parent[1],parent[2],parent[3],from)&Piece.TYPE;
        boolean capture=Long.bitCount(parent[0]|parent[1]|parent[2])>Long.bitCount(child[0]|child[1]|child[2]);
        int target=Board.getSquare(parent[0],parent[1],parent[2],parent[3],to)&Piece.TYPE;
        int moved=Board.getSquare(child[0],child[1],child[2],child[3],to)&Piece.TYPE;
        return (type==Piece.KING?KING:0)|(capture?CAPTURE:0)|(type==Piece.PAWN&&moved!=Piece.PAWN?PROMOTION:0)
                |(type==Piece.KING&&Math.abs(to-from)==2?CASTLE:0)|(type==Piece.PAWN&&capture&&target==0?EN_PASSANT:0);
    }
    static void add(List<Request> requests,long[] parent,int limit,Random random) {
        var moves=new long[512];int count=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
        for(int i=count-1;i>0;i--){int j=random.nextInt(i+1);long t=moves[i];moves[i]=moves[j];moves[j]=t;}
        for(int i=0;i<Math.min(count,limit);i++) {
            var child=new long[6];Board.makeMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],moves[i],child);
            requests.add(new Request(parent.clone(),child,moves[i],flags(parent,child,moves[i])));
        }
    }
    static List<Request> requests(List<long[]> parents) {
        var result=new ArrayList<Request>();var random=new Random(190519);
        for(var parent:parents)add(result,parent,8,random);
        for(String fen:List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"))
            add(result,Board.fromFen(fen),512,random);
        if(result.isEmpty())throw new IllegalArgumentException("No transitions");
        return result;
    }
    static String digest(List<Request> requests)throws IOException {
        var hash=DataFiles.digest();try(var out=new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(),hash))) {
            for(var r:requests){for(long x:r.parent)out.writeLong(x);for(long x:r.child)out.writeLong(x);out.writeLong(r.move);out.writeInt(r.flags);}
        }
        return HexFormat.of().formatHex(hash.digest());
    }
    static Map<String,Object> verify(List<Request> requests,ExactEvaluator incremental,Supplier<ExactEvaluator> freshEvaluator) {
        int maximum=0,changed=0,parentChanged=0,index=0;double[] jumps=new double[requests.size()];
        Map<String,Object> largestJump=Map.of();double largest=-1,sumSquares=0;
        for(var r:requests) {
            long[] parent=r.parent.clone(),child=r.child.clone();incremental.initialize(parent);int before=incremental.evaluate(parent,0);
            incremental.child(parent,child,0);int actual=incremental.evaluate(child,1);
            // BRN initialize is intentionally a no-op: a NEW workspace is necessary for a true full-refresh oracle.
            var refreshed=freshEvaluator.get();refreshed.initialize(child);int expected=refreshed.evaluate(child,0);
            int delta=Math.abs(actual-expected);maximum=Math.max(maximum,delta);if(delta!=0)changed++;
            int parentMaterial=NnueMaterialBootstrap.forSideToMove(parent,NnueMaterialBootstrap.whiteScore(parent));
            int childMaterial=NnueMaterialBootstrap.forSideToMove(child,NnueMaterialBootstrap.whiteScore(child));
            // Child score is for the opposite STM. Remove known material change before comparing residuals.
            double jump=Math.abs(-(actual-childMaterial)-(before-parentMaterial));jumps[index++]=jump;sumSquares+=jump*jump;
            if(jump>largest){largest=jump;largestJump=Map.of("parentFen",Fen.fromBoard(parent),"childFen",Fen.fromBoard(child),
                    "move",Long.toUnsignedString(r.move,16),"absoluteResidualChangeScoreUnits",jump,"parentScore",before,"childScore",actual);}
            int parentDelta=Math.abs(incremental.evaluate(parent,0)-before);if(parentDelta!=0)parentChanged++;
            if(delta>1||parentDelta>1)throw new AssertionError("Transition/full-refresh or parent score mismatch: "+delta+"/"+parentDelta);
            if(!Arrays.equals(parent,r.parent)||!Arrays.equals(child,r.child))throw new AssertionError("Evaluator modified board input");
        }
        Arrays.sort(jumps);var result=new LinkedHashMap<String,Object>();result.put("requests",requests.size());
        result.put("maximumAbsoluteIntegerScoreDifference",maximum);result.put("nonzeroChildScoreDifferences",changed);result.put("nonzeroParentScoreDifferences",parentChanged);
        result.put("integerTolerance",1);result.put("reason","Allow one score unit for different float accumulation orders; larger errors block timing");
        result.put("absoluteResidualTransitionChange",Map.of("medianScoreUnits",Brn3RuntimeMeasurement.median(jumps),
                "p95ScoreUnits",jumps[Math.min(jumps.length-1,(int)Math.ceil(.95*jumps.length)-1)],"rmsScoreUnits",Math.sqrt(sumSquares/jumps.length),"largest",largestJump,
                "scope","Descriptive calibrated residual changes over common legal moves after removing fixed material; large changes may be useful positional information,not automatically noise or error"));
        return result;
    }
    static boolean inGroup(Request r,int group) {
        return switch(group){case 0->true;case 1->(r.flags&KING)==0;case 2->(r.flags&KING)!=0;
            case 3->(r.flags&CAPTURE)!=0;case 4->(r.flags&PROMOTION)!=0;case 5->(r.flags&CASTLE)!=0;case 6->(r.flags&EN_PASSANT)!=0;default->throw new AssertionError();};
    }
    static volatile long sink;
    public static void main(String[] a)throws Exception {
        if(a.length!=4)throw new IllegalArgumentException("DATA RUN NEW_OUT GAIN");
        Path dataPath=Path.of(a[0]),run=Path.of(a[1]),out=Path.of(a[2]);double gain=Double.parseDouble(a[3]);BrnArchitectureControls.requireGain(gain);
        if(Files.exists(out))throw new IOException("Use a fresh transition output");
        String modelHash=BrnArchitectureControls.modelIdentity(run),metadataHash=BrnArchitectureControls.metadataIdentity(run);
        var held=BrnResearchData.read(dataPath,false).validation();
        var requests=requests(held.subList(0,Math.min(128,held.size())).stream().map(BrnResearchData.Example::board).toList());
        var view=BrnArchitectureControls.loadView(run,gain);var evaluator=view.evaluator().get();
        var report=new LinkedHashMap<String,Object>();report.put("schema","brn-architecture-transitions-v1");report.put("arguments",a);
        report.put("modelSha256",modelHash);report.put("modelMetadataIdentity",metadataHash);report.put("gain",gain);
        report.put("datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class));report.put("requestSha256",digest(requests));
        report.put("verification",verify(requests,evaluator,view.evaluator()));
        String[] names={"all","nonking","king","capture","promotion","castle","enPassant"};int[] counts=new int[names.length];
        for(var r:requests)for(int g=0;g<names.length;g++)if(inGroup(r,g))counts[g]++;
        double[][] ns=new double[names.length][9];long total=0;
        for(int round=-64;round<9;round++) {
            long[] sums=new long[names.length];
            for(var r:requests) {
                evaluator.initialize(r.parent);total+=evaluator.evaluate(r.parent,0);
                long start=System.nanoTime();evaluator.child(r.parent,r.child,0);int score=evaluator.evaluate(r.child,1);long elapsed=System.nanoTime()-start;
                total+=score;if(round>=0)for(int g=0;g<names.length;g++)if(inGroup(r,g))sums[g]+=elapsed;
            }
            if(round>=0)for(int g=0;g<names.length;g++)ns[g][round]=counts[g]==0?0:sums[g]/(double)counts[g];
        }
        sink=total;
        var groups=new LinkedHashMap<String,Object>();for(int g=0;g<names.length;g++)if(counts[g]>0)
            groups.put(names[g],Map.of("requestsPerPass",counts[g],"nanosecondsPerRequestPasses",ns[g],"medianNs",Brn3RuntimeMeasurement.median(ns[g])));
        report.put("groups",groups);report.put("method","Same first128 validation parents,up to8 deterministic shuffled legal children each plus all legal moves from three special fixtures;64 warmup passes then9 timed passes;initialize+parent score outside each child+score interval;System.nanoTime per child,overhead included;one private native evaluator;no search or TT;groups overlap");
        BrnArchitectureControls.requireIdentity(run,modelHash,metadataHash);report.put("modelIdentityStable",true);
        if(!report.get("requestSha256").equals(digest(requests)))throw new AssertionError("Timed evaluator modified requests");
        Files.createDirectory(out);DataFiles.write(out.resolve("result.json"),report);System.out.println(DataFiles.JSON.toJson(report));
    }
    private BrnArchitectureTransitions(){}
}
