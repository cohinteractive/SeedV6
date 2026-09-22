package com.ohinteractive.seedv6.core.brn2;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;
import static org.junit.jupiter.api.Assertions.*;

class Brn2FeaturesTest {
    @Test void allOwnershipTypesSquaresAndRelationEndpointsUseCanonicalRoles() {
        for (int perspective = 0; perspective < 2; perspective++) for (int type = 1; type <= 6; type++)
            for (int role = 0; role < 2; role++) for (int square = 0; square < 64; square++) {
                int code = type | (role << 3), other = square ^ 63;
                var board = Brn2CoreTest.raw(perspective, code ^ (perspective << 3), square ^ (perspective * 56),
                        5 ^ (perspective << 3), other ^ (perspective * 56));
                var f = new Brn2Features(); f.extract(board);
                int node = square < other ? 1 : 2;
                assertEquals(BrnFeatureSchema.nodeIndex(code, square), f.indexAt(node));
                assertEquals(BrnFeatureSchema.nodeIndex(5, other), f.indexAt(3 - node));
                assertEquals(BrnFeatureSchema.relationIndex(code, square, 5, other), f.indexAt(3));
                assertEquals(4, f.size()); // bias, two nodes and one relation; no learned STM bit
            }
    }

    @Test void individualCastlingRightsEpDirectionsAndHalfmoveClockHaveExplicitCanonicalMeaning() {
        for (int perspective = 0; perspective < 2; perspective++) {
            for (int right = 0; right < 4; right++) {
                long raw = perspective | (1L << (Board.CASTLING_SHIFT + right));
                assertEquals(1L << (Board.CASTLING_SHIFT + (perspective == 0 ? right : right ^ 2)), Brn2Features.status(raw, perspective));
            }
            for (int file = 0; file < 8; file++) {
                int ep = (perspective == 0 ? 40 : 16) + file;
                assertEquals((40L + file) << Board.ESQUARE_SHIFT,
                        Brn2Features.status(perspective | ((long) ep << Board.ESQUARE_SHIFT), perspective));
            }
            assertEquals(0, Brn2Features.status(perspective, perspective)); // absent EP stays absent
            for (int clock = 0; clock <= 127; clock++) {
                long expected = (long) clock << Board.HALF_MOVE_CLOCK_SHIFT;
                assertEquals(expected, Brn2Features.status(expected | perspective, perspective));
            }
        }
    }

    @Test void fullmoveReservedBitsAndKeyCannotReachFeaturesOrEvaluation() {
        var model = new Brn2Model();
        for (String side : new String[] {"w", "b"}) {
            var board = Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 " + side + " - - 17 1");
            var f = new Brn2Features(); f.extract(board); var ids = ids(f);
            double value = model.evaluate(board, new Brn2Workspace());
            for (int bit = Board.FULL_MOVE_NUMBER_SHIFT; bit < 64; bit++) {
                var changed = board.clone(); changed[Board.STATUS] ^= 1L << bit; changed[Board.KEY] ^= -1L;
                f.extract(changed); assertArrayEquals(ids, ids(f));
                assertEquals(value, model.evaluate(changed, new Brn2Workspace()));
            }
        }
    }

    @Test void statusOnlyFlipAtPeriodicBoundaryCopiesBothPerspectiveCachesWithoutRebuild() throws Exception {
        var model = new Brn2Model(); var board = Board.startingPosition();
        var parent = new Brn2Accumulator(model); parent.rebuild(board);
        // Observe cache preservation directly, including at the periodic placement-rebuild threshold.
        var distance = Brn2Accumulator.class.getDeclaredField("distance"); distance.setAccessible(true); distance.setInt(parent, 31);
        var field = Brn2Accumulator.class.getDeclaredField("relations"); field.setAccessible(true);
        double[][] caches = (double[][]) field.get(parent);
        for (double[] cache : caches) for (int i = 0; i < cache.length; i++) cache[i] = Math.nextUp(cache[i]);
        var flipped = board.clone(); flipped[Board.STATUS] ^= 1;
        var child = new Brn2Accumulator(model); child.update(board, flipped, parent);
        var copies = (double[][]) field.get(child);
        assertEquals(31, distance.getInt(child));
        for (int perspective = 0; perspective < 2; perspective++) {
            assertNotSame(caches[perspective], copies[perspective]);
            for (int sq = 0; sq < 64; sq++) if ((Brn2Features.orient(Brn2Features.occupied(board), perspective) & (1L << sq)) != 0)
                assertArrayEquals(Arrays.copyOfRange(caches[perspective], sq * 32, (sq + 1) * 32),
                        Arrays.copyOfRange(copies[perspective], sq * 32, (sq + 1) * 32));
        }
        assertEquals(model.evaluate(flipped, new Brn2Workspace()), child.evaluate(flipped), 2e-13);
    }

    private static int[] ids(Brn2Features f) {
        int[] ids = new int[f.size()]; for (int i = 0; i < ids.length; i++) ids[i] = f.indexAt(i); return ids;
    }
}
