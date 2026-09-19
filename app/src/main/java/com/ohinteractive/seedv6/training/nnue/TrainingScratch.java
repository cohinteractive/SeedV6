package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueFeatureSchema;
import com.ohinteractive.seedv6.core.nnue.NnueMath;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

import java.util.Arrays;
import java.util.Objects;

/** Reused forward/backward scratch; no sample objects or successful-path allocations. */
final class TrainingScratch {
    final int[] whiteFeatures = new int[64], blackFeatures = new int[64];
    final float[] white = new float[64], black = new float[64];
    final float[] input = new float[128], hiddenRaw = new float[32], hidden = new float[32];
    final double[] inputDelta = new double[128];
    int features, first;
    float raw;
    double value;

    double forward(Parameters p, long[] board) {
        collect(board);
        System.arraycopy(p.groups[0], 0, white, 0, 64);
        System.arraycopy(p.groups[0], 0, black, 0, 64);
        for (int i = 0; i < features; i++) {
            NnueMath.addFeature(p.groups[1], whiteFeatures[i], white);
            NnueMath.addFeature(p.groups[1], blackFeatures[i], black);
        }
        first = Board.player((int) board[Board.STATUS]);
        for (int i = 0; i < 64; i++) {
            requireFinite(white[i]);
            requireFinite(black[i]);
            input[i] = NnueNetwork.clip01(first == Value.WHITE ? white[i] : black[i]);
            input[64 + i] = NnueNetwork.clip01(first == Value.WHITE ? black[i] : white[i]);
        }
        raw = NnueMath.forward(p.groups[2], p.groups[3], p.groups[4][0], p.groups[5],
                input, hiddenRaw, hidden);
        for (float sum : hiddenRaw) requireFinite(sum);
        requireFinite(raw);
        value = StrictMath.tanh(raw);
        return value;
    }

    void backward(Parameters p, BatchGradients gradients, double target) {
        float[][] g = gradients.values.groups;
        // V1: L = 0.5*(v-target)^2; v = tanh(u); dL/du = (v-target)*(1-v*v).
        double outputDelta = (value - target) * (1 - value * value);
        g[4][0] += outputDelta;
        Arrays.fill(inputDelta, 0);
        for (int unit = 0; unit < 32; unit++) {
            g[5][unit] += outputDelta * hidden[unit];
            double delta = outputDelta * p.groups[5][unit] * clipDerivative(hiddenRaw[unit]);
            g[2][unit] += delta;
            int offset = unit * 128;
            for (int i = 0; i < 128; i++) {
                g[3][offset + i] += delta * input[i];
                inputDelta[i] += delta * p.groups[3][offset + i];
            }
        }
        int w = first == Value.WHITE ? 0 : 64;
        int b = 64 - w;
        for (int i = 0; i < 64; i++) {
            inputDelta[w + i] *= clipDerivative(white[i]);
            inputDelta[b + i] *= clipDerivative(black[i]);
            g[0][i] += inputDelta[w + i] + inputDelta[b + i];
        }
        for (int i = 0; i < features; i++) {
            gradients.addRow(whiteFeatures[i], inputDelta, w);
            gradients.addRow(blackFeatures[i], inputDelta, b);
        }
    }

    /** Derivative is zero at both clipping boundaries, including exactly 0 and 1. */
    static int clipDerivative(float x) { return x > 0 && x < 1 ? 1 : 0; }

    private void collect(long[] board) {
        Objects.requireNonNull(board, "board");
        if (board.length < Board.MAX_BITBOARDS) throw new IllegalArgumentException("Short board.");
        long occupied = board[0] | board[1] | board[2];
        long kings = board[0] & ~board[1] & ~board[2];
        if ((board[3] & ~occupied) != 0 || (board[0] & board[1] & board[2]) != 0
                || Long.bitCount(kings & ~board[3]) != 1 || Long.bitCount(kings & board[3]) != 1) {
            throw new IllegalArgumentException("Invalid piece encoding or king count.");
        }
        int whiteKing = Long.numberOfTrailingZeros(kings & ~board[3]);
        int blackKing = Long.numberOfTrailingZeros(kings & board[3]);
        features = 0;
        // Same ascending occupied-square order and schema as inference, including both kings.
        while (occupied != 0) {
            int square = Long.numberOfTrailingZeros(occupied);
            occupied &= occupied - 1;
            int piece = Board.getSquare(board[0], board[1], board[2], board[3], square);
            int colour = piece >>> Board.PLAYER_SHIFT;
            int type = piece & Piece.TYPE;
            whiteFeatures[features] = NnueFeatureSchema.featureIndex(
                    Value.WHITE, whiteKing, colour, type, square);
            blackFeatures[features++] = NnueFeatureSchema.featureIndex(
                    Value.BLACK, blackKing, colour, type, square);
        }
    }

    private static void requireFinite(float value) {
        if (!Float.isFinite(value)) throw new ArithmeticException("Nonfinite forward calculation.");
    }
}
