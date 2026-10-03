package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;
import java.io.*;
import java.util.*;

/** R3 research: absolute king/entity relations, shared perspective pooling, pawn residual.
 * King identity selects an anchor; there are no attack or king-safety features. The
 * relation indexing resembles HalfKP deliberately: this tests whether retaining
 * absolute relation geometry fixes the compressed candidates' learning deficit.
 * It does not claim a novel feature schema. Material/output semantics are new.
 */
public final class AnchoredRelationCandidate {
    static final int WIDTH = 32, HIDDEN_WIDTH = 32, RELATION_ROWS = 64 * 768;
    static final int POOL_BIAS = RELATION_ROWS * WIDTH, DENSE = POOL_BIAS + WIDTH,
            BIAS = DENSE + 2 * WIDTH * HIDDEN_WIDTH, HEAD = BIAS + HIDDEN_WIDTH,
            OUTPUT_BIAS = HEAD + HIDDEN_WIDTH, PARAMETERS = OUTPUT_BIAS + 1;
    final double[] weights = new double[PARAMETERS], first = new double[PARAMETERS], second = new double[PARAMETERS];
    final double[] gradient = new double[PARAMETERS];
    final int[] touched = new int[PARAMETERS], stamps = new int[PARAMETERS];
    final int[][] rows = new int[2][64];
    final double[][] pooled = new double[2][WIDTH], poolGradient = new double[2][WIDTH];
    final double[] hidden = new double[HIDDEN_WIDTH];
    int serial, size, count; long updates;
    double norm;

    public AnchoredRelationCandidate(long seed) {
        var rng = new Random(seed);
        for (int i = 0; i < POOL_BIAS; i++) weights[i] = (2 * rng.nextDouble() - 1) * .1;
        for (int i = DENSE; i < BIAS; i++) weights[i] = (2 * rng.nextDouble() - 1) * Math.sqrt(6.0 / (2 * WIDTH + HIDDEN_WIDTH));
        // Head and output bias are zero: exact material baseline, in pawns.
    }

    public double predict(long[] board) {
        int stm = Board.player((int) board[Board.STATUS]);
        for (int role = 0; role < 2; role++) {
            int perspective = stm ^ role, king = -1;
            for (long occupied = board[0] | board[1] | board[2]; occupied != 0; occupied &= occupied - 1) {
                int sq = Long.numberOfTrailingZeros(occupied);
                int code = Board.getSquare(board[0], board[1], board[2], board[3], sq);
                if ((code & Piece.TYPE) == Piece.KING && (code >>> 3) == perspective) king = sq ^ (perspective * 56);
            }
            if (king < 0) throw new IllegalArgumentException("Missing relation anchor");
            count = 0;
            System.arraycopy(weights, POOL_BIAS, pooled[role], 0, WIDTH);
            for (long occupied = board[0] | board[1] | board[2]; occupied != 0; occupied &= occupied - 1) {
                int sq = Long.numberOfTrailingZeros(occupied);
                int code = RelationalCandidate.channel(Board.getSquare(board[0], board[1], board[2], board[3], sq) ^ (perspective << 3));
                rows[role][count++] = (king * 768 + code * 64 + (sq ^ (perspective * 56))) * WIDTH;
            }
            norm = 1.0 / Math.sqrt(Math.max(1, count));
            for (int n = 0; n < count; n++) for (int c = 0; c < WIDTH; c++) pooled[role][c] += norm * weights[rows[role][n] + c];
            for (int c = 0; c < WIDTH; c++) pooled[role][c] = Math.tanh(pooled[role][c]);
        }
        double residual = weights[OUTPUT_BIAS];
        for (int h = 0; h < HIDDEN_WIDTH; h++) {
            double z = weights[BIAS + h];
            for (int p = 0; p < 2; p++) for (int c = 0; c < WIDTH; c++) z += weights[DENSE + h * 2 * WIDTH + p * WIDTH + c] * pooled[p][c];
            hidden[h] = Math.tanh(z); residual += weights[HEAD + h] * hidden[h];
        }
        double value = RelationalCandidate.material(board) + residual;
        if (!Double.isFinite(value)) throw new ArithmeticException("Nonfinite R3 prediction");
        return value;
    }

