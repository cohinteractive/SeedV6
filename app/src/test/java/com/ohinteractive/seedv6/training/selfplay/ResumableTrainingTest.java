package com.ohinteractive.seedv6.training.selfplay;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.*;
import static org.junit.jupiter.api.Assertions.*;

class ResumableTrainingTest {
    NetworkTrainingState initial(TrainingArchitecture a) {
        return switch (a) {
            case NNUE -> new NetworkTrainingState.Nnue(new NnueTrainer(TrainableNnue.initialized(17)));
            case BRN -> new NetworkTrainingState.Brn(new com.ohinteractive.seedv6.core.brn.BrnTrainer(.001));
            case BRN1 -> new NetworkTrainingState.Brn1(new com.ohinteractive.seedv6.core.brn1.Brn1Trainer(.001));
            case BRN2 -> new NetworkTrainingState.Brn2(new com.ohinteractive.seedv6.core.brn2.Brn2Trainer(.001));
        };
    }
    Optional<SelfPlayTraining.Statistics> train(NetworkTrainingState state, SelfPlayControl control, Consumer<SelfPlayTraining.Progress> progress) {
        var samples = TrajectorySampler.sample(HeadlessGameTest.foolsMate().trajectory(), 32);
        var cfg = new SelfPlayTraining.Config(state.architecture() == TrainingArchitecture.NNUE ? 3 : 1,
                state.architecture() == TrainingArchitecture.NNUE ? 3 : 1, true, 871);
        return switch (state) {
            case NetworkTrainingState.Nnue n -> SelfPlayTraining.trainSamples(n.trainer(), samples, cfg, control, progress);
            case NetworkTrainingState.Brn b -> BrnSelfPlayTraining.trainSamples(b.trainer(), samples, cfg, control, progress);
            case NetworkTrainingState.Brn1 b -> Brn1SelfPlayTraining.trainSamples(b.trainer(), samples, cfg, control, progress);
            case NetworkTrainingState.Brn2 b -> Brn2SelfPlayTraining.trainSamples(b.trainer(), samples, cfg, control, progress);
        };
    }
    @ParameterizedTest @EnumSource(TrainingArchitecture.class)
    void serializedOptimizerAndCursorResumeExactShuffledWorkload(TrainingArchitecture architecture) throws Exception {
        var expected = initial(architecture); var whole = train(expected, new SelfPlayControl(), p -> {}).orElseThrow();
        var first = initial(architecture); var stop = new SelfPlayControl();
        train(first, stop, p -> { if (p.optimizerUpdates() == 2) stop.cancel(); });
        var cursor = stop.trainingCursor(); assertEquals(2, cursor.updates());
        var restored = NetworkTrainingState.read(architecture, new ByteArrayInputStream(first.encode()));
        var resume = new SelfPlayControl(); resume.trainingCursor(cursor); var updates = new ArrayList<Long>();
        var resumed = train(restored, resume, p -> updates.add(p.optimizerUpdates())).orElseThrow();
        assertEquals(3L, updates.getFirst()); assertEquals(whole, resumed);
        assertArrayEquals(expected.encode(), restored.encode());
    }
}
