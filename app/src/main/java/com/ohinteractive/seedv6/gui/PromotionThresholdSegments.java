package com.ohinteractive.seedv6.gui;

import java.util.*;
import com.ohinteractive.seedv6.training.history.GenerationRecord;

/** Horizontal paths only. Unknown rows, missing generations and regime changes break the path. */
final class PromotionThresholdSegments {
    record Segment(long first, long last, double value) {}
    static List<Segment> from(List<GenerationRecord> records) {
        var segments = new ArrayList<Segment>();
        int stride = Math.max(1, (records.size() + 599) / 600);
        GenerationRecord previous = null;
        for (int offset = 0; offset < records.size(); offset += stride) {
            int end = Math.min(records.size(), offset + stride);
            var first = records.get(offset); var last = records.get(end - 1);
            boolean known = first.rawPromotionThreshold() != null;
            for (int i = offset + 1; known && i < end; i++) {
                var r = records.get(i);
                known = r.generation() == records.get(i - 1).generation() + 1
                        && sameRegime(first, r);
            }
            // A mixed large-history bucket is a gap, never a fabricated average threshold.
            if (!known) { previous = null; continue; }
            if (previous != null && previous.generation() + 1 == first.generation() && sameRegime(previous, first)) {
                var prior = segments.removeLast();
                segments.add(new Segment(prior.first(), last.generation(), prior.value()));
            } else segments.add(new Segment(first.generation(), last.generation(), first.rawPromotionThreshold()));
            previous = last;
        }
        return List.copyOf(segments);
    }
    private static boolean sameRegime(GenerationRecord a, GenerationRecord b) {
        return Objects.equals(a.rawPromotionThreshold(), b.rawPromotionThreshold()) && a.regime().equals(b.regime());
    }
    private PromotionThresholdSegments() {}
}
