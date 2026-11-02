package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Research-only calibration of discarded TC-P1 predictions against resumed HL-G1.
 * Reads existing synchronous trace/counters; changes neither kernel nor evaluator.
 * The reference is the nominal selective traversal, not an exhaustive chess oracle. */
public final class P4TacticalCalibration {
    static final class Frame {
        final long[] board,path;
        final int ply,depth,alpha,beta;
        final long nodesBefore,probeBefore;
        boolean predicted;
        long probeNodes;
        int raw;
        Frame(long[] b,long[] p,int ply,int depth,int a,int beta,long nodes,long probes) {
            this.board=b.clone();this.path=Arrays.copyOfRange(p,1,ply+1);
            this.ply=ply;this.depth=depth;this.alpha=a;this.beta=beta;
            nodesBefore=nodes;probeBefore=probes;
        }
        boolean matches(long[] b,long[] p,int ply,int d) {
            if(this.ply!=ply || depth!=d || !Arrays.equals(board,b))return false;
            for(int i=0;i<path.length;i++)if(path[i]!=p[i+1])return false;
            return true;
        }
    }

    static final class Trace implements SelectivityTrace {
        final ExactSearch exact;
        final FlatSearchState state;
        final Frame[] frames=new Frame[ExactSearch.MAX_DEPTH+1];
        final long[][] byDepth=new long[9][3]; // detected, nominal agrees, nominal disagrees
        final List<Map<String,Object>> witnesses=new ArrayList<>();
        Frame lastSurvivor;
        long seen,detected,agrees,disagrees;
        Trace(ExactSearch exact) throws Exception {
            this.exact=exact;state=(FlatSearchState)P4TtFoundationStudy.field(exact,"constructionState");
        }
        void begin() {
            Arrays.fill(frames,null);lastSurvivor=null;seen=detected=agrees=disagrees=0;
            witnesses.clear();for(long[] row:byDepth)Arrays.fill(row,0);
        }
        void capture() {
            long count=state.tacticalCutoffs;
            if(count==seen)return;
            if(count!=seen+1 || lastSurvivor==null || lastSurvivor.predicted)
                throw new AssertionError("Ambiguous tactical prediction attribution");
            Frame f=lastSurvivor;
            if(f.ply==0 || f.depth<4 || f.depth>8 || f.beta!=f.alpha+1)
                throw new AssertionError("Prediction outside declared eligibility");
            f.predicted=true;f.raw=state.rawEval[f.ply];
            f.probeNodes=state.tacticalProbeNodes-f.probeBefore;
            if(f.probeNodes<=0)throw new AssertionError("Prediction without charged probe work");
            seen=count;detected++;byDepth[f.depth][0]++;
        }
        @Override public void survivor(long[] board,long[] path,int ply,int depth,int alpha,int beta,
                boolean scout,boolean checked,int raw) {
            capture();
            if(frames[ply]!=null)throw new AssertionError("Overlapping nominal invocation frame");
            frames[ply]=new Frame(board,path,ply,depth,alpha,beta,exact.visitedNodes(),state.tacticalProbeNodes);
            lastSurvivor=frames[ply];
        }
        @Override public void reduction(long[] b,long move,long[] path,int ply,int depth,int ordinal,int quiets,
                int a,int beta,int h,int c,int raw,int previous,int base,int assigned,boolean caution) {capture();}
        @Override public void omission(long[] b,long move,long[] path,int ply,int depth,int a,int beta,
                int raw,int h,int c,int assigned,int margin) {capture();}
        @Override public void completed(long[] board,long[] path,int ply,int depth,int score,long nodes) {
            capture();Frame f=frames[ply];
            if(f==null)return; // Terminal/q/TT/domain return did not reach survivor().
            if(!f.matches(board,path,ply,depth))throw new AssertionError("Nominal completion/frame mismatch");
            frames[ply]=null;
            if(!f.predicted)return;
            boolean agree=score>=f.beta;
            if(agree)agrees++;else disagrees++;
            byDepth[f.depth][agree?1:2]++;
            long nominalNodes=nodes-f.nodesBefore+1-(state.tacticalProbeNodes-f.probeBefore);
            if(nominalNodes<1)throw new AssertionError("Invalid nominal-only work accounting");
            // First twelve disagreements per invocation, in traversal order, never selected by a game result.
            if(!agree && witnesses.size()<12) {
                var row=new LinkedHashMap<String,Object>();
                row.put("board",f.board);row.put("path",f.path);row.put("ply",ply);row.put("depth",depth);
                row.put("alpha",f.alpha);row.put("beta",f.beta);row.put("raw",f.raw);
                row.put("nominalScore",score);row.put("probeNodes",f.probeNodes);
                row.put("nominalSubtreeNodes",nominalNodes);witnesses.add(row);
            }
        }
        Map<String,Object> finish(boolean complete,long[] root) {
            // Aborted invocation cleanup may clear published counters. Retain partial labels separately.
            if(complete) {
                capture();
                if(detected!=state.tacticalCutoffs || detected!=agrees+disagrees
                        || Arrays.stream(frames).anyMatch(Objects::nonNull))
                    throw new AssertionError("Missing prediction or nominal completion");
            }
            for(var witness:witnesses) {
                long[] path=(long[])witness.get("path");Phase2Oracle.legalPv(root,path);
                long[] board=root.clone();for(long move:path)board=ExhaustiveOracle.child(board,move);
                if(!Arrays.equals(board,(long[])witness.get("board")))throw new AssertionError("Witness real history mismatch");
            }
            return Map.of("detected",detected,"nominalAgrees",agrees,"nominalDisagrees",disagrees,
                    "unresolved",detected-agrees-disagrees,"byDepth",byDepth,"witnesses",witnesses,
                    "completeInvocation",complete);
        }
    }

