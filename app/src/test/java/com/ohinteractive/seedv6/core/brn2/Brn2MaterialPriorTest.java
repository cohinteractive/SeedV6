package com.ohinteractive.seedv6.core.brn2;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.evaluation.BrnScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import static com.ohinteractive.seedv6.core.brn2.Brn2Model.*;
import static org.junit.jupiter.api.Assertions.*;

class Brn2MaterialPriorTest {
    private record MaterialCase(String name, String us, String them, int score) {}
    private static final MaterialCase[] CASES = {
        new MaterialCase("equal material", "P7", "p7", 0),
        new MaterialCase("pawn", "P7", "8", 100),
        new MaterialCase("opponent pawn", "8", "p7", -100),
        new MaterialCase("knight", "N7", "8", 320),
        new MaterialCase("bishop", "B7", "8", 330),
        new MaterialCase("rook", "R7", "8", 500),
        new MaterialCase("queen", "Q7", "8", 900),
        new MaterialCase("rook versus knight", "R7", "n7", 180),
        new MaterialCase("queen versus rook", "Q7", "r7", 400),
        new MaterialCase("two rooks versus queen", "RR6", "q7", 100)
    };
    private static long[] board(MaterialCase c) {
        return Board.fromFen("7k/" + c.them() + "/8/8/8/8/" + c.us() + "/7K w - - 0 1");
    }
    private static int score(Brn2Model model, long[] board, boolean incremental) {
        var definition = incremental ? SearchEvaluation.brn2(model) : SearchEvaluation.brn2FullRecompute(model);
        var state = definition.newState(2); state.initialize(board, 0);
        return state.evaluate(board, 0);
    }

    @Test void freshMaterialScoresAreExactAcrossInferenceTrainingAndBothCanonicalPerspectives() {
        assertEquals(BrnScoreMapping.SCALE, Brn2MaterialPrior.SCORE_SCALE);
        var model = new Brn2Model(); var trainer = new Brn2Trainer(.001); var scratch = new Brn2Workspace();
        assertEquals(Brn2MaterialPrior.BASIC_V1, model.materialPrior());
        for (int i = OUTPUT_WEIGHT_OFFSET; i < PARAMETER_COUNT; i++) assertEquals(0, model.weight(i));
        for (var c : CASES) {
            long[] board = board(c);
            for (int perspective = 0; perspective < 2; perspective++) {
                board[Board.STATUS] = (board[Board.STATUS] & ~Board.PLAYER_BIT) | perspective;
                int expected = perspective == 0 ? c.score() : -c.score();
                assertEquals(expected, model.materialPrior().score(board), c.name());
                double value = model.evaluate(board, scratch);
                assertEquals(expected / (double) BrnScoreMapping.SCALE, value, 2e-17);
                assertEquals(value, model.evaluateReference(board, scratch));
                assertEquals(value, trainer.predict(board));
                assertEquals(expected, BrnScoreMapping.map(value), c.name());
                assertEquals(expected, score(model, board, true), c.name());
                assertEquals(expected, score(model, board, false), c.name());
            }
            System.out.printf("BASIC_V1 %s: %d cp (opposite perspective: %d cp)%n", c.name(), c.score(), -c.score());
        }
        assertEquals(0, score(model, Board.startingPosition(), true));
        assertEquals(0, score(model, Board.fromFen("7k/8/8/8/8/8/8/7K w - - 0 1"), true));
        // Different locations, rule state and extra bishops add only literal current-piece values.
        assertEquals(660, score(model, Board.fromFen("7k/8/8/8/8/3B4/8/B6K w - - 91 42"), true));
        assertEquals(900, score(new Brn2Model(123), board(CASES[6]), true));
    }

    @Test void realPromotionsUseOnlyTheCurrentPieceTypeAndIncrementalPlacement() {
        var model = new Brn2Model();
        for (var promotion : new String[]{"q", "r", "b", "n"}) {
            long[] parent = Board.fromFen("7k/P7/8/8/8/8/8/7K w - - 0 1");
            var game = new HeadlessGame(parent, 2);
            game.play(Arrays.stream(game.legalMoves()).filter(m -> Move.coordinate(m).equals("a7a8" + promotion))
                    .findFirst().orElseThrow());
            long[] child = game.boardSnapshot();
            int value = switch (promotion) { case "q" -> 900; case "r" -> 500; case "b" -> 330; default -> 320; };
            var state = SearchEvaluation.brn2(model).newState(2); state.initialize(parent, 0); state.child(parent, child, 0);
            assertEquals(-value, state.evaluate(child, 1)); // Promotion switched STM to the opponent.
            assertEquals(-value, score(model, child, false));
            child[Board.STATUS] ^= Board.PLAYER_BIT;
            assertEquals(value, score(model, child, true));
        }
    }

