package com.ohinteractive.seedv6.training.model;

import java.io.*;
import java.util.Objects;
import com.ohinteractive.seedv6.core.brn.*;
import com.ohinteractive.seedv6.core.brn1.*;
import com.ohinteractive.seedv6.training.nnue.*;

/** Single-owner model/optimizer state. Each architecture retains its own accepted codec and trainer. */
public sealed interface NetworkTrainingState {
    NetworkModel snapshot();
    long step();
    // The existing sidecar holds these four binary64 Adam values for either optimizer.
    AdamHyperparameters hyperparameters();
    void write(OutputStream output) throws IOException;

    record Nnue(NnueTrainer trainer) implements NetworkTrainingState {
        public Nnue { Objects.requireNonNull(trainer); }
        public NetworkModel snapshot() { return new NetworkModel.Nnue(trainer.model().snapshot()); }
        public long step() { return trainer.optimizer().step(); }
        public AdamHyperparameters hyperparameters() { return trainer.optimizer().hyperparameters(); }
        public void write(OutputStream out) throws IOException { TrainingStateCodec.write(trainer, out); }
    }
    record Brn(BrnTrainer trainer) implements NetworkTrainingState {
        public Brn { Objects.requireNonNull(trainer); }
        public NetworkModel snapshot() { return new NetworkModel.Brn(trainer.snapshot()); }
        public long step() { return trainer.optimizer().step(); }
        public AdamHyperparameters hyperparameters() {
            var c = trainer.config();
            return new AdamHyperparameters(c.learningRate(), c.beta1(), c.beta2(), c.epsilon());
        }
        public void write(OutputStream out) throws IOException { BrnCodec.writeTraining(trainer, out); }
    }

    record Brn1(Brn1Trainer trainer) implements NetworkTrainingState {
        public Brn1 { Objects.requireNonNull(trainer); }
        public NetworkModel snapshot() { return new NetworkModel.Brn1(trainer.snapshot()); }
        public long step() { return trainer.optimizer().step(); }
        public AdamHyperparameters hyperparameters() {
            var c = trainer.config();
            return new AdamHyperparameters(c.learningRate(), c.beta1(), c.beta2(), c.epsilon());
        }
        public void write(OutputStream out) throws IOException { Brn1Codec.writeTraining(trainer, out); }
    }

    default TrainingArchitecture architecture() {
        return switch (this) {
            case Nnue n -> TrainingArchitecture.NNUE;
            case Brn b -> TrainingArchitecture.BRN;
            case Brn1 b -> TrainingArchitecture.BRN1;
        };
    }
    default byte[] encode() throws IOException {
        return switch (this) {
            case Nnue n -> TrainingStateCodec.encode(n.trainer());
            case Brn b -> BrnCodec.encodeTraining(b.trainer());
            case Brn1 b -> Brn1Codec.encodeTraining(b.trainer());
        };
    }
    static NetworkTrainingState read(TrainingArchitecture architecture, InputStream input) throws IOException {
        return switch (architecture) {
            case NNUE -> new Nnue(TrainingStateCodec.read(input));
            case BRN -> new Brn(BrnCodec.readTraining(input));
            case BRN1 -> new Brn1(Brn1Codec.readTraining(input));
        };
    }
}
