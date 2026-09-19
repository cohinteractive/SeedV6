package com.ohinteractive.seedv6.core.nnue;

import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class NnueFeatureSchemaTest {

    // Explicit vocabulary order, deliberately unlike Seed's numeric type order.
    private static final int[] TYPES = {
            Piece.PAWN, Piece.KNIGHT, Piece.BISHOP, Piece.ROOK, Piece.QUEEN, Piece.KING
    };

    @Test
    void v1IdentityAndDimensionsAreFixed() {
        assertEquals(1, NnueFeatureSchema.VERSION);
        assertEquals("seedv6.nnue.halfkp.v1", NnueFeatureSchema.ID);
        assertEquals(64, NnueFeatureSchema.SQUARE_COUNT);
        assertEquals(12, NnueFeatureSchema.PIECE_CHANNEL_COUNT);
        assertEquals(49_152, NnueFeatureSchema.FEATURE_COUNT);
        assertEquals(64, NnueNetwork.ACCUMULATOR_SIZE);
        assertEquals(128, NnueNetwork.CONCATENATED_SIZE);
        assertEquals(32, NnueNetwork.HIDDEN_SIZE);
        assertEquals(1, NnueNetwork.OUTPUT_SIZE);
        assertEquals(3_145_728, NnueNetwork.FEATURE_WEIGHT_COUNT);
        assertEquals(4_096, NnueNetwork.HIDDEN_WEIGHT_COUNT);
        assertEquals(3_149_953, NnueNetwork.PARAMETER_COUNT);
    }

    @Test
    void everyPieceAndColourHasTheExplicitRelativeChannel() {
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, new int[] {
                NnueFeatureSchema.FRIENDLY_PAWN, NnueFeatureSchema.FRIENDLY_KNIGHT,
                NnueFeatureSchema.FRIENDLY_BISHOP, NnueFeatureSchema.FRIENDLY_ROOK,
                NnueFeatureSchema.FRIENDLY_QUEEN, NnueFeatureSchema.FRIENDLY_KING,
                NnueFeatureSchema.ENEMY_PAWN, NnueFeatureSchema.ENEMY_KNIGHT,
                NnueFeatureSchema.ENEMY_BISHOP, NnueFeatureSchema.ENEMY_ROOK,
                NnueFeatureSchema.ENEMY_QUEEN, NnueFeatureSchema.ENEMY_KING
        });
        for (int i = 0; i < TYPES.length; i++) {
            assertEquals(i, NnueFeatureSchema.relativePieceChannel(Value.WHITE, Value.WHITE, TYPES[i]));
            assertEquals(i + 6, NnueFeatureSchema.relativePieceChannel(Value.WHITE, Value.BLACK, TYPES[i]));
            assertEquals(i + 6, NnueFeatureSchema.relativePieceChannel(Value.BLACK, Value.WHITE, TYPES[i]));
            assertEquals(i, NnueFeatureSchema.relativePieceChannel(Value.BLACK, Value.BLACK, TYPES[i]));
        }
        assertEquals(5, NnueFeatureSchema.relativePieceChannel(Value.WHITE, Value.WHITE, Piece.KING));
        assertEquals(0, NnueFeatureSchema.relativePieceChannel(Value.WHITE, Value.WHITE, Piece.PAWN));
    }

    @Test
    void orientationFlipsOnlyRanksIncludingCornersAndCentre() {
        int[] original = {0, 7, 56, 63, 27, 28, 35, 36};
        int[] black = {56, 63, 0, 7, 35, 36, 27, 28};
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], NnueFeatureSchema.orientSquare(Value.WHITE, original[i]));
            assertEquals(black[i], NnueFeatureSchema.orientSquare(Value.BLACK, original[i]));
        }
        for (int square = 0; square < 64; square++) {
            int byRankAndFile = (7 - square / 8) * 8 + square % 8;
            assertEquals(byRankAndFile, NnueFeatureSchema.orientSquare(Value.BLACK, square));
            assertEquals(square ^ 56, NnueFeatureSchema.orientSquare(Value.BLACK, square));
        }
    }

    @Test
    void literalIndicesAndExhaustiveCoverageFixTheFeatureLayout() {
        assertEquals(0, NnueFeatureSchema.featureIndex(Value.WHITE, 0, Value.WHITE, Piece.PAWN, 0));
        assertEquals(49_151, NnueFeatureSchema.featureIndex(Value.WHITE, 63, Value.BLACK, Piece.KING, 63));
        assertEquals(3_084, NnueFeatureSchema.featureIndex(Value.WHITE, 4, Value.WHITE, Piece.PAWN, 12));
        assertEquals(3_771, NnueFeatureSchema.featureIndex(Value.WHITE, 4, Value.BLACK, Piece.QUEEN, 59));
        assertEquals(3_084, NnueFeatureSchema.featureIndex(Value.BLACK, 60, Value.BLACK, Piece.PAWN, 52));
        assertEquals(3_771, NnueFeatureSchema.featureIndex(Value.BLACK, 60, Value.WHITE, Piece.QUEEN, 3));

        for (int perspective : new int[] {Value.WHITE, Value.BLACK}) {
            boolean[] seen = new boolean[49_152];
            for (int king = 0; king < 64; king++) {
                for (int colour : new int[] {Value.WHITE, Value.BLACK}) {
                    for (int type : TYPES) {
                        for (int square = 0; square < 64; square++) {
                            int index = NnueFeatureSchema.featureIndex(perspective, king, colour, type, square);
                            assertTrue(index >= 0 && index < 49_152);
                            assertFalse(seen[index], "Feature collision");
                            seen[index] = true;
                        }
                    }
                }
            }
            for (boolean present : seen) assertTrue(present);
        }
    }

    @Test
    void colourSwappedRankMirroredPrimitivePositionsHaveCorrespondingFeatureSets() {
        int[] squares = {4, 62, 12, 18, 43, 56, 35, 49, 27, 37, 7, 58};
        int[] colours = {0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1};
        int[] types = {Piece.KING, Piece.KING, Piece.PAWN, Piece.KNIGHT,
                Piece.QUEEN, Piece.ROOK, Piece.QUEEN, Piece.PAWN,
                Piece.BISHOP, Piece.KNIGHT, Piece.ROOK, Piece.BISHOP};
        for (int perspective : new int[] {Value.WHITE, Value.BLACK}) {
            int king = squares[perspective];
            int mirroredKing = (7 - king / 8) * 8 + king % 8;
            int[] originalFeatures = new int[squares.length];
            int[] mirroredFeatures = new int[squares.length];
            for (int i = 0; i < squares.length; i++) {
                int mirroredSquare = (7 - squares[i] / 8) * 8 + squares[i] % 8;
                originalFeatures[i] = NnueFeatureSchema.featureIndex(perspective, king,
                        colours[i], types[i], squares[i]);
                mirroredFeatures[i] = NnueFeatureSchema.featureIndex(1 - perspective, mirroredKing,
                        1 - colours[i], types[i], mirroredSquare);
            }
            Arrays.sort(originalFeatures);
            Arrays.sort(mirroredFeatures);
            assertArrayEquals(originalFeatures, mirroredFeatures);
        }
    }

    @Test
    void invalidSchemaInputsFailExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> NnueFeatureSchema.orientSquare(2, 0));
        assertThrows(IllegalArgumentException.class, () -> NnueFeatureSchema.orientSquare(0, -1));
        assertThrows(IllegalArgumentException.class, () -> NnueFeatureSchema.orientSquare(1, 64));
        assertThrows(IllegalArgumentException.class, () -> NnueFeatureSchema.relativePieceChannel(0, 2, Piece.KING));
        assertThrows(IllegalArgumentException.class, () -> NnueFeatureSchema.relativePieceChannel(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> NnueFeatureSchema.relativePieceChannel(0, 0, 7));
        assertThrows(IllegalArgumentException.class, () -> NnueFeatureSchema.featureIndex(0, 64, 0, Piece.KING, 0));
        assertThrows(IllegalArgumentException.class, () -> NnueFeatureSchema.featureIndex(0, 0, 0, Piece.KING, 64));
    }
}
