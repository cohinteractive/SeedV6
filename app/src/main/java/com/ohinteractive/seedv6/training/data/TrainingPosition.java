package com.ohinteractive.seedv6.training.data;

import com.ohinteractive.seedv6.corpus.CorpusPosition;

/** Architecture-neutral position and raw target. Only readers know external formats. */
public interface TrainingPosition {
    CorpusPosition position();
    int targetKind();
    int target();
    int perspective();
}
