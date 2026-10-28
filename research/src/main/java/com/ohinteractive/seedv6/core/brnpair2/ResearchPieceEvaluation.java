package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import java.nio.file.*;
import java.io.IOException;

/** Research-owned immutable load; optimizer state is never exposed or updated during search. */
public final class ResearchPieceEvaluation {
    public static SearchEvaluation load(Path state)throws IOException {
        final ContextualPieceNetwork model;
        try(var input=Files.newInputStream(state)){model=ContextualPieceNetwork.read(input);}
        if(model.compose)throw new IOException("ONE contextual-piece checkpoint required");
        return SearchEvaluation.researchCentipawnsFullRecompute(()->{
            var workspace=new ContextualPieceNetwork.Workspace();
            return board->model.scoreCp(board,workspace);
        });
    }
    private ResearchPieceEvaluation(){}
}
