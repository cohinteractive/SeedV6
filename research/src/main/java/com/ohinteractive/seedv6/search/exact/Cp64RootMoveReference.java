package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;

/** Independent restricted-root references, with real root ply and original history. */
public final class Cp64RootMoveReference {
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("model one-root-corpus NEW-output");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        if(roots.size()!=1 || !roots.get(0).id().equals("played-01"))throw new AssertionError("Frozen witness selection");
        var root=roots.get(0);var boardBefore=root.board().clone();long[] moves={722584,722520};
        for(long move:moves)if(Arrays.stream(ExhaustiveOracle.legalMoves(root.board())).noneMatch(m->m==move))throw new AssertionError("Illegal selected move");
        var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","networkSha256",ConventionalNnueEngine.SHA256,"arguments",args,
                "reference","Independent recursive TT-off ordered alpha-beta with unpruned SR-001A qsearch; restricted root move, real root ply/history; no full-root optimality claim")));
            out.newLine();out.flush();
            for(int depth:new int[]{7,9})for(long move:moves) {
                long budget=depth==7?5000000:10000000;
                var reference=ExactSearch.quiescenceResearch(ExactEvaluator.from(model.evaluation()));long start=System.nanoTime();
                var result=reference.searchRootMove(root.board(),root.history(),depth,move,-ExactSearch.INFINITY,ExactSearch.INFINITY,false,
                    ()->reference.visitedNodes()>=budget || System.nanoTime()-start>=30_000_000_000L);
                long elapsed=System.nanoTime()-start;
                Phase2Oracle.legalPv(root.board(),result.principalVariation());
                if(result.completed() && result.bestMove()!=move)throw new AssertionError("Restricted root identity");
                if(!Arrays.equals(root.board(),boardBefore))throw new AssertionError("Root mutation");
                if(!reference.search(root.board(),root.history(),1,()->false).completed())throw new AssertionError("Reference owner reuse");
                out.write(gson.toJson(Map.of("type","reference","id",root.id(),"rootMove",Move.coordinate(move),"depth",depth,
                    "nodeBudget",budget,"timeBudgetNanos",30_000_000_000L,"elapsedNanos",elapsed,"outcome",result,"reusePassed",true)));
                out.newLine();out.flush();
            }
            out.write(gson.toJson(Map.of("status","passed","references",4)));out.newLine();
        }
    }
    private Cp64RootMoveReference() {}
}
