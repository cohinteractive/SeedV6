package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.validation.ValidationConfig;
import com.ohinteractive.seedv6.core.util.Fen;
import java.nio.file.*;
import java.util.*;

/** Reproduce a failed fixed-node game with its full opening history and prior TT searches. */
public final class BrnSuccessorReplay {
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("PLAN RECEIPT NEW_OUTPUT");
        var plan=DataFiles.read(Path.of(args[0]),BrnSuccessorMatch.Plan.class);
        var receipt=DataFiles.read(Path.of(args[1]),BrnSuccessorMatch.Receipt.class);
        if(!receipt.planHash().equals(DataFiles.hash(Files.readString(Path.of(args[0])))))throw new IllegalStateException("Plan mismatch");
        Path output=Path.of(args[2]);if(Files.exists(output))throw new IllegalStateException("Preserve prior replay");
        var candidate=plan.models().stream().filter(m->m.checkpoint().equals(receipt.candidate())).findFirst().orElseThrow();
        var reference=plan.models().getFirst();
        var config=new ValidationConfig(plan.pairs(),plan.seed(),4,12,64,1,NnueScoreMapping.V1,plan.maximumPlies());
        var opening=BrnSuccessorMatch.opening(plan,config,receipt.pair());
        if(!opening.identity().equals(receipt.openingHash()))throw new IllegalStateException("Opening mismatch");
        var game=opening.newGame(plan.maximumPlies());
        var rows=new ArrayList<Object>();
        try(var a=new SearchDriver(BrnSuccessorAudit.common(CheckpointStore.readSnapshot(Path.of(candidate.root()),candidate.checkpoint()).model().evaluation(NnueScoreMapping.V1)));
            var b=new SearchDriver(BrnSuccessorAudit.common(CheckpointStore.readSnapshot(Path.of(reference.root()),reference.checkpoint()).model().evaluation(NnueScoreMapping.V1)))) {
            for(int ply=0;ply<=receipt.moves().size();ply++) {
                var control=SearchControl.controlled(plan.nodes(),System.nanoTime(),2_000_000_000L,TimeSource.SYSTEM);
                var outcome=(game.sideToMove()==receipt.colour()?a:b).search(new SearchRequest(game.boardSnapshot(),game.historySnapshot(),64,control));
                var result=outcome.lastCompletedResult();
                var row=new LinkedHashMap<String,Object>();row.put("ply",ply);row.put("fen",Fen.fromBoard(game.boardSnapshot()));
                row.put("termination",control.termination());row.put("nodes",control.nodes());row.put("elapsedNanos",control.elapsedNanos());
                row.put("result",result);rows.add(row);
                if(ply<receipt.moves().size()) {
                    var expected=receipt.moves().get(ply);
                    if(result==null||result.bestMove()!=expected.move())throw new IllegalStateException("Replay diverged at ply "+ply);
                    game.play(expected.move());
                    if(!Fen.fromBoard(game.boardSnapshot()).equals(expected.fenAfter()))throw new IllegalStateException("Move/FEN mismatch");
                }
            }
        } finally { DataFiles.write(output,rows); }
        System.out.println("Replayed complete prefix and failed root: "+output);
    }
    private BrnSuccessorReplay() {}
}
