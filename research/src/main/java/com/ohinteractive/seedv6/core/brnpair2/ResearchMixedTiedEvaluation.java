package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import java.nio.file.*;
import java.io.IOException;

/** Research-owned immutable load; optimizer state is never exposed or updated during search. */
public final class ResearchMixedTiedEvaluation {
    public static SearchEvaluation load(Path state)throws IOException {
        final MixedTiedPieceNetwork model;
        try(var input=Files.newInputStream(state)){model=MixedTiedPieceNetwork.read(input);}
        if(!model.mix)throw new IOException("MIX neural-recurrence checkpoint required");
        return SearchEvaluation.researchCentipawnsFullRecompute(()->{
            var workspace=new MixedTiedPieceNetwork.Workspace();
            return board->model.scoreCp(board,workspace);
        });
    }
    private ResearchMixedTiedEvaluation(){}
}
