package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;

/** R2: piece-local learned typed-displacement messages, normalized aggregation,
 * tanh node states and a zero-initialized pawn-residual head. No rule-state inputs. */
public final class MessageRelationCandidate {
    static final int WIDTH = 16, RELATIONS = 768 * WIDTH, HEAD = RELATIONS + 12 * 12 * 225 * WIDTH, PARAMETERS = HEAD + WIDTH;
    final double[] weights = new double[PARAMETERS], first = new double[PARAMETERS], second = new double[PARAMETERS], gradient = new double[PARAMETERS];
    final int[] touched = new int[PARAMETERS], stamps = new int[PARAMETERS]; int serial, size; long updates;
    final int[][] squares = new int[2][64], codes = new int[2][64];
    final double[][] local = new double[2][64 * WIDTH];
    int count;
    public MessageRelationCandidate(long seed) {
        var rng = new Random(seed);
        for (int i = 0; i < HEAD; i++) weights[i] = (2 * rng.nextDouble() - 1) * (i < RELATIONS ? .1 : .025);
    }
    int relation(int p, int i, int j) {
        int a = squares[p][i], b = squares[p][j], dx = (b & 7) - (a & 7), dy = (b >>> 3) - (a >>> 3);
        return RELATIONS + ((codes[p][i] * 12 + codes[p][j]) * 225 + (dy + 7) * 15 + dx + 7) * WIDTH;
    }
    public double predict(long[] board) {
        int stm = Board.player((int)board[4]); double residual = 0;
        for (int p = 0; p < 2; p++) {
            count = 0;
            for (long occupied = board[0] | board[1] | board[2]; occupied != 0; occupied &= occupied - 1) {
                int sq = Long.numberOfTrailingZeros(occupied); squares[p][count] = sq ^ (p * 56);
                codes[p][count++] = RelationalCandidate.channel(Board.getSquare(board[0], board[1], board[2], board[3], sq) ^ (p << 3));
            }
            double norm = 1.0 / Math.max(1, count - 1), pool = 1.0 / Math.sqrt(Math.max(1, count));
            double value = 0;
            for (int i = 0; i < count; i++) {
                int row = (codes[p][i] * 64 + squares[p][i]) * WIDTH, offset = i * WIDTH;
                System.arraycopy(weights, row, local[p], offset, WIDTH);
                for (int j = 0; j < count; j++) if (i != j) {
                    int relation = relation(p, i, j);
                    for (int c = 0; c < WIDTH; c++) local[p][offset + c] += norm * weights[relation + c];
                }
                for (int c = 0; c < WIDTH; c++) { local[p][offset + c] = Math.tanh(local[p][offset + c]); value += pool * local[p][offset + c] * weights[HEAD + c]; }
            }
            residual += (p == stm ? .5 : -.5) * value;
        }
        double value = RelationalCandidate.material(board) + residual;
        if (!Double.isFinite(value)) throw new ArithmeticException("Nonfinite R2 inference"); return value;
    }
    void add(int index, double value) {
        if (stamps[index] != serial) { stamps[index] = serial; gradient[index] = 0; touched[size++] = index; } gradient[index] += value;
    }
    void backward(long[] board, double derivative) {
        if (++serial == 0) { Arrays.fill(stamps, 0); serial = 1; } size = 0;
        int stm = Board.player((int)board[4]); double norm = 1.0 / Math.max(1, count - 1), pool = 1.0 / Math.sqrt(Math.max(1, count));
        double[] nodeGradient = new double[WIDTH];
        for (int p = 0; p < 2; p++) {
            double scale = derivative * (p == stm ? .5 : -.5) * pool;
            for (int i = 0; i < count; i++) {
                int row = (codes[p][i] * 64 + squares[p][i]) * WIDTH;
                for (int c = 0; c < WIDTH; c++) {
                    double h = local[p][i * WIDTH + c]; add(HEAD + c, scale * h);
                    nodeGradient[c] = scale * weights[HEAD + c] * (1 - h * h); add(row + c, nodeGradient[c]);
                }
                for (int j = 0; j < count; j++) if (i != j) {
                    int relation = relation(p, i, j); for (int c = 0; c < WIDTH; c++) add(relation + c, norm * nodeGradient[c]);
                }
            }
        }
    }
    public void train(BrnResearchData.Example e, double rate, boolean outcomeLoss) {
        double value = predict(e.board()), error = value - e.cp()/100.0, derivative = Math.max(-2, Math.min(2, error));
        if (outcomeLoss) { double[] o = RelationalCandidate.outcome(value, e.sfMaterial()); derivative = .05*derivative + (o[0]-e.outcome())*o[1]; }
        backward(e.board(), derivative); updates++;
        double c1 = 1-Math.pow(.9,updates), c2 = 1-Math.pow(.999,updates);
        for (int k=0;k<size;k++) { int i=touched[k]; double g=gradient[i]; first[i]=.9*first[i]+.1*g; second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8); if(!Double.isFinite(weights[i])) throw new ArithmeticException("Nonfinite R2 update"); }
    }
    public void write(OutputStream stream) throws IOException {
        var out=new DataOutputStream(stream); out.writeLong(0x5336523252455331L); out.writeLong(updates);
        for(var block:new double[][]{weights,first,second})for(double x:block)out.writeDouble(x);out.flush();
    }
    public static MessageRelationCandidate read(InputStream stream)throws IOException{
        var in=new DataInputStream(stream);if(in.readLong()!=0x5336523252455331L)throw new IOException("Wrong R2 format");
        var model=new MessageRelationCandidate(0);model.updates=in.readLong();if(model.updates<0)throw new IOException("Invalid step");
        for(var block:new double[][]{model.weights,model.first,model.second})for(int i=0;i<block.length;i++){
            block[i]=in.readDouble();if(!Double.isFinite(block[i])||block==model.second&&block[i]<0)throw new IOException("Invalid state");}
        if(in.read()!=-1)throw new IOException("Trailing bytes");return model;
    }
}