    /** model corpus NEW-output maximumDepth per-invocation-node-limit */
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("model corpus NEW-output depth nodes");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        int depth=Integer.parseInt(args[3]);long budget=Long.parseLong(args[4]);
        if(depth<5 || depth>10 || budget<1)throw new IllegalArgumentException("Bounded calibration arguments");
        var gson=new GsonBuilder().create();long predictions=0,disagreements=0;int completed=0,aborted=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","source",ApplicationVersion.load().sourceRevision(),
                    "build",ApplicationVersion.load().build(),"modelSha256",ConventionalNnueEngine.SHA256,"args",args,
                    "reference","Resumed nominal selective HL-G1 traversal; no exhaustive-oracle or playing-strength claim")));
            out.newLine();out.flush();
            for(var root:roots) {
                var base=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),new TTable(64),P4TacticalStudy.flags("guided",1));
                var observed=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),new TTable(64),P4TacticalStudy.flags("probcut",1)).withTacticalObservation(true);
                var trace=new Trace(observed);observed.withSelectivityTrace(trace);var saved=root.board().clone();
                base.beginRequest();observed.beginRequest();
                try {
                    for(int d=1;d<=depth;d++) {
                        trace.begin();
                        var a=base.search(root.board(),root.history(),d,()->base.visitedNodes()>=budget);
                        var b=observed.search(root.board(),root.history(),d,()->observed.visitedNodes()>=budget);
                        if(b.completed()&&!a.completed())throw new AssertionError("Observed extra-work traversal completed when its nominal control did not");
                        boolean complete=a.completed()&&b.completed();
                        var calibration=trace.finish(b.completed(),root.board());
                        if(!Arrays.equals(saved,root.board()))throw new AssertionError("Root changed");
                        if(b.completed())Phase2Oracle.legalPv(root.board(),b.principalVariation());
                        if(complete) {
                            if(a.score()!=b.score() || !Arrays.equals(a.principalVariation(),b.principalVariation())
                                    || a.nodes()+trace.state.tacticalProbeNodes!=b.nodes())
                                throw new AssertionError("Discarded-probe nominal neutrality/accounting failed");
                            var state=(FlatSearchState)P4TtFoundationStudy.field(base,"constructionState");
                            if(!Arrays.equals(state.captures,trace.state.captures) || !Arrays.equals(state.continuation,trace.state.continuation)
                                    || !Arrays.equals((int[])P4TtFoundationStudy.field(base,"quietHistory"),(int[])P4TtFoundationStudy.field(observed,"quietHistory")))
                                throw new AssertionError("Discarded probe changed retained learning");
                            predictions+=trace.detected;disagreements+=trace.disagrees;completed++;
                        } else aborted++;
                        out.write(gson.toJson(Map.of("type","invocation","root",root.id(),"depth",d,
                                "baseline",a,"observed",b,"calibration",calibration,
                                "probeNodes",trace.state.tacticalProbeNodes,"primaryCompletedPair",complete)));
                        out.newLine();out.flush();
                        if(!complete)break;
                    }
                } finally {base.endRequest();observed.endRequest();}
            }
            if(predictions==0)throw new AssertionError("No completed tactical predictions calibrated");
            out.write(gson.toJson(Map.of("status","passed","completedInvocationPairs",completed,
                    "abortedInvocationPairs",aborted,"predictions",predictions,"nominalDisagreements",disagreements,
                    "limits","Dependent predictions along an uncut nominal trajectory, not a population error rate. Nested subtree costs overlap. Selective horizon disagreement is not a legality defect or strength result.")));
            out.newLine();
        }
    }
    private P4TacticalCalibration() {}
}
