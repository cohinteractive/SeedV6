package com.ohinteractive.seedv6.core.nnue.conventional;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import java.io.*;
import java.nio.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Direct128EquivalenceTest {
    private final ConventionalNetwork production = BaselineEngine.load().conventional().model();
    private final CompactNetwork reference;
    Direct128EquivalenceTest() throws Exception {
        byte[] promoted;
        try (var in = getClass().getResourceAsStream(ConventionalNnueEngine.Model.DIRECT128.resource())) { promoted = in.readAllBytes(); }
        byte[] payload = Arrays.copyOfRange(promoted,72,promoted.length);
        var original = ByteBuffer.allocate(76+payload.length).order(ByteOrder.LITTLE_ENDIAN);
        original.put("S6P2N002".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        for (int n : new int[]{2,0,7680,128,0,255,64,400,payload.length}) original.putInt(n);
        original.put(MessageDigest.getInstance("SHA-256").digest(payload)).put(payload);
        assertEquals("defaf21da465d6f63fc13a2299299e927029d82558d663c7bbc801e7c35e9d1c",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original.array())));
        assertThrows(IOException.class, () -> ConventionalNetwork.read(new ByteArrayInputStream(original.array())));
        reference = CompactNetwork.read(new ByteArrayInputStream(original.array()));
    }
    private void compare(long[] board, ConventionalAccumulator actual) {
        var fresh = production.newAccumulator(); fresh.rebuild(board);
        var independent = reference.newAccumulator(); independent.rebuild(board);
        for (int side=0;side<2;side++) for(int unit=0;unit<128;unit++) {
            assertEquals(independent.raw(side,unit), actual.raw(side,unit));
            assertEquals(fresh.raw(side,unit), actual.raw(side,unit));
        }
        assertEquals(reference.fullRebuild(board), actual.evaluate(board));
        assertEquals(production.fullRebuild(board), actual.evaluate(board));
    }
    @Test void trainedWeightsSpecialMovesKingContextsSiblingsUndoAndPrivateLifecycle() throws Exception {
        var rng = new SplittableRandom(991);
        String[] roots = {Board.FEN_STARTING_POSITION,
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                "1r5k/P7/1p6/8/8/8/3N1N2/4K3 w - - 0 1",
                "4k3/8/8/3p4/4P3/2N5/8/3K4 w - - 0 1",
                "8/8/8/3k4/8/3K4/8/8 b - - 0 1"};
        for (String fen: roots) {
            long[] parent=Board.fromFen(fen); var state=production.newAccumulator(); state.rebuild(parent);
            for(int ply=0;ply<80;ply++) {
                long[] moves=new long[256]; int count=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
                if(count==0)break;
                for(int i=0;i<(ply==0?count:Math.min(count,3));i++) {
                    long[] child=CompactNetworkTest.move(parent,moves[i]); var next=production.newAccumulator(); next.update(parent,child,state);
                    compare(child,next); compare(parent,state);
                    var undo=production.newAccumulator(); undo.update(child,parent,next); compare(parent,undo);
                }
                long[] child=CompactNetworkTest.move(parent,moves[rng.nextInt(count)]);
                var next=production.newAccumulator(); next.update(parent,child,state); parent=child; state=next;
            }
        }
        var definition=SearchEvaluation.conventional(production);
        var main=definition.newState(257); var q=definition.newState(257); var worker=definition.newState(257);
        long[] board=Board.startingPosition(), child=new long[6];
        Board.nullMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],child);
        main.initialize(board,255); main.child(board,child,255); q.initializeFrom(child,256,main); worker.initialize(board,0);
        assertEquals(reference.fullRebuild(child),q.evaluate(child,256));
        assertEquals(reference.fullRebuild(board),worker.evaluate(board,0));
        assertEquals(reference.fullRebuild(board),main.evaluate(board,255));
        assertThrows(IllegalArgumentException.class,()->main.initializeFrom(board,0,main));
        var foreign=BaselineEngine.load(BaselineEngine.Evaluator.CONVENTIONAL_WDL25).evaluation().newState(257);
        foreign.initialize(board,255);
        assertThrows(IllegalArgumentException.class,()->main.initializeFrom(board,255,foreign));
    }
    @Test void productionAndResearchSearchMatchExactlyAndWorkersUsePrivateState() throws Exception {
        for(String fen:new String[]{Board.FEN_STARTING_POSITION,"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                "7k/P7/8/8/8/8/8/7K w - - 0 1"}) {
            var board=Board.fromFen(fen);
            var request=new SearchRequest(board,GameHistory.initial(board),4,SearchObserver.NONE,SearchControl.unlimited(),true);
            SearchResult expected;
            try(var search=BaselineEngine.configuration(1,8).create(reference.evaluation())) { expected=search.search(request); }
            try(var search=BaselineEngine.load().create(1,8)) {
                var actual=search.search(request); assertTrue(actual.completed());
                assertEquals(expected.score(),actual.score()); assertEquals(expected.nodes(),actual.nodes());
                assertEquals(expected.bestMove(),actual.bestMove()); assertArrayEquals(expected.principalVariation(),actual.principalVariation());
            }
            // Each actual helper evaluation is checked against the independent research oracle.
            var checked=SearchEvaluation.researchCentipawnsStateful(capacity -> new SearchEvaluation.ResearchCpLifecycle() {
                final SearchEvaluation.State state=SearchEvaluation.conventional(production).newState(capacity);
                public void initialize(long[] b,int ply){state.initialize(b,ply);}
                public void child(long[] p,long[] c,int ply){state.child(p,c,ply);}
                public void initializeFrom(long[] b,int ply,SearchEvaluation.ResearchCpLifecycle source){state.initialize(b,ply);}
                public double evaluate(long[] b,int ply){int value=state.evaluate(b,ply);assertEquals(reference.fullRebuild(b),value);return value;}
            });
            try(var search=BaselineEngine.configuration(2,8).create(checked)) { assertTrue(search.search(request).completed()); }
        }
    }
}
