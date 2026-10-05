package com.ohinteractive.seedv6.training.model;

import java.io.*;
import java.util.Objects;
import com.ohinteractive.seedv6.core.brn.*;
import com.ohinteractive.seedv6.core.brn1.*;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.training.nnue.*;

/** Single-owner model/optimizer state. Each architecture retains its own accepted codec and trainer. */
public sealed interface NetworkTrainingState {
    NetworkModel snapshot();
    long step();
    // The existing sidecar holds these four binary64 Adam values for either optimizer.
    AdamHyperparameters hyperparameters();
    default void setLearningRate(double rate) {
        switch (this) {
            case Nnue n -> n.trainer().optimizer().setLearningRate(rate);
            case NnueMaterial n -> n.trainer().optimizer().setLearningRate(rate);
            case Brn b -> b.trainer().setLearningRate(rate);
            case Brn1 b -> b.trainer().setLearningRate(rate);
            case Brn2 b -> b.trainer().setLearningRate(rate);
            case Brn3 b -> b.trainer().setLearningRate(rate);
        }
    }
    void write(OutputStream output) throws IOException;
    /** Common built-in initializer; arbitrary imported trainers retain unknown initializer provenance. */
    static NetworkTrainingState initialized(TrainingArchitecture architecture, long seed, double learningRate) {
        NetworkTrainingState state = switch (architecture) {
            case NNUE -> new Nnue(new NnueTrainer(TrainableNnue.initialized(seed)));
            case NNUE_MATERIAL -> new NnueMaterial(NnueTrainer.materialParity(TrainableNnue.initialized(seed)));
            case BRN -> new Brn(new BrnTrainer(learningRate));
            case BRN1 -> new Brn1(new Brn1Trainer(learningRate));
            case BRN2 -> new Brn2(new Brn2Trainer(learningRate));
            case BRN3 -> new Brn3(new Brn3Trainer(seed, new BrnAdamConfig(learningRate)));
        };
        state.setLearningRate(learningRate); return state;
    }

    record Nnue(NnueTrainer trainer) implements NetworkTrainingState {
        public Nnue { Objects.requireNonNull(trainer); if (trainer.materialBootstrap()) throw new IllegalArgumentException("Material NNUE requires its own model identity"); }
        public NetworkModel snapshot() { return new NetworkModel.Nnue(trainer.model().snapshot()); }
        public long step() { return trainer.optimizer().step(); }
        public AdamHyperparameters hyperparameters() { return trainer.optimizer().hyperparameters(); }
        public void write(OutputStream out) throws IOException { TrainingStateCodec.write(trainer, out); }
    }
    record NnueMaterial(NnueTrainer trainer) implements NetworkTrainingState {
        public NnueMaterial { Objects.requireNonNull(trainer); if (!trainer.materialBootstrap()) throw new IllegalArgumentException("Material NNUE requires material-aware training"); }
        public NetworkModel snapshot() { return new NetworkModel.NnueMaterial(trainer.model().snapshot()); }
        public long step() { return trainer.optimizer().step(); }
        public AdamHyperparameters hyperparameters() { return trainer.optimizer().hyperparameters(); }
        public void write(OutputStream out) throws IOException { TrainingStateCodec.writeMaterial(trainer, out); }
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

    record Brn2(Brn2Trainer trainer) implements NetworkTrainingState {
        public Brn2 { Objects.requireNonNull(trainer); }
        public NetworkModel snapshot() { return new NetworkModel.Brn2(trainer.snapshot()); }
        public long step() { return trainer.optimizer().step(); }
        public AdamHyperparameters hyperparameters() {
            var c = trainer.config();
            return new AdamHyperparameters(c.learningRate(), c.beta1(), c.beta2(), c.epsilon());
        }
        public void write(OutputStream out) throws IOException { Brn2Codec.writeTraining(trainer, out); }
    }

    record Brn3(Brn3Trainer trainer) implements NetworkTrainingState {
        public Brn3 { Objects.requireNonNull(trainer); }
        public NetworkModel snapshot(){return new NetworkModel.Brn3(trainer.snapshot());}
        public long step(){return trainer.step();}
        public AdamHyperparameters hyperparameters(){var c=trainer.config();return new AdamHyperparameters(c.learningRate(),c.beta1(),c.beta2(),c.epsilon());}
        public void write(OutputStream out)throws IOException{Brn3Codec.writeTraining(trainer,out);}
    }
    default TrainingArchitecture architecture() {
        return switch (this) {
            case Nnue n -> TrainingArchitecture.NNUE;
            case NnueMaterial n -> TrainingArchitecture.NNUE_MATERIAL;
            case Brn b -> TrainingArchitecture.BRN;
            case Brn1 b -> TrainingArchitecture.BRN1;
            case Brn2 b -> TrainingArchitecture.BRN2;
            case Brn3 b -> TrainingArchitecture.BRN3;
        };
    }
    default byte[] encode() throws IOException {
        return switch (this) {
            case Nnue n -> TrainingStateCodec.encode(n.trainer());
            case NnueMaterial n -> TrainingStateCodec.encodeMaterial(n.trainer());
            case Brn b -> BrnCodec.encodeTraining(b.trainer());
            case Brn1 b -> Brn1Codec.encodeTraining(b.trainer());
            case Brn2 b -> Brn2Codec.encodeTraining(b.trainer());
            case Brn3 b -> Brn3Codec.encodeTraining(b.trainer());
        };
    }
    static NetworkTrainingState read(TrainingArchitecture architecture, InputStream input) throws IOException {
        return switch (architecture) {
            case NNUE -> new Nnue(TrainingStateCodec.read(input));
            case NNUE_MATERIAL -> new NnueMaterial(TrainingStateCodec.readMaterial(input));
            case BRN -> new Brn(BrnCodec.readTraining(input));
            case BRN1 -> new Brn1(Brn1Codec.readTraining(input));
            case BRN2 -> new Brn2(Brn2Codec.readTraining(input));
            case BRN3 -> new Brn3(Brn3Codec.readTraining(input));
        };
    }
}
