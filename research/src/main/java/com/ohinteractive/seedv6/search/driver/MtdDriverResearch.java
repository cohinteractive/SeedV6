package com.ohinteractive.seedv6.search.driver;

import java.util.Objects;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.exact.ParallelSearch;

/** Workstream 2 only. Shared flattened kernel, unchanged TT/proof/history policy.
 * Selective brackets are estimates, NEVER mathematical proof. Only a searched
 * interior result (or full fallback) can complete an iteration. No table writes here. */
public final class MtdDriverResearch implements SingleDepthSearch {
    public enum Policy { PVS, PREV, PARITY, BRACKET, TWO_PASS, FULL_RECOVERY, GUARDED_TWO }
    @FunctionalInterface public interface Window { SearchResult search(SearchRequest r,int alpha,int beta); }
    @FunctionalInterface public interface Observer { void attempt(int depth,String phase,int alpha,int beta,
            int lower,int upper,SearchResult result); }
    private final SingleDepthSearch owner;
    private final Window window;
    private final Policy policy;
    private final Observer observer;
    private int previousDepth,previousScore,earlierScore;
    private boolean active,requestActive;
    private final long[] root=new long[Board.MAX_BITBOARDS];

    public MtdDriverResearch(ExactSearchAdapter owner,Policy policy,Observer observer) {
        this(owner,owner::searchWindow,policy,observer);
        if(policy==Policy.GUARDED_TWO) owner.withRepeatedRootPassResearch();
    }
    public MtdDriverResearch(ParallelSearch owner,Policy policy,Observer observer) {
        this(owner,owner::searchWindow,policy,observer);
        if(policy==Policy.GUARDED_TWO) owner.withRepeatedRootPassResearch();
    }
    /** Deterministic failure/cancellation tests; production never constructs this facility. */
    public MtdDriverResearch(SingleDepthSearch owner,Window window,Policy policy,Observer observer) {
        this.owner=Objects.requireNonNull(owner);this.window=Objects.requireNonNull(window);
        this.policy=Objects.requireNonNull(policy);this.observer=observer;
    }
    @Override public SearchResult search(SearchRequest request) {
        if(active) throw new IllegalStateException("Reentrant root driver");
        active=true;
        try {return resolve(request);} finally {active=false;}
    }
    private SearchResult resolve(SearchRequest request) {
        request.copyBoardInto(root);
        var invocation=new SearchRequest(root,request.gameHistory(),request.depth(),SearchObserver.NONE,
                request.control(),request.diagnosticsEnabled());
        int lower=-ExactSearch.MATE_SCORE,upper=ExactSearch.MATE_SCORE;
        int g=policy==Policy.PARITY && previousDepth>1?earlierScore:previousScore;
        int pass=0,direction=0,distance=1;
        boolean bracketed=false,selective=false;
        long nodes=0,start=System.nanoTime();
        var diagnostics=request.diagnosticsEnabled()?SearchDiagnosticsSnapshot.enabledEmpty():SearchDiagnosticsSnapshot.disabled();
        boolean full=policy==Policy.PVS || !requestActive || previousDepth!=request.depth()-1 || request.depth()<2;
        boolean materialize=false;
        SearchResult result=new SearchResult(0,false,Value.INVALID,request.depth(),0,0,false);
        while(available(request,true)) {
            String phase=full?(pass==0?"full":"fallback"):materialize?"materialize":"zero";
            int beta=full?ExactSearch.INFINITY:materialize?lower+1:g==lower?g+1:g;
            if(!full && !materialize && policy==Policy.BRACKET && pass>0) {
                beta=bracketed?lower+(upper-lower+1)/2:direction>0?
                        (int)Math.min(upper,(long)lower+distance):(int)Math.max((long)lower+1,(long)upper-distance+1);
            }
            int alpha=full?-ExactSearch.INFINITY:materialize?lower-1:beta-1;
            if(!full && !materialize && (beta<=lower || beta>upper))
                throw new IllegalStateException("Non-progressing threshold");
            result=window.search(invocation,alpha,beta);
            nodes+=result.nodes();selective|=result.selective();
            if(request.diagnosticsEnabled()) diagnostics=diagnostics.mergeWorkers(result.diagnostics());
            if(observer!=null) observer.attempt(request.depth(),phase,alpha,beta,lower,upper,result);
            if(!result.completed() || !available(request,false)) break;
            int score=result.score();
            if(score < -ExactSearch.MATE_SCORE || score>ExactSearch.MATE_SCORE)
                throw new IllegalStateException("Invalid Search score");
            if(full || (materialize && score>alpha && score<beta)) {
                earlierScore=previousScore;previousScore=score;previousDepth=request.depth();
                var completed=aggregate(request,result,nodes,diagnostics,selective,true);
                request.observer().onSearchFinished(completed,System.nanoTime()-start);
                return completed;
            }
            if(materialize) {full=true;continue;} // Window-dependent estimates disagree: discard, do not prove.
            pass++;
            int oldLower=lower,oldUpper=upper;
            if(score>=beta) lower=Math.max(lower,score);else upper=Math.min(upper,score);
            if(lower>upper || (lower==oldLower && upper==oldUpper)) {
                // Even an untainted legacy result is not automatically mathematical authority.
                full=true;continue;
            }
            if(policy==Policy.BRACKET && !bracketed) {
                int next=score>=beta?1:-1;
                if(direction==0) direction=next;
                else if(direction!=next) bracketed=true;
                else distance=Math.min(2*ExactSearch.MATE_SCORE,distance*2);
            }
            g=score;
            if(lower==upper) {
                full=policy==Policy.FULL_RECOVERY || policy==Policy.GUARDED_TWO;
                materialize=!full;
            } else if((policy==Policy.TWO_PASS || policy==Policy.GUARDED_TWO) && pass>=2) full=true;
            // Strict interval shrink gives a finite 65536-pass domain bound.
            // Budgets/deadlines are checked before EVERY subsequent invocation.
        }
        return aggregate(request,result,nodes,diagnostics,selective,false);
    }
    private static SearchResult aggregate(SearchRequest request,SearchResult r,long nodes,
            SearchDiagnosticsSnapshot diagnostics,boolean selective,boolean complete) {
        return new SearchResult(complete?r.bestMove():0,complete&&r.hasMove(),complete?r.score():Value.INVALID,
                request.depth(),nodes,r.legalRootMoves(),complete,complete?r.principalVariation():new long[0],diagnostics,selective);
    }
    private static boolean available(SearchRequest request,boolean retry) {
        if(Thread.currentThread().isInterrupted()) request.control().request(SearchTermination.STOPPED);
        return !Thread.currentThread().isInterrupted() && request.control().checkpoint()
                && (!retry || request.control().checkpointNodeBudget());
    }
    @Override public void beginRequest() {
        if(active || requestActive) throw new IllegalStateException("Request active");
        previousDepth=0;owner.beginRequest();requestActive=true;
    }
    @Override public void endRequest() {owner.endRequest();previousDepth=0;requestActive=false;}
    @Override public void newGame() {owner.newGame();previousDepth=0;}
    @Override public void close() {owner.close();}
    @Override public int maxSupportedDepth() {return owner.maxSupportedDepth();}
    @Override public int activeSearchThreads() {return owner.activeSearchThreads();}
}