    @Test void materialIsAddedBeforeTanhAndTheFullPositionLossTrainsBothResidualSigns() {
        long[] board = board(CASES[6]);
        double[] weights = new double[PARAMETER_COUNT]; weights[OUTPUT_BIAS] = .2;
        var model = new Brn2Model(weights, Brn2MaterialPrior.BASIC_V1); var scratch = new Brn2Workspace();
        double material = 900.0 / BrnScoreMapping.SCALE;
        double expectedRaw = .2 + .5 * (StrictMath.log1p(material) - StrictMath.log1p(-material));
        assertEquals(StrictMath.tanh(expectedRaw), model.evaluate(board, scratch));
        assertEquals(expectedRaw, scratch.raw());
        var accumulator = new Brn2Accumulator(model); accumulator.rebuild(board);
        assertEquals(model.evaluate(board, scratch), accumulator.evaluate(board));
        assertEquals(expectedRaw, accumulator.raw());
        for (double target : new double[]{.4, -.4}) {
            var trainer = new Brn2Trainer(.001); double prediction = trainer.predict(board);
            double[] before = trainer.snapshot().copyWeights();
            assertEquals(.5 * (prediction - target) * (prediction - target), trainer.train(board, target));
            assertTrue(Arrays.equals(before, 0, OUTPUT_WEIGHT_OFFSET, trainer.weights, 0, OUTPUT_WEIGHT_OFFSET));
            assertNotEquals(0, trainer.weights[OUTPUT_BIAS]);
            assertFalse(Arrays.equals(before, OUTPUT_WEIGHT_OFFSET, PARAMETER_COUNT,
                    trainer.weights, OUTPUT_WEIGHT_OFFSET, PARAMETER_COUNT));
            int trainedScore = score(trainer.snapshot(), board, true);
            assertTrue(target > 0 ? trainedScore > 900 : trainedScore < 900);
            trainer.train(board, target);
            assertFalse(Arrays.equals(before, 0, OUTPUT_WEIGHT_OFFSET, trainer.weights, 0, OUTPUT_WEIGHT_OFFSET),
                    "Upstream learning starts after the first head update");
            assertEquals(900, trainer.materialPrior().score(board));
            assertEquals(Brn2MaterialPrior.BASIC_V1, trainer.snapshot().materialPrior());
            var trained = trainer.snapshot(); accumulator = new Brn2Accumulator(trained); accumulator.rebuild(board);
            assertEquals(trainer.predict(board), accumulator.evaluate(board), 2e-16);
        }
    }

    @Test void modelAndTrainingPayloadsPersistTheFixedPriorAndResumeExactly() throws Exception {
        var trainer = new Brn2Trainer(.001);
        for (boolean trained : new boolean[]{false, true}) {
            if (trained) { trainer.train(board(CASES[6]), -.4); trainer.train(board(CASES[3]), .3); }
            byte[] modelBytes = Brn2Codec.encodeModel(trainer.snapshot()), trainingBytes = Brn2Codec.encodeTraining(trainer);
            assertEquals(2, ByteBuffer.wrap(modelBytes).getInt(8));
            assertEquals(2, ByteBuffer.wrap(trainingBytes).getInt(8));
            assertEquals(Brn2Codec.MODEL_BYTES, modelBytes.length);
            assertEquals(Brn2Codec.TRAINING_BYTES, trainingBytes.length);
            var model = Brn2Codec.decodeModel(modelBytes); var restored = Brn2Codec.decodeTraining(trainingBytes);
            assertEquals(Brn2MaterialPrior.BASIC_V1, model.materialPrior());
            assertEquals(Brn2MaterialPrior.BASIC_V1, restored.materialPrior());
            assertArrayEquals(modelBytes, Brn2Codec.encodeModel(model));
            assertArrayEquals(trainingBytes, Brn2Codec.encodeTraining(restored));
            for (var c : CASES) {
                assertEquals(trainer.predict(board(c)), restored.predict(board(c)));
                assertEquals(trainer.predict(board(c)), model.evaluate(board(c), new Brn2Workspace()));
                assertEquals(c.score(), restored.materialPrior().score(board(c)));
                if (!trained) assertEquals(c.score(), score(model, board(c), true));
            }
            assertEquals(trainer.train(board(CASES[4]), .1), restored.train(board(CASES[4]), .1));
            assertArrayEquals(Brn2Codec.encodeTraining(trainer), Brn2Codec.encodeTraining(restored));
        }
    }

    @Test void formatOneMissingPriorStateKeepsLegacyPredictionsWeightsMomentsAndBytes() throws Exception {
        var trainer = new Brn2Trainer(.001);
        byte[] legacyModel = legacy(Brn2Codec.encodeModel(trainer.snapshot()));
        byte[] legacyTraining = legacy(Brn2Codec.encodeTraining(trainer));
        var model = Brn2Codec.decodeModel(legacyModel); var restored = Brn2Codec.decodeTraining(legacyTraining);
        assertEquals(Brn2MaterialPrior.NONE, model.materialPrior());
        assertEquals(Brn2MaterialPrior.NONE, restored.materialPrior());
        assertArrayEquals(legacyModel, Brn2Codec.encodeModel(model));
        assertArrayEquals(legacyTraining, Brn2Codec.encodeTraining(restored));
        assertEquals(0, score(model, board(CASES[6]), true));
        assertEquals(0, restored.predict(board(CASES[6])));
        // Historical nonzero heads must also remain material-independent and train in the original domain.
        double[] weights = model.copyWeights(); weights[OUTPUT_BIAS] = .2;
        var original = new Brn2Trainer(new Brn2Model(weights), new BrnAdamConfig(.001));
        restored = Brn2Codec.decodeTraining(Brn2Codec.encodeTraining(original));
        assertEquals(StrictMath.tanh(.2), restored.predict(board(CASES[6])));
        assertEquals(StrictMath.tanh(.2), restored.predict(board(CASES[0])));
        assertEquals(original.train(board(CASES[6]), -.3), restored.train(board(CASES[6]), -.3));
        assertArrayEquals(Brn2Codec.encodeTraining(original), Brn2Codec.encodeTraining(restored));
        assertEquals(1, ByteBuffer.wrap(Brn2Codec.encodeTraining(restored)).getInt(8));
    }

    private static byte[] legacy(byte[] bytes) {
        ByteBuffer.wrap(bytes).putInt(8, 1);
        CRC32 crc = new CRC32(); crc.update(bytes, 0, bytes.length - 4);
        ByteBuffer.wrap(bytes).putInt(bytes.length - 4, (int) crc.getValue());
        return bytes;
    }
}
