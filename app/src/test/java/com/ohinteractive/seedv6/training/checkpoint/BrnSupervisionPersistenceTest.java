package com.ohinteractive.seedv6.training.checkpoint;

import java.nio.file.*;
import java.io.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.validation.HeldOutLoss;
import static org.junit.jupiter.api.Assertions.*;

class BrnSupervisionPersistenceTest {
    @TempDir Path temp;
    static final String HASH = "0".repeat(64), ID = "g000000-s000000000-" + HASH;
    @Test void originalWdlPlanAndValidationBytesRemainExact() throws Exception {
        var source = TrainingSource.bootstrap(temp.resolve("nnue"));
        byte[] original = SmallRecord.encode("brn-bootstrap-plan-v1", out -> {
            out.writeUTF(ID); out.writeUTF(ID); out.writeLong(1); out.writeUTF(source.generatorStore());
            out.writeUTF(ID); out.writeUTF(HASH); out.writeUTF("settings"); out.writeLong(81);
        });
        Path file = temp.resolve("old.plan"); Files.write(file, original);
        var plan = BootstrapPlan.read(file); assertEquals(BrnSupervision.WDL, plan.supervision());
        assertArrayEquals(original, plan.encode());
        byte[] oldValidation = SmallRecord.encode("validation", out -> {
            out.writeUTF(ID); out.writeUTF(ID); out.writeUTF("brn-terminal-wdl-heldout-v1");
            out.writeUTF(source.generatorStore()); out.writeUTF(ID); out.writeUTF(HASH); out.writeUTF(HASH);
            out.writeLong(81); out.writeInt(2); out.writeInt(2); out.writeInt(2);
            out.writeInt(2); out.writeDouble(.4); out.writeDouble(.5);
        });
        var record = SmallRecord.decode(oldValidation, "validation", in -> ValidationRecord.read("v-" + HASH, in));
        assertEquals(BrnSupervision.WDL, record.bootstrap().supervision()); assertNull(record.bootstrap().teacherLoss());
        assertArrayEquals(oldValidation, record.encode());
    }
    @Test void blendedPlanAndEvidenceRoundTripExactBinaryWeightAndDescriptiveLosses() throws Exception {
        for (double weight : new double[] {0, .5, .75, 1, .12345678901234567}) {
            var supervision = BrnSupervision.blended(weight);
            var plan = new BootstrapPlan(ID, ID, 1, TrainingSource.bootstrap(temp.resolve("nnue")), ID, HASH, "settings", 81, supervision);
            Path file = temp.resolve("new.plan"); Files.write(file, plan.encode());
            var restored = BootstrapPlan.read(file); assertEquals(plan, restored); assertEquals(plan.hash(), restored.hash());
            var evidence = new BootstrapEvidence(plan.source().generatorStore(), ID, HASH, HASH, 81, 2, 2, 2,
                    new HeldOutLoss.Comparison(2, .1, .2), supervision,
                    new HeldOutLoss.Comparison(2, .8, .3), new HeldOutLoss.Comparison(2, .2, .5));
            var record = ValidationRecord.create(ID, ID, evidence);
            var decoded = SmallRecord.decode(record.encode(), "validation", in -> ValidationRecord.read(record.id(), in));
            assertEquals(record, decoded); assertArrayEquals(record.encode(), decoded.encode());
            assertEquals(com.ohinteractive.seedv6.training.validation.PromotionPolicy.Decision.PROMOTE, decoded.decision());
        }
    }
    @Test void initializedObjectiveSurvivesInterruptedBootstrapAndCannotBeRewritten() throws Exception {
        Path root = temp.resolve("new");
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            store.initializeBrnSupervision(BrnSupervision.blended(.75));
        }
        assertTrue(CheckpointInspection.freshRoot(root, TrainingArchitecture.BRN2));
        assertEquals(BrnSupervision.blended(.75), CheckpointStore.readBrnSupervision(root).orElseThrow());
        byte[] before = Files.readAllBytes(root.resolve(CheckpointStore.BRN_SUPERVISION_FILE));
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN2)) {
            store.initializeBrnSupervision(BrnSupervision.blended(.75));
            assertThrows(IOException.class, () -> store.initializeBrnSupervision(BrnSupervision.WDL));
        }
        assertArrayEquals(before, Files.readAllBytes(root.resolve(CheckpointStore.BRN_SUPERVISION_FILE)));
    }
    @Test void corruptOrUnknownSupervisionFailsAndDoesNotDefaultToWdl() throws Exception {
        Path root = Files.createDirectory(temp.resolve("invalid")); Path file = root.resolve(CheckpointStore.BRN_SUPERVISION_FILE);
        Files.write(file, SmallRecord.encode("brn-supervision-v1", out -> { out.writeUTF("UNKNOWN"); out.writeDouble(.75); }));
        assertThrows(IOException.class, () -> CheckpointStore.readBrnSupervision(root));
        for (double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -.1, 1.1}) {
            Files.write(file, SmallRecord.encode("brn-supervision-v1", out -> { out.writeUTF("NNUE_BLENDED"); out.writeDouble(value); }));
            assertThrows(IOException.class, () -> CheckpointStore.readBrnSupervision(root));
        }
        byte[] valid = SmallRecord.encode("brn-supervision-v1", BrnSupervision.blended(.75)::write);
        valid[valid.length - 1] ^= 1; Files.write(file, valid);
        assertThrows(IOException.class, () -> CheckpointStore.readBrnSupervision(root));
    }
}
