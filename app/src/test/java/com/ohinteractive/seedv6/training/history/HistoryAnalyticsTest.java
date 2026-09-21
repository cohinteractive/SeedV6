package com.ohinteractive.seedv6.training.history;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.history.HistoryFixtures.*;

class HistoryAnalyticsTest {
    private final Clock clock=Clock.fixed(Instant.parse("2026-09-20T02:00:00Z"),ZoneId.of("Pacific/Auckland"));
    @Test void rangesTodayAndConsecutiveRegimes() {
        List<GenerationRecord> records=new ArrayList<>();
        for(int n=1;n<=120;n++) records.add(record(n,n%10==0,n<61?4:5,clock.instant().minusSeconds((120-n)*600L),60_000_000_000L));
        for(var range:HistoryAnalytics.Range.values()) {
            var selection=HistoryAnalytics.select(records,range,clock);
            int expected=switch(range){case LAST_25->25;case LAST_50->50;case LAST_100->100;case ALL->120;case TODAY->85;};
            assertEquals(expected,selection.records().size(),range.toString());
        }
        var all=HistoryAnalytics.select(records,HistoryAnalytics.Range.ALL,clock);
        assertEquals(2,all.regimes().size());assertEquals(60,all.regimes().getFirst().last());assertEquals(61,all.regimes().getLast().first());
        var s=all.summary();assertEquals(12,s.promotions());assertEquals(.1,s.promotionFrequency(),1e-10);
        assertEquals(60,s.averageSeconds());assertEquals(60,s.generationsPerHour());assertEquals(10,s.generationsPerPromotion());assertEquals(600,s.secondsPerPromotion());
    }
    @Test void absentDurationsZeroPromotionsAndEmptyAreHonest() {
        var records=List.of(record(1,false,4,clock.instant(),null),record(2,false,4,clock.instant(),120_000_000_000L));
        var s=HistoryAnalytics.select(records,HistoryAnalytics.Range.ALL,clock).summary();
        assertEquals(1,s.timedGenerations());assertEquals(0,s.promotionFrequency());assertNull(s.generationsPerPromotion());assertNull(s.secondsPerPromotion());
        assertEquals(120,s.averageSeconds());assertEquals(30,s.generationsPerHour());
        s=HistoryAnalytics.select(List.of(),HistoryAnalytics.Range.ALL,clock).summary();
        assertNull(s.averageSeconds());assertNull(s.promotionFrequency());assertNull(s.generationsPerHour());
    }
    @Test void todayUsesLocalDayAndRegimesCanReturnToEarlierSettings() {
        var r=List.of(record(1,false,4,Instant.parse("2026-09-19T11:59:59Z"),null),
                record(2,true,5,Instant.parse("2026-09-19T12:00:00Z"),null),record(3,false,4,clock.instant(),null));
        assertEquals(2,HistoryAnalytics.select(r,HistoryAnalytics.Range.TODAY,clock).records().size());
        assertEquals(3,HistoryAnalytics.select(r,HistoryAnalytics.Range.ALL,clock).regimes().size());
    }
}
