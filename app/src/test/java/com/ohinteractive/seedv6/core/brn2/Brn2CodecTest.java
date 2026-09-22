package com.ohinteractive.seedv6.core.brn2;

import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;
import static com.ohinteractive.seedv6.core.brn2.Brn2Model.*;
import static org.junit.jupiter.api.Assertions.*;

class Brn2CodecTest {
    @TempDir Path temporary;
    private static final long[][] BOARDS = {Board.startingPosition(),
            Board.fromFen("7k/8/8/8/8/8/PPP5/7K w - - 0 1"),
            Board.fromFen("r3k2r/pppq1ppp/2npbn2/3Np3/2B1P3/2N2Q1P/PPP2PP1/R3K2R b KQkq - 4 12"),
            new long[5], new long[] {0, 0, 0, 1L << 63, Long.MIN_VALUE}};

    @Test
    void modelRoundTripPreservesEveryBinary64BitAndEvaluation() throws Exception {
        double[] weights = initialWeights();
        weights[PARAMETER_COUNT - 1] = -0.0;
        weights[PARAMETER_COUNT - 2] = Double.MIN_VALUE;
        Brn2Model original = new Brn2Model(weights);
        byte[] bytes = Brn2Codec.encodeModel(original);
        assertEquals(13165364, bytes.length);
        assertEquals(Brn2Codec.MODEL_BYTES, bytes.length);
        ByteBuffer header = ByteBuffer.wrap(bytes);
        assertEquals(Brn2Codec.MODEL_MAGIC, header.getLong());
        assertEquals(1, header.getInt());
        assertEquals(1, header.getInt());
        assertEquals(960, header.getInt());
        assertEquals(25200, header.getInt());
        assertEquals(64, header.getInt());
        assertEquals(2, header.getInt());
        assertEquals(32, header.getInt());
        assertEquals(1645665, header.getInt());
        Path file = temporary.resolve("model.brn");
        try (var output = new java.io.BufferedOutputStream(Files.newOutputStream(file))) { Brn2Codec.writeModel(original, output); }
        Brn2Model restored;
        try (var input = new java.io.BufferedInputStream(Files.newInputStream(file))) { restored = Brn2Codec.readModel(input); }
        assertArrayEquals(bytes, Files.readAllBytes(file));
        assertArrayEquals(bytes, Brn2Codec.encodeModel(restored));
        for (int i = 0; i < PARAMETER_COUNT; i++) {
            assertEquals(Double.doubleToRawLongBits(original.weight(i)), Double.doubleToRawLongBits(restored.weight(i)));
        }
        for (long[] board : BOARDS) {
            assertEquals(original.evaluate(board, new Brn2Workspace()), restored.evaluate(board, new Brn2Workspace()));
        }
    }

    @Test
    void resumedAdamIsBitIdenticalOnTheNextExampleAndAcrossInactiveFeatureGaps() throws Exception {
        BrnAdamConfig hp = new BrnAdamConfig(0.0037, 0.83, 0.971, 2e-7);
        Brn2Trainer uninterrupted = new Brn2Trainer(new Brn2Model(initialWeights()), hp);
        for (int step = 0; step < 47; step++) uninterrupted.train(BOARDS[step % BOARDS.length], target(step));
        Path file = temporary.resolve("training.brn");
        try (var output = new java.io.BufferedOutputStream(Files.newOutputStream(file))) { Brn2Codec.writeTraining(uninterrupted, output); }
        assertEquals(39496044, Files.size(file));
        assertEquals(Brn2Codec.TRAINING_BYTES, Files.size(file));
        Brn2Trainer restored;
        try (var input = new java.io.BufferedInputStream(Files.newInputStream(file))) { restored = Brn2Codec.readTraining(input); }
        assertEquals(hp, restored.config());
        assertEquals(47, restored.optimizer().step());
        assertArrayEquals(Brn2Codec.encodeTraining(uninterrupted), Brn2Codec.encodeTraining(restored));
        for (int step = 47; step < 57; step++) {
            long[] board = BOARDS[step % BOARDS.length];
            assertEquals(uninterrupted.predict(board), restored.predict(board));
            assertEquals(uninterrupted.train(board, target(step)), restored.train(board, target(step)));
            // Entire payload comparison includes every weight, both moments, config and global clock.
            assertArrayEquals(Brn2Codec.encodeTraining(uninterrupted), Brn2Codec.encodeTraining(restored));
        }
        for (long[] board : BOARDS) {
            Brn2Model restoredInference = Brn2Codec.decodeModel(Brn2Codec.encodeModel(restored.snapshot()));
            assertEquals(uninterrupted.predict(board), restoredInference.evaluate(board, new Brn2Workspace()));
        }
    }

