package com.ohinteractive.seedv6.core.brn3;

import java.util.Objects;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** Immutable folded float32 network. Outputs are fixed material plus a pawn-unit residual. */
public final class Brn3Model {
    public static final int PARAMETER_COUNT=Brn3Layout.MODEL_PARAMETERS;
    final float[] weights;
    public Brn3Model(float[] weights) {
        Objects.requireNonNull(weights);
        if(weights.length!=PARAMETER_COUNT)throw new IllegalArgumentException("BRN-3 parameter count");
        this.weights=weights.clone();
        for(float value:this.weights)if(!Float.isFinite(value))throw new IllegalArgumentException("Nonfinite BRN-3 weight");
    }
    public float weight(int index){return weights[index];}
    public Brn3Workspace newWorkspace(){return new Brn3Workspace(this);}
    /** Engine convention only: 100 score units per pawn, symmetric rounding, mate-band protection. */
    public static int score(double pawns) {
        if(!Double.isFinite(pawns))throw new IllegalArgumentException("Nonfinite pawn evaluation");
        return (int)Math.copySign(Math.min(TranspositionScores.MAX_NORMAL_SCORE,Math.floor(Math.abs(pawns)*100+.5)),pawns);
    }
}