    void resetGradient() { if (++serial == 0) { Arrays.fill(stamps, 0); serial = 1; } size = 0; }
    void add(int index, double value) {
        if (stamps[index] != serial) { stamps[index] = serial; gradient[index] = 0; touched[size++] = index; }
        gradient[index] += value;
    }
    /** Accumulates against scratch from the immediately preceding prediction. */
    void backward(double derivative) {
        for (var block : poolGradient) Arrays.fill(block, 0);
        add(OUTPUT_BIAS, derivative);
        for (int h = 0; h < HIDDEN_WIDTH; h++) {
            add(HEAD + h, derivative * hidden[h]);
            double dz = derivative * weights[HEAD + h] * (1 - hidden[h] * hidden[h]); add(BIAS + h, dz);
            for (int p = 0; p < 2; p++) for (int c = 0; c < WIDTH; c++) {
                int index = DENSE + h * 2 * WIDTH + p * WIDTH + c;
                add(index, dz * pooled[p][c]); poolGradient[p][c] += dz * weights[index];
            }
        }
        for (int p = 0; p < 2; p++) for (int c = 0; c < WIDTH; c++) {
            double dz = poolGradient[p][c] * (1 - pooled[p][c] * pooled[p][c]); add(POOL_BIAS + c, dz);
            for (int n = 0; n < count; n++) add(rows[p][n] + c, norm * dz);
        }
    }
    public void trainBatch(List<BrnResearchData.Example> examples, int[] order, int offset, int batch, double rate, double auxiliary) {
        trainBatch(examples, order, offset, batch, rate, auxiliary, false);
    }
    public void trainBatch(List<BrnResearchData.Example> examples, int[] order, int offset, int batch, double rate, double auxiliary, boolean crossEntropy) {
        resetGradient();
        for (int n = 0; n < batch; n++) {
            var e = examples.get(order[offset + n]); double value = predict(e.board());
            double[] outcome = RelationalCandidate.outcome(value, e.sfMaterial());
            double derivative = (crossEntropy ? RelationalCandidate.crossEntropy(value, e.sfMaterial(), e.outcome())[1] : (outcome[0] - e.outcome()) * outcome[1])
                    + auxiliary * Math.max(-2, Math.min(2, value - e.cp() / 100.0));
            backward(derivative);
        }
        update(rate, batch);
    }
    void update(double rate, int batch) {
        updates++; double c1 = 1 - Math.pow(.9, updates), c2 = 1 - Math.pow(.999, updates);
        for (int n = 0; n < size; n++) {
            int i = touched[n]; double g = gradient[i] / batch;
            first[i] = .9 * first[i] + .1 * g; second[i] = .999 * second[i] + .001 * g * g;
            weights[i] -= rate * (first[i] / c1) / (Math.sqrt(second[i] / c2) + 1e-8);
            if (!Double.isFinite(weights[i])) throw new ArithmeticException("Nonfinite R3 update");
        }
    }
    public void write(OutputStream stream) throws IOException {
        var out = new DataOutputStream(stream); out.writeLong(0x5336523352455331L); out.writeLong(updates);
        for (var block : new double[][]{weights, first, second}) for (double value : block) out.writeDouble(value); out.flush();
    }
    public static AnchoredRelationCandidate read(InputStream stream) throws IOException {
        var in = new DataInputStream(stream); if (in.readLong() != 0x5336523352455331L) throw new IOException("Wrong R3 format");
        var model = new AnchoredRelationCandidate(0); model.updates = in.readLong(); if (model.updates < 0) throw new IOException("Invalid step");
        for (var block : new double[][]{model.weights, model.first, model.second}) for (int i = 0; i < block.length; i++) {
            block[i] = in.readDouble(); if (!Double.isFinite(block[i]) || block == model.second && block[i] < 0) throw new IOException("Invalid state");
        }
        if (in.read() != -1) throw new IOException("Trailing bytes"); return model;
    }
}
