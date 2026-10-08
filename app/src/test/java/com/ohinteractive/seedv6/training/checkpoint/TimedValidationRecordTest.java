package com.ohinteractive.seedv6.training.checkpoint;

import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimedValidationRecordTest {
    @Test void timeAndDepthKeepDifferentImmutableEvidenceAndDepthRetainsItsLegacyEncoding() throws Exception {
        String candidate = "g000001-s000000001-" + "1".repeat(64), incumbent = "g000000-s000000000-" + "0".repeat(64);
        var game = new ValidationResult.Game(GameTermination.STALEMATE, 0);
        var policy = new PromotionPolicy(1, .05, 0);
        ValidationRecord old = null;
        for (long millis : new long[]{0, 50}) {
            var config = new ValidationConfig(1, 1, 0, 8, 4, 4, NnueScoreMapping.V1, 1024, millis);
            var result = new ValidationResult(config, "0".repeat(64), List.of(new ValidationResult.Pair("0".repeat(64), game, game)));
            var record = ValidationRecord.create(candidate, incumbent, result, policy);
            var roundTrip = SmallRecord.decode(record.encode(), "validation", in -> ValidationRecord.read(record.id(), in));
            assertEquals(record, roundTrip); assertEquals(candidate, roundTrip.candidateId()); assertEquals(incumbent, roundTrip.incumbentId());
            assertEquals(2, roundTrip.statistics().draws()); assertEquals(millis, roundTrip.config().moveMillis());
            if (millis == 0) {
                old = record;
                assertTrue(new String(record.encode(), StandardCharsets.ISO_8859_1).contains(ValidationConfig.SEARCH_POLICY));
            } else {
                assertNotEquals(old.id(), record.id());
                assertTrue(new String(record.encode(), StandardCharsets.ISO_8859_1).contains(ValidationConfig.TIMED_SEARCH_POLICY));
                assertArrayEquals(old.encode(), SmallRecord.decode(old.encode(), "validation", in -> ValidationRecord.read("v-" + "0".repeat(64), in)).encode());
            }
        }
    }
}
