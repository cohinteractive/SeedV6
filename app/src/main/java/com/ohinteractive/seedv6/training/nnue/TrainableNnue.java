package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.nnue.NnueFeatureSchema;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;

import java.util.Objects;

/**
 * Mutable offline V1 model. A single caller owns the model and its trainer; neither is
 * thread-safe. Search may consume only an independent immutable snapshot.
 */
public final class TrainableNnue {
    final Parameters parameters;

    // Exclusive ownership transfer from package factories/decoder only.
    TrainableNnue(Parameters parameters) {
        parameters.validate(false);
        this.parameters = parameters;
    }

    /** Uses the immutable model's authoritative deterministic initialization, bit for bit. */
    public static TrainableNnue initialized(long seed) {
        return fromNetwork(NnueNetwork.initialized(seed));
    }

    public static TrainableNnue fromNetwork(NnueNetwork network) {
        return new TrainableNnue(Parameters.from(Objects.requireNonNull(network, "network")));
    }

    public int schemaVersion() { return NnueFeatureSchema.VERSION; }
    public String schemaId() { return NnueFeatureSchema.ID; }

    /** Copies all parameters; later training cannot change the exported network. */
    public NnueNetwork snapshot() { return parameters.snapshot(); }
}
