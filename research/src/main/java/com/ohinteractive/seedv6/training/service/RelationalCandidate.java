package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;
import java.util.*;
import java.io.*;

/** R0 research candidate: fixed pawn material plus learned signed pair potentials.
 * Reads placement and STM only. This class is not shipped or a checkpoint format. */
public final class RelationalCandidate {
    public static final int NODE_ROWS = 12 * 64, PAIR_ROWS = 12 * 12 * 225;
    public static final int PARAMETERS = NODE_ROWS + PAIR_ROWS;
    final double[] weights = new double[PARAMETERS], squares = new double[PARAMETERS];
    final double[] features = new double[PARAMETERS];
    final int[] touched = new int[PARAMETERS], stamps = new int[PARAMETERS];
    final int[] locations = new int[64], codes = new int[64];
    int stamp, size;
    final boolean normalize;
    public long updates;
    public RelationalCandidate(boolean normalize) { this.normalize = normalize; }

    static int channel(int code) {
        int type = code & Piece.TYPE;
        if (type < Piece.KING || type > Piece.PAWN) throw new IllegalArgumentException("Invalid piece");
        return (type - 1) + ((code >>> 3) & 1) * 6;
    }
    public static double material(long[] board) {
        double white = 0;
        for (long occupied = board[0] | board[1] | board[2]; occupied != 0; occupied &= occupied - 1) {
            int code = Board.getSquare(board[0], board[1], board[2], board[3], Long.numberOfTrailingZeros(occupied));
            double value = switch (code & Piece.TYPE) { case Piece.PAWN -> 1; case Piece.KNIGHT -> 3.2;
                case Piece.BISHOP -> 3.3; case Piece.ROOK -> 5; case Piece.QUEEN -> 9; default -> 0; };
            white += (code & 8) == 0 ? value : -value;
        }
        return Board.player((int) board[Board.STATUS]) == 0 ? white : -white;
    }
    void add(int index, double value) {
        if (stamps[index] != stamp) { stamps[index] = stamp; features[index] = 0; touched[size++] = index; }
        features[index] += value;
    }
    void encode(long[] board) {
        if (++stamp == 0) { Arrays.fill(stamps, 0); stamp = 1; }
        size = 0;
        int stm = Board.player((int) board[Board.STATUS]);
        for (int perspective = 0; perspective < 2; perspective++) {
            double sign = perspective == stm ? .5 : -.5;
            long occupied = board[0] | board[1] | board[2];
            int count = 0;
            for (; occupied != 0; occupied &= occupied - 1) {
                int physical = Long.numberOfTrailingZeros(occupied), square = physical ^ (perspective * 56);
                int code = channel(Board.getSquare(board[0], board[1], board[2], board[3], physical) ^ (perspective << 3));
                locations[count] = square; codes[count++] = code;
                add(code * 64 + square, sign);
            }
            double pairWeight = sign / (normalize ? Math.max(1, count - 1) : 16.0);
            for (int i = 0; i < count; i++) for (int j = i + 1; j < count; j++) {
                int a = locations[i] < locations[j] ? i : j, b = a == i ? j : i;
                int dx = (locations[b] & 7) - (locations[a] & 7), dy = (locations[b] >>> 3) - (locations[a] >>> 3);
                add(NODE_ROWS + (codes[a] * 12 + codes[b]) * 225 + (dy + 7) * 15 + dx + 7, pairWeight);
            }
        }
    }
    public double predict(long[] board) {
        encode(board); double value = material(board);
        for (int i = 0; i < size; i++) value += weights[touched[i]] * features[touched[i]];
        if (!Double.isFinite(value)) throw new ArithmeticException("Nonfinite candidate prediction");
        return value;
    }
    /** Huber residual loss, delta 2 pawns; accumulated per-parameter AdaGrad in pawn units. */
    public double train(long[] board, double target, double rate) {
        if (!Double.isFinite(target) || !Double.isFinite(rate) || rate <= 0) throw new IllegalArgumentException();
        double error = predict(board) - target, derivative = Math.max(-2, Math.min(2, error));
        for (int i = 0; i < size; i++) {
            int index = touched[i]; double gradient = derivative * features[index];
            squares[index] += gradient * gradient;
            weights[index] -= rate * gradient / (Math.sqrt(squares[index]) + 1e-8);
            if (!Double.isFinite(weights[index])) throw new ArithmeticException("Nonfinite update");
        }
        updates++;
        return Math.abs(error) <= 2 ? .5 * error * error : 2 * (Math.abs(error) - 1);
    }
    /** Differentiable supervision only; never a runtime feature or inference score mapping. */
    static double[] outcome(double pawns, int material) {
        double m = Math.max(17, Math.min(78, material)) / 58.0;
        double a = ((-13.50030198 * m + 40.92780883) * m - 36.82753545) * m + 386.83004070;
        double b = ((96.53354896 * m - 165.79058388) * m + 90.89679019) * m + 49.29561889;
        double w = 1 / (1 + Math.exp((a - pawns * a) / b)), l = 1 / (1 + Math.exp((a + pawns * a) / b));
        return new double[]{w - l, a / b * (w * (1 - w) + l * (1 - l))};
    }
    /** Binary expected-score cross entropy through the SAME smooth WDL link.
     * Complement probabilities are evaluated directly to avoid cancellation in
     * confident material positions. Returns loss and derivative in pawn units. */
    static double[] crossEntropy(double pawns, int material, double target) {
        double m = Math.max(17, Math.min(78, material)) / 58.0;
        double a = ((-13.50030198 * m + 40.92780883) * m - 36.82753545) * m + 386.83004070;
        double b = ((96.53354896 * m - 165.79058388) * m + 90.89679019) * m + 49.29561889;
        double k = a / b, z1 = k * (pawns - 1), z2 = k * (pawns + 1);
        double w = sigmoid(z1), s = sigmoid(z2), wc = sigmoid(-z1), sc = sigmoid(-z2);
        double denominator = (w + s) * (wc + sc);
        double ratio = denominator < 1e-200 ? k / 2 : k * (w * wc + s * sc) / denominator;
        double prediction = pawns >= 0 ? 1 - wc - sc : w + s - 1;
        double logP = logAdd(logSigmoid(z1), logSigmoid(z2)) - Math.log(2);
        double logQ = logAdd(logSigmoid(-z1), logSigmoid(-z2)) - Math.log(2);
        return new double[]{-.5 * ((1 + target) * logP + (1 - target) * logQ), (prediction - target) * ratio};
    }
    private static double sigmoid(double z) { double e = Math.exp(-Math.abs(z)); return z >= 0 ? 1 / (1 + e) : e / (1 + e); }
    private static double logSigmoid(double z) { return -Math.max(0, -z) - Math.log1p(Math.exp(-Math.abs(z))); }
    private static double logAdd(double a, double b) { double max = Math.max(a,b); return max + Math.log(Math.exp(a-max)+Math.exp(b-max)); }
    public double trainOutcome(BrnResearchData.Example example, double rate) {
        double value = predict(example.board()), error = value - example.cp() / 100.0;
        double[] outcome = outcome(value, example.sfMaterial());
        double derivative = (outcome[0] - example.outcome()) * outcome[1] + .05 * Math.max(-2, Math.min(2, error));
        for (int i = 0; i < size; i++) {
            int index = touched[i]; double gradient = derivative * features[index]; squares[index] += gradient * gradient;
            weights[index] -= rate * gradient / (Math.sqrt(squares[index]) + 1e-8);
            if (!Double.isFinite(weights[index])) throw new ArithmeticException("Nonfinite update");
        }
        updates++; return .5 * Math.pow(outcome[0] - example.outcome(), 2);
    }
    public void write(OutputStream stream) throws IOException {
        var out = new DataOutputStream(stream); out.writeLong(0x5336523052455331L); out.writeBoolean(normalize); out.writeLong(updates);
        for (double w : weights) out.writeDouble(w); for (double g : squares) out.writeDouble(g); out.flush();
    }
    public static RelationalCandidate read(InputStream stream) throws IOException {
        var in = new DataInputStream(stream); if (in.readLong() != 0x5336523052455331L) throw new IOException("Wrong R0 format");
        var model = new RelationalCandidate(in.readBoolean()); model.updates = in.readLong();
        if (model.updates < 0) throw new IOException("Invalid update count");
        for (int i = 0; i < PARAMETERS; i++) { model.weights[i] = in.readDouble(); if (!Double.isFinite(model.weights[i])) throw new IOException("Nonfinite model"); }
        for (int i = 0; i < PARAMETERS; i++) { model.squares[i] = in.readDouble(); if (!Double.isFinite(model.squares[i]) || model.squares[i] < 0) throw new IOException("Invalid optimizer"); }
        if (in.read() != -1) throw new IOException("Trailing data"); return model;
    }
}
