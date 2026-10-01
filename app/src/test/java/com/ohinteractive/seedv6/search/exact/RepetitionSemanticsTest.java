package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.InsufficientMaterial;
import com.ohinteractive.seedv6.rules.PositionIdentity;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

/** SR-010: graph history is value state, not a twofold cycle heuristic. */
class RepetitionSemanticsTest {
    private static final String[] CYCLE = {"g1f3", "g8f6", "f3g1", "f6g8"};
    private record Line(long[] board, GameHistory game, List<long[]> positions) {}

    private static Line cycle(int plies, int start) {
        List<long[]> positions = new ArrayList<>();
        long[] b = Board.startingPosition(); positions.add(b);
        for(int i=0;i<plies;i++) { b=play(b,CYCLE[i%4]); positions.add(b); }
        var builder=GameHistory.builder(positions.get(start));
        for(int i=start+1;i<positions.size();i++) builder.appendPosition(positions.get(i));
        return new Line(b,builder.snapshot(),positions);
    }

    @Test void legalSuffixWithSameCurrentCountCanHaveADifferentFutureDrawValue() {
        var full=cycle(7,0); var suffix=cycle(7,3);
        assertArrayEquals(full.board,suffix.board);
        assertEquals(2,full.game.currentOccurrences(full.board));
        assertEquals(2,suffix.game.currentOccurrences(suffix.board));
        long[] next=play(full.board,"f6g8");
        assertEquals(2,full.game.occurrences(PositionIdentity.repetitionKey(next)));
        assertEquals(1,suffix.game.occurrences(PositionIdentity.repetitionKey(next)));
        assertNotEquals(key(full),key(suffix));
        var table=new TTable(1); var on=new ExactSearch((b,p)->37,table);on.beginRequest();
        for(var line:List.of(full,suffix,full)) {
            int expected=line==suffix?-37:0;
            assertEquals(expected,oracle(line,1,(b,p)->37));
            assertEquals(expected,new ExactSearch((b,p)->37).search(line.board,line.game,1,ExactSearch.NEVER_CANCELLED).score());
            assertEquals(expected,on.search(line.board,line.game,1,ExactSearch.NEVER_CANCELLED).score());
        }
        on.endRequest();
    }

    @Test void firstAndSecondOccurrencesRemainStaticButThirdIsFormalDraw() {
        for(int plies:new int[]{0,4,8}) {
            var line=cycle(plies,0);
            assertEquals(1+plies/4,line.game.currentOccurrences(line.board));
            for(TTable table:new TTable[]{null,new TTable(1)}) {
                assertEquals(plies==8?0:37,new ExactSearch((b,p)->37,table)
                        .search(line.board,line.game,0,ExactSearch.NEVER_CANCELLED).score());
            }
        }
        var full=cycle(8,0); var line=new SearchLineHistory(GameHistory.initial(full.positions.get(0)));
        for(int i=1;i<full.positions.size();i++) {
            line.pushRealPosition(full.positions.get(i));
            assertEquals(i==8,line.isFormalThreefold(full.positions.get(i)));
        }
        line.restoreRoot();assertEquals(1,line.size());
    }

    @Test void availableFormalDrawDoesNotOverrideABetterLegalContinuation() {
        var line=cycle(7,0);
        for(int leaf:new int[]{37,-37}) {
            int expected=leaf==37?0:37;
            assertEquals(expected,oracle(line,1,(b,p)->leaf));
            for(TTable table:new TTable[]{null,new TTable(1)}) assertEquals(expected,
                    new ExactSearch((b,p)->leaf,table).search(line.board,line.game,1,ExactSearch.NEVER_CANCELLED).score());
        }
    }

