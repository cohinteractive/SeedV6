package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;
import java.io.*;

/** R1: factorized pair interactions -> nonlinear pooling -> pawn residual.
 * Each pair contributes the channel-wise product of its two learned entity embeddings.
 * Sum-products identity evaluates all pairs in O(pieces*width), without attack features. */
public final class PooledRelationCandidate {
    static final int WIDTH = 32, EMBEDDINGS = 768 * WIDTH, HIDDEN = EMBEDDINGS,
            BIAS = HIDDEN + WIDTH * WIDTH, HEAD = BIAS + WIDTH, PARAMETERS = HEAD + WIDTH;
    final double[] weights = new double[PARAMETERS], first = new double[PARAMETERS], second = new double[PARAMETERS];
    final double[] gradient = new double[PARAMETERS]; final int[] touched = new int[PARAMETERS], stamp = new int[PARAMETERS];
    int serial, touchedCount; long updates;
    final int[][] rows = new int[2][64];
    final double[][] sums = new double[2][WIDTH], squares = new double[2][WIDTH], pooled = new double[2][WIDTH], hidden = new double[2][WIDTH];
    final double[] pooledGradient = new double[WIDTH];
    int count;
    final boolean antisymmetric;
    public PooledRelationCandidate(long seed) { this(seed, true); }
    public PooledRelationCandidate(long seed, boolean antisymmetric) {
        this.antisymmetric = antisymmetric;
        var random = new Random(seed);
        for (int i = 0; i < EMBEDDINGS; i++) weights[i] = (random.nextDouble() * 2 - 1) * .1;
        for (int i = HIDDEN; i < BIAS; i++) weights[i] = (random.nextDouble() * 2 - 1) * Math.sqrt(6.0 / (2 * WIDTH));
        // Head is zero: the exact fixed material prior is the fresh evaluator.
    }
    public double predict(long[] board) {
        int stm = Board.player((int) board[Board.STATUS]); double residual = 0;
        for (int p = 0; p < 2; p++) {
            Arrays.fill(sums[p], 0); Arrays.fill(squares[p], 0); count = 0;
            for (long occupied = board[0] | board[1] | board[2]; occupied != 0; occupied &= occupied - 1) {
                int square = Long.numberOfTrailingZeros(occupied);
                int channel = RelationalCandidate.channel(Board.getSquare(board[0], board[1], board[2], board[3], square) ^ (p << 3));
                int row = (channel * 64 + (square ^ (p * 56))) * WIDTH; rows[p][count++] = row;
                for (int c = 0; c < WIDTH; c++) { double e = weights[row + c]; sums[p][c] += e; squares[p][c] += e * e; }
            }
            for (int c = 0; c < WIDTH; c++) pooled[p][c] = (sums[p][c] * sums[p][c] - squares[p][c]) / Math.max(1, count - 1);
            double value = 0;
            for (int h = 0; h < WIDTH; h++) {
                double z = weights[BIAS + h]; for (int c = 0; c < WIDTH; c++) z += weights[HIDDEN + h * WIDTH + c] * pooled[p][c];
                hidden[p][h] = Math.tanh(z); value += hidden[p][h] * weights[HEAD + h];
            }
            residual += (antisymmetric ? p == stm ? .5 : -.5 : p == stm ? 1 : 0) * value;
        }
        double prediction = RelationalCandidate.material(board) + residual;
        if (!Double.isFinite(prediction)) throw new ArithmeticException("Nonfinite R1 inference"); return prediction;
    }
    void add(int index, double value) {
        if (stamp[index] != serial) { stamp[index] = serial; touched[touchedCount++] = index; gradient[index] = 0; }
        gradient[index] += value;
    }
    void backward(long[] board, double derivative) {
        if (++serial == 0) { Arrays.fill(stamp, 0); serial = 1; } touchedCount = 0;
        int stm = Board.player((int) board[Board.STATUS]);
        for (int p = 0; p < 2; p++) {
            double scale = derivative * (antisymmetric ? p == stm ? .5 : -.5 : p == stm ? 1 : 0); Arrays.fill(pooledGradient, 0);
            for (int h = 0; h < WIDTH; h++) {
                add(HEAD + h, scale * hidden[p][h]);
                double dz = scale * weights[HEAD + h] * (1 - hidden[p][h] * hidden[p][h]); add(BIAS + h, dz);
                for (int c = 0; c < WIDTH; c++) { add(HIDDEN + h * WIDTH + c, dz * pooled[p][c]); pooledGradient[c] += dz * weights[HIDDEN + h * WIDTH + c]; }
            }
            for (int i = 0; i < count; i++) for (int c = 0; c < WIDTH; c++) {
                int index = rows[p][i] + c;
                add(index, pooledGradient[c] * 2 * (sums[p][c] - weights[index]) / Math.max(1, count - 1));
            }
        }
    }
    public double train(BrnResearchData.Example example, double rate, boolean outcomeLoss) {
        return train(example, rate, outcomeLoss, false);
    }
    public double train(BrnResearchData.Example example, double rate, boolean outcomeLoss, boolean crossEntropy) {
        double value = predict(example.board()), error = value - example.cp() / 100.0;
        double derivative = Math.max(-2, Math.min(2, error));
        if (outcomeLoss) { double[] o = RelationalCandidate.outcome(value, example.sfMaterial()); derivative = .05 * derivative + (o[0] - example.outcome()) * o[1]; }
        if (crossEntropy) derivative = RelationalCandidate.crossEntropy(value, example.sfMaterial(), example.outcome())[1] + .05 * Math.max(-2, Math.min(2, error));
        backward(example.board(), derivative); updates++;
        double correction1 = 1 - Math.pow(.9, updates), correction2 = 1 - Math.pow(.999, updates);
        for (int n = 0; n < touchedCount; n++) {
            int i = touched[n]; double g = gradient[i]; first[i] = .9 * first[i] + .1 * g; second[i] = .999 * second[i] + .001 * g * g;
            weights[i] -= rate * (first[i] / correction1) / (Math.sqrt(second[i] / correction2) + 1e-8);
            if (!Double.isFinite(weights[i])) throw new ArithmeticException("Nonfinite R1 update");
        }
        return Math.abs(error) <= 2 ? .5 * error * error : 2 * (Math.abs(error) - 1);
    }
    public void write(OutputStream stream) throws IOException {
        var out = new DataOutputStream(stream); out.writeLong(0x5336523152455332L); out.writeBoolean(antisymmetric); out.writeLong(updates);
        for (double[] block : new double[][]{weights, first, second}) for (double x : block) out.writeDouble(x); out.flush();
    }
    public static PooledRelationCandidate read(InputStream stream) throws IOException {
        var in = new DataInputStream(stream); long magic = in.readLong();
        if (magic != 0x5336523152455331L && magic != 0x5336523152455332L) throw new IOException("Wrong R1 format");
        var model = new PooledRelationCandidate(0, magic == 0x5336523152455331L || in.readBoolean()); model.updates = in.readLong(); if (model.updates < 0) throw new IOException("Invalid step");
        for (double[] block : new double[][]{model.weights, model.first, model.second}) for (int i = 0; i < block.length; i++) {
            block[i] = in.readDouble(); if (!Double.isFinite(block[i]) || block == model.second && block[i] < 0) throw new IOException("Invalid state");
        }
        if (in.read() != -1) throw new IOException("Trailing bytes"); return model;
    }
}
