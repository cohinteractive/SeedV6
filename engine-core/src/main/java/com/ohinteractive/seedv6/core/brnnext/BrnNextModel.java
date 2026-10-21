package com.ohinteractive.seedv6.core.brnnext;

import java.util.Arrays;
import java.util.Objects;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Compact, immutable research-candidate inference. No teacher or training dependency. */
public final class BrnNextModel {
    public static final double RESIDUAL_GAIN = .25;
    public static final String CALIBRATION_ID = "BRN_NEXT_MATERIAL_RESIDUAL_QUARTER_V1";
    private final BrnNextArchitecture architecture;
    private final double[] weights;

    public BrnNextModel(BrnNextArchitecture architecture, double[] weights) {
        this.architecture = Objects.requireNonNull(architecture);
        if (weights.length != architecture.parameters()) throw new IllegalArgumentException("BRN-next layout mismatch");
        this.weights = weights.clone();
        for (double value : weights) if (!Double.isFinite(value)) throw new IllegalArgumentException("Nonfinite BRN-next weight");
    }
    public BrnNextArchitecture architecture() { return architecture; }
    public double weight(int index) { return weights[index]; }
    public Workspace newWorkspace() { return new Workspace(architecture); }
    /** Explicit inference composition, never a change to this component's codec or optimizer.
     * Callers must bind both immutable components as a distinct research function. */
    public Workspace newResearchWorkspace(com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model frozenBase) {
        if (!architecture.frozenBase()) throw new IllegalArgumentException("Frozen-base correction required");
        return new Workspace(architecture, Objects.requireNonNull(frozenBase));
    }
    public double rawPawns(long[] board, Workspace workspace) { return workspace.forward(weights, board, architecture); }
    public int score(long[] board, Workspace workspace) {
        double raw = rawPawns(board, workspace);
        return Brn3Model.score(workspace.material + RESIDUAL_GAIN * (raw - workspace.material));
    }

    /** Private per worker. Full canonical refresh is deterministic and allocation-free.
     * No floating rolling cache is claimed; the independent reference precedes optimization. */
    public static final class Workspace {
        final BrnNextArchitecture architecture;
        final int[] features = new int[64], codes = new int[64], pairs = new int[210];
        final double[] first, second, third, output, gates;
        private final com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model.Workspace base;
        int count;
        double material;
        public Workspace(BrnNextArchitecture architecture) {
            this(architecture, architecture.frozenBase() ? BrnNextBase.model() : null);
        }
        private Workspace(BrnNextArchitecture architecture,
                          com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model frozenBase) {
            this.architecture = Objects.requireNonNull(architecture);
            base = frozenBase == null ? null : frozenBase.newWorkspace();
            int width = architecture.width();
            first = new double[width]; second = new double[width]; third = new double[width];
            output = new double[width]; gates = new double[width];
        }
        double forward(double[] weights, long[] board, BrnNextArchitecture expected) {
            if (architecture != expected) throw new IllegalArgumentException("BRN-next workspace mismatch");
            int stm = Board.player((int) board[4]); count = 0;
            // Canonical order makes color/rank transformed positions bit-identical.
            for (int square = 0; square < 64; square++) {
                int code = Board.getSquare(board[0], board[1], board[2], board[3], square ^ (stm * 56));
                int category = (code & 7) == 0 ? 0 : (code & 7) + (((code >>> 3) ^ stm) & 1) * 6;
                if (category < 0 || category > 12) throw new IllegalArgumentException("Invalid piece category");
                codes[square] = category;
                if (category != 0) features[count++] = (category - 1) * 64 + square;
            }
            material = BrnPair2Features.material(board);
            double residual = weights[0];
            for (int i = 0; i < count; i++) residual += PIECE_NORM * weights[UNARY + features[i]];
            Arrays.fill(first, 0); Arrays.fill(second, 0); Arrays.fill(third, 0);
            int width = architecture.width(), head = architecture.head();
            for (int i = 0; i < count; i++) {
                int base = EMBEDDING + features[i] * width;
                for (int r = 0; r < width; r++) {
                    double value = weights[base + r];
                    if (architecture.correctionKind() == CUBIC) {
                        third[r] += second[r] * value;
                        second[r] += first[r] * value;
                    }
                    first[r] += value;
                }
            }
            switch (architecture) {
                case CUBIC, GEN32_CUBIC_R4, GEN32_CUBIC -> {
                    for (int r = 0; r < width; r++) residual += weights[head + r] * second[r] * DEGREE2_NORM
                            + weights[head + width + r] * third[r] * DEGREE3_NORM;
                }
                case POOLED -> {
                    for (int r = 0; r < width; r++) {
                        output[r] = Math.tanh(first[r] * PIECE_NORM + weights[head + r]);
                        residual += weights[head + width + r] * output[r];
                    }
                }
                case GATED_PAIRS, GEN32_GATED -> {
                    int n = 0;
                    for (int shape = 0; shape < 4; shape++) {
                        int dx = shape == 1 ? 0 : shape == 3 ? -1 : 1;
                        int dy = shape == 0 ? 0 : 1;
                        for (int square = 0; square < 64; square++) {
                            int x = (square & 7) + dx, y = (square >>> 3) + dy;
                            if (x >= 0 && x < 8 && y < 8)
                                pairs[n++] = shape * 169 + codes[square] * 13 + codes[y * 8 + x];
                        }
                    }
                    double max = Double.NEGATIVE_INFINITY, total = 0;
                    for (int r = 0; r < width; r++) {
                        gates[r] = first[r] * PIECE_NORM + weights[head + r];
                        max = Math.max(max, gates[r]);
                    }
                    for (int r = 0; r < width; r++) { gates[r] = Math.exp(gates[r] - max); total += gates[r]; }
                    for (int r = 0; r < width; r++) {
                        gates[r] /= total;
                        double sum = 0; int base = head + width + r * PAIR_STATES;
                        for (int address : pairs) sum += weights[base + address];
                        output[r] = sum * PAIR_NORM;
                        residual += gates[r] * output[r];
                    }
                }
            }
            double result = (base == null ? material : base.evaluatePawns(board)) + residual;
            if (!Double.isFinite(result)) throw new ArithmeticException("Nonfinite BRN-next evaluation");
            return result;
        }
    }
}
