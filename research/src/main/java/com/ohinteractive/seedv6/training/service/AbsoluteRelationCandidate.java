package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;

/** R4: explicit absolute entity-pair embeddings, nonlinear local composition,
 * invariant node pooling, us/them dense readout and an anchored pawn residual.
 * All piece pairs participate; no chess attack or tactical rules select edges. */
public final class AbsoluteRelationCandidate {
    static final int HIDDEN_WIDTH = 32, EDGE_ROWS = 768 * 767 / 2;
    final int WIDTH, POOL_WIDTH, NODES, DENSE, BIAS, HEAD, OUTPUT_BIAS, PARAMETERS, RELATIVE_PARAMETERS;
    final boolean typedPooling;
    static final int[] RELATIVE_ROW = relativeRows();
    final double[] weights, first, second, gradient;
    final int[] touched, stamps;
    final int[][] entities = new int[2][64];
    final int[][][] edges = new int[2][64][64];
    final double[][][] local, localGradient;
    final double[][] pooled, poolGradient;
    final double[] hidden = new double[HIDDEN_WIDTH];
    int count, serial, size; long updates; double relationNorm, poolNorm;
    final boolean singlePerspective;
    final boolean relu;
    final boolean relative;
    final double relativeScale;
    final boolean denseAdam;
    final int[] activeIndices;final boolean[] active;int activeCount;

