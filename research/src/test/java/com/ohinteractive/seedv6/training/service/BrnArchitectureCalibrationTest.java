package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.tools.nnue.cglhw.HalfKpResidualModel;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;
import java.util.function.DoubleFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureCalibrationTest {
    @TempDir Path temp;
    static final long[][] BOARDS={Board.startingPosition(),Board.fromFen("4k3/8/8/3q4/8/8/4R3/4K3 w - - 0 1")};
    static final double[] TARGETS={.3,.6};

    @Test void researchRatesPersistAndResumeWithoutChangingObjectives()throws Exception {
        for(var kind:BrnArchitectureControls.Kind.values()) {
            var a=new BrnArchitectureControls.Control(kind,71,.0003);
            for(int i=0;i<3;i++)a.train(BOARDS,TARGETS,2);
            Path out=temp.resolve(kind.name());Files.createDirectory(out);a.save(out,"last");
            var b=BrnArchitectureControls.Control.load(kind,out.resolve("last.state"));
            a.train(BOARDS,TARGETS,2);b.train(BOARDS,TARGETS,2);a.save(out,"a");b.save(out,"b");
            assertEquals(-1,Files.mismatch(out.resolve("a.state"),out.resolve("b.state")));
            if(a.nnue!=null)assertEquals(.0003,b.nnue.optimizer().hyperparameters().learningRate(),0);
            for(var board:BOARDS)assertEquals(a.outcome(board),b.outcome(board),0);
        }
    }

    @Test void allBrnViewsChangeOnlyResidualSearchGain() {
        var brn=new Brn3Trainer(71);for(int i=0;i<4;i++)brn.trainBatch(BOARDS,TARGETS,2);
        var bm=brn.snapshot();check(g->BrnArchitectureControls.brnView(bm,g));
        var energy=new BrnEnergyTrainer(0,71,.003);for(int i=0;i<4;i++)energy.trainBatch(BOARDS,TARGETS,2);
        var em=energy.snapshot();check(g->BrnEnergyExperiment.view(em,g));
        var cubic=new BrnCubicTrainer(8,71,.003);for(int i=0;i<4;i++)cubic.trainBatch(BOARDS,TARGETS,2);
        var cm=cubic.snapshot();check(g->BrnCubicExperiment.view(cm,g));
        var spline=new BrnSplineTrainer(16,71,.003,.5);for(int i=0;i<4;i++)spline.trainBatch(BOARDS,TARGETS,2);
        var sm=spline.snapshot();check(g->BrnSplineExperiment.view(sm,g));
        var context=new BrnContextTrainer(true,71,.003);for(int i=0;i<4;i++)context.trainBatch(BOARDS,TARGETS,2);
        var xm=context.snapshot();check(g->BrnContextExperiment.view(xm,g));
        var logic=new BrnLogicTrainer(1,71,.003);logic.harden();for(int i=0;i<4;i++)logic.trainBatch(BOARDS,TARGETS,2);
        var lm=logic.snapshot();check(g->BrnLogicExperiment.view(lm,g));
        var tuple=new BrnTupleTrainer(3,.01);for(int i=0;i<4;i++)tuple.trainBatch(BOARDS,TARGETS,2);
        var tm=tuple.snapshot();check(g->BrnTupleExperiment.view(tm,g));
    }
    static void check(DoubleFunction<BrnArchitectureControls.View> factory) {
        var reference=factory.apply(.25);
        for(double gain:new double[]{.125,.25,.5,1}) {
            var view=factory.apply(gain);var evaluator=view.evaluator().get();
            for(var board:BOARDS) {
                assertEquals(reference.pawns().applyAsDouble(board),view.pawns().applyAsDouble(board),0);
                assertEquals(reference.outcome().applyAsDouble(board),view.outcome().applyAsDouble(board),0);
                double m=Brn3Features.material(board);evaluator.initialize(board);
                assertEquals(Brn3Model.score(m+gain*(view.pawns().applyAsDouble(board)-m)),evaluator.evaluate(board,0));
            }
        }
        assertThrows(IllegalArgumentException.class,()->factory.apply(Double.NaN));
        assertThrows(IllegalArgumentException.class,()->factory.apply(0));
    }

    @Test void nnueGainPreservesNativeIncrementalParentAndSpecialTransitions() {
        for(var kind:new BrnArchitectureControls.Kind[]{BrnArchitectureControls.Kind.NNUE_MATERIAL_V2,BrnArchitectureControls.Kind.NNUE_STRICT}) {
            var trainer=new BrnArchitectureControls.Control(kind,71);for(int i=0;i<8;i++)trainer.train(BOARDS,TARGETS,2);
            var network=trainer.strict!=null?trainer.strict.snapshot():trainer.nnue.model().snapshot();
            var raw=new NnueEvaluator(network);double nativeGain=kind==BrnArchitectureControls.Kind.NNUE_STRICT?.25:1;
            ExactEvaluator nativeEvaluator=kind==BrnArchitectureControls.Kind.NNUE_STRICT?new HalfKpResidualModel(network).worker(257)
                    :ExactEvaluator.from(SearchEvaluation.incrementalWithMaterial(network,NnueScoreMapping.V1));
            for(double gain:new double[]{.125,.25,.5,1}) {
                var evaluator=BrnArchitectureControls.nnueView(kind,network,gain).evaluator().get();
                var boards=new ArrayList<long[]>();boards.add(Board.startingPosition());
                for(String fen:new String[]{"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"})boards.add(Board.fromFen(fen));
                var random=new Random(1777);long[] moves=new long[512];
                for(int i=0;i<200;i++) {
                    long[] parent=boards.get(i<boards.size()?i:boards.size()-1),copy=parent.clone();evaluator.initialize(parent);nativeEvaluator.initialize(parent);
                    int original=evaluator.evaluate(parent,0);
                    if(gain==nativeGain)assertEquals(nativeEvaluator.evaluate(parent,0),original);
                    int n=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
                    for(int j=0;j<n;j++) {
                        var child=new long[6];Board.makeMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],moves[j],child);
                        evaluator.child(parent,child,0);nativeEvaluator.child(parent,child,0);
                        int actual=evaluator.evaluate(child,1),material=NnueMaterialBootstrap.forSideToMove(child,NnueMaterialBootstrap.whiteScore(child));double residual=raw.evaluate(child);
                        int expected=kind==BrnArchitectureControls.Kind.NNUE_STRICT?Brn3Model.score(material/100.+gain*residual)
                                :NnueMaterialBootstrap.combine(material,new NnueScoreMapping(32511*gain).map(raw.boundedValue()));
                        assertEquals(expected,actual,1,"Independent full refresh versus native incremental");
                        if(gain==nativeGain)assertEquals(nativeEvaluator.evaluate(child,1),actual);
                        assertEquals(original,evaluator.evaluate(parent,0));
                    }
                    assertArrayEquals(copy,parent);
                    var next=new long[6];if(n>0)Board.makeMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],moves[random.nextInt(n)],next);
                    boards.add(n>0&&i%49!=48?next:Board.startingPosition());
                }
            }
        }
    }

    @Test void epochEndpointIsImmutableLoadableAndHashChecked()throws Exception {
        var trainer=new BrnTupleTrainer(2,.01);Path out=temp.resolve("run");Files.createDirectory(out);
        trainer.trainBatch(BOARDS,TARGETS,2);BrnTupleExperiment.save(trainer,out,"last");
        var event=Map.<String,Object>of("epoch",1,"trainingSeconds",2.5);
        BrnArchitectureControls.retainEpoch(out,1,event,Map.of("kind","TUPLE"));
        Path endpoint=out.resolve("epochs/epoch-001");var view=BrnArchitectureControls.loadView(endpoint,.5);
        for(var board:BOARDS)assertEquals(trainer.snapshot().newWorkspace().evaluatePawns(board),view.pawns().applyAsDouble(board),0);
        assertThrows(FileAlreadyExistsException.class,()->BrnArchitectureControls.retainEpoch(out,1,event,Map.of("kind","TUPLE")));
        var bytes=Files.readAllBytes(endpoint.resolve("selected.model"));bytes[20]^=1;Files.write(endpoint.resolve("selected.model"),bytes);
        assertThrows(java.io.IOException.class,()->BrnArchitectureControls.loadView(endpoint));
    }
}
