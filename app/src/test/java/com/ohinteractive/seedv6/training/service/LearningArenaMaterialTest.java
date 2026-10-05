package com.ohinteractive.seedv6.training.service;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Features;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.HeldOutLoss;
import com.ohinteractive.seedv6.tools.nnue.cglhw.*;
import static org.junit.jupiter.api.Assertions.*;

class LearningArenaMaterialTest {
    @TempDir Path temporary;
    // Reproduced real Arena opening and frozen source positions from the read-only E012 audit.
    static final List<String> FENS = List.of(
            TrainerConfig.STANDARD_START,
            "r1bqkbnr/ppppp1pp/8/5p2/1nB1P1P1/5N2/PPPP1P1P/RNBQK2R b KQkq - 3 4",
            "7r/1p3k2/p1bPR3/5p2/2B2P1p/8/PP4P1/3K4 b - - 0 1",
            "4k3/8/8/8/8/8/Q7/4K3 w - - 0 1",
            "4k3/8/8/8/8/8/Q7/4K3 b - - 0 1");

    @Test void arenaFactoryPublishedCheckpointsResumeAndCglhwAgreeBeforeAndAfterTraining() throws Exception {
        var competitor = new LearningArenaConfig.Competitor("material", TrainingArchitecture.NNUE_MATERIAL, 71, 2);
        var fresh = (NetworkTrainingState.NnueMaterial)LearningArenaTraining.fresh(competitor);
        assertTrue(fresh.trainer().materialBootstrap());
        assertEquals("seedv6.nnue.halfkp.v1/1:existing-nnue-initializer-adam-v1:" + NnueScoreMapping.V1_ID,
                LearningArenaTraining.recipe(TrainingArchitecture.NNUE), "Legacy campaign bindings must remain valid");
        assertTrue(LearningArenaTraining.recipe(competitor.architecture()).contains(NnueMaterialBootstrap.TRAINING_ID));
        try (var store = new CheckpointStore(temporary.resolve("A"), competitor.architecture())) {
            String parent = "";
            for (int generation=0;generation<2;generation++) {
                if (generation==1) {
                    var samples = FENS.stream().map(f -> new TrajectorySampler.Sample(Board.fromFen(f), 1)).toList();
                    new TrainerService.Operations().trainBootstrap(fresh, samples, new SelfPlayTraining.Config(2,2,true,71), new SelfPlayControl(), p -> {});
                    assertEquals(6, fresh.step());
                }
                var checkpoint = store.publish(fresh, new CheckpointManifest.Metadata(generation, 3, parent));
                parent = checkpoint.manifest().id();
                var loaded = CheckpointStore.readSnapshot(store.root(), parent);
                assertEquals(competitor.architecture(), loaded.manifest().architecture());
                var model = (NetworkModel.NnueMaterial)loaded.model();
                var resumed = (NetworkTrainingState.NnueMaterial)store.resumeState(parent);
                assertArrayEquals(fresh.encode(), resumed.encode());
                var arena = model.evaluation(NnueScoreMapping.V1).newState(2);
                var research = new MaterialBootstrapModel(new HalfKpHead(model.network())).worker(2);
                var full = SearchEvaluation.fullRecomputeWithMaterial(model.network(),NnueScoreMapping.V1).newState(2);
                var neural = new NnueEvaluator(model.network());
                var samples = new ArrayList<TrajectorySampler.Sample>(); double expectedLoss=0;
                for (String fen : FENS) {
                    var board = Board.fromFen(fen); arena.initialize(board,0); research.refresh(board,0);
                    assertEquals(research.evaluate(board,0),arena.evaluate(board,0));
                    assertEquals(full.evaluate(board,0),arena.evaluate(board,0));
                    neural.evaluate(board);
                    int material = (int)Math.round(Brn3Features.material(board)*100);
                    assertEquals(NnueMaterialBootstrap.combine(material,NnueScoreMapping.V1.map(neural.boundedValue())),arena.evaluate(board,0));
                    double combined=Math.max(-1,Math.min(1,neural.boundedValue()+material/32511.0));
                    assertEquals(combined,resumed.trainer().predict(board));
                    assertEquals(combined*32511,arena.evaluate(board,0),1.000001);
                    expectedLoss += .5*(combined-1)*(combined-1);
                    samples.add(new TrajectorySampler.Sample(board,1));
                    // Every legal child exercises the Arena evaluator's actual incremental state.
                    for (long move : new HeadlessGame(board,100).legalMoves()) {
                        var child=new long[6]; Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],move,child);
                        arena.child(board,child,0);research.transition(board,child,0,1);
                        assertEquals(full.evaluate(child,1),arena.evaluate(child,1));
                        assertEquals(research.evaluate(child,1),arena.evaluate(child,1));
                    }
                }
                assertEquals(expectedLoss/samples.size(),HeldOutLoss.compare(model,model,samples).candidateLoss(),1e-15);
            }
            assertThrows(IOException.class, () -> CheckpointInspection.freshRoot(store.root(),TrainingArchitecture.NNUE));
        }
    }
}