    @Test void differentLegalChronologiesWithTheSameCountsHaveTheSameFixedDepthValues() {
        String[] first={"g1f3","g8f6","b1c3","b8c6"};
        String[] second={"b1c3","b8c6","g1f3","g8f6"};
        String[] back={"f3g1","c6b8","c3b1","f6g8"};
        List<Line> lines=new ArrayList<>();
        for(boolean reverse:new boolean[]{false,true}) {
            long[] b=Board.startingPosition();var game=GameHistory.builder(b);
            for(String[] part:new String[][]{reverse?second:first,back,reverse?first:second}) {
                for(String move:part) {
                    b=play(b,move);game.appendPosition(b);
                    assertTrue(game.snapshot().currentOccurrences(b)<3,"No fixture crosses an earlier formal draw");
                }
            }
            lines.add(new Line(b,game.snapshot(),List.of(b)));
        }
        var a=lines.get(0);var b=lines.get(1);assertArrayEquals(a.board,b.board);
        assertEquals(2,a.game.currentOccurrences(a.board));
        for(int i=0;i<a.game.size();i++) assertEquals(a.game.occurrences(a.game.keyAt(i)),b.game.occurrences(a.game.keyAt(i)));
        var search=new ExactSearch((board,ply)->Eval.evaluate(board),new TTable(4));search.beginRequest();
        for(int d=1;d<=3;d++) {
            int expected=oracle(a,d,(board,ply)->Eval.evaluate(board));
            assertEquals(expected,oracle(b,d,(board,ply)->Eval.evaluate(board)));
            assertEquals(expected,search.search(a.board,a.game,d,ExactSearch.NEVER_CANCELLED).score());
            assertEquals(expected,search.search(b.board,b.game,d,ExactSearch.NEVER_CANCELLED).score());
        }
        search.endRequest();
    }

    @Test void terminalZeroStoresItsOwnCallerWindowClassificationAndIgnoresPoisonedEvidence() {
        var line=cycle(8,0);
        int[][] windows={{-1,1,TTable.TYPE_EXACT},{-2,0,TTable.TYPE_LOWER},{0,2,TTable.TYPE_UPPER}};
        for(int[] w:windows) {
            var table=new TTable(1);var search=new ExactSearch((b,p)->{throw new AssertionError("terminal evaluated");},table);
            search.beginRequest();table.save(key(line),2,TTable.TYPE_EXACT,901,0);
            var result=search.searchWindow(line.board,line.game,2,w[0],w[1],ExactSearch.NEVER_CANCELLED);
            assertEquals(0,result.score());assertEquals(1,result.nodes());
            // Replacement may retain an existing EXACT over a same-depth bound.
            // Clear that deliberately poisoned entry to inspect the new store's type.
            table.clear();
            assertEquals(0,search.searchWindow(line.board,line.game,2,w[0],w[1],ExactSearch.NEVER_CANCELLED).score());
            var entry=new TTable.TEntry();assertTrue(table.probe(key(line),entry));
            assertEquals(w[2],(entry.data>>>8)&3);assertEquals(0,(int)(entry.data>>32));search.endRequest();
        }
    }

    @Test void fullClockIsNeededAndDepthRemainsSeparateFromRealPly() {
        var table=new TTable(1);var search=new ExactSearch((b,p)->-37,table);search.beginRequest();
        for(int clock:new int[]{0,98,99,100}) {
            long[] b=Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - "+clock+" 1");
            assertEquals(clock>=99?0:37,search.search(b,1).score());
        }
        long[] zeroing=Board.fromFen("4k3/8/8/8/8/8/P7/R3K3 w - - 99 1");
        assertEquals(37,search.search(zeroing,1).score());
        var line=cycle(4,0);
        assertEquals(37,search.search(line.board,line.game,1,ExactSearch.NEVER_CANCELLED).score());
        assertEquals(-37,search.search(line.board,line.game,2,ExactSearch.NEVER_CANCELLED).score());
        search.endRequest();
    }

    @Test void unreachableRule50DoesNotMakeClockIndependentEvaluatorSemantics() {
        // ExactEvaluator permits all board status; supported BRN models use these bits.
        var table=new TTable(1);var search=new ExactSearch((b,p)->Board.halfMoveClock((int)b[Board.STATUS]),table);
        search.beginRequest();
        for(int clock:new int[]{0,20}) {
            long[] b=Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - "+clock+" 1");
            assertEquals(-clock-1,search.search(b,1).score());
        }
        search.endRequest();
    }

