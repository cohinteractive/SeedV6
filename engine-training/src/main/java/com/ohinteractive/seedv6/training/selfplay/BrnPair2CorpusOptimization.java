package com.ohinteractive.seedv6.training.selfplay;

import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Trainer;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayTraining.*;
import java.util.*;
import java.util.function.*;

/** Pair-2 mathematics with the common exact-continuation corpus loop. */
public final class BrnPair2CorpusOptimization {
    public static Optional<Statistics> trainSamples(BrnPair2Trainer trainer, List<TrajectorySampler.Sample> samples,
            Config config, SelfPlayControl control, Consumer<Progress> observer,
            ToDoubleFunction<TrajectorySampler.Sample> target) {
        Objects.requireNonNull(trainer, "trainer");
        return BatchedCorpusOptimization.trainSamples(trainer::step, trainer::trainBatch, trainer::predictOutcome,
                samples, config, control, observer, target);
    }
    private BrnPair2CorpusOptimization() {}
}
