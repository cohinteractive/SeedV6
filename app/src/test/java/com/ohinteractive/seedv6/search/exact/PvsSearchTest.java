package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

class PvsSearchTest {
    static final int POLICY = ExactSearch.SEE_MATERIAL_QUIET_HISTORY;
    static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);

    static ExactSearch search(ExactEvaluator eval, TTable table, int traversal) {
        return new ExactSearch(eval, table, POLICY, ExactSearch.STAGED_LAZY, ExactSearch.SORT_CROSSOVER, traversal);
    }

    @Test void referencePreservesRecordedDepthFiveResultsAndPvsMatchesExactLines() {
        long[][] recorded = {{31418,83934,4861,20832,119270,1563}, {27911,65481,4280,19925,103563,1532}};
        int[] scores = {197,403,1177,-81,250,6};
        String[] lines = {"e2e3 e7e5 d1f3 g8e7 f3f7", "d5e6 e7e6 e2a6 h3g2 f3f6",
                "e4d5 e6d5 d3g6 e8d8 g6c6", "c4c5 a3b4 c5b6 b2a1q d1a1",
                "c3d5 e7d7 g5f6 g7f6 f3e5", "e1d2 e8d7 d2c3 d7d6 c3d4"};
        for(int tt = 0; tt < 2; tt++) for(int i = 0; i < 6; i++) {
            long[] b = Board.fromFen(ExactSearchHarness.orderingPositions().get(i).fen());
            var a = search(HCE, tt == 0 ? null : new TTable(4), ExactSearch.ORDERED_ALPHA_BETA).search(b, 5);
            var p = search(HCE, tt == 0 ? null : new TTable(4), ExactSearch.PVS).search(b, 5);
            assertEquals(recorded[tt][i], a.nodes()); assertEquals(scores[i], a.score());
            assertEquals(lines[i], text(a.principalVariation()));
            assertEquals(a.score(), p.score()); assertEquals(a.bestMove(), p.bestMove());
            assertArrayEquals(a.principalVariation(), p.principalVariation());
            assertExactPrefixes(b, GameHistory.initial(b), 5, p, HCE);
        }
        long[] b = Board.startingPosition();
        StagedSearchTest.same(new ExactSearch(HCE, null, POLICY, ExactSearch.STAGED_LAZY).search(b, 4),
                search(HCE, null, ExactSearch.ORDERED_ALPHA_BETA).search(b, 4));
        assertThrows(IllegalArgumentException.class, () -> search(HCE, null, 2));
    }

    @Test void exhaustiveValuesSpecialMovesTerminalLeavesAndDeterministicTies() {
        int comparisons = 0;
        for(String fen : StagedGenerationTest.fens()) {
            long[] b = Board.fromFen(fen), before = b.clone();
            var game = GameHistory.initial(b);
            for(int depth = 0; depth <= 2; depth++) {
                int exact = new ExhaustiveOracle(HCE).score(b, new SearchLineHistory(game), depth, 0);
                for(boolean tt : new boolean[] {false, true}) {
                    var p = search(HCE, tt ? new TTable(1) : null, ExactSearch.PVS).search(b, depth);
                    var a = search(HCE, tt ? new TTable(1) : null, ExactSearch.ORDERED_ALPHA_BETA).search(b, depth);
                    assertEquals(exact, p.score(), fen + " depth=" + depth);
                    assertEquals(a.bestMove(), p.bestMove(), fen);
                    ExactSearchTTableTest.assertOptimalAndPv(b, game, depth, p, HCE);
                    assertArrayEquals(before, b); comparisons++;
                }
            }
        }
        assertTrue(comparisons >= 120);
    }

    @Test void firstFullLaterFailLowEqualityAtAlphaAndMultipleImprovements() {
        for(boolean tt : new boolean[] {false, true}) {
            Fixture low = new Fixture(-10, -30, -10);
            var result = low.run(-100, 100, tt);
            assertEquals(-10, result.score()); assertEquals(low.moves[0], result.bestMove());
            low.calls(1, 1, 1);
            Fixture equality = new Fixture(0, 5, -10);
            result = equality.run(5, 100, tt);
            assertEquals(5, result.score()); // fail-soft upper bound at original alpha
            assertEquals(equality.moves[0], result.bestMove(), "Fail-low scout must not replace the discovered PV");
            equality.calls(1, 1, 1);
            Fixture improving = new Fixture(-10, -5, 0, 5);
            result = improving.run(-100, 100, tt);
            assertEquals(5, result.score()); assertEquals(improving.moves[3], result.bestMove());
            improving.calls(1, 2, 2, 2);
            Fixture firstCut = new Fixture(150, 0);
            assertEquals(150, firstCut.run(-100, 100, tt).score());
            firstCut.calls(1, 0);
        }
    }

    @Test void scoutBetaEqualityAndFailSoftOvershootNeverResearch() {
        for(boolean tt : new boolean[] {false, true}) for(int value : new int[] {50, 83}) {
            Fixture f = new Fixture(0, value, -10);
            var result = f.run(-100, 50, tt);
            assertEquals(value, result.score()); assertEquals(f.moves[1], result.bestMove());
            assertArrayEquals(new long[] {f.moves[1]}, result.principalVariation());
            f.calls(1, 1, 0);
        }
    }

    @Test void integerExtremesNarrowWindowsAndMateBandsPreserveBounds() {
        for(boolean tt : new boolean[] {false, true}) {
            for(int value : new int[] {-ExactSearch.MAX_STATIC_SCORE, 0, ExactSearch.MAX_STATIC_SCORE}) {
                Fixture f = new Fixture(value, value);
                for(int[] w : new int[][] {{-ExactSearch.INFINITY,ExactSearch.INFINITY},
                        {-ExactSearch.INFINITY,-ExactSearch.INFINITY+1}, {ExactSearch.INFINITY-1,ExactSearch.INFINITY},
                        {value-1,value}, {value,value+1}, {value-1,value+1}}) {
                    assertEquals(value, f.run(w[0], w[1], tt).score());
                }
            }
            for(String fen : List.of("7k/8/5KQ1/8/8/8/8/8 w - - 0 1", "6k1/8/5K2/8/8/8/8/Q7 b - - 0 1")) {
                long[] b = Board.fromFen(fen); var game = GameHistory.initial(b);
                int exact = search(HCE, null, ExactSearch.ORDERED_ALPHA_BETA).search(b, 4).score();
                for(int[] w : new int[][] {{-ExactSearch.INFINITY,ExactSearch.INFINITY},
                        {exact-1,exact}, {exact,exact+1}, {exact-1,exact+1},
                        {-32513,-32512}, {32512,32513}}) {
                    var result = search(HCE, tt ? new TTable(1) : null, ExactSearch.PVS)
                            .searchWindow(b, game, 4, w[0], w[1], ExactSearch.NEVER_CANCELLED);
                    bound(exact, result.score(), w[0], w[1]);
                    ExactSearchTTableTest.assertLegalPv(b, result);
                    if(exact > w[0] && exact < w[1]) assertExactPrefixes(b, game, 4, result, HCE);
                }
            }
        }
        var pvs = search(HCE, null, ExactSearch.PVS); long[] b = Board.startingPosition(); var game = GameHistory.initial(b);
        for(int[] w : new int[][] {{Integer.MIN_VALUE,0}, {0,Integer.MAX_VALUE}, {0,0}, {1,0}})
            assertThrows(IllegalArgumentException.class, () -> pvs.searchWindow(b, game, 1, w[0], w[1], ExactSearch.NEVER_CANCELLED));
    }

    @Test void scoutTtBoundsAreNotExactAndWiderResearchRevisitsTheSameChild() {
        // Depth two: root move values are board-derived, with every reply equal.
        // The later child must store UPPER in its scout, then EXACT on full re-search.
        for(int later : new int[] {-20, 0, 20, 50, 83}) {
            long[] b = Board.startingPosition(), legal = ordered(b);
            Map<Long,Integer> values = new HashMap<>();
            for(int i = 0; i < legal.length; i++) for(long reply : ExhaustiveOracle.legalMoves(ExhaustiveOracle.child(b, legal[i]))) {
                long[] leaf = ExhaustiveOracle.child(ExhaustiveOracle.child(b, legal[i]), reply);
                int v = i == 0 ? 0 : i == 1 ? later : -30;
                Integer old = values.putIfAbsent(leaf[Board.KEY], v);
                assertTrue(old == null || old == v, "Fixture must be a board-only evaluator");
            }
            long[] first = ExhaustiveOracle.child(b, legal[0]), second = ExhaustiveOracle.child(b, legal[1]);
            var game = GameHistory.initial(b);
            long firstKey = ExactSearchTTableTest.key(first, GameHistory.builder(game).appendPosition(first).snapshot());
            long secondKey = ExactSearchTTableTest.key(second, GameHistory.builder(game).appendPosition(second).snapshot());
            record Save(int depth, int type, int score) {}
            var firstWrites = new ArrayList<Save>(); var secondWrites = new ArrayList<Save>();
            var table = new TTable(4) {
                @Override public void save(long key, int depth, int type, int score, long move) {
                    if(key == firstKey) firstWrites.add(new Save(depth,type,score));
                    if(key == secondKey) secondWrites.add(new Save(depth,type,score));
                    super.save(key,depth,type,score,move);
                }
            };
            ExactEvaluator evaluator = (board, ply) -> Objects.requireNonNull(values.get(board[Board.KEY]));
            var p = search(evaluator, table, ExactSearch.PVS);
            var result = p.searchWindow(b, game, 2, -100, 50, ExactSearch.NEVER_CANCELLED);
            assertEquals(Math.max(0,later), result.score());
            assertEquals(new Save(1,TTable.TYPE_EXACT,0), firstWrites.getFirst(), "First child receives [-50,100]");
            assertEquals(new Save(1,later <= 0 ? TTable.TYPE_LOWER : TTable.TYPE_UPPER,-later), secondWrites.getFirst(),
                    "Later child receives [-1,0]; classification uses that original window");
            assertEquals(later > 0 && later < 50 ? 2 : 1, secondWrites.size());
            if(secondWrites.size() == 2) assertEquals(new Save(1,TTable.TYPE_EXACT,-later), secondWrites.getLast());
            assertEquals(search(evaluator, null, ExactSearch.ORDERED_ALPHA_BETA)
                    .searchWindow(b,game,2,-100,50,ExactSearch.NEVER_CANCELLED).score(), result.score());
        }
    }

    @Test void currentDepthGenerationCutoffOnlyTtAndNoLeafParticipationRemainIntact() {
        long[] b = Board.startingPosition(); var game = GameHistory.initial(b);
        long hash = ordered(b)[0], key = ExactSearchTTableTest.key(b,game);
        for(int type : new int[] {TTable.TYPE_EXACT,TTable.TYPE_LOWER,TTable.TYPE_UPPER}) {
            for(int depth : new int[] {1,3}) {
                var table = new TTable(1); var p = search((board,ply)->0,table,ExactSearch.PVS); p.beginRequest();
                table.save(key,depth,type,type == TTable.TYPE_UPPER ? -1000 : 1000,hash);
                assertEquals(0,p.search(b,2).score()); p.endRequest();
            }
            var table = new TTable(1); var p = search((board,ply)->0,table,ExactSearch.PVS); p.beginRequest();
            table.save(key,2,type,type == TTable.TYPE_UPPER ? -1000 : 1000,hash); p.endRequest(); p.beginRequest();
            assertEquals(0,p.search(b,2).score()); p.endRequest();
        }
        for(int type : new int[] {TTable.TYPE_LOWER,TTable.TYPE_UPPER}) {
            var table = new TTable(1); var p = search((board,ply)->0,table,ExactSearch.PVS); p.beginRequest();
            int score = type == TTable.TYPE_LOWER ? -5 : 5;
            table.save(key,2,type,score,hash);
            assertEquals(0,p.searchWindow(b,game,2,-10,10,ExactSearch.NEVER_CANCELLED).score()); p.endRequest();
        }
        var table = new TTable(1) {
            @Override public boolean probe(long k,TEntry e) { throw new AssertionError("Leaf probe"); }
            @Override public void save(long k,int d,int t,int s,long m) { throw new AssertionError("Leaf store"); }
        };
        assertEquals(7, search((board,ply)->7,table,ExactSearch.PVS).search(b,0).score());
    }

    @Test void terminalDrawCancellationHistoryAndEvaluatorSlotsRemainReusable() throws Exception {
        for(boolean tt : new boolean[] {false,true}) {
            for(String fen : List.of("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1", "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
                    "4k3/8/8/8/8/8/8/4K3 w - - 0 1", "4k3/8/8/8/8/8/8/R3K3 w - - 100 1")) {
                var p = search((board,ply)->{ throw new AssertionError("Terminal evaluation"); },tt ? new TTable(1) : null,ExactSearch.PVS);
                for(int d : new int[] {0,3}) {
                    var r = p.search(Board.fromFen(fen),d);
                    assertEquals(fen.startsWith("7k/6Q1") ? -32768 : 0,r.score()); assertEquals(1,r.nodes());
                    assertFalse(p.search(Board.fromFen(fen),GameHistory.initial(Board.fromFen(fen)),d,()->true).completed());
                }
            }
            long[] b = Board.startingPosition(); var builder = GameHistory.builder(b);
            for(int cycle=0;cycle<2;cycle++) for(String m : List.of("g1f3","g8f6","f3g1","f6g8")) {
                b=ExactSearchTTableTest.play(b,m); builder.appendPosition(b);
            }
            assertEquals(0,search(HCE,tt ? new TTable(1) : null,ExactSearch.PVS)
                    .search(b,builder.snapshot(),4,ExactSearch.NEVER_CANCELLED).score());
            b=Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen()); var game=GameHistory.initial(b);
            for(int limit : new int[] {0,40,400,4000}) {
                var calls=new AtomicInteger(); var table=tt ? new TTable(1) : null;
                var p=search(HCE,table,ExactSearch.PVS);
                var r=p.search(b,game,5,()->calls.incrementAndGet()>limit);
                assertFalse(r.completed()); assertEquals(Value.INVALID,r.score()); assertEquals(0,r.bestMove());
                assertEquals(0,r.principalVariation().length);
                assertTrue(Arrays.stream(StagedSearchTest.history(p)).allMatch(v->v==0));
                if(table!=null) { assertFalse(table.probe(ExactSearchTTableTest.key(b,game),new TTable.TEntry())); table.clear(); }
                var fresh=search(HCE,tt ? new TTable(1) : null,ExactSearch.PVS).search(b,3);
                StagedSearchTest.same(fresh,p.search(b,3));
            }
            var keys = new long[ExactSearch.MAX_DEPTH+1];
            var p=search(new ExactEvaluator() {
                public void initialize(long[] board) { keys[0]=board[Board.KEY]; }
                public void child(long[] parent,long[] child,int ply) { assertEquals(keys[ply],parent[Board.KEY]); keys[ply+1]=child[Board.KEY]; }
                public int evaluate(long[] board,int ply) { assertEquals(keys[ply],board[Board.KEY]); return Eval.evaluate(board); }
            },tt ? new TTable(1) : null,ExactSearch.PVS);
            assertEquals(search(HCE,null,ExactSearch.ORDERED_ALPHA_BETA).search(b,4).score(),p.search(b,4).score());
            Thread.currentThread().interrupt();
            try { assertFalse(p.search(b,1).completed()); assertTrue(Thread.currentThread().isInterrupted()); }
            finally { Thread.interrupted(); }
        }
    }

    @Test void cancellationDuringScoutOrResearchCannotPublishRootEvidence() throws Exception {
        for(boolean tt : new boolean[] {false,true}) for(int stopAt : new int[] {2,3}) {
            long[] b=Board.startingPosition(), moves=ordered(b); var calls=new AtomicInteger(); var stop=new AtomicBoolean();
            long improvingKey=ExhaustiveOracle.child(b,moves[1])[Board.KEY];
            var table=tt ? new TTable(1) : null;
            var p=search((board,ply)->{
                int call=calls.incrementAndGet(); if(call==stopAt) stop.set(true);
                return board[Board.KEY]==improvingKey ? -20 : 0;
            },table,ExactSearch.PVS);
            var game=GameHistory.initial(b);
            var result=p.search(b,game,1,stop::get);
            assertFalse(result.completed()); assertEquals(stopAt,calls.get());
            assertEquals(Value.INVALID,result.score()); assertEquals(0,result.principalVariation().length);
            assertTrue(Arrays.stream(StagedSearchTest.history(p)).allMatch(v->v==0));
            if(table!=null) assertFalse(table.probe(ExactSearchTTableTest.key(b,game),new TTable.TEntry()));
        }
    }

    @Test void completedQuietCutoffLearnsOnceAndIndependentInvocationsResetHistory() throws Exception {
        long[] b=Board.startingPosition(), moves=ordered(b); var game=GameHistory.initial(b);
        for(boolean tt : new boolean[] {false,true}) {
            var table=tt ? new TTable(1) : null;
            var p=search((board,ply)->0,table,ExactSearch.PVS);
            for(int repeat=0;repeat<2;repeat++) {
                if(table!=null) table.clear();
                var r=p.searchWindow(b,game,1,-100,-50,ExactSearch.NEVER_CANCELLED);
                assertEquals(moves[0],r.bestMove());
                int[] learned=StagedSearchTest.history(p);
                assertEquals(1,learned[QuietHistory.index(moves[0])]);
                assertEquals(1,Arrays.stream(learned).filter(v->v!=0).count());
            }
        }
    }

    @Test void abortedInteriorScoutAndResearchNeverStoreCompletedEvidence() throws Exception {
        long[] b=Board.startingPosition(), second=ExhaustiveOracle.child(b,ordered(b)[1]);
        var game=GameHistory.initial(b);
        long key=ExactSearchTTableTest.key(second,GameHistory.builder(game).appendPosition(second).snapshot());
        var leaves=new HashSet<Long>();
        for(long reply : ExhaustiveOracle.legalMoves(second)) leaves.add(ExhaustiveOracle.child(second,reply)[Board.KEY]);
        for(int stopAt : new int[] {1,leaves.size()+1}) {
            var writes=new ArrayList<Integer>(); var stop=new AtomicBoolean(); var visits=new AtomicInteger();
            var table=new TTable(4) {
                @Override public void save(long k,int d,int type,int score,long move) {
                    if(k==key) writes.add(type);
                    super.save(k,d,type,score,move);
                }
            };
            var p=search((board,ply)->{
                if(leaves.contains(board[Board.KEY])) {
                    if(visits.incrementAndGet()==stopAt) stop.set(true);
                    return 20;
                }
                return 0;
            },table,ExactSearch.PVS);
            var result=p.searchWindow(b,game,2,-100,100,stop::get);
            assertFalse(result.completed()); assertEquals(stopAt,visits.get());
            assertFalse(table.probe(ExactSearchTTableTest.key(b,game),new TTable.TEntry()));
            // A completed scout may remain; its cancelled full re-search must not store EXACT.
            assertEquals(stopAt==1 ? List.of() : List.of(TTable.TYPE_UPPER),writes);
            assertTrue(Arrays.stream(StagedSearchTest.history(p)).allMatch(v->v==0));
        }
    }

    @Test void firstSearchedMoveIsTheLegalHashAcrossTacticalAndQuietClasses() {
        boolean good=false,bad=false,quiet=false;
        for(String fen : ExactSearchMechanicsTest.fens()) {
            long[] b=Board.fromFen(fen); var game=GameHistory.initial(b);
            int ep=Board.enPassantSquare((int)b[Board.STATUS]);
            for(long move : ExhaustiveOracle.legalMoves(b)) {
                boolean tactical=CaptureHistory.isTactical(move,ep);
                if(!tactical) quiet=true;
                else if(com.ohinteractive.seedv6.core.See.atLeastGeneratedLegal(b,move,0)) good=true;
                else bad=true;
                var table=new TTable(1); var visits=new ArrayList<Long>();
                var p=search(new ExactEvaluator() {
                    public int evaluate(long[] board,int ply) { return 0; }
                    public void child(long[] parent,long[] child,int ply) { if(ply==0) visits.add(child[Board.KEY]); }
                },table,ExactSearch.PVS);
                p.beginRequest();
                // Inapplicable depth supplies ordering only, never a score.
                table.save(ExactSearchTTableTest.key(b,game),2,TTable.TYPE_EXACT,999,move);
                var r=p.search(b,1); p.endRequest();
                assertEquals(ExhaustiveOracle.child(b,move)[Board.KEY],visits.getFirst());
                // Some high-branching artificial boards have immediate mates;
                // equal static ties retain the first move on non-mating fixtures.
                if(r.score()==0) assertEquals(move,r.bestMove());
                assertEquals(visits.size(),new HashSet<>(visits).size());
            }
        }
        assertTrue(good && bad && quiet);
    }

    static void bound(int exact,int returned,int alpha,int beta) {
        if(returned<=alpha) assertTrue(exact<=returned, "Upper bound is unsound");
        else if(returned>=beta) assertTrue(exact>=returned, "Lower bound is unsound");
        else assertEquals(exact,returned);
    }
    static String text(long[] pv) { return String.join(" ",Arrays.stream(pv).mapToObj(Move::coordinate).toList()); }
    static long[] ordered(long[] b) {
        Map<Long,Long> byKey=new HashMap<>();
        for(long move : ExhaustiveOracle.legalMoves(b)) byKey.put(ExhaustiveOracle.child(b,move)[Board.KEY],move);
        var result=new ArrayList<Long>();
        search(new ExactEvaluator() {
            public int evaluate(long[] board,int ply) { return 0; }
            public void child(long[] parent,long[] child,int ply) { if(ply==0) result.add(byKey.get(child[Board.KEY])); }
        },null,ExactSearch.ORDERED_ALPHA_BETA).search(b,1);
        return result.stream().mapToLong(Long::longValue).toArray();
    }
    static void assertExactPrefixes(long[] b,GameHistory game,int depth,ExactSearchResult r,ExactEvaluator eval) {
        ExactSearchTTableTest.assertLegalPv(b,r);
        var history=GameHistory.builder(game); int ply=0;
        for(long move : r.principalVariation()) {
            b=ExhaustiveOracle.child(b,move); history.appendPosition(b); ply++;
            int value=new ExactSearch(eval).search(b,history.snapshot(),depth-ply,ExactSearch.NEVER_CANCELLED).score();
            if(value>=TranspositionScores.MATE_THRESHOLD) value-=ply;
            else if(value<=-TranspositionScores.MATE_THRESHOLD) value+=ply;
            assertEquals(r.score(),ply%2==0 ? value : -value);
        }
    }
    static final class Fixture {
        final long[] board=Board.startingPosition(), moves=ordered(board);
        final Map<Long,Integer> values=new HashMap<>(), visits=new HashMap<>();
        Fixture(int... gains) {
            for(int i=0;i<moves.length;i++) values.put(ExhaustiveOracle.child(board,moves[i])[Board.KEY],
                    -(i<gains.length ? gains[i] : -ExactSearch.MAX_STATIC_SCORE));
        }
        ExactSearchResult run(int alpha,int beta,boolean tt) {
            visits.clear();
            return search((b,p)->{ visits.merge(b[Board.KEY],1,Integer::sum); return values.get(b[Board.KEY]); },
                    tt ? new TTable(1) : null,ExactSearch.PVS)
                    .searchWindow(board,GameHistory.initial(board),1,alpha,beta,ExactSearch.NEVER_CANCELLED);
        }
        void calls(int... counts) {
            for(int i=0;i<counts.length;i++) assertEquals(counts[i],visits.getOrDefault(
                    ExhaustiveOracle.child(board,moves[i])[Board.KEY],0));
        }
    }
}
