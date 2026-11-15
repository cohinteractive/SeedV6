package com.ohinteractive.seedv6.core.nnue.conventional;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Direct128ProductionTest {
    static byte[] bytes() throws Exception {
        try (var in = Direct128ProductionTest.class.getResourceAsStream(ConventionalNnueEngine.Model.DIRECT128.resource())) {
            return in.readAllBytes();
        }
    }
    @Test void defaultAndBothRollbacksKeepSearchAndScale() throws Exception {
        var normal = BaselineEngine.load();
        assertEquals(BaselineEngine.Evaluator.CONVENTIONAL_DIRECT128, normal.evaluator());
        assertEquals(128, normal.conventional().model().hidden());
        assertEquals("S6CNU003", normal.conventional().model().schema());
        assertEquals(8, normal.conventional().model().outputBuckets());
        for (var choice : new BaselineEngine.Evaluator[]{BaselineEngine.Evaluator.CONVENTIONAL_CP64,
                BaselineEngine.Evaluator.CONVENTIONAL_WDL25}) {
            var rollback = BaselineEngine.load(choice);
            assertEquals(256, rollback.conventional().model().hidden());
            for (int workers = 1; workers <= 16; workers++) {
                var config = normal.formulation(workers, 8).search();
                assertEquals(config, rollback.formulation(workers, 8).search());
                assertEquals(config, BaselineEngine.configuration(normal.evaluation(), workers, 8));
                assertEquals(3329490943L, config.mechanisms().flags());
                assertTrue(normal.identity(workers, 8).contains("scoreMapping=BT4_Q_LOGIT400_V1"));
            }
        }
    }
    @Test void independentPythonGoldenScoresMatchScalarVectorAndFullRebuild() throws Exception {
        var automatic = BaselineEngine.load().conventional().model();
        String previous = System.getProperty("seed.nnue.scalar");
        ConventionalNetwork scalar;
        try {
            System.setProperty("seed.nnue.scalar", "true");
            scalar = ConventionalNetwork.read(new ByteArrayInputStream(bytes()));
        } finally {
            if (previous == null) System.clearProperty("seed.nnue.scalar"); else System.setProperty("seed.nnue.scalar", previous);
        }
        assertEquals("scalar-long", scalar.headBackend());
        var a = automatic.newAccumulator(); var b = scalar.newAccumulator();
        try (var reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/direct128-golden.tsv"), StandardCharsets.UTF_8))) {
            int count = 0;
            for (String line : reader.lines().toList()) {
                if (line.startsWith("#")) continue;
                String[] row = line.split("\t"); int expected = Integer.parseInt(row[0]);
                var board = Board.fromFen(row[1]); a.rebuild(board); b.rebuild(board);
                assertEquals(expected, a.evaluate(board), row[1]);
                assertEquals(expected, b.evaluate(board), row[1]);
                assertEquals(expected, automatic.fullRebuild(board), row[1]); count++;
            }
            assertEquals(259, count);
        }
    }
    @Test void strictGeometryLengthChecksumAndBoundedWeightRejection() throws Exception {
        byte[] source = bytes();
        for (int index : new int[]{0,7,8,12,16,20,24,28,32,36,40,72,source.length-1}) {
            byte[] bad = source.clone(); bad[index] ^= 1;
            assertThrows(IOException.class, () -> ConventionalNetwork.read(new ByteArrayInputStream(bad)));
        }
        for (int size : new int[]{0,71,source.length-1,source.length+1})
            assertThrows(IOException.class, () -> ConventionalNetwork.read(new ByteArrayInputStream(Arrays.copyOf(source,size))));
        for (short weight : new short[]{128,-128,32767,-32768}) {
            byte[] bad = source.clone();
            ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN).putShort(72 + (7680 * 128 + 128) * 2, weight);
            System.arraycopy(MessageDigest.getInstance("SHA-256").digest(Arrays.copyOfRange(bad,72,bad.length)),0,bad,40,32);
            assertThrows(IOException.class, () -> ConventionalNetwork.read(new ByteArrayInputStream(bad)));
        }
    }
    @Test void boundedRawDotMatchesBigIntegerIncludingWorstCase() {
        if (!ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent()) return;
        var kernel = new Direct128VectorHead(); var rng = new SplittableRandom(17);
        for (int trial = 0; trial < 200; trial++) {
            int[] us = new int[128], them = new int[128], weights = new int[2048]; int offset = trial % 8 * 256;
            var expected = java.math.BigInteger.ZERO;
            for (int i = 0; i < 256; i++) {
                int raw = trial < 2 ? 255 : rng.nextInt(-1000000,1000000);
                int weight = trial == 0 ? -127 : trial == 1 ? 127 : rng.nextInt(-127,128);
                if (i < 128) us[i] = raw; else them[i-128] = raw;
                weights[offset+i] = weight;
                expected = expected.add(java.math.BigInteger.valueOf(Math.clamp(raw,0,255)).pow(2)
                        .multiply(java.math.BigInteger.valueOf(weight)));
            }
            assertEquals(expected.longValueExact(), kernel.dot(us,them,weights,offset));
        }
    }
}
