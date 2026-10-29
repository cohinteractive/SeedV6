package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;

/** Learned signed key/query relations with nonlinear per-piece recurrent composition. */
public final class LowRankRelationNetwork {
    static final int WIDTH=8,RANK=4,E=769,Q=E+768*WIDTH,V=Q+768*WIDTH,
        KEY=V+768*WIDTH,QUERY=KEY+768*RANK,M=QUERY+768*RANK,PARAMETERS=M+WIDTH*WIDTH;
    final boolean compose;final double rate;
    final double[] weights=new double[PARAMETERS],first=new double[PARAMETERS],second=new double[PARAMETERS],gradient=new double[PARAMETERS];
    final int[] stamps=new int[PARAMETERS],touched=new int[PARAMETERS];int serial,count;long step;
    public LowRankRelationNetwork(boolean compose,double rate,long seed) {
        if(!Double.isFinite(rate)||rate<=0)throw new IllegalArgumentException("Rate");
        this.compose=compose;this.rate=rate;var random=new Random(seed);
        for(int i=E;i<V;i++)weights[i]=.1*random.nextGaussian();
        for(int i=KEY;i<M;i++)weights[i]=.1*random.nextGaussian();
    }
    static final class Features {
        final int[] nodes=new int[32];int size;double material,norm;
        void copyFrom(Features f){size=f.size;material=f.material;norm=f.norm;System.arraycopy(f.nodes,0,nodes,0,size);}
        Features frozen(){var f=new Features();f.copyFrom(this);return f;}
    }
    public static final class Workspace {
        final Features features=new Features();
        final double[] input=new double[256],a=new double[256],h=new double[256],message=new double[256],b=new double[256],
            dh=new double[256],db=new double[256],dm=new double[256],da=new double[256],dx=new double[256];
        final double[] firstPool=new double[RANK*WIDTH],secondPool=new double[RANK*WIDTH],poolGradient=new double[RANK*WIDTH];
        Features encode(long[] board) {
            var f=features;f.size=0;f.material=BrnPair2Features.material(board)/4;int stm=Board.player((int)board[4]);
            for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1) {
                int square=Long.numberOfTrailingZeros(bits),piece=Board.getSquare(board[0],board[1],board[2],board[3],square);
                if(f.size==32)throw new IllegalArgumentException("Too many pieces");
                f.nodes[f.size++]=((piece&7)-1+6*((piece>>>3)^stm))*64+(square^(stm*56));
            }
            f.norm=f.size>1?1/Math.sqrt(RANK*(f.size-1)):0;return f;
        }
    }
    static double activate(double x){return x/(1+Math.abs(x));}
    static double derivative(double x){double d=1+Math.abs(x);return 1/(d*d);}
    double unary(Features f){double v=f.material+weights[768];for(int i=0;i<f.size;i++)v+=weights[f.nodes[i]];return v;}
    void aggregate(Features f,double[] content,double[] pool,double[] output) {
        Arrays.fill(pool,0);
        for(int j=0;j<f.size;j++)for(int r=0;r<RANK;r++) {
            double key=weights[KEY+f.nodes[j]*RANK+r];
            for(int d=0;d<WIDTH;d++)pool[r*WIDTH+d]+=key*content[j*WIDTH+d];
        }
        for(int i=0;i<f.size;i++) {
            double self=0;for(int r=0;r<RANK;r++)self+=weights[QUERY+f.nodes[i]*RANK+r]*weights[KEY+f.nodes[i]*RANK+r];
            for(int d=0;d<WIDTH;d++) {
                double value=0;for(int r=0;r<RANK;r++)value+=weights[QUERY+f.nodes[i]*RANK+r]*pool[r*WIDTH+d];
                output[i*WIDTH+d]=f.norm*(value-self*content[i*WIDTH+d]);
            }
        }
    }
    double logit(Features f,Workspace w) {
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++)w.input[i*WIDTH+d]=weights[E+f.nodes[i]*WIDTH+d];
        aggregate(f,w.input,w.firstPool,w.a);double value=unary(f);
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
            int k=i*WIDTH+d;w.h[k]=activate(w.a[k]);value+=weights[Q+f.nodes[i]*WIDTH+d]*w.h[k]/WIDTH;
        }
        if(compose) {
            for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
                double m=w.h[i*WIDTH+d];for(int e=0;e<WIDTH;e++)m+=weights[M+d*WIDTH+e]*w.h[i*WIDTH+e]/Math.sqrt(WIDTH);
                w.message[i*WIDTH+d]=m;
            }
            aggregate(f,w.message,w.secondPool,w.b);
            for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++)value+=weights[V+f.nodes[i]*WIDTH+d]*activate(w.b[i*WIDTH+d])/WIDTH;
        }
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite relational score");return value;
    }
    public double scoreCp(long[] board,Workspace w){return 400*logit(w.encode(board),w);}
    void add(int i,double value){if(stamps[i]!=serial){stamps[i]=serial;touched[count++]=i;gradient[i]=0;}gradient[i]+=value;}
    /** Reverse the factorized relation, including the excluded self edge, in O(n*r*d). */
    void backAggregate(Features f,double[] content,double[] pool,double[] delta,double[] contentDelta,double[] poolDelta) {
        Arrays.fill(poolDelta,0);
        for(int i=0;i<f.size;i++)for(int r=0;r<RANK;r++) {
            double query=weights[QUERY+f.nodes[i]*RANK+r];
            for(int d=0;d<WIDTH;d++)poolDelta[r*WIDTH+d]+=f.norm*delta[i*WIDTH+d]*query;
        }
        for(int i=0;i<f.size;i++) {
            double self=0;for(int r=0;r<RANK;r++)self+=weights[QUERY+f.nodes[i]*RANK+r]*weights[KEY+f.nodes[i]*RANK+r];
            for(int d=0;d<WIDTH;d++) {
                double value=0;for(int r=0;r<RANK;r++)value+=weights[KEY+f.nodes[i]*RANK+r]*poolDelta[r*WIDTH+d];
                contentDelta[i*WIDTH+d]=value-f.norm*delta[i*WIDTH+d]*self;
            }
            for(int r=0;r<RANK;r++) {
                int key=KEY+f.nodes[i]*RANK+r,query=QUERY+f.nodes[i]*RANK+r;double dk=0,dq=0;
                for(int d=0;d<WIDTH;d++) {
                    double t=f.norm*delta[i*WIDTH+d],x=content[i*WIDTH+d];
                    dk+=x*(poolDelta[r*WIDTH+d]-t*weights[query]);
                    dq+=t*(pool[r*WIDTH+d]-weights[key]*x);
                }
                add(key,dk);add(query,dq);
            }
        }
    }
    double gradients(Features[] batch,double[] targets) {
        if(batch.length==0||batch.length!=targets.length)throw new IllegalArgumentException("Batch");
        if(++serial==0){Arrays.fill(stamps,0);serial=1;}count=0;double loss=0;var w=new Workspace();
        for(int row=0;row<batch.length;row++) {
            if(!Double.isFinite(targets[row])||targets[row]<0||targets[row]>1)throw new IllegalArgumentException("Target");
            var f=batch[row];double p=PairObjectiveDiagnostic.sigmoid(logit(f,w)),error=p-targets[row];loss+=error*error;
            double g=2*error*p*(1-p)/batch.length;add(768,g);for(int i=0;i<f.size;i++)add(f.nodes[i],g);
            for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
                int k=i*WIDTH+d,q=Q+f.nodes[i]*WIDTH+d;add(q,g*w.h[k]/WIDTH);w.dh[k]=g*weights[q]/WIDTH;
                if(compose){int v=V+f.nodes[i]*WIDTH+d;add(v,g*activate(w.b[k])/WIDTH);w.db[k]=g*weights[v]*derivative(w.b[k])/WIDTH;}
            }
            if(compose) {
                backAggregate(f,w.message,w.secondPool,w.db,w.dm,w.poolGradient);
                for(int i=0;i<f.size;i++) {
                    for(int e=0;e<WIDTH;e++)w.dh[i*WIDTH+e]+=w.dm[i*WIDTH+e];
                    for(int d=0;d<WIDTH;d++)for(int e=0;e<WIDTH;e++) {
                        add(M+d*WIDTH+e,w.dm[i*WIDTH+d]*w.h[i*WIDTH+e]/Math.sqrt(WIDTH));
                        w.dh[i*WIDTH+e]+=w.dm[i*WIDTH+d]*weights[M+d*WIDTH+e]/Math.sqrt(WIDTH);
                    }
                }
            }
            for(int k=0;k<f.size*WIDTH;k++)w.da[k]=w.dh[k]*derivative(w.a[k]);
            backAggregate(f,w.input,w.firstPool,w.da,w.dx,w.poolGradient);
            for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++)add(E+f.nodes[i]*WIDTH+d,w.dx[i*WIDTH+d]);
        }
        return loss/batch.length;
    }
    double train(Features[] batch,double[] targets){double loss=gradients(batch,targets);updateFromGradients();return loss;}
    void updateFromGradients() {
        if(step==Long.MAX_VALUE)throw new ArithmeticException("Step");step++;
        double c1=1-Math.pow(.9,step),c2=1-Math.pow(.999,step);
        for(int i=0;i<weights.length;i++)weights[i]*=1-rate*.01;
        for(int n=0;n<count;n++) {
            int i=touched[n];double g=gradient[i];first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite relational parameter");
        }
        for(int i=0;i<weights.length;i++)weights[i]=Math.max(-.99,Math.min(.99,weights[i]));
    }
    void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(0x42524e4c52523031L);out.writeInt(compose?1:0);out.writeInt(PARAMETERS);out.writeDouble(rate);out.writeLong(step);
        for(var array:new double[][]{weights,first,second})for(double value:array)out.writeDouble(value);
        out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static LowRankRelationNetwork read(InputStream stream)throws IOException {
        byte[] bytes=stream.readNBytes(41+24*PARAMETERS);if(bytes.length!=40+24*PARAMETERS)throw new IOException("Low-rank state length");
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readLong()!=0x42524e4c52523031L)throw new IOException("Low-rank magic");int mode=in.readInt();if(mode<0||mode>1||in.readInt()!=PARAMETERS)throw new IOException("Low-rank geometry");
        double rate=in.readDouble();long step=in.readLong();if(!Double.isFinite(rate)||rate<=0||step<0)throw new IOException("Low-rank optimizer");
        var model=new LowRankRelationNetwork(mode==1,rate,0);model.step=step;
        for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++){array[i]=in.readDouble();if(!Double.isFinite(array[i])||(array==model.second&&array[i]<0))throw new IOException("Low-rank nonfinite/moment");}
        if(in.readLong()!=crc.getValue())throw new IOException("Low-rank CRC");return model;
    }
}
