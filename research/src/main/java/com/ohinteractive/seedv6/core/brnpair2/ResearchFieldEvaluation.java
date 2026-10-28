package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import java.nio.file.*;
import java.io.IOException;

/** Research-owned immutable load; optimizer state is never exposed or updated during search. */
public final class ResearchFieldEvaluation {
    public static SearchEvaluation load(Path state)throws IOException {
        final ComposedAttackFieldNetwork model;
        try(var input=Files.newInputStream(state)){model=ComposedAttackFieldNetwork.read(input);}
        if(!model.compose)throw new IOException("COMPOSE8 checkpoint required");
        return SearchEvaluation.researchCentipawnsFullRecompute(()->{
            var workspace=new ComposedAttackFieldNetwork.Workspace();
            return board->model.scoreCp(board,workspace);
        });
    }
    private ResearchFieldEvaluation(){}
}
