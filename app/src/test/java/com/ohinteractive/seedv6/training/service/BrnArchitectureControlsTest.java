package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.core.brn3.Brn3Features;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brn3.BrnTupleTrainer;
import com.ohinteractive.seedv6.core.nnue.NnueNetworkCodec;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureControlsTest {
    @TempDir Path temp;
    @Test void persistedControlsKeepTheirActualObjectiveAndExactNextUpdate()throws Exception {
        long[][] boards={Board.startingPosition(),Board.fromFen("4k3/8/8/3q4/8/8/4R3/4K3 w - - 0 1")};
        double[] targets={.1,.4};
        for(var kind:BrnArchitectureControls.Kind.values()) {
            var control=new BrnArchitectureControls.Control(kind,71);
            for(int i=0;i<3;i++)control.train(boards,targets,2);
            var view=control.view();
            for(var b:boards)assertEquals(control.outcome(b),view.outcome().applyAsDouble(b),2e-6,kind.toString());
            Path out=temp.resolve(kind.name());Files.createDirectory(out);control.save(out,"selected");
            if(kind!=BrnArchitectureControls.Kind.NNUE_STRICT) {
                String architecture=kind==BrnArchitectureControls.Kind.BRN3?"BRN3":kind==BrnArchitectureControls.Kind.NNUE_LEGACY?"NNUE":"NNUE_MATERIAL";
                for(int encoding=0;encoding<(kind==BrnArchitectureControls.Kind.NNUE_MATERIAL_V2?2:1);encoding++) {
                    if(encoding==1)try(var stream=Files.newOutputStream(out.resolve("selected.model"))){NnueNetworkCodec.writeMaterial(control.nnue.model().snapshot(),stream);}
                    DataFiles.write(out.resolve("result.json"),Map.of("kind","NATIVE","architecture",architecture,"complete",true,
                            "selectedModelSha256",BrnResearchComparison.digest(out.resolve("selected.model"))));
                    var nativeView=BrnArchitectureControls.loadView(out);
                    for(var b:boards) {
                        assertEquals(view.outcome().applyAsDouble(b),nativeView.outcome().applyAsDouble(b),0);
                        var actual=nativeView.evaluator().get();var expected=view.evaluator().get();actual.initialize(b);expected.initialize(b);
                        assertEquals(expected.evaluate(b,0),actual.evaluate(b,0));
                    }
                }
            }
            var restored=BrnArchitectureControls.Control.load(kind,out.resolve("selected.state"));
            assertEquals(3,restored.step());
            for(var b:boards)assertEquals(control.outcome(b),restored.outcome(b),0);
            control.train(boards,targets,2);restored.train(boards,targets,2);
            control.save(out,"a");restored.save(out,"b");
            assertEquals(-1,Files.mismatch(out.resolve("a.state"),out.resolve("b.state")),kind.toString());
            if(kind==BrnArchitectureControls.Kind.NNUE_LEGACY)assertNull(view.pawns());
        }
    }
    @Test void metricsUseCalibratedProductionSearchAndDoNotModifyExamples() {
        var control=new BrnArchitectureControls.Control(BrnArchitectureControls.Kind.BRN3,71);
        var b=Board.fromFen("4k3/8/8/3q4/8/8/4R3/4K3 w - - 0 1");var copy=b.clone();
        control.train(new long[][]{b},new double[]{.5},1);
        var view=control.view();var evaluator=view.evaluator().get();evaluator.initialize(b);
        var example=new BrnResearchData.Example(b,50,14,1);
        double error=Brn3Objective.outcome(evaluator.evaluate(b,0)/100.,b)-example.outcome();
        var metrics=BrnArchitectureControls.metrics(List.of(example),view);
        assertEquals(.5*error*error,(double)metrics.get("searchHalfMse"),0);
        assertArrayEquals(copy,b);
    }
    @Test void tupleLoaderPreservesFoldedValuesAndQuarterResidualSearch()throws Exception {
        long[][] boards={Board.startingPosition(),Board.fromFen("4k3/8/8/3q4/8/8/4R3/4K3 w - - 0 1")};
        for(int order:new int[]{2,3}) {
            var trainer=new BrnTupleTrainer(order,.003);for(int i=0;i<4;i++)trainer.trainBatch(boards,new double[]{.2,.4},2);
            var out=temp.resolve("tuple"+order);Files.createDirectory(out);BrnTupleExperiment.save(trainer,out,"selected");
            DataFiles.write(out.resolve("result.json"),Map.of("kind","TUPLE","complete",true,"selectedModelSha256",BrnResearchComparison.digest(out.resolve("selected.model"))));
            var view=BrnArchitectureControls.loadView(out);var expected=trainer.snapshot().newWorkspace();var evaluator=view.evaluator().get();
            for(var board:boards) {
                double raw=expected.evaluatePawns(board),material=Brn3Features.material(board);
                assertEquals(raw,view.pawns().applyAsDouble(board),0);evaluator.initialize(board);
                assertEquals(Brn3Model.score(material+.25*(raw-material)),evaluator.evaluate(board,0));
            }
        }
    }
    @Test @SuppressWarnings("unchecked") void diagnosticStrataHaveExactCoverageAndWeightedLoss() {
        var examples=List.of(new BrnResearchData.Example(Board.startingPosition(),200,78,1),
                new BrnResearchData.Example(Board.fromFen("4k3/pppp4/8/8/8/8/PPPPP3/3QK3 w - - 0 1"),-200,18,1),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"),50,1,1));
        var view=new BrnArchitectureControls.View(b->0,b->0,()->(b,ply)->0);
        var result=BrnArchitectureControls.metrics(examples,view);
        for(String name:List.of("phaseStrata","materialStrata")) {
            var buckets=(Map<String,Map<String,Object>>)result.get(name);assertEquals(3,buckets.size());double total=0;
            for(var bucket:buckets.values()){assertEquals(1,bucket.get("positions"));total+=(double)bucket.get("outcomeHalfMse");assertEquals(bucket.get("outcomeHalfMse"),bucket.get("searchHalfMse"));}
            assertEquals((double)result.get("outcomeHalfMse"),total/3,1e-15);
        }
        assertThrows(IllegalArgumentException.class,()->BrnArchitectureControls.metrics(List.of(),view));
    }
}