    @Test void castlingRightLossCanMakeOlderHistoryUnreachableWithoutZeroingTheClock() {
        long[] initial=Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        List<long[]> positions=new ArrayList<>();positions.add(initial);long[] b=initial;
        var full=GameHistory.builder(b);
        for(String move:new String[]{"e1f1","e8f8","f1e1","f8e8"}){b=play(b,move);positions.add(b);full.appendPosition(b);}
        assertArrayEquals(Arrays.copyOf(initial,4),Arrays.copyOf(b,4));
        assertEquals(4,Board.halfMoveClock((int)b[Board.STATUS]));
        assertNotEquals(PositionIdentity.repetitionKey(initial),PositionIdentity.repetitionKey(b));
        var shorter=GameHistory.builder(positions.get(2)).appendPosition(positions.get(3)).appendPosition(b).snapshot();
        var a=new Line(b,full.snapshot(),positions);var c=new Line(b,shorter,positions);
        assertEquals(1,a.game.currentOccurrences(b));assertEquals(1,c.game.currentOccurrences(b));
        // Current key is deliberately conservative across this stronger barrier.
        assertNotEquals(key(a),key(c));
        for(int d=0;d<=2;d++)assertEquals(oracle(a,d,(board,ply)->Eval.evaluate(board)),oracle(c,d,(board,ply)->Eval.evaluate(board)));
    }

    @Test void matePrecedesRule50AndRepetitionAtDifferentRealPlies() throws Exception {
        long[] mate=Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1");
        var game=GameHistory.builder(mate).appendPosition(mate).appendPosition(mate).snapshot();
        // Repeated terminal boards are an adversarial primitive fixture, not a legal game.
        for(int ply:new int[]{0,4,11}) {
            var search=new ExactSearch((b,p)->37,new TTable(1));search.beginRequest();
            assertEquals(-ExactSearch.MATE_SCORE+ply,atPly(search,mate,game,3,ply));
            var repeated=cycle(8,0);
            assertEquals(0,atPly(search,repeated.board,repeated.game,3,ply));search.endRequest();
        }
    }

    @Test void independentListOracleAgreesAcrossHistoriesClocksAndPvsWindows() {
        var roots=new ArrayList<Line>();
        roots.add(cycle(4,0));roots.add(cycle(7,0));roots.add(cycle(7,3));
        for(String fen:new String[]{
                "4k3/8/8/8/8/8/8/R3K3 w - - 98 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2",
                "4r1k1/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 20 8"}) {
            long[] b=Board.fromFen(fen);roots.add(new Line(b,GameHistory.initial(b),List.of(b)));
        }
        var search=new ExactSearch((b,p)->Eval.evaluate(b),new TTable(4));search.beginRequest();
        for(var root:roots) for(int d=0;d<=3;d++) {
            int expected=oracle(root,d,(b,p)->Eval.evaluate(b));
            assertEquals(expected,new ExactSearch().search(root.board,root.game,d,ExactSearch.NEVER_CANCELLED).score());
            assertEquals(expected,search.search(root.board,root.game,d,ExactSearch.NEVER_CANCELLED).score());
            assertEquals(expected,search.searchWindow(root.board,root.game,d,expected-1,expected+1,ExactSearch.NEVER_CANCELLED).score());
        }
        search.endRequest();
    }

    @Test void cancellationUnwindsExactHistoryAndReuseStartsFromTheSamePrefix() throws Exception {
        var root=cycle(7,0);var search=new ExactSearch((b,p)->Eval.evaluate(b),new TTable(1));
        var polls=new AtomicInteger();var captured=new AtomicReference<SearchLineHistory>();
        search.beginRequest();
        var cancelled=search.search(root.board,root.game,5,()->{
            try { if(field("history").get(search) instanceof SearchLineHistory h) captured.set(h); }
            catch(ReflectiveOperationException e){throw new AssertionError(e);}
            return polls.incrementAndGet()>150;
        });
        assertFalse(cancelled.completed());assertNotNull(captured.get());
        assertEquals(root.game.size(),captured.get().size());
        assertEquals(2,captured.get().currentOccurrences(root.board));
        assertEquals(new ExactSearch().search(root.board,root.game,3,ExactSearch.NEVER_CANCELLED).score(),
                search.search(root.board,root.game,3,ExactSearch.NEVER_CANCELLED).score());search.endRequest();
    }

