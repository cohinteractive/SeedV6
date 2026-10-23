package com.ohinteractive.seedv6.search.exact;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Immutable Construction policy shared by competitive bindings and explicit research controls.
 * Primitive flags are read directly by the shared flattened kernel. */
public record FlatSearchConfig(long flags,int futilityMargin,int reverseMargin) {
    public enum Feature { QXRAY, LMR, LMP, EFFORT, COMPLETED_HINTS,
        CAPTURE_HISTORY, CONTINUATION, KILLERS, COUNTERMOVES, NOMINAL_HISTORY, BAD_TACTICAL_LAST,
        IMPROVING, CAUTION, CHECK_EXTENSION, RECAPTURE_EXTENSION, FUTILITY, REVERSE_FUTILITY, HISTORY_PROTECTION,
        LAZY_IMPROVING, REDUCTION_FUTILITY, NOMINAL_FUTILITY, VERIFIED_NULL }

    public FlatSearchConfig(long flags) { this(flags,100,200); }

    public FlatSearchConfig {
        long known=(1L<<Feature.values().length)-1;
        if((flags&~known)!=0) throw new IllegalArgumentException("Unknown Search mechanisms");
        if(futilityMargin<1 || futilityMargin>2000 || reverseMargin<1 || reverseMargin>2000)
            throw new IllegalArgumentException("Margins must be 1..2000 evaluation units per nominal ply");
        if((flags&((1L<<Feature.IMPROVING.ordinal())|(1L<<Feature.CAUTION.ordinal())))!=0
                && (flags&(1L<<Feature.LMR.ordinal()))==0)
            throw new IllegalArgumentException("Reduction relief requires LMR");
        if((flags&(1L<<Feature.HISTORY_PROTECTION.ordinal()))!=0
                && (flags&((1L<<Feature.LMP.ordinal())|(1L<<Feature.FUTILITY.ordinal())))==0)
            throw new IllegalArgumentException("History protection requires LMP or futility");
        if((flags&(1L<<Feature.LAZY_IMPROVING.ordinal()))!=0 && (flags&(1L<<Feature.IMPROVING.ordinal()))==0)
            throw new IllegalArgumentException("Lazy improving requires improving");
        long deeper=(1L<<Feature.REDUCTION_FUTILITY.ordinal())|(1L<<Feature.NOMINAL_FUTILITY.ordinal());
        if((flags&deeper)!=0 && ((flags&(1L<<Feature.LMR.ordinal()))==0 || (flags&(1L<<Feature.FUTILITY.ordinal()))==0))
            throw new IllegalArgumentException("Deeper futility research requires LMR and shallow futility");
        if((flags&deeper)==deeper) throw new IllegalArgumentException("Choose one deeper futility margin model");
        if((flags&(1L<<Feature.VERIFIED_NULL.ordinal()))!=0 && (flags&262143L)!=262143L)
            throw new IllegalArgumentException("Verified-null research requires the full original Construction foundation");
    }
    public boolean has(Feature feature) { return (flags&(1L<<feature.ordinal()))!=0; }
    public FlatSearchConfig with(Feature feature,boolean enabled) {
        long bit=1L<<Objects.requireNonNull(feature).ordinal();
        return new FlatSearchConfig(enabled?flags|bit:flags&~bit,futilityMargin,reverseMargin);
    }
    public static FlatSearchConfig exact() { return new FlatSearchConfig(0); }
    public static FlatSearchConfig policy4() { return new FlatSearchConfig(31); }
    public static FlatSearchConfig ordered() { return new FlatSearchConfig((1L<<11)-1); }
    public static FlatSearchConfig candidate() { return new FlatSearchConfig((1L<<18)-1); }
    public boolean extensions() { return has(Feature.CHECK_EXTENSION)||has(Feature.RECAPTURE_EXTENSION); }
    public String identity() {
        return "flat-pvs-v1["+Stream.of(Feature.values()).filter(this::has)
                .map(Enum::name).collect(Collectors.joining(","))+"];futility="+futilityMargin
                +"d;reverse="+reverseMargin+"d;extensionCap=1";
    }
}
