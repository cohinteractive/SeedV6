package com.ohinteractive.seedv6.training.history;

import java.time.Instant;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Explicit synthetic analytics measurements, never production training evidence. */
public final class HistoryFixtures {
    public static GenerationRecord record(int gen, boolean promoted, int depth, Instant completed, Long duration) {
        return record(gen,promoted,depth,completed,duration,"g000000-s000000000-"+"b".repeat(64));
    }
    public static GenerationRecord record(int gen, boolean promoted, int depth, Instant completed, Long duration, String incumbent) {
        int wins=promoted?30:gen%20, draws=promoted?8:20, losses=64-wins-draws;
        double score=(wins+.5*draws)/64;
        String candidate=String.format("g%06d-s%09d-",gen,gen)+"a".repeat(64), best=incumbent;
        return new GenerationRecord(gen,candidate,best,promoted?candidate:best,
                promoted?GenerationRecord.Outcome.PROMOTED:GenerationRecord.Outcome.RETAINED,
                promoted?PromotionPolicy.Decision.PROMOTE:PromotionPolicy.Decision.RETAIN_INCUMBENT,
                wins,draws,losses,32,0,score,score-.02,.5,new GenerationRecord.Regime(depth,32,32,6),
                32,0,1024L,.14,duration==null?null:completed.minusNanos(duration),completed,null,null,null,duration);
    }
    private HistoryFixtures() {}
}
