package com.ohinteractive.seedv6.training.history;

import java.time.*;
import java.util.*;

/** Pure range calculations; duration metrics use recorded active time, never wall-clock gaps. */
public final class HistoryAnalytics {
    public enum Range {
        LAST_25("Last 25",25), LAST_50("Last 50",50), LAST_100("Last 100",100), TODAY("Today",0), ALL("All",0);
        private final String label; final int limit;
        Range(String label,int limit) { this.label=label; this.limit=limit; }
        @Override public String toString() { return label; }
    }
    public record Summary(int generations, long promotions, int timedGenerations, double activeSeconds,
            Double promotionFrequency, Double averageSeconds, Double generationsPerHour,
            Double generationsPerPromotion, Double secondsPerPromotion) {}
    public record RegimeSpan(long first, long last, GenerationRecord.Regime regime) {}
    public record Selection(List<GenerationRecord> records, Summary summary, List<RegimeSpan> regimes) {}
    public static Selection select(List<GenerationRecord> source, Range range, Clock clock) {
        List<GenerationRecord> records;
        if (range == Range.TODAY) {
            LocalDate day = LocalDate.now(clock);
            records = source.stream().filter(r -> r.completed().atZone(clock.getZone()).toLocalDate().equals(day)).toList();
        } else records = List.copyOf(source.subList(range.limit == 0 ? 0 : Math.max(0,source.size()-range.limit),source.size()));
        long promotions = records.stream().filter(GenerationRecord::promoted).count();
        int timed = 0; double seconds = 0;
        List<RegimeSpan> regimes = new ArrayList<>();
        for (var r : records) {
            if (r.totalNanos() != null) { timed++; seconds += r.totalNanos()/1e9; }
            if (regimes.isEmpty() || !regimes.getLast().regime().equals(r.regime()))
                regimes.add(new RegimeSpan(r.generation(),r.generation(),r.regime()));
            else { var previous = regimes.removeLast(); regimes.add(new RegimeSpan(previous.first(),r.generation(),r.regime())); }
        }
        return new Selection(records, new Summary(records.size(),promotions,timed,seconds,
                records.isEmpty()?null:(double)promotions/records.size(), timed==0?null:seconds/timed,
                seconds<=0?null:3600*timed/seconds, promotions==0?null:(double)records.size()/promotions,
                promotions==0 || timed==0?null:seconds/promotions),List.copyOf(regimes));
    }
    private HistoryAnalytics() {}
}