    @Test
    void freshStateIsExactlyResumableAndModelPayloadCannotMasqueradeAsTraining() throws Exception {
        Brn2Trainer original = new Brn2Trainer(0.01);
        Brn2Trainer restored = Brn2Codec.decodeTraining(Brn2Codec.encodeTraining(original));
        assertEquals(0, restored.optimizer().step());
        original.train(BOARDS[0], -0.5);
        restored.train(BOARDS[0], -0.5);
        assertArrayEquals(Brn2Codec.encodeTraining(original), Brn2Codec.encodeTraining(restored));
        assertThrows(IOException.class, () -> Brn2Codec.decodeTraining(Brn2Codec.encodeModel(original.snapshot())));
        assertThrows(IOException.class, () -> Brn2Codec.decodeModel(Brn2Codec.encodeTraining(original)));
        byte[] nnueHeader = ByteBuffer.allocate(20).putLong(0x53364e4e55453031L).array();
        assertThrows(IOException.class, () -> Brn2Codec.decodeModel(nnueHeader));
    }

    @Test
    void corruptTruncatedTrailingAndUnsupportedFramesAreRejected() throws Exception {
        byte[] model = Brn2Codec.encodeModel(new Brn2Model(initialWeights()));
        byte[] training = Brn2Codec.encodeTraining(new Brn2Trainer(0.01));
        for (byte[] bytes : new byte[][] {model, training}) {
            for (int length : new int[] {0, 7, 19, 20, bytes.length / 2, bytes.length - 1}) {
                assertRejected(Arrays.copyOf(bytes, length), bytes == training);
            }
            assertRejected(Arrays.copyOf(bytes, bytes.length + 1), bytes == training);
            for (int offset : new int[] {0, 8, 12, 16, 100, bytes.length - 1}) {
                byte[] corrupt = bytes.clone();
                corrupt[offset] ^= 0x40;
                assertRejected(corrupt, bytes == training);
            }
            for (int offset : new int[] {0, 8, 12, 16, 20, 24, 28, 32, 36}) {
                byte[] unsupported = bytes.clone();
                unsupported[offset] ^= 0x40;
                checksum(unsupported);
                assertRejected(unsupported, bytes == training);
            }
        }
    }

    @Test
    void checksumValidButMathematicallyInvalidPayloadsAreRejected() throws Exception {
        byte[] model = Brn2Codec.encodeModel(new Brn2Model());
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            byte[] corrupt = model.clone();
            ByteBuffer.wrap(corrupt).putDouble(Brn2Codec.HEADER_BYTES, bad);
            checksum(corrupt);
            assertRejected(corrupt, false);
        }
        Brn2Trainer trainer = new Brn2Trainer(0.01);
        trainer.train(BOARDS[1], 0.5);
        byte[] training = Brn2Codec.encodeTraining(trainer);
        int weightsOffset = Brn2Codec.HEADER_BYTES + 40;
        int firstOffset = weightsOffset + 8 * PARAMETER_COUNT;
        int secondOffset = firstOffset + 8 * PARAMETER_COUNT;
        for (int offset : new int[] {48, 56, 64, 72, weightsOffset, firstOffset, secondOffset}) {
            byte[] corrupt = training.clone();
            ByteBuffer.wrap(corrupt).putDouble(offset, Double.NaN);
            checksum(corrupt);
            assertRejected(corrupt, true);
        }
        byte[] negativeSecond = training.clone();
        ByteBuffer.wrap(negativeSecond).putDouble(secondOffset, -1);
        checksum(negativeSecond);
        assertRejected(negativeSecond, true);
        for (long step : new long[] {-1, 0}) {
            byte[] invalidStep = training.clone();
            ByteBuffer.wrap(invalidStep).putLong(Brn2Codec.HEADER_BYTES, step);
            checksum(invalidStep);
            assertRejected(invalidStep, true);
        }
    }

    @Test
    void codecsHandleShortReadsAndLeaveStreamsCallerOwned() throws Exception {
        class Output extends ByteArrayOutputStream {
            boolean closed;
            @Override public void close() { closed = true; }
        }
        class Input extends ByteArrayInputStream {
            boolean closed;
            Input(byte[] bytes) { super(bytes); }
            @Override public synchronized int read(byte[] bytes, int offset, int length) {
                return super.read(bytes, offset, Math.min(length, 3));
            }
            @Override public void close() { closed = true; }
        }
        Output modelOutput = new Output(), trainingOutput = new Output();
        Brn2Trainer trainer = new Brn2Trainer(0.01);
        Brn2Codec.writeModel(trainer.snapshot(), modelOutput);
        Brn2Codec.writeTraining(trainer, trainingOutput);
        Input modelInput = new Input(modelOutput.toByteArray()), trainingInput = new Input(trainingOutput.toByteArray());
        Brn2Codec.readModel(modelInput);
        Brn2Codec.readTraining(trainingInput);
        assertFalse(modelOutput.closed || trainingOutput.closed || modelInput.closed || trainingInput.closed);
    }

    private static double[] initialWeights() {
        double[] weights = new double[PARAMETER_COUNT];
        for (int i = 0; i < weights.length; i++) weights[i] = (i % 17 - 8) * 0.000001;
        return weights;
    }
    private static double target(int step) { return (step % 9 - 4) / 5.0; }
    private static void assertRejected(byte[] bytes, boolean training) {
        if (training) assertThrows(IOException.class, () -> Brn2Codec.decodeTraining(bytes));
        else assertThrows(IOException.class, () -> Brn2Codec.decodeModel(bytes));
    }
    private static void checksum(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, bytes.length - 4);
        ByteBuffer.wrap(bytes).putInt(bytes.length - 4, (int) crc.getValue());
    }
}
