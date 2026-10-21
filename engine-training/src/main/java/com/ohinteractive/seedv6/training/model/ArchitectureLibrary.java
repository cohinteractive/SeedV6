package com.ohinteractive.seedv6.training.model;

import java.util.List;

/** Creation policy is separate from stable, implemented checkpoint schema identities. */
public final class ArchitectureLibrary {
    public enum Status { AVAILABLE, LEGACY, RESEARCH }
    public record Descriptor(String name, TrainingArchitecture implementation, Status status,
                             List<String> aliases, String capabilities) {
        public Descriptor { aliases = List.copyOf(aliases); }
        public boolean canCreate() { return implementation != null && (status == Status.AVAILABLE || status == Status.RESEARCH); }
        public TrainingRecipe defaults() {
            if (implementation == null) throw new IllegalStateException("No executable implementation");
            return TrainingRecipe.defaults(implementation);
        }
    }
    private static final List<Descriptor> ENTRIES = List.of(
        new Descriptor("NNUE (material parity)", TrainingArchitecture.NNUE_MATERIAL, Status.AVAILABLE,
            List.of("NNUE-Material"), "Adam; datasets or live self-play"),
        new Descriptor("BRE-Pair 2", TrainingArchitecture.BRN_PAIR2, Status.AVAILABLE,
            List.of("BRN_PAIR2", "BRN-Pair2"), "Masked Adam; compatible CP / BT4 Q datasets"),
        new Descriptor("BRN-3", TrainingArchitecture.BRN3, Status.AVAILABLE,
            List.of("BRN3"), "Masked Adam; compatible CP / BT4 Q datasets"),
        new Descriptor("NNUE (legacy, no material)", TrainingArchitecture.NNUE, Status.LEGACY,
            List.of("NNUE"), "Existing lineages remain supported"),
        new Descriptor("BRN / BRN-0", TrainingArchitecture.BRN, Status.LEGACY,
            List.of("BRN", "BRN-0"), "Existing lineages; online Adam"),
        new Descriptor("BRN-1", TrainingArchitecture.BRN1, Status.LEGACY,
            List.of("BRN1"), "Existing lineages; online Adam"),
        new Descriptor("BRN-2", TrainingArchitecture.BRN2, Status.LEGACY,
            List.of("BRN2"), "Existing lineages; online Adam / CP datasets"),
        new Descriptor("BRN-4", null, Status.RESEARCH, List.of(), "Future research; no implementation"),
        new Descriptor("BRN Gated Pairs (research)", TrainingArchitecture.BRN_GATED, Status.RESEARCH,
            List.of("W1-GATE"), "Four context-gated local pair experts; corpus Adam"),
        new Descriptor("BRN Cubic-16 (research)", TrainingArchitecture.BRN_CUBIC, Status.RESEARCH,
            List.of("W1-CUBIC"), "Rank16 quadratic/cubic ANOVA interactions; corpus Adam"),
        new Descriptor("BRN Pooled-16 (control)", TrainingArchitecture.BRN_POOLED, Status.RESEARCH,
            List.of("W1-POOL"), "Small pooled tanh nonlinear control; corpus Adam"),
        new Descriptor("Gen32 + Gated Residual (research)", TrainingArchitecture.BRN_GEN32_GATED, Status.RESEARCH,
            List.of("W3-RESGATE"), "Frozen Gen32 plus context-gated correction; corpus Adam; Policy4"),
        new Descriptor("Gen32 + Cubic Residual (research)", TrainingArchitecture.BRN_GEN32_CUBIC, Status.RESEARCH,
            List.of("W3-RESCUBIC"), "Frozen Gen32 plus rank16 cubic correction; corpus Adam; Policy4"),
        new Descriptor("Gen32 + Cubic-4 Residual (research)", TrainingArchitecture.BRN_GEN32_CUBIC_R4, Status.RESEARCH,
            List.of("W5-RESCUBIC-R4"), "Frozen Gen32 plus four cubic components; corpus Adam; Policy4"),
        new Descriptor("Gen32 + Quadratic Residual (control)", TrainingArchitecture.BRN_GEN32_QUADRATIC, Status.RESEARCH,
            List.of("GEN32-QUADRATIC-CONTROL"), "Additive pairwise control without cubic terms; corpus Adam; Policy4"),
        new Descriptor("Gen32 + Quadratic-4 Residual (control)", TrainingArchitecture.BRN_GEN32_QUADRATIC_R4, Status.RESEARCH,
            List.of("GEN32-QUADRATIC-R4-CONTROL"), "Four-component additive pairwise control without cubic terms; corpus Adam; Policy4"));
    public static List<Descriptor> entries() { return ENTRIES; }
    public static Descriptor describe(TrainingArchitecture architecture) {
        return ENTRIES.stream().filter(e -> e.implementation() == architecture).findFirst().orElseThrow();
    }
    private ArchitectureLibrary() {}
}
