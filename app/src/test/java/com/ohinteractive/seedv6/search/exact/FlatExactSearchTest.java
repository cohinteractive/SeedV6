package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.search.exact.ExactSearch.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

/** Exact event-by-event comparisons, deliberately outside the timing harness. */
class FlatExactSearchTest {
    private static final ExactEvaluator HCE = (b,p) -> Eval.evaluate(b);

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void fixtureMatrixMatchesEveryTransitionEvaluationProbeStoreAndOracle(boolean localLeaves) throws Exception {
        int comparisons = 0;
        for(String fen : StagedGenerationTest.fens()) {
            long[] board = Board.fromFen(fen);
            var pair = new Pair(HCE, localLeaves);
            pair.begin();
            for(int depth = 0; depth <= 3; depth++) {
                var r = pair.run(board, GameHistory.initial(board), depth, -INFINITY, INFINITY);
                assertEquals(new ExactSearch(HCE).search(board, depth).score(), r.score(), fen);
                PvsSearchTest.assertExactPrefixes(board, GameHistory.initial(board), depth, r, HCE);
                comparisons++;
            }
            pair.end();
        }
        assertEquals(96, comparisons);
        System.out.println("SR-002 fixture/oracle/event-trace comparisons=" + comparisons);
    }

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void productionDepthFiveTreesAndHistoryAreIdentical(boolean localLeaves) throws Exception {
        for(var position : ExactSearchHarness.orderingPositions()) {
            long[] b = Board.fromFen(position.fen());
            var pair = new Pair(HCE, localLeaves);
            var r = pair.run(b, GameHistory.initial(b), 5, -INFINITY, INFINITY);
            assertEquals(new ExactSearch(HCE).search(b, 5).score(), r.score());
            PvsSearchTest.assertExactPrefixes(b, GameHistory.initial(b), 5, r, HCE);
            System.out.printf("SR-002 tree position=%s depth=5 nodes=%d events=%d%n",
                    position.name(), r.nodes(), pair.left.events.size());
            System.out.printf("SR-002 localLeaves=%s evaluations=%d positive_probes=%d%n",localLeaves,
                    pair.left.evaluations,pair.left.events.stream().filter(e->e.kind==2).count());
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void scoutFailLowImmediateCutoffInteriorResearchAndQuietRewardMalus(boolean localLeaves) throws Exception {
        long[] b = Board.startingPosition(), ordered = PvsSearchTest.ordered(b);
        for(int later : new int[] {-20,0,20,50,83}) {
            long secondKey = ExhaustiveOracle.child(b, ordered[1])[Board.KEY];
            var pair = new Pair((board,ply) -> board[Board.KEY] == secondKey ? -later : 0, localLeaves);
            var r = pair.run(b, GameHistory.initial(b), 1, -100, 50);
            assertEquals(Math.max(0,later), r.score());
            long visits = pair.left.events.stream().filter(e -> e.kind == 1 && e.a == secondKey).count();
            assertEquals(later > 0 && later < 50 ? 2 : 1, visits);
            if(later >= 50) {
                assertArrayEquals(new long[] {ordered[1]}, r.principalVariation());
                int[] h = history(pair.flat);
                assertEquals(-1, h[QuietHistory.index(ordered[0])]);
                assertEquals(1, h[QuietHistory.index(ordered[1])]);
                for(int i=2;i<ordered.length;i++) assertEquals(0,h[QuietHistory.index(ordered[i])]);
            }
            // Full re-search must classify its own window, with unchanged depth.
            Map<Long,Integer> values = new HashMap<>();
            for(int i=0;i<ordered.length;i++) {
                long[] child = ExhaustiveOracle.child(b, ordered[i]);
                for(long reply : ExhaustiveOracle.legalMoves(child))
                    values.put(ExhaustiveOracle.child(child,reply)[Board.KEY], i==1 ? later : 0);
            }
            pair = new Pair((board,ply) -> Objects.requireNonNull(values.get(board[Board.KEY])), localLeaves);
            r = pair.run(b, GameHistory.initial(b), 2, -100, 50);
            assertEquals(Math.max(0,later), r.score());
            long[] second = ExhaustiveOracle.child(b,ordered[1]);
            long key = ExactSearchTTableTest.key(second, GameHistory.builder(b).appendPosition(second).snapshot());
            var writes = pair.left.events.stream().filter(e -> e.kind==3 && e.a==key).toList();
            assertEquals(later>0 && later<50 ? 2 : 1,writes.size());
            assertEquals(later<=0 ? TTable.TYPE_LOWER : TTable.TYPE_UPPER,writes.getFirst().c);
            assertEquals(1,writes.getFirst().b);
            if(writes.size()==2) assertEquals(TTable.TYPE_EXACT,writes.getLast().c);
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void boundsDepthGenerationInvalidHashesAndStaticLeafExclusion(boolean localLeaves) throws Exception {
        long[] b = Board.startingPosition(); var game=GameHistory.initial(b);
        long hash = PvsSearchTest.ordered(b)[0], key=ExactSearchTTableTest.key(b,game);
        for(int type=0;type<3;type++) for(int storedDepth : new int[] {0,1,2,3})
            for(boolean old : new boolean[] {false,true}) {
                var pair = new Pair((board,p)->-37, localLeaves); pair.begin();
                pair.seed(key,storedDepth,type,type==2 ? -1000 : 1000,hash);
                if(old) { pair.end(); pair.begin(); }
                var r=pair.run(b,game,1,-50,50);
                assertEquals(!old && storedDepth==1 ? (type==2 ? -1000 : 1000) : 37,r.score());
                pair.end();
            }
        for(int[] bound : new int[][] {{0,7,7},{1,50,50},{2,-50,-50},{1,49,37},{2,-49,37}}) {
            var pair=new Pair((board,p)->-37, localLeaves); pair.begin();
            pair.seed(key,1,bound[0],bound[1],hash);
            assertEquals(bound[2],pair.run(b,game,1,-50,50).score()); pair.end();
        }
        for(long move : new long[] {0,Long.MAX_VALUE,hash}) {
            var pair = new Pair((board,p)->7, localLeaves); pair.begin();
            pair.seed(key,0,0,999,move);
            assertEquals(7,pair.run(b,game,0,-50,50).score());
            assertTrue(pair.left.events.stream().noneMatch(e -> e.kind==2 || e.kind==3));
            pair.end();
            pair = new Pair((board,p)->0, localLeaves); pair.begin();
            pair.seed(key,1,0,7,move);
            assertEquals(move==hash ? 7 : 0,pair.run(b,game,1,-50,50).score()); pair.end();
        }
        var pair = new Pair(HCE, localLeaves); pair.begin();
        var first=pair.run(b,game,3,-INFINITY,INFINITY);
        assertEquals(1,pair.run(b,game,3,-INFINITY,INFINITY).nodes()); pair.end();
        for(int i=0;i<256;i++) { pair.begin(); pair.end(); }
        assertEquals(first.score(),pair.run(b,game,3,-INFINITY,INFINITY).score());
    }

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void everyLegalHashIsFirstAndUniqueAcrossTacticalQuietPromotionAndEpClasses(boolean localLeaves) throws Exception {
        int compared=0;
        for(String fen : ExactSearchMechanicsTest.fens()) {
            long[] b=Board.fromFen(fen); var game=GameHistory.initial(b);
            var pair=new Pair((board,p)->0, localLeaves);
            for(long move : ExhaustiveOracle.legalMoves(b)) {
                pair.clear(); pair.begin();
                pair.seed(ExactSearchTTableTest.key(b,game),2,0,999,move);
                pair.run(b,game,1,-INFINITY,INFINITY); pair.end();
                var visits=pair.left.events.stream().filter(e->e.kind==0 && e.c==0).toList();
                assertEquals(ExhaustiveOracle.child(b,move)[Board.KEY],visits.getFirst().b);
                assertEquals(visits.size(),visits.stream().map(e->e.b).distinct().count());
                compared++;
            }
        }
        assertTrue(compared>300);
        System.out.println("SR-002 legal hash first/unique comparisons="+compared);
    }

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void terminalsRule50RepetitionAndHistorySensitiveEvidence(boolean localLeaves) throws Exception {
        for(String fen : List.of("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
                "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1","4k3/8/8/8/8/8/8/4K3 w - - 0 1",
                "4k3/8/8/8/8/8/8/R3K3 w - - 100 1")) {
            long[] b=Board.fromFen(fen); var game=GameHistory.initial(b);
            for(int depth : new int[] {0,3,MAX_DEPTH}) {
                var pair=new Pair((board,p)->{throw new AssertionError("Terminal evaluation");}, localLeaves); pair.begin();
                pair.seed(ExactSearchTTableTest.key(b,game),Math.min(depth,255),0,12345,0);
                var r=pair.run(b,game,depth,-INFINITY,INFINITY);
                assertEquals(fen.startsWith("7k/6Q1") ? -MATE_SCORE : 0,r.score());
                assertEquals(1,r.nodes()); pair.end();
            }
        }
        long[] b=Board.startingPosition(); var builder=GameHistory.builder(b);
        var pair=new Pair(HCE, localLeaves); pair.begin();
        for(int i=0;i<9;i++) {
            var game=builder.snapshot();
            var r=pair.run(b,game,3,-INFINITY,INFINITY);
            assertEquals(new ExactSearch(HCE).search(b,game,3,NEVER_CANCELLED).score(),r.score());
            pair.run(b,GameHistory.initial(b),3,-INFINITY,INFINITY);
            if(i<8) { b=ExactSearchTTableTest.play(b,new String[]{"g1f3","g8f6","f3g1","f6g8"}[i%4]); builder.appendPosition(b); }
        }
        pair.end();
        pair=new Pair((board,p)->-37, localLeaves); pair.begin();
        for(int clock : new int[] {0,99,100}) {
            b=Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - "+clock+" 1");
            assertEquals(clock==0 ? 37 : 0,pair.run(b,GameHistory.initial(b),1,-INFINITY,INFINITY).score());
        }
        pair.end();
    }

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void narrowWindowsAndMateDistanceReuseRemainFailSoft(boolean localLeaves) throws Exception {
        for(String fen : List.of(ExactSearchHarness.orderingPositions().get(0).fen(),
                "7k/8/5KQ1/8/8/8/8/8 w - - 0 1","6k1/8/5K2/8/8/8/8/Q7 b - - 0 1")) {
            long[] b=Board.fromFen(fen); var game=GameHistory.initial(b);
            int exact=new ExactSearch(HCE).search(b,4).score();
            var pair=new Pair(HCE, localLeaves); pair.begin();
            for(int[] w : new int[][] {{-INFINITY,INFINITY},{exact-1,exact},{exact,exact+1},
                    {exact-1,exact+1},{-32513,-32512},{32512,32513}}) {
                var r=pair.run(b,game,4,w[0],w[1]);
                PvsSearchTest.bound(exact,r.score(),w[0],w[1]);
                ExactSearchTTableTest.assertLegalPv(b,r);
            }
            pair.end();
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void cancellationAtEveryCheckpointInSmallTreeAndReuseMatch(boolean localLeaves) throws Exception {
        long[] b=Board.fromFen(ExactSearchHarness.orderingPositions().getLast().fen());
        var game=GameHistory.initial(b); var pair=new Pair(HCE, localLeaves);
        pair.run(b,game,2,-INFINITY,INFINITY);
        int checkpoints=pair.left.checkpoints;
        for(int limit=0;limit<=checkpoints;limit++) {
            pair.clear(); pair.limit=limit;
            var r=pair.run(b,game,2,-INFINITY,INFINITY);
            assertEquals(limit==checkpoints,r.completed());
            if(!r.completed()) {
                assertEquals(Value.INVALID,r.score()); assertEquals(0,r.principalVariation().length);
                assertTrue(Arrays.stream(history(pair.flat)).allMatch(v->v==0));
                assertFalse(pair.flatTable.probe(ExactSearchTTableTest.key(b,game),new TTable.TEntry()));
            }
        }
        pair.limit=Integer.MAX_VALUE; pair.clear();
        assertTrue(pair.run(b,game,3,-INFINITY,INFINITY).completed());
        Thread.currentThread().interrupt();
        try { assertFalse(pair.run(b,game,3,-INFINITY,INFINITY).completed()); }
        finally { Thread.interrupted(); }
        System.out.println("SR-002 cancellation checkpoints="+checkpoints);
    }

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void cancellationRaisedByScoutResearchOrFinalEvaluationDiscardsIncompleteEvidence(boolean localLeaves) throws Exception {
        long[] b=Board.startingPosition(), ordered=PvsSearchTest.ordered(b);
        long improving=ExhaustiveOracle.child(b,ordered[1])[Board.KEY];
        for(int stop : new int[] {1,2,3,21}) {
            var pair=new Pair((board,p)->board[Board.KEY]==improving ? -20 : 0, localLeaves);
            pair.stopEvaluation=stop;
            assertFalse(pair.run(b,GameHistory.initial(b),1,-INFINITY,INFINITY).completed());
            assertTrue(Arrays.stream(history(pair.flat)).allMatch(v->v==0));
            assertFalse(pair.flatTable.probe(ExactSearchTTableTest.key(b,GameHistory.initial(b)),new TTable.TEntry()));
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false,true})
    void incrementalEvaluatorSlotsAndNonHceValuesRemainIndependent(boolean localLeaves) throws Exception {
        var network=NnueNetwork.initialized(73);
        var left=ExactEvaluator.from(SearchEvaluation.incremental(network));
        var right=ExactEvaluator.from(SearchEvaluation.incremental(network));
        var pair=new Pair(left,right, localLeaves);
        for(var position : ExactSearchHarness.orderingPositions().subList(0,3)) {
            long[] b=Board.fromFen(position.fen());
            var r=pair.run(b,GameHistory.initial(b),2,-INFINITY,INFINITY);
            assertEquals(new ExactSearch(ExactEvaluator.from(SearchEvaluation.incremental(network))).search(b,2).score(),r.score());
        }
    }

    @Test void boundariesExceptionsAndInvocationOwnershipAreReusable() {
        var flat=new FlatExactSearch(HCE,new TTable(1)); long[] b=Board.startingPosition();
        assertThrows(NullPointerException.class,()->new FlatExactSearch(HCE,null));
        for(int depth : new int[] {-1,MAX_DEPTH+1}) assertThrows(IllegalArgumentException.class,()->flat.search(b,depth));
        for(int[] w : new int[][] {{0,0},{1,0},{Integer.MIN_VALUE,0},{0,Integer.MAX_VALUE}})
            assertThrows(IllegalArgumentException.class,()->flat.searchWindow(b,GameHistory.initial(b),1,w[0],w[1],NEVER_CANCELLED));
        flat.beginRequest();
        assertThrows(IllegalStateException.class,flat::beginRequest);
        assertThrows(IllegalStateException.class,flat::newGame); flat.endRequest();
        assertThrows(IllegalStateException.class,flat::endRequest); flat.newGame();
        var fail=new AtomicBoolean(true);
        var throwing=new FlatExactSearch((board,p)->{
            if(fail.getAndSet(false)) throw new IllegalStateException("test");
            return 37;
        },new TTable(1));
        assertThrows(IllegalStateException.class,()->throwing.search(b,2));
        assertTrue(throwing.search(b,2).completed());
        for(int score : new int[] {-32512,32512,Integer.MIN_VALUE,Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->new FlatExactSearch((board,p)->score,new TTable(1)).search(b,0));
    }

    static int[] history(Object search) throws Exception {
        var field=search.getClass().getDeclaredField("quietHistory"); field.setAccessible(true);
        return (int[])field.get(search);
    }

    record Event(int kind,long a,long b,long c,long d,long e) {}
    static final class Trace implements ExactEvaluator {
        final ExactEvaluator delegate;
        final List<Event> events=new ArrayList<>();
        final long[] slots=new long[MAX_DEPTH+1];
        int evaluations,checkpoints,stopEvaluation;
        boolean stop;
        Trace(ExactEvaluator delegate) { this.delegate=delegate; }
        void reset(int stopEvaluation) { events.clear(); evaluations=checkpoints=0; stop=false; this.stopEvaluation=stopEvaluation; }
        public void initialize(long[] b) { slots[0]=b[Board.KEY]; delegate.initialize(b); }
        public void child(long[] parent,long[] child,int ply) {
            assertEquals(slots[ply],parent[Board.KEY]); slots[ply+1]=child[Board.KEY];
            events.add(new Event(0,parent[Board.KEY],child[Board.KEY],ply,parent[Board.STATUS],child[Board.STATUS]));
            delegate.child(parent,child,ply);
        }
        public int evaluate(long[] b,int ply) {
            assertEquals(slots[ply],b[Board.KEY]);
            int value=delegate.evaluate(b,ply);
            events.add(new Event(1,b[Board.KEY],ply,value,b[Board.STATUS],0));
            if(++evaluations==stopEvaluation) stop=true;
            return value;
        }
        BooleanSupplier cancellation(int limit) { return () -> ++checkpoints>limit || stop; }
    }
    static final class TracedTable extends TTable {
        final Trace trace;
        TracedTable(Trace trace) { super(4); this.trace=trace; }
        @Override public boolean probe(long key,TEntry entry) {
            boolean hit=super.probe(key,entry);
            trace.events.add(new Event(2,key,hit?1:0,hit?entry.data:0,hit?entry.hashMove:0,0)); return hit;
        }
        @Override public void save(long key,int depth,int type,int score,long move) {
            trace.events.add(new Event(3,key,depth,type,score,move)); super.save(key,depth,type,score,move);
        }
    }
    static final class Pair {
        final Trace left,right;
        final TracedTable recursiveTable,flatTable;
        final ExactSearch recursive;
        final FlatExactSearch flat;
        int limit=Integer.MAX_VALUE,stopEvaluation=-1;
        Pair(ExactEvaluator eval, boolean localLeaves) { this(eval,eval,localLeaves); }
        Pair(ExactEvaluator a,ExactEvaluator b, boolean localLeaves) {
            left=new Trace(a); right=new Trace(b);
            recursiveTable=new TracedTable(left); flatTable=new TracedTable(right);
            recursive=new ExactSearch(left,recursiveTable); flat=new FlatExactSearch(right,flatTable,localLeaves);
        }
        void begin() { recursive.beginRequest(); flat.beginRequest(); }
        void end() { recursive.endRequest(); flat.endRequest(); }
        void clear() { recursiveTable.clear(); flatTable.clear(); }
        void seed(long k,int d,int t,int s,long m) { recursiveTable.save(k,d,t,s,m); flatTable.save(k,d,t,s,m); }
        ExactSearchResult run(long[] b,GameHistory game,int depth,int alpha,int beta) throws Exception {
            left.reset(stopEvaluation); right.reset(stopEvaluation); long[] before=b.clone();
            var a=recursive.searchWindow(b,game,depth,alpha,beta,left.cancellation(limit));
            var f=flat.searchWindow(b,game,depth,alpha,beta,right.cancellation(limit));
            assertEquals(a.completed(),f.completed()); assertEquals(a.score(),f.score());
            assertEquals(a.bestMove(),f.bestMove()); assertEquals(a.nodes(),f.nodes());
            assertArrayEquals(a.principalVariation(),f.principalVariation()); assertArrayEquals(before,b);
            assertEquals(left.checkpoints,right.checkpoints);
            assertEquals(left.events.size(),right.events.size());
            for(int i=0;i<left.events.size();i++) assertEquals(left.events.get(i),right.events.get(i),"event "+i);
            assertArrayEquals(history(recursive),history(flat));
            return f;
        }
    }
}