    @Test void pvsResearchReusesOneHistoryPushAndRestoresTheRealPrefix() throws Exception {
        var root=cycle(7,0);var owner=new AtomicReference<ExactSearch>();
        var captured=new AtomicReference<SearchLineHistory>();var transitions=new AtomicInteger();
        ExactEvaluator evaluator=new ExactEvaluator() {
            private void check(long[] board,int ply) {
                try {
                    var history=(SearchLineHistory)field("history").get(owner.get());captured.set(history);
                    assertEquals(root.game.size()+ply,history.size());
                    assertEquals(PositionIdentity.repetitionKey(board),history.currentKey());
                }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            }
            @Override public void child(long[] parent,long[] child,int parentPly) {check(parent,parentPly);transitions.incrementAndGet();}
            @Override public int evaluate(long[] board,int ply) {check(board,ply);return Eval.evaluate(board);}
        };
        var search=new ExactSearch(evaluator,new TTable(4));owner.set(search);
        var result=search.search(root.board,root.game,3,ExactSearch.NEVER_CANCELLED);
        assertTrue(result.nodes()>transitions.get()+1,"Fixture must actually execute PVS re-search");
        assertEquals(root.game.size(),captured.get().size());
        assertEquals(2,captured.get().currentOccurrences(root.board));
        assertEquals(oracle(root,3,(b,p)->Eval.evaluate(b)),result.score());
    }

    private static Field field(String name) throws ReflectiveOperationException {var f=ExactSearch.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private static int atPly(ExactSearch s,long[] b,GameHistory g,int depth,int ply) throws Exception {
        ((long[][])field("boards").get(s))[ply]=b.clone();
        field("history").set(s,new SearchLineHistory(g));
        ((long[])field("historyKeys").get(s))[ply]=SearchKey.rootHistory(b,g);
        field("cancelled").set(s,ExactSearch.NEVER_CANCELLED);
        var m=ExactSearch.class.getDeclaredMethod("negamax",int.class,int.class,int.class,int.class,long.class,boolean.class);m.setAccessible(true);
        return (int)m.invoke(s,depth,ply,-ExactSearch.INFINITY,ExactSearch.INFINITY,0L,false);
    }
    private static long key(Line l){return SearchKey.key(l.board,SearchKey.rootHistory(l.board,l.game));}
    private static long[] legal(long[] b){long[] moves=new long[256];int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[Board.STATUS],b[Board.KEY],true,moves,new long[Board.MAX_BITBOARDS]);return Arrays.copyOf(moves,n);}
    private static long[] child(long[] b,long m){long[] c=new long[Board.MAX_BITBOARDS];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[Board.STATUS],b[Board.KEY],m,c);return c;}
    private static long[] play(long[] b,String m){for(long move:legal(b))if(Move.coordinate(move).equals(m))return child(b,move);throw new AssertionError(m);}
    private static int oracle(Line root,int depth,ExactEvaluator eval){
        List<Long> keys=new ArrayList<>();for(int i=0;i<root.game.size();i++)keys.add(root.game.keyAt(i));
        return oracle(root.board,keys,depth,0,eval);
    }
    // Independent unpruned recurrence and history counting, sharing only Board/Gen,
    // canonical repetition identity and the separately owned material predicate.
    private static int oracle(long[] b,List<Long> keys,int depth,int ply,ExactEvaluator eval){
        long[] moves=legal(b);int status=(int)b[Board.STATUS];
        if(moves.length==0)return Board.isPlayerInCheck(b[0],b[1],b[2],b[3],Board.player(status))?-ExactSearch.MATE_SCORE+ply:0;
        int clock=Board.halfMoveClock(status);if(clock>=100)return 0;
        long current=PositionIdentity.repetitionKey(b);int count=0;
        for(int i=Math.max(0,keys.size()-1-clock);i<keys.size();i++)if(keys.get(i)==current)count++;
        if(count>=3||InsufficientMaterial.isAutomaticDraw(b[0],b[1],b[2],b[3]))return 0;
        if(depth==0)return eval.evaluate(b,ply);
        int best=-ExactSearch.INFINITY;
        for(long move:moves){long[] c=child(b,move);keys.add(PositionIdentity.repetitionKey(c));best=Math.max(best,-oracle(c,keys,depth-1,ply+1,eval));keys.remove(keys.size()-1);}
        return best;
    }
}
