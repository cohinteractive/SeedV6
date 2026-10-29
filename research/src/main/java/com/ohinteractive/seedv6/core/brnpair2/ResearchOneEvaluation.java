package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import java.nio.file.*;

/** Explicit research-only immutable ONE checkpoint selection. */
public final class ResearchOneEvaluation {
    public static SearchEvaluation load(Path state)throws Exception {
        TrainableOne model;try(var in=Files.newInputStream(state)){model=TrainableOne.read(in);}
        return new IncrementalOne(model).searchDefinition();
    }
    private ResearchOneEvaluation(){}
}
