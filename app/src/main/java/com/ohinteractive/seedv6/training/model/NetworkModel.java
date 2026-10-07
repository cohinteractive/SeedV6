package com.ohinteractive.seedv6.training.model;

import java.io.*;
import java.util.Objects;
import com.ohinteractive.seedv6.core.brn.*;
import com.ohinteractive.seedv6.core.brn1.*;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.evaluation.*;

/** Immutable pinned actor at the training/search/persistence boundary. No fallback evaluator. */
public sealed interface NetworkModel {
    TrainingArchitecture architecture();
    SearchEvaluation evaluation(NnueScoreMapping nnueMapping);
    void write(OutputStream output) throws IOException;

    record Nnue(NnueNetwork network) implements NetworkModel {
        public Nnue { Objects.requireNonNull(network); }
        public TrainingArchitecture architecture() { return TrainingArchitecture.NNUE; }
        public SearchEvaluation evaluation(NnueScoreMapping mapping) { return SearchEvaluation.incremental(network, mapping); }
        public void write(OutputStream out) throws IOException { NnueNetworkCodec.write(network, out); }
    }
    /** E008 practical baseline: unchanged HalfKP initializer/mapping plus incremental fixed material. */
    record NnueMaterial(NnueNetwork network, boolean calibratedOutcome) implements NetworkModel {
        /** Retained E012 low-level constructor; new lineages use the versioned trainer factory. */
        public NnueMaterial(NnueNetwork network) { this(network, false); }
        public NnueMaterial { Objects.requireNonNull(network); }
        public TrainingArchitecture architecture() { return TrainingArchitecture.NNUE_MATERIAL; }
        public SearchEvaluation evaluation(NnueScoreMapping mapping) {
            if (!NnueScoreMapping.V1.equals(mapping)) throw new IllegalArgumentException("Material NNUE requires its persisted V1 residual scale.");
            return SearchEvaluation.incrementalWithMaterial(network, mapping);
        }
        public void write(OutputStream out) throws IOException {
            if (calibratedOutcome) NnueNetworkCodec.writeCalibratedMaterial(network, out);
            else NnueNetworkCodec.writeMaterial(network, out);
        }
    }
    record Brn(BrnModel model) implements NetworkModel {
        public Brn { Objects.requireNonNull(model); }
        public TrainingArchitecture architecture() { return TrainingArchitecture.BRN; }
        public SearchEvaluation evaluation(NnueScoreMapping mapping) {
            if (!NnueScoreMapping.V1.equals(mapping)) throw new IllegalArgumentException("BRN uses fixed full-range search units.");
            return SearchEvaluation.brn(model);
        }
        public void write(OutputStream out) throws IOException { BrnCodec.writeModel(model, out); }
    }

    record Brn1(Brn1Model model) implements NetworkModel {
        public Brn1 { Objects.requireNonNull(model); }
        public TrainingArchitecture architecture() { return TrainingArchitecture.BRN1; }
        public SearchEvaluation evaluation(NnueScoreMapping mapping) {
            if (!NnueScoreMapping.V1.equals(mapping)) throw new IllegalArgumentException("BRN-1 uses fixed full-range search units.");
            return SearchEvaluation.brn1(model);
        }
        public void write(OutputStream out) throws IOException { Brn1Codec.writeModel(model, out); }
    }

    record Brn2(Brn2Model model) implements NetworkModel {
        public Brn2 { Objects.requireNonNull(model); }
        public TrainingArchitecture architecture() { return TrainingArchitecture.BRN2; }
        public SearchEvaluation evaluation(NnueScoreMapping mapping) {
            if (!NnueScoreMapping.V1.equals(mapping)) throw new IllegalArgumentException("BRN-2 uses fixed full-range search units.");
            return SearchEvaluation.brn2(model);
        }
        public void write(OutputStream out) throws IOException { Brn2Codec.writeModel(model, out); }
    }

    record Brn3(Brn3Model model) implements NetworkModel {
        public Brn3 { Objects.requireNonNull(model); }
        public TrainingArchitecture architecture(){return TrainingArchitecture.BRN3;}
        public SearchEvaluation evaluation(NnueScoreMapping mapping) {
            if(!NnueScoreMapping.V1.equals(mapping))throw new IllegalArgumentException("BRN-3 uses 100 engine units per pawn.");
            return SearchEvaluation.brn3(model);
        }
        public void write(OutputStream out)throws IOException{Brn3Codec.writeModel(model,out);}
    }

    record BrnPair2(BrnPair2Model model) implements NetworkModel {
        public BrnPair2 { Objects.requireNonNull(model); }
        public TrainingArchitecture architecture(){return TrainingArchitecture.BRN_PAIR2;}
        public SearchEvaluation evaluation(NnueScoreMapping mapping) {
            if(!NnueScoreMapping.V1.equals(mapping))throw new IllegalArgumentException("Pair-2 uses 100 engine units per pawn.");
            return SearchEvaluation.brnPair2(model);
        }
        public void write(OutputStream out)throws IOException{BrnPair2Codec.writeModel(model,out);}
    }

    /** Legacy NNUE-only consumers (including Play) fail explicitly on BRN. */
    default NnueNetwork nnue() {
        if (this instanceof Nnue n) return n.network();
        throw new IllegalStateException("Architecture mismatch: expected NNUE, found " + architecture());
    }

    static NetworkModel read(TrainingArchitecture architecture, InputStream input) throws IOException {
        return switch (architecture) {
            case NNUE -> new Nnue(NnueNetworkCodec.read(input));
            case NNUE_MATERIAL -> {
                var stream = new PushbackInputStream(input, 8);
                byte[] prefix = stream.readNBytes(8); stream.unread(prefix);
                boolean calibrated = prefix.length == 8 && java.nio.ByteBuffer.wrap(prefix).getLong() == NnueNetworkCodec.CALIBRATED_MATERIAL_MAGIC;
                yield new NnueMaterial(calibrated ? NnueNetworkCodec.readCalibratedMaterial(stream) : NnueNetworkCodec.readMaterial(stream), calibrated);
            }
            case BRN -> new Brn(BrnCodec.readModel(input));
            case BRN1 -> new Brn1(Brn1Codec.readModel(input));
            case BRN2 -> new Brn2(Brn2Codec.readModel(input));
            case BRN3 -> new Brn3(Brn3Codec.readModel(input));
            case BRN_PAIR2 -> new BrnPair2(BrnPair2Codec.readModel(input));
        };
    }
}
