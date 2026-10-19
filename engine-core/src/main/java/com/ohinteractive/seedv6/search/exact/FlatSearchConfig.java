package com.ohinteractive.seedv6.search.exact;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Immutable construction-time policy. No production entry point selects it.
 * Primitive flags are read directly by the shared flattened kernel. */
public record FlatSearchConfig(long flags) {
    public enum Feature { QXRAY, LMR, LMP, EFFORT, COMPLETED_HINTS }

    public FlatSearchConfig {
        long known=(1L<<Feature.values().length)-1;
        if((flags&~known)!=0) throw new IllegalArgumentException("Unknown Search mechanisms");
    }
    public boolean has(Feature feature) { return (flags&(1L<<feature.ordinal()))!=0; }
    public FlatSearchConfig with(Feature feature,boolean enabled) {
        long bit=1L<<Objects.requireNonNull(feature).ordinal();
        return new FlatSearchConfig(enabled?flags|bit:flags&~bit);
    }
    public static FlatSearchConfig exact() { return new FlatSearchConfig(0); }
    public static FlatSearchConfig policy4() { return new FlatSearchConfig(31); }
    public String identity() {
        return "flat-pvs-v1["+Stream.of(Feature.values()).filter(this::has)
                .map(Enum::name).collect(Collectors.joining(","))+"]";
    }
}
