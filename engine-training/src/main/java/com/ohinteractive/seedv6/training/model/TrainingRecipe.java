package com.ohinteractive.seedv6.training.model;

import com.ohinteractive.seedv6.training.nnue.AdamHyperparameters;

/** Architecture-owned recipe values, independent of search, sources and run termination. */
public record TrainingRecipe(double learningRate, int minibatchSize, int epochs) {
    public TrainingRecipe {
        new AdamHyperparameters(learningRate, .9, .999, 1e-8);
        if (minibatchSize < 1 || minibatchSize > 100000 || epochs < 1 || epochs > 100000)
            throw new IllegalArgumentException("Recipe batch size and epochs must be 1..100000.");
    }
    public static TrainingRecipe defaults(TrainingArchitecture architecture) {
        return architecture == TrainingArchitecture.BRN_PAIR2 ? new TrainingRecipe(.01, 128, 8)
                : architecture == TrainingArchitecture.BRN3 ? new TrainingRecipe(.003, 128, 8)
                : architecture.nnueFamily() ? new TrainingRecipe(.001, 32, 1) : new TrainingRecipe(.001, 1, 1);
    }
    public void requireSupported(TrainingArchitecture architecture) {
        if (!architecture.nnueFamily() && !architecture.corpusOnly() && (minibatchSize != 1 || epochs != 1))
            throw new IllegalArgumentException("This BRN architecture uses one online pass per generation.");
    }
}
