package com.ohinteractive.seedv6.training.checkpoint;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.training.selfplay.*;

class StreamingRecordTest {
    @TempDir Path temporary;
    @Test void streamingFrameRetainsExactLegacyBytesHashAndLazySamples() throws Exception {
        SmallRecord.Writer payload = out -> {
            out.writeInt(1001);
            for (int i = 0; i < 1001; i++) { for (int j = 0; j < 6; j++) out.writeLong(i + j); out.writeDouble(i % 3 - 1); }
        };
        byte[] legacy = SmallRecord.encode("test", payload, 100000);
        Path file = temporary.resolve("record.bin"); SmallRecord.write(file, "test", payload, 100000);
        assertArrayEquals(legacy, Files.readAllBytes(file));
        assertEquals(SmallRecord.hash(legacy), SmallRecord.hash("test", payload, 100000));
        var samples = SmallRecord.read(file, "test", in -> SmallRecord.samples(in, in.readInt()), 100000);
        assertInstanceOf(SampleStore.class, samples);
        for (int i = 0; i < samples.size(); i++) { assertEquals(i, samples.get(i).board()[0]); assertEquals(i % 3 - 1, samples.get(i).target()); }
        legacy[100] ^= 1; Files.write(file, legacy);
        assertThrows(java.io.IOException.class, () -> SmallRecord.read(file, "test", in -> SmallRecord.samples(in, in.readInt()), 100000));
    }
    @Test void validHashDoesNotHideInvalidSampleOrTrailingBytes() throws Exception {
        Path invalid = temporary.resolve("invalid.bin");
        SmallRecord.write(invalid, "test", out -> { for (int i = 0; i < 6; i++) out.writeLong(0); out.writeDouble(.5); }, 100000);
        assertThrows(java.io.IOException.class, () -> SmallRecord.read(invalid, "test", in -> SmallRecord.samples(in, 1), 100000));
        Path trailing = temporary.resolve("trailing.bin");
        SmallRecord.write(trailing, "test", out -> out.writeLong(0), 100000);
        assertThrows(java.io.IOException.class, () -> SmallRecord.read(trailing, "test", in -> in.readInt(), 100000));
    }
    @Test void extendedLengthFrameReadsAndRejectsInvalidLengthsWithoutAllocatingThePayload() throws Exception {
        for (long length : new long[] {8, (long) Integer.MAX_VALUE + 8}) {
            var bytes = new java.io.ByteArrayOutputStream();
            var out = new java.io.DataOutputStream(bytes);
            out.writeInt(0x53364350); out.writeInt(2); out.writeUTF("wide-test"); out.writeLong(length); out.writeLong(71);
            out.write(SmallRecord.digest().digest(bytes.toByteArray()));
            Path file = temporary.resolve("wide-" + length); Files.write(file, bytes.toByteArray());
            if (length == 8) assertEquals(71L, SmallRecord.read(file, "wide-test", java.io.DataInputStream::readLong, SmallRecord.MAX_DATA_BYTES));
            else assertThrows(java.io.IOException.class, () -> SmallRecord.read(file, "wide-test", java.io.DataInputStream::readLong, SmallRecord.MAX_DATA_BYTES));
        }
    }
    @Test void generatedDatasetCanExceedTheOldSixtyFourMiBLimitWithoutSerializationArrays() throws Exception {
        try (var workspace = new SampleStore.Workspace()) {
            int count = 1_200_000;
            var sample = new TrajectorySampler.Sample(com.ohinteractive.seedv6.core.Board.startingPosition(), 1);
            var training = SampleStore.mutable(); for (int i = 0; i < count; i++) training.add(sample);
            var partition = new BootstrapPartition(training, List.of(sample, sample), List.of(0, 1), List.of(2, 3));
            var statistics = new SelfPlayBatch.Statistics(4, 4, 0, 4, 0, 0, 0, count + 2, 1, count,
                    (count + 2) / 4.0, count + 2, count + 2);
            var data = new BootstrapData("0".repeat(64), partition, statistics, 0);
            Path file = temporary.resolve("large-data"); data.write(file);
            assertTrue(Files.size(file) > 64L * 1024 * 1024);
            var restored = BootstrapData.read(file);
            assertInstanceOf(SampleStore.class, restored.partition().training());
            assertEquals(count, restored.partition().training().size());
            assertEquals(sample.target(), restored.partition().training().get(count - 1).target());
            assertEquals(data.hash(), SmallRecord.hash(file));
        }
    }
}