    public AbsoluteRelationCandidate(long seed) {
        this(seed,false);
    }
    public AbsoluteRelationCandidate(long seed, boolean singlePerspective) {
        this(seed,singlePerspective,false);
    }
    public AbsoluteRelationCandidate(long seed, boolean singlePerspective, boolean relu) {
        this(seed,singlePerspective,relu,false);
    }
    public AbsoluteRelationCandidate(long seed, boolean singlePerspective, boolean relu, boolean relative) {
        this(seed,singlePerspective,relu,relative,.25);
    }
    public AbsoluteRelationCandidate(long seed, boolean singlePerspective, boolean relu, boolean relative, double relativeScale) {
        this(seed,singlePerspective,relu,relative,relativeScale,8);
    }
    public AbsoluteRelationCandidate(long seed, boolean singlePerspective, boolean relu, boolean relative, double relativeScale, int width) {
        this(seed,singlePerspective,relu,relative,relativeScale,width,false);
    }
    public AbsoluteRelationCandidate(long seed, boolean singlePerspective, boolean relu, boolean relative, double relativeScale, int width, boolean denseAdam) {
        this(seed,singlePerspective,relu,relative,relativeScale,width,denseAdam,false);
    }
    public AbsoluteRelationCandidate(long seed, boolean singlePerspective, boolean relu, boolean relative, double relativeScale, int width, boolean denseAdam, boolean typedPooling) {
        if(width!=8 && width!=16 && width!=32)throw new IllegalArgumentException("Research relation width");
        WIDTH=width;this.typedPooling=typedPooling;POOL_WIDTH=WIDTH*(typedPooling?12:1);
        NODES=EDGE_ROWS*WIDTH;DENSE=NODES+768*WIDTH;BIAS=DENSE+2*POOL_WIDTH*HIDDEN_WIDTH;
        HEAD=BIAS+HIDDEN_WIDTH;OUTPUT_BIAS=HEAD+HIDDEN_WIDTH;PARAMETERS=OUTPUT_BIAS+1;RELATIVE_PARAMETERS=12*12*225*WIDTH;
        local=new double[2][64][WIDTH];localGradient=new double[2][64][WIDTH];pooled=new double[2][POOL_WIDTH];poolGradient=new double[2][POOL_WIDTH];
        this.singlePerspective=singlePerspective;
        this.relu=relu;
        this.relative=relative;
        if(!Double.isFinite(relativeScale)||relativeScale<0)throw new IllegalArgumentException("Relative scale");this.relativeScale=relativeScale;
        int parameters=PARAMETERS+(relative?RELATIVE_PARAMETERS:0);
        weights=new double[parameters];first=new double[parameters];second=new double[parameters];gradient=new double[parameters];
        touched=new int[parameters];stamps=new int[parameters];
        this.denseAdam=denseAdam;activeIndices=denseAdam?new int[parameters]:null;active=denseAdam?new boolean[parameters]:null;
        var rng = new Random(seed);
        for (int i = 0; i < DENSE; i++) weights[i] = (2 * rng.nextDouble() - 1) * .1;
        for (int i = DENSE; i < BIAS; i++) weights[i] = (2 * rng.nextDouble() - 1) * Math.sqrt(6.0 / (2 * POOL_WIDTH + HIDDEN_WIDTH));
    }
    int edge(int a, int b) {
        int large = Math.max(a,b), small = Math.min(a,b);
        if (large == small) throw new IllegalArgumentException("Self edge");
        return (large * (large - 1) / 2 + small) * WIDTH;
    }
    static int[] relativeRows() {
        var rows=new int[EDGE_ROWS];
        for(int large=1;large<768;large++) for(int small=0;small<large;small++) {
            int a=small%64,b=large%64,dx=(b&7)-(a&7),dy=(b>>>3)-(a>>>3);
            rows[large*(large-1)/2+small]=(small/64*12+large/64)*225+(dy+7)*15+dx+7;
        }
        return rows;
    }
    public double predict(long[] board) {
        int stm = Board.player((int) board[4]);
        for (int role = 0; role < (singlePerspective ? 1 : 2); role++) {
            int perspective = stm ^ role; count = 0; Arrays.fill(pooled[role], 0);
            for (long occupied = board[0] | board[1] | board[2]; occupied != 0; occupied &= occupied - 1) {
                int sq = Long.numberOfTrailingZeros(occupied), code = RelationalCandidate.channel(Board.getSquare(board[0], board[1], board[2], board[3], sq) ^ (perspective << 3));
                int entity = code * 64 + (sq ^ (perspective * 56)); entities[role][count] = entity;
                System.arraycopy(weights, NODES + entity * WIDTH, local[role][count++], 0, WIDTH);
            }
            relationNorm = 1.0 / Math.sqrt(Math.max(1, count - 1)); poolNorm = 1.0 / Math.sqrt(Math.max(1, count));
            for (int i = 0; i < count; i++) for (int j = i + 1; j < count; j++) {
                int row = edge(entities[role][i], entities[role][j]); edges[role][i][j] = row;
                int sharedRow=relative?PARAMETERS+RELATIVE_ROW[row/WIDTH]*WIDTH:0;
                for (int c = 0; c < WIDTH; c++) {
                    double message = relationNorm * (weights[row + c] + (relative ? relativeScale * weights[sharedRow+c] : 0)); local[role][i][c] += message; local[role][j][c] += message;
                }
            }
            for (int i = 0; i < count; i++) for (int c = 0; c < WIDTH; c++) {
                local[role][i][c] = activate(local[role][i][c]); pooled[role][poolIndex(entities[role][i],c)] += poolNorm * local[role][i][c];
            }
        }
        double residual = weights[OUTPUT_BIAS];
        for (int h = 0; h < HIDDEN_WIDTH; h++) {
            double z = weights[BIAS + h];
            for (int p = 0; p < 2; p++) for (int c = 0; c < POOL_WIDTH; c++) z += weights[DENSE + h * 2 * POOL_WIDTH + p * POOL_WIDTH + c] * pooled[p][c];
            hidden[h] = activate(z); residual += weights[HEAD + h] * hidden[h];
        }
        double value = RelationalCandidate.material(board) + residual;
        if (!Double.isFinite(value)) throw new ArithmeticException("Nonfinite R4 prediction"); return value;
    }
    double activate(double value) { return relu ? Math.max(0,value) : Math.tanh(value); }
    int poolIndex(int entity,int channel){return (typedPooling?entity/64*WIDTH:0)+channel;}
    double activationDerivative(double activated) { return relu ? activated > 0 ? 1 : 0 : 1-activated*activated; }
    void resetGradient() { if (++serial == 0) { Arrays.fill(stamps, 0); serial = 1; } size = 0; }
    void add(int index, double value) {
        if (stamps[index] != serial) { stamps[index] = serial; gradient[index] = 0; touched[size++] = index; } gradient[index] += value;
    }
    void backward(double derivative) {
        for (var g : poolGradient) Arrays.fill(g, 0); add(OUTPUT_BIAS, derivative);
        for (int h = 0; h < HIDDEN_WIDTH; h++) {
            add(HEAD + h, derivative * hidden[h]); double dz = derivative * weights[HEAD + h] * activationDerivative(hidden[h]); add(BIAS+h,dz);
            for (int p = 0; p < 2; p++) for (int c = 0; c < POOL_WIDTH; c++) {
                int index = DENSE + h * 2 * POOL_WIDTH + p * POOL_WIDTH + c; add(index,dz*pooled[p][c]); poolGradient[p][c] += dz*weights[index];
            }
        }
        for (int p = 0; p < (singlePerspective ? 1 : 2); p++) {
            for (int i = 0; i < count; i++) for (int c = 0; c < WIDTH; c++) {
                double h = local[p][i][c]; double dz = poolGradient[p][poolIndex(entities[p][i],c)] * poolNorm * activationDerivative(h);
                localGradient[p][i][c] = dz; add(NODES + entities[p][i] * WIDTH + c, dz);
            }
            for (int i = 0; i < count; i++) for (int j = i+1; j < count; j++) {
                int sharedRow=relative?PARAMETERS+RELATIVE_ROW[edges[p][i][j]/WIDTH]*WIDTH:0;
                for(int c=0;c<WIDTH;c++){
                    double gradient=relationNorm*(localGradient[p][i][c]+localGradient[p][j][c]);
                    add(edges[p][i][j]+c,gradient);if(relative)add(sharedRow+c,relativeScale*gradient);
                }
            }
        }
    }
    public void trainBatch(List<BrnResearchData.Example> examples, int[] order, int offset, int batch, double rate, boolean crossEntropy) {
        trainBatch(examples,order,offset,batch,rate,crossEntropy,.05);
    }
    public void trainBatch(List<BrnResearchData.Example> examples, int[] order, int offset, int batch, double rate, boolean crossEntropy, double auxiliary) {
        resetGradient();
        for (int n = 0; n < batch; n++) {
            var e = examples.get(order[offset+n]); double value = predict(e.board()); double[] outcome = RelationalCandidate.outcome(value,e.sfMaterial());
            double derivative = crossEntropy ? RelationalCandidate.crossEntropy(value,e.sfMaterial(),e.outcome())[1] : (outcome[0]-e.outcome())*outcome[1];
            backward(derivative + auxiliary*Math.max(-2,Math.min(2,value-e.cp()/100.0)));
        }
        update(batch,rate);
    }
    void update(int batch,double rate) {
        if(updates==Long.MAX_VALUE)throw new ArithmeticException("Adam step exhausted");
        updates++; double c1 = 1-Math.pow(.9,updates), c2 = 1-Math.pow(.999,updates);
        if(denseAdam)for(int n=0;n<size;n++){int i=touched[n];if(!active[i]&&gradient[i]!=0){active[i]=true;activeIndices[activeCount++]=i;}}
        int[] indices=denseAdam?activeIndices:touched;int limit=denseAdam?activeCount:size;
        for (int n = 0; n < limit; n++) {
            int i=indices[n]; double g=stamps[i]==serial?gradient[i]/batch:0; first[i]=.9*first[i]+.1*g; second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if (!Double.isFinite(weights[i])) throw new ArithmeticException("Nonfinite R4 update");
        }
    }
    public void write(OutputStream stream) throws IOException {
        var out = new DataOutputStream(stream); out.writeLong(0x5336523452455338L); out.writeInt(WIDTH); out.writeBoolean(typedPooling); out.writeBoolean(singlePerspective); out.writeBoolean(relu); out.writeBoolean(relative); out.writeDouble(relativeScale); out.writeBoolean(denseAdam); out.writeLong(updates);
        for (var block : new double[][]{weights,first,second}) for (double value : block) out.writeDouble(value); out.flush();
    }
    public static AbsoluteRelationCandidate read(InputStream stream) throws IOException {
        var in=new DataInputStream(stream); long magic=in.readLong(); if(magic<0x5336523452455331L || magic>0x5336523452455338L) throw new IOException("Wrong R4 format");
        int width=magic>=0x5336523452455336L?in.readInt():8;
        boolean typed=magic>=0x5336523452455338L && in.readBoolean();
        boolean single=magic>=0x5336523452455332L && in.readBoolean(),relu=magic>=0x5336523452455333L && in.readBoolean(),relative=magic>=0x5336523452455334L && in.readBoolean();
        double scale=magic>=0x5336523452455335L?in.readDouble():.25;
        boolean dense=magic>=0x5336523452455337L && in.readBoolean();
        if((width!=8 && width!=16 && width!=32)||!Double.isFinite(scale)||scale<0)throw new IOException("Invalid R4 configuration");
        var model=new AbsoluteRelationCandidate(0,single,relu,relative,scale,width,dense,typed); model.updates=in.readLong(); if(model.updates<0) throw new IOException("Invalid step");
        for(var block:new double[][]{model.weights,model.first,model.second}) for(int i=0;i<block.length;i++) {
            block[i]=in.readDouble(); if(!Double.isFinite(block[i]) || block==model.second && block[i]<0) throw new IOException("Invalid state");
        }
        if(dense)for(int i=0;i<model.weights.length;i++)if(model.first[i]!=0 || model.second[i]!=0){model.active[i]=true;model.activeIndices[model.activeCount++]=i;}
        if(in.read()!=-1) throw new IOException("Trailing bytes"); return model;
    }
    /** Inference-only algebraic folding. Optimizer state is intentionally not copied. */
    AbsoluteRelationCandidate foldedInference() {
        var copy=new AbsoluteRelationCandidate(0,singlePerspective,relu,false,.25,WIDTH,false,typedPooling);
        System.arraycopy(weights,0,copy.weights,0,PARAMETERS);
        if(relative)for(int row=0;row<EDGE_ROWS;row++)for(int c=0;c<WIDTH;c++)copy.weights[row*WIDTH+c]+=relativeScale*weights[PARAMETERS+RELATIVE_ROW[row]*WIDTH+c];
        return copy;
    }
}
