package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import com.ohinteractive.seedv6.core.Board;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;

/** Private primitive scratch for opt-in flattened compositions; no score/proof cache. */
final class FlatSearchState {
    final FlatSearchConfig config;
    final long[] pathMoves=new long[ExactSearch.MAX_DEPTH+1];
    final int[] credits=new int[ExactSearch.MAX_DEPTH+1],rawEval=new int[ExactSearch.MAX_DEPTH+1];
    final HistoryLearning nominal=new HistoryLearning(true,null);
    final int[] captures;
    final short[] continuation;
    final long[] killers,counters;
    long captureUpdates,quietUpdates,filtered,orderingChanges;
    long reductions,verifications,fullRetries,refuted,relief,extensions,futilityOmissions,reverseCutoffs,protectedMoves,evaluations;

    FlatSearchState(FlatSearchConfig config) {
        this.config=config;
        captures=config.has(CAPTURE_HISTORY)?new int[CaptureHistory.SIZE]:null;
        continuation=config.has(CONTINUATION)?new short[ContinuationHistory.SIZE]:null;
        killers=config.has(KILLERS)?new long[(ExactSearch.MAX_DEPTH+1)*KillerMoves.SLOTS]:null;
        counters=config.has(COUNTERMOVES)?new long[Countermoves.SIZE]:null;
    }
    void reset() {
        Arrays.fill(pathMoves,0);
        Arrays.fill(credits,0);Arrays.fill(rawEval,Integer.MIN_VALUE);
        if(captures!=null) Arrays.fill(captures,0);
        if(continuation!=null) Arrays.fill(continuation,(short)0);
        if(killers!=null) Arrays.fill(killers,0);
        if(counters!=null) Arrays.fill(counters,0);
        captureUpdates=quietUpdates=filtered=orderingChanges=0;
        reductions=verifications=fullRetries=refuted=relief=extensions=futilityOmissions=reverseCutoffs=protectedMoves=evaluations=0;
    }
    void order(long[] moves,int from,int count,int ep,long hash,int[] keys,int base,int ply,int[] history) {
        int context=ContinuationHistory.context(pathMoves[ply]);
        int counter=Countermoves.context(pathMoves[ply]);
        for(int i=from;i<count;i++) {
            long move=moves[i];if(move==hash) continue;
            int old=keys[base+i],group=old>>>25,evidence;
            if(CaptureHistory.isTactical(move,ep)) {
                evidence=ExactSearch.tacticalMaterialValue(move,ep);
                if(captures!=null) evidence=evidence*8+captures[CaptureHistory.index(move,ep)]/16;
                if(group==1 && config.has(BAD_TACTICAL_LAST)) group=-1;
            } else {
                evidence=history[QuietHistory.index(move)];
                if(continuation!=null && context>=0) evidence+=continuation[ContinuationHistory.index(context,move)]/2;
                evidence=Math.max(-QuietHistory.LIMIT,Math.min(QuietHistory.LIMIT,evidence));
                if(killers!=null && move==killers[ply*2]) {group=1;evidence=12000;}
                else if(killers!=null && move==killers[ply*2+1]) {group=1;evidence=10000;}
                else if(counters!=null && counter>=0 && move==counters[counter]) {group=1;evidence=8000;}
            }
            keys[base+i]=(group<<25)+((evidence+QuietHistory.LIMIT)<<9)+(511-i);
            if(keys[base+i]!=old) orderingChanges++;
        }
    }
    void cutoff(int[] history,long[] moves,int cutoff,int ep,int depth,int ply) {
        long winner=moves[cutoff];
        if(!nominal.observed(ply,cutoff)) throw new IllegalStateException("Unverified cutoff label");
        if(config.has(NOMINAL_HISTORY)) nominal.cutoff(history,moves,cutoff,ep,depth,ply);
        else QuietHistory.recordCutoff(history,moves,cutoff,ep,depth);
        int d=Math.min(depth,64),bonus=d*d;
        boolean tactical=CaptureHistory.isTactical(winner,ep);
        int context=ContinuationHistory.context(pathMoves[ply]);
        if(tactical) {
            if(captures!=null) {CaptureHistory.update(captures,CaptureHistory.index(winner,ep),bonus);captureUpdates++;}
        } else {
            if(continuation!=null && context>=0) {ContinuationHistory.update(continuation,ContinuationHistory.index(context,winner),bonus);quietUpdates++;}
            if(killers!=null) KillerMoves.recordCutoff(killers,ply,winner,ep);
            if(counters!=null) Countermoves.recordCutoff(counters,Countermoves.context(pathMoves[ply]),winner,ep);
        }
        for(int i=0;i<cutoff;i++) {
            long move=moves[i];
            if(!nominal.observed(ply,i)) {filtered++;continue;}
            if(CaptureHistory.isTactical(move,ep)!=tactical) continue;
            if(tactical && captures!=null) CaptureHistory.update(captures,CaptureHistory.index(move,ep),-bonus);
            else if(!tactical && continuation!=null && context>=0)
                ContinuationHistory.update(continuation,ContinuationHistory.index(context,move),-bonus);
        }
    }
}
