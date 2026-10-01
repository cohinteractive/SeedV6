package com.ohinteractive.seedv6.search.tablebase;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;

class SyzygyRootTest {
    static final String QUEEN = "7k/8/8/8/8/8/8/KQ6 w - - 0 1";
    static long move(long[] board, String uci) {
        long[] moves = new long[512];
        int n = Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,new long[6]);
        return Arrays.stream(moves,0,n).filter(m -> Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
    }
    static int packed(long move, int wdl, int dtz) {
        int p = switch(Move.promotion(move)) {
            case NONE -> 0; case QUEEN -> 1; case ROOK -> 2; case BISHOP -> 3; case KNIGHT -> 4;
        };
        return wdl | Move.fromSquare(move)<<10 | Move.toSquare(move)<<4 | p<<16 | dtz<<20;
    }
    static long[] child(long[] b,long move) {
        long[] c=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],move,c);return c;
    }
    @Test void separateWinningDecisionPreservesBoardAndHistory() {
        long[] b=Board.fromFen(QUEEN), saved=b.clone();var h=GameHistory.initial(b);
        long m=move(b,"b1g1");var policy=new SyzygyRoot(x->packed(m,4,13));
        var win=policy.probeWin(b,h,SearchControl.unlimited());
        assertEquals(new TablebaseWin(m,13),win);assertArrayEquals(saved,b);assertTrue(h.matchesCurrent(b));assertEquals(1,h.size());
    }
    @Test void fullActiveHistoryGuardIncludesPreviouslyRepeatedOtherPositions() {
        long[] b=Board.fromFen("4Q3/8/3k4/8/3K4/8/8/8 b - - 0 1");var h=GameHistory.builder(b);
        for(String uci:"d6c7 e8c8 c7d6 c8e8 d6c7 e8c8 c7d6".split(" ")){b=child(b,move(b,uci));h.appendPosition(b);}
        var calls=new AtomicInteger();var policy=new SyzygyRoot(x->{calls.incrementAndGet();return 0;});
        assertEquals(2,h.snapshot().currentOccurrences(b));assertNull(policy.probeWin(b,h.snapshot(),SearchControl.unlimited()));
        b=child(b,move(b,"c8a8"));h.appendPosition(b);assertEquals(1,h.snapshot().currentOccurrences(b));
        assertNull(policy.probeWin(b,h.snapshot(),SearchControl.unlimited()));assertEquals(0,calls.get());
    }
    @Test void zeroingBoundaryDiscardsOldOccurrencesWithoutManufacturingHistory() {
        long[] b=Board.fromFen(QUEEN);var h=GameHistory.builder(b).appendPosition(b).appendPosition(b).snapshot();
        var policy=new SyzygyRoot(x->packed(move(x,"b1g1"),4,13));
        assertNotNull(policy.probeWin(b,h,SearchControl.unlimited())); // Synthetic old-prefix fixture, not a played sequence.
    }
    @Test void terminalCastlingCardinalityAndClockGuardsAvoidProbe() {
        var calls=new AtomicInteger();var policy=new SyzygyRoot(x->{calls.incrementAndGet();return 0;});
        for(String fen:new String[]{"7k/6Q1/5K2/8/8/8/8/8 b - - 0 1","7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
                "7k/8/8/8/8/8/8/KQ6 w - - 99 1","7k/8/8/8/8/8/8/KQ6 w - - 100 1",
                "7k/8/8/8/8/8/8/KB6 w - - 0 1","4k3/8/8/8/8/8/8/4K2R w K - 0 1",Board.FEN_STARTING_POSITION}) {
            long[] b=Board.fromFen(fen);assertNull(policy.probeWin(b,GameHistory.initial(b),SearchControl.unlimited()),fen);
        }assertEquals(0,calls.get());
    }
    @Test void otherOutcomesSentinelsMalformedMovesAndRoundedBoundaryAreUnknown() {
        long[] b=Board.fromFen(QUEEN);var h=GameHistory.initial(b);long m=move(b,"b1g1");
        for(int r:new int[]{-1,2,4,packed(m,0,13),packed(m,1,113),packed(m,2,0),packed(m,3,113),packed(m,4,99),packed(m,4,0),4|(1<<20)})
            assertNull(new SyzygyRoot(x->r).probeWin(b,h,SearchControl.unlimited()));
        long[] late=Board.fromFen("7k/8/8/8/8/8/8/KQ6 w - - 80 1");
        assertNotNull(new SyzygyRoot(x->packed(m,4,18)).probeWin(late,GameHistory.initial(late),SearchControl.unlimited()));
        assertNull(new SyzygyRoot(x->packed(m,4,19)).probeWin(late,GameHistory.initial(late),SearchControl.unlimited()));
    }
    @Test void legalResolverRetainsEpAndPromotionMetadataAndRejectsImmediateDraw() {
        for(String[] fixture:new String[][]{{"8/8/8/3pP3/8/8/8/K6k w - d6 0 1","e5d6"},
                {"8/P7/8/8/8/8/7k/K7 w - - 0 1","a7a8q"}}){
            long[] b=Board.fromFen(fixture[0]);long m=move(b,fixture[1]);
            assertEquals(m,new SyzygyRoot(x->packed(m,4,1)).probeWin(b,GameHistory.initial(b),SearchControl.unlimited()).bestMove());
        }
        long[] b=Board.fromFen("8/P7/8/8/8/8/7k/K7 w - - 0 1");long m=move(b,"a7a8n");
        assertNull(new SyzygyRoot(x->packed(m,4,1)).probeWin(b,GameHistory.initial(b),SearchControl.unlimited()));
        b=Board.fromFen(QUEEN);long stale=move(b,"b1g6");
        assertNull(new SyzygyRoot(x->packed(stale,4,1)).probeWin(b,GameHistory.initial(b),SearchControl.unlimited()));
    }
    @Test void cancellationAndHardLimitsWinBeforeAndAfterNativeWork() {
        long[] b=Board.fromFen(QUEEN);var h=GameHistory.initial(b);long m=move(b,"b1g1");var calls=new AtomicInteger();
        var policy=new SyzygyRoot(x->{calls.incrementAndGet();return packed(m,4,13);});
        var zero=SearchControl.controlled(0,0,-1,()->0);assertNull(policy.probeWin(b,h,zero));assertEquals(0,calls.get());
        var stopped=SearchControl.controlled(-1,0,-1,()->0);stopped.request(SearchTermination.STOPPED);assertNull(policy.probeWin(b,h,stopped));
        var time=new AtomicLong();var control=SearchControl.controlled(-1,0,10,time::get);
        assertNull(new SyzygyRoot(x->{time.set(10);return packed(m,4,13);}).probeWin(b,h,control));assertEquals(SearchTermination.TIME_LIMIT,control.termination());
    }
}
