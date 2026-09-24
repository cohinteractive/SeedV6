package com.ohinteractive.seedv6.training.selfplay;

import java.util.*;
import java.util.function.ToDoubleFunction;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import com.ohinteractive.seedv6.training.service.*;
import static org.junit.jupiter.api.Assertions.*;

class BrnCaptureTrainingTest {
    static final String[] FENS = {
        "7k/6rp/5KQ1/8/8/8/8/8 w - - 0 1",
        "7k/8/8/3pP3/8/8/8/K7 w - d6 0 1",
        "k6r/6P1/8/8/8/8/8/K7 w - - 0 1",
        "7k/8/3p4/4P3/8/8/8/K7 b - - 0 1",
        TrainerConfig.STANDARD_START, "7k/8/8/8/8/8/1r6/KR6 w - - 0 1",
        "7k/8/8/8/8/8/8/K7 w - - 0 1"
    };
    static List<TrajectorySampler.Sample> samples() {
        var rows = new ArrayList<TrajectorySampler.Sample>();
        for (int i = 0; i < 28; i++) rows.add(new TrajectorySampler.Sample(Board.fromFen(FENS[i % FENS.length]), i % 3 - 1));
        return rows;
    }
    @Test void acceptedUniformSortedNonterminalCaptureRuleAndHalfTeacherDeltaForBothSides() {
        var model = NnueNetwork.initialized(17); var teacher = new NnueEvaluator(model);
        ToDoubleFunction<long[]> value = b -> { teacher.evaluate(b); return teacher.boundedValue(); };
        var rng = new SplittableRandom(2026092404L); var oracleRng = new SplittableRandom(2026092404L);
        int pairs = 0;
        for (var sample : samples()) {
            long[] parent = sample.board(), before = parent.clone(); double np = value.applyAsDouble(parent);
            var legal = new HeadlessGame(parent, 2); var moves = new ArrayList<Long>();
            for (long m : legal.legalMoves()) {
                if (((m >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) == 0
                        && !(((m >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) == 0 && MoveOrdering.isTactical(parent, m))) continue;
                var next = new HeadlessGame(parent, 2); next.play(m);
                if (new HeadlessGame(next.boardSnapshot(), 2).active()) moves.add(m);
            }
            moves.sort(Comparator.comparing(Move::coordinate));
            var actual = BrnCaptureTraining.relation(parent, np, value, rng);
            if (moves.isEmpty()) assertNull(actual);
            else {
                pairs++;
                long selected = moves.get(oracleRng.nextInt(moves.size())); assertEquals(selected, actual.move());
                var game = new HeadlessGame(parent, 2); game.play(selected);
                assertArrayEquals(game.boardSnapshot(), actual.child());
                assertNotEquals(Board.player((int)parent[4]), Board.player((int)actual.child()[4]));
                double nc = value.applyAsDouble(actual.child());
                assertEquals(.5 * (-nc - np), actual.targetDelta());
                double parentTarget = .5*sample.target()+.5*np;
                assertEquals(-(-.5*sample.target()+.5*nc)-parentTarget, actual.targetDelta(), 2e-16);
                assertEquals(Long.bitCount(parent[0]|parent[1]|parent[2])-1, Long.bitCount(actual.child()[0]|actual.child()[1]|actual.child()[2]));
            }
            assertArrayEquals(before, parent);
        }
        assertTrue(pairs >= 12); assertEquals(oracleRng.nextLong(), rng.nextLong());
    }
    @Test void disabledDelegatesWithoutAnyTeacherOrAuxiliaryWorkAndKeepsCursorAndLossExactly() throws Exception {
        var a = new Brn2Trainer(.001); var b = new Brn2Trainer(.001);
        var ca = new SelfPlayControl(); var cb = new SelfPlayControl();
        var cfg = new SelfPlayTraining.Config(1,1,true,31);
        var expected = Brn2SelfPlayTraining.trainSamples(a, samples(), cfg, ca, p -> {}, TrajectorySampler.Sample::target);
        var actual = BrnCaptureTraining.trainSamples(b, samples(), cfg, cb, p -> {}, TrajectorySampler.Sample::target,
                BrnCaptureConsistency.OFF, board -> { throw new AssertionError("Disabled teacher/capture work"); }, 99);
        assertEquals(expected, actual); assertEquals(ca.trainingCursor(), cb.trainingCursor());
        assertArrayEquals(Brn2Codec.encodeTraining(a), Brn2Codec.encodeTraining(b));
    }
    @Test void enabledMatchesManualFormulaNoChildBaseTargetAndResumesExactlyWithImmutableTeacher() throws Exception {
        var network = NnueNetwork.initialized(17); byte[] teacherBefore = NnueNetworkCodec.encode(network);
        var teacher = new NnueEvaluator(network);
        ToDoubleFunction<long[]> value = board -> { teacher.evaluate(board); return teacher.boundedValue(); };
        var data = samples(); var cfg = new SelfPlayTraining.Config(1,1,true,71); var lambda = new BrnCaptureConsistency(2);
        var a = new Brn2Trainer(.001); var b = new Brn2Trainer(.001); var manual = new Brn2Trainer(.001);
        var control = new SelfPlayControl(); var partialControl = new SelfPlayControl();
        var expected = BrnCaptureTraining.trainSamples(a,data,cfg,control,p->{},null,lambda,value,101).orElseThrow();
        var rows = new BrnCaptureTraining.Relation[data.size()]; var targets = new double[data.size()];
        var captureRng = new SplittableRandom(101);
        for (int i=0;i<data.size();i++) {
            double np=value.applyAsDouble(data.get(i).board()); targets[i]=.5*data.get(i).target()+.5*np;
            rows[i]=BrnCaptureTraining.relation(data.get(i).board(),np,value,captureRng);
        }
        int[] order = java.util.stream.IntStream.range(0,data.size()).toArray(); var shuffle = new SplittableRandom(71);
        for(int i=order.length-1;i>0;i--) { int j=shuffle.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v; }
        double baseSum=0;
        for(int i:order) baseSum+=rows[i]==null ? manual.train(data.get(i).board(),targets[i])
                : manual.trainCapture(data.get(i).board(),targets[i],rows[i].child(),rows[i].targetDelta(),2).base();
        assertEquals(baseSum/data.size(),expected.meanTrainingLoss());
        assertEquals(data.size(),manual.optimizer().step());
        assertArrayEquals(Brn2Codec.encodeTraining(a),Brn2Codec.encodeTraining(manual));
        BrnCaptureTraining.trainSamples(b,data,cfg,partialControl,p->{if(p.samplesTrained()==7)partialControl.cancel();},null,lambda,value,101);
        b=Brn2Codec.decodeTraining(Brn2Codec.encodeTraining(b));
        var resumedControl=new SelfPlayControl();resumedControl.trainingCursor(partialControl.trainingCursor());
        var resumed=BrnCaptureTraining.trainSamples(b,data,cfg,resumedControl,p->{},null,lambda,value,101).orElseThrow();
        assertEquals(expected,resumed); assertEquals(control.trainingCursor(),resumedControl.trainingCursor());
        assertArrayEquals(Brn2Codec.encodeTraining(a),Brn2Codec.encodeTraining(b));
        assertArrayEquals(teacherBefore,NnueNetworkCodec.encode(network));
    }
}
