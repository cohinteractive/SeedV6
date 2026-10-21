package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features;
import java.util.SplittableRandom;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BrnSuccessorVariantsTest {
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CUBIC","GEN32_CUBIC_R4"})
    void cubicOnlyRetainsThirdDegreeAndUnaryWithExplicitScaling(BrnNextArchitecture kind) {
        var random=new SplittableRandom(117);double[] weights=new double[kind.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-.7,.7);
        var source=new BrnNextModel(kind,weights);
        var unary=BrnSuccessorVariants.correction(source,1,"unary",null);
        var quadratic=BrnSuccessorVariants.correction(source,1,"quadratic",null);
        for(double scale:new double[]{0,.5,1}) {
            var cubic=BrnSuccessorVariants.correction(source,scale,"cubic-only",null);
            for(var board:BrnSuccessorAudit.boards()) {
                double base=BrnNextBase.model().newWorkspace().evaluatePawns(board);
                double expected=base+scale*(source.rawPawns(board,source.newWorkspace())
                        -quadratic.rawPawns(board,quadratic.newWorkspace())+unary.rawPawns(board,unary.newWorkspace())-base);
                assertEquals(expected,cubic.rawPawns(board,cubic.newWorkspace()),1e-12);
            }
            for(int r=0;r<kind.width();r++) {
                assertEquals(0,cubic.weight(kind.head()+r));
                assertEquals(scale*weights[kind.head()+kind.width()+r],cubic.weight(kind.head()+kind.width()+r));
            }
        }
        for(int i=0;i<weights.length;i++)assertEquals(weights[i],source.weight(i));
        var gate=new BrnNextTrainer(BrnNextArchitecture.GEN32_GATED,1,.001).snapshot();
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.correction(gate,1,"cubic-only",null));
    }
    @org.junit.jupiter.api.Test
    void additiveAverageMatchesIndependentFunctionEnsembleWithoutExtraRuntimeStorage() throws Exception {
        var trainer=new com.ohinteractive.seedv6.core.brnpair2.BrnPair2Trainer(.003);
        var models=new java.util.ArrayList<com.ohinteractive.seedv6.training.model.NetworkModel>();
        for(var board:BrnSuccessorAudit.boards().subList(0,8)) {
            trainer.trainBatch(new long[][]{board},new double[]{.3},1);
            models.add(new com.ohinteractive.seedv6.training.model.NetworkModel.BrnPair2(
                    new com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model(trainer.snapshot())));
        }
        var average=BrnSuccessorVariants.average(models);var actual=BrnCheckpointAverageProbe.raw(average);
        var functions=models.stream().map(BrnCheckpointAverageProbe::raw).toList();
        for(int pass=0;pass<3;pass++)for(var board:BrnSuccessorAudit.boards()) {
            double expected=0;for(var f:functions)expected+=f.applyAsDouble(board)/8;
            assertEquals(expected,actual.applyAsDouble(board),1e-11);
        }
        var model=((com.ohinteractive.seedv6.training.model.NetworkModel.BrnPair2)average).model();
        var source=((com.ohinteractive.seedv6.training.model.NetworkModel.BrnPair2)models.getFirst()).model();
        assertEquals(source.immutablePrimitiveBytes(),model.immutablePrimitiveBytes());
        assertEquals(source.newWorkspace().primitiveBytes(),model.newWorkspace().primitiveBytes());
        assertSame(models.getFirst(),BrnSuccessorVariants.average(java.util.List.of(models.getFirst())));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.average(java.util.List.of()));
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_GATED","GEN32_CUBIC"})
    void averagePreservesZeroCorrectionAndRejectsMixedSchemas(BrnNextArchitecture kind) throws Exception {
        var models=new java.util.ArrayList<com.ohinteractive.seedv6.training.model.NetworkModel>();
        for(int seed=0;seed<8;seed++)models.add(new com.ohinteractive.seedv6.training.model.NetworkModel.BrnNext(
                new BrnNextTrainer(kind,seed,.003).snapshot()));
        var result=BrnCheckpointAverageProbe.raw(BrnSuccessorVariants.average(models));
        var base=BrnNextBase.model().newWorkspace();
        for(var board:BrnSuccessorAudit.boards())assertEquals(base.evaluatePawns(board),result.applyAsDouble(board),1e-12);
        models.add(new com.ohinteractive.seedv6.training.model.NetworkModel.BrnPair2(BrnNextBase.model()));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.average(models));
    }
    @org.junit.jupiter.api.Test
    void additiveInterpolationKeepsSingleTableCostAndPreservesSource() throws Exception {
        var trainer=new com.ohinteractive.seedv6.core.brnpair2.BrnPair2Trainer(.003);
        var boards=BrnSuccessorAudit.boards();
        for(var board:boards)trainer.trainBatch(new long[][]{board},new double[]{.3},1);
        var source=new com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model(trainer.snapshot());
        var base=BrnNextBase.model();
        var before=new java.io.ByteArrayOutputStream();
        com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec.writeModel(source,before);
        for(double scale:new double[]{0,.5,1}) {
            var variant=BrnSuccessorVariants.additiveChange(source,scale);
            assertEquals(source.immutablePrimitiveBytes(),variant.immutablePrimitiveBytes());
            assertEquals(source.newWorkspace().primitiveBytes(),variant.newWorkspace().primitiveBytes());
            var actual=variant.newWorkspace();var ref=base.newWorkspace();var candidate=source.newWorkspace();
            for(int cycle=0;cycle<4;cycle++)for(var board:boards) {
                double raw=ref.evaluatePawns(board);
                double expected=raw+scale*(candidate.evaluatePawns(board)-raw);
                assertEquals(expected,actual.evaluatePawns(board),1e-11);
                double material=BrnPair2Features.material(board);
                assertEquals(Brn3Model.score(material+.25*(expected-material)),
                        Brn3Model.score(variant.newWorkspace().evaluatePawns(board,.25)));
            }
        }
        var after=new java.io.ByteArrayOutputStream();
        com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec.writeModel(source,after);
        assertArrayEquals(before.toByteArray(),after.toByteArray());
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.additiveChange(source,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.additiveChange(source,1.1));
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class, names={"GEN32_GATED","GEN32_CUBIC"})
    void correctionControlPreservesFrozenBaseAndSourceAcrossPositions(BrnNextArchitecture kind) {
        var random=new SplittableRandom(229);double[] weights=new double[kind.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-.4,.4);
        var source=new BrnNextModel(kind,weights);
        for(double scale:new double[]{0,.25,.5,1,1.5}) {
            var variant=BrnSuccessorVariants.correction(source,scale,null,null);
            for(var board:BrnSuccessorAudit.boards()) {
                double base=BrnNextBase.model().newWorkspace().evaluatePawns(board);
                double expected=base+scale*(source.rawPawns(board,source.newWorkspace())-base);
                assertEquals(expected,variant.rawPawns(board,variant.newWorkspace()),1e-12);
                double material=BrnPair2Features.material(board);
                assertEquals(Brn3Model.score(material+.25*(expected-material)),variant.score(board,variant.newWorkspace()));
            }
        }
        for(String ablation:new String[]{"unary",kind.correctionKind()==BrnNextArchitecture.CUBIC?"quadratic":"constant-gates"}) {
            var variant=BrnSuccessorVariants.correction(source,0,ablation,
                    ablation.equals("constant-gates")?new double[]{.1,.2,.3,.4}:null);
            for(var board:BrnSuccessorAudit.boards())
                assertEquals(BrnNextBase.model().newWorkspace().evaluatePawns(board),variant.rawPawns(board,variant.newWorkspace()),1e-12);
        }
        for(int i=0;i<weights.length;i++)assertEquals(weights[i],source.weight(i));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.correction(source,Double.NaN,null,null));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.correction(source,1,"unknown",null));
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class, names={"GEN32_GATED","GEN32_CUBIC"})
    void oldGainControlCannotSilentlyRescaleOnlyTheCorrection(BrnNextArchitecture kind) {
        var model=new BrnNextModel(kind,new double[kind.parameters()]);
        assertSame(model,BrnSuccessorVariants.gain(model,.25));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.gain(model,.5));
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class, names={"GATED_PAIRS","CUBIC","POOLED"})
    void outputScalingChangesOnlyResidualGainAndPreservesSource(BrnNextArchitecture kind) {
        var random=new SplittableRandom(913);double[] weights=new double[kind.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-.4,.4);
        var source=new BrnNextModel(kind,weights);
        for(double gain:new double[]{0,.25,.5,1}) {
            var variant=BrnSuccessorVariants.gain(source,gain);
            for(String fen:new String[]{"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    "r3k2r/8/8/3pP3/8/8/8/R3K2R b KQkq - 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"}) {
                var board=Board.fromFen(fen);double material=BrnPair2Features.material(board);
                double expected=material+gain*(source.rawPawns(board,source.newWorkspace())-material);
                assertEquals(Brn3Model.score(expected),variant.score(board,variant.newWorkspace()));
                double raw=variant.rawPawns(board,variant.newWorkspace());
                assertEquals(expected,material+.25*(raw-material),1e-12);
            }
        }
        for(int i=0;i<weights.length;i++)assertEquals(weights[i],source.weight(i));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.gain(source,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorVariants.gain(source,1.01));
    }
}
