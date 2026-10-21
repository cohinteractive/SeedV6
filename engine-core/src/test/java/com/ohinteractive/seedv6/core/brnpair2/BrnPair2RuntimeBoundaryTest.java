package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Runs with only engine-core: compilation and model decoding need no trainer/store. */
class BrnPair2RuntimeBoundaryTest {
    @Test void foldedInputOwnsItsWeightsAndRejectsUnsupportedShapes() {
        float[] values = new float[BrnPair2Codec.INFERENCE_PARAMETERS];
        values[0] = .125f;
        var snapshot = new BrnPair2Weights(2, values);
        values[0] = 10;
        assertEquals(.125f, snapshot.weight(0));
        assertEquals(values.length, snapshot.parameters());
        assertThrows(IllegalArgumentException.class, () -> new BrnPair2Weights(3, values));
        assertThrows(IllegalArgumentException.class, () -> new BrnPair2Weights(2, new float[1]));
        values[10] = Float.NaN;
        assertThrows(IllegalArgumentException.class, () -> new BrnPair2Weights(2, values));
    }

    @Test void compiledCodecPreservesBytesAndInferenceWithoutTrainingClasses() throws Exception {
        float[] values = new float[BrnPair2Codec.INFERENCE_PARAMETERS];
        var random = new Random(2402);
        for (int i = 0; i < values.length; i++) values[i] = (random.nextFloat() - .5f) / 16;
        var compiled = new BrnPair2Model(new BrnPair2Weights(2, values));
        byte[] bytes = encode(compiled);
        assertEquals(795032, bytes.length);
        var decoded = BrnPair2Codec.readModel(new ByteArrayInputStream(bytes));
        assertArrayEquals(bytes, encode(decoded));
        assertEquals(294, decoded.pairs());
        for (String fen : new String[] {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "4k3/P7/8/8/8/8/7p/4K3 b - - 0 1",
                "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1"}) {
            var board = Board.fromFen(fen);
            assertEquals(compiled.newWorkspace().evaluatePawns(board, .25),
                    decoded.newWorkspace().evaluatePawns(board, .25), 0);
        }
        bytes[100] ^= 1;
        assertThrows(IOException.class, () -> BrnPair2Codec.readModel(new ByteArrayInputStream(bytes)));
        assertThrows(IOException.class, () -> BrnPair2Codec.readModel(new ByteArrayInputStream(new byte[44])));
    }

    private static byte[] encode(BrnPair2Model model) throws IOException {
        var out = new ByteArrayOutputStream();
        BrnPair2Codec.writeModel(model, out);
        return out.toByteArray();
    }

    @Test void cachedRawValuesFollowLastPlacementForBothPerspectivesWithoutUpdates() {
        float[] values=new float[BrnPair2Codec.INFERENCE_PARAMETERS];
        var random=new Random(731);
        for(int i=0;i<values.length;i++)values[i]=(random.nextFloat()-.5f)/16;
        var model=new BrnPair2Model(new BrnPair2Weights(2,values));var cache=model.newWorkspace();
        assertThrows(IllegalStateException.class,()->cache.cachedPawns(0));
        for(String fen:new String[]{
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1",
                "4k3/P7/8/8/8/8/7p/4K3 b - - 0 1"}) {
            var board=Board.fromFen(fen);int side=Board.player((int)board[Board.STATUS]);
            double raw=cache.evaluatePawns(board);long rebuilds=cache.rebuilds,updates=cache.updates,repeats=cache.repeats;
            assertEquals(Double.doubleToLongBits(raw),Double.doubleToLongBits(cache.cachedPawns(side)));
            var flipped=board.clone();flipped[Board.STATUS]^=Board.PLAYER_BIT;
            double opposite=cache.cachedPawns(side^1);
            assertEquals(model.newWorkspace().evaluatePawns(flipped),opposite,1e-12);
            assertEquals(rebuilds,cache.rebuilds);assertEquals(updates,cache.updates);assertEquals(repeats,cache.repeats);
            assertEquals(Double.doubleToLongBits(opposite),Double.doubleToLongBits(cache.evaluatePawns(flipped)));
        }
        assertThrows(IllegalStateException.class,()->cache.cachedPawns(-1));
        assertThrows(IllegalStateException.class,()->cache.cachedPawns(2));
    }
}
