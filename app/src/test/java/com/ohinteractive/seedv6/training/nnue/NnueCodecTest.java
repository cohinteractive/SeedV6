package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.nnue.NnueBinaryFormat;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.nnue.NnueNetworkCodec;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import static com.ohinteractive.seedv6.training.nnue.TrainingFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class NnueCodecTest {
    @Test
    void immutableCodecRoundTripsEveryFloatBitIncludingSignedZeroSubnormalsAndFiniteExtremes() throws IOException {
        Parameters p = TrainableNnue.initialized(129).parameters;
        for (float[] group : p.groups) {
            group[0] = -0.0f;
            if (group.length > 1) {
                group[1] = Float.MIN_VALUE;
                group[group.length - 1] = -Float.MAX_VALUE;
            }
        }
        NnueNetwork original = p.snapshot();
        byte[] encoded = NnueNetworkCodec.encode(original);
        assertEquals(NnueNetworkCodec.ENCODED_BYTES, encoded.length);
        NnueNetwork decoded = NnueNetworkCodec.decode(encoded);
        assertBits(p, Parameters.from(decoded));
        assertArrayEquals(encoded, NnueNetworkCodec.encode(decoded));
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(decoded.outputBias()));
        System.out.printf("NETWORK_CODEC version=%d bytes=%d header=%d exactBits=true%n",
                NnueNetworkCodec.VERSION, encoded.length, NnueBinaryFormat.HEADER_BYTES);
    }

    @Test
    void trainingRoundTripAndResumeAreBitIdenticalToUninterruptedIncludingUntouchedMomentRows() throws IOException {
        NnueTrainer continuous = new NnueTrainer(model(), new AdamHyperparameters(0.007, 0.8, 0.95, 1e-7));
        long[][] batchA = {WHITE, BLACK};
        double[] targetsA = {0.8, -0.8};
        continuous.trainBatch(batchA, targetsA, 2);
        byte[] boundary = TrainingStateCodec.encode(continuous);
        assertEquals(TrainingStateCodec.ENCODED_BYTES, boundary.length);
        NnueTrainer resumed = TrainingStateCodec.decode(boundary);
        assertArrayEquals(boundary, TrainingStateCodec.encode(resumed));
        assertBits(continuous.model().parameters, resumed.model().parameters);
        assertBits(continuous.optimizer().firstMoment, resumed.optimizer().firstMoment);
        assertBits(continuous.optimizer().secondMoment, resumed.optimizer().secondMoment);
        assertEquals(continuous.optimizer().hyperparameters(), resumed.optimizer().hyperparameters());
        assertEquals(continuous.optimizer().step(), resumed.optimizer().step());
        // B changes king anchors, so A's rows now have zero gradients but surviving moments.
        long[][] batchB = {OTHER};
        double[] targetsB = {0.25};
        float previous = continuous.model().parameters.groups[1][WHITE_PAWN * 64];
        for (int i = 0; i < 3; i++) {
            assertEquals(continuous.trainBatch(batchB, targetsB, 1), resumed.trainBatch(batchB, targetsB, 1));
        }
        assertNotEquals(previous, continuous.model().parameters.groups[1][WHITE_PAWN * 64]);
        assertArrayEquals(TrainingStateCodec.encode(continuous), TrainingStateCodec.encode(resumed));
        System.out.printf("TRAINING_CODEC version=%d bytes=%d exactBits=true resumedSteps=%d%n",
                TrainingStateCodec.VERSION, boundary.length, resumed.optimizer().step());
    }

    @Test
    void trainingCodecPreservesSignedZerosAndSubnormalMomentsAndDerivesTheirRowCache() throws IOException {
        TrainableNnue model = model();
        Parameters first = new Parameters(), second = new Parameters();
        for (int group = 0; group < first.groups.length; group++) {
            first.groups[group][0] = -0.0f;
            second.groups[group][0] = -0.0f;
        }
        first.groups[1][64] = Float.MIN_VALUE;
        second.groups[1][64] = Float.MIN_VALUE;
        NnueTrainer original = new NnueTrainer(model, new AdamOptimizer(AdamHyperparameters.DEFAULT, 5, first, second));
        byte[] encoded = TrainingStateCodec.encode(original);
        NnueTrainer decoded = TrainingStateCodec.decode(encoded);
        assertArrayEquals(encoded, TrainingStateCodec.encode(decoded));
        original.trainBatch(new long[][] {WHITE}, new double[] {0.5}, 1);
        decoded.trainBatch(new long[][] {WHITE}, new double[] {0.5}, 1);
        assertBits(original.optimizer().firstMoment, decoded.optimizer().firstMoment);
        assertBits(original.optimizer().secondMoment, decoded.optimizer().secondMoment);
        assertBits(original.model().parameters, decoded.model().parameters);
    }

    @Test
    void bothCodecsRejectIdentityDimensionsTruncationCrcDamageAndTrailingData() throws IOException {
        byte[] network = NnueNetworkCodec.encode(model().snapshot());
        verifyFraming(network, false);
        byte[] training = TrainingStateCodec.encode(new NnueTrainer(model()));
        verifyFraming(training, true);
        assertThrows(IOException.class, () -> TrainingStateCodec.decode(network));
        assertThrows(IOException.class, () -> NnueNetworkCodec.decode(training));
    }

    @Test
    void checksummedNonfiniteParametersAndImpossibleOptimizerStateAreRejected() throws IOException {
        byte[] network = NnueNetworkCodec.encode(model().snapshot());
        int header = NnueBinaryFormat.HEADER_BYTES;
        for (int bits : new int[] {0x7fc00001, 0x7f800000, 0xff800000}) {
            patchIntReject(network, header, bits, false);
        }
        NnueTrainer trained = new NnueTrainer(model());
        trained.trainBatch(new long[][] {WHITE}, new double[] {0.8}, 1);
        byte[] training = TrainingStateCodec.encode(trained);
        int parameters = header + 40;
        patchIntReject(training, parameters, 0x7fc00001, true);
        patchIntReject(training, parameters + NnueBinaryFormat.PARAMETER_BYTES, 0x7f800000, true);
        patchIntReject(training, parameters + 2 * NnueBinaryFormat.PARAMETER_BYTES, Float.floatToRawIntBits(-0.1f), true);
        patchLongReject(training, header, -1); // negative step
        patchLongReject(training, header, 0); // trained moments cannot exist at step zero
        patchLongReject(training, header + 8, Double.doubleToRawLongBits(Double.NaN));
        patchLongReject(training, header + 16, Double.doubleToRawLongBits(1)); // beta1
        patchLongReject(training, header + 24, Double.doubleToRawLongBits(-0.1)); // beta2
        patchLongReject(training, header + 32, Double.doubleToRawLongBits(0)); // epsilon
    }

    @Test
    void streamApisLeaveCallerStreamsOpen() throws IOException {
        class Output extends ByteArrayOutputStream {
            boolean closed;
            @Override public void close() { closed = true; }
        }
        class Input extends ByteArrayInputStream {
            boolean closed;
            Input(byte[] bytes) { super(bytes); }
            @Override public void close() { closed = true; }
        }
        Output output = new Output();
        NnueNetworkCodec.write(model().snapshot(), output);
        assertFalse(output.closed);
        Input input = new Input(output.toByteArray());
        NnueNetworkCodec.read(input);
        assertFalse(input.closed);
        output.reset();
        TrainingStateCodec.write(new NnueTrainer(model()), output);
        assertFalse(output.closed);
        input = new Input(output.toByteArray());
        TrainingStateCodec.read(input);
        assertFalse(input.closed);
    }

    private static void verifyFraming(byte[] bytes, boolean training) throws IOException {
        int header = NnueBinaryFormat.HEADER_BYTES;
        // Magic, codec version, schema version, hostile ID length, schema byte, every dimension.
        for (int offset : new int[] {0, 8, 12, 16, 20, header - 32, header - 28, header - 24,
                header - 20, header - 16, header - 12, header - 8, header - 4}) {
            byte saved = bytes[offset];
            bytes[offset] ^= 0x40;
            assertThrows(IOException.class, () -> read(bytes, bytes.length, training));
            bytes[offset] = saved;
        }
        for (int length : new int[] {0, 1, 8, 12, 16, 20, header - 1, header,
                header + 100, bytes.length / 2, bytes.length - 4, bytes.length - 1}) {
            assertThrows(IOException.class, () -> read(bytes, length, training));
        }
        int corruption = bytes.length - 8;
        bytes[corruption] ^= 1;
        IOException checksum = assertThrows(IOException.class, () -> read(bytes, bytes.length, training));
        assertTrue(checksum.getMessage().contains("CRC32"));
        bytes[corruption] ^= 1;
        SequenceInputStream trailing = new SequenceInputStream(new ByteArrayInputStream(bytes),
                new ByteArrayInputStream(new byte[] {0}));
        assertThrows(IOException.class, () -> {
            if (training) TrainingStateCodec.read(trailing); else NnueNetworkCodec.read(trailing);
        });
    }

    private static void read(byte[] bytes, int length, boolean training) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(bytes, 0, length);
        if (training) TrainingStateCodec.read(input); else NnueNetworkCodec.read(input);
    }

    private static void patchIntReject(byte[] bytes, int offset, int bits, boolean training) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int saved = buffer.getInt(offset);
        buffer.putInt(offset, bits);
        checksum(bytes);
        assertThrows(IOException.class, () -> read(bytes, bytes.length, training));
        buffer.putInt(offset, saved);
        checksum(bytes);
    }

    private static void patchLongReject(byte[] bytes, int offset, long bits) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long saved = buffer.getLong(offset);
        buffer.putLong(offset, bits);
        checksum(bytes);
        assertThrows(IOException.class, () -> read(bytes, bytes.length, true));
        buffer.putLong(offset, saved);
        checksum(bytes);
    }

    private static void checksum(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, bytes.length - 4);
        ByteBuffer.wrap(bytes).putInt(bytes.length - 4, (int) crc.getValue());
    }
}
