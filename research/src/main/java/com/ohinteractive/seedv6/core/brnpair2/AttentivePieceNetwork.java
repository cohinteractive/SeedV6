package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;

/** Research-only receiver-dependent routing between contextual neural piece states. */
public final class AttentivePieceNetwork {
    static final int WIDTH=8,E=769,R=E+768*WIDTH,Q=R+2700*WIDTH,S=Q+768*WIDTH,V=S+2700*WIDTH,B=V+768*WIDTH,K=B+2700,PARAMETERS=K+WIDTH;
    final boolean compose=true;
    final int attention; // 0 uniform,1 geometry,2 contextual
    final double rate;
    final double[] weights=new double[PARAMETERS],first=new double[PARAMETERS],second=new double[PARAMETERS],gradient=new double[PARAMETERS];
    final int[] stamps=new int[PARAMETERS],touched=new int[PARAMETERS];
    int serial,count;
    long step;
    public AttentivePieceNetwork(int attention,double rate,long seed) {
        if(!Double.isFinite(rate)||rate<=0)throw new IllegalArgumentException("Rate");
        if(attention<0||attention>2)throw new IllegalArgumentException("Attention mode");
        this.attention=attention;this.rate=rate;var rng=new Random(seed);
        for(int i=E;i<V;i++)weights[i]=.1*rng.nextGaussian();
    }
    static final class Features {
        final int[] nodes=new int[32],relations=new int[1024];
        int size;double norm,material;
        void copyFrom(Features f){size=f.size;norm=f.norm;material=f.material;System.arraycopy(f.nodes,0,nodes,0,size);System.arraycopy(f.relations,0,relations,0,size*size);}
        Features frozen(){var result=new Features();result.copyFrom(this);return result;}
    }
    public static final class Workspace {
        final Features features=new Features();
        final double[] a=new double[256],h=new double[256],b=new double[256],dh=new double[256],db=new double[256];
        double baseLogit;
        final double[] content=new double[256],alpha=new double[1024],routeGradient=new double[1024];
        Features encode(long[] board) {
            var f=features;f.size=0;f.material=BrnPair2Features.material(board)/4;
            int stm=Board.player((int)board[4]);
            for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1) {
                int sq=Long.numberOfTrailingZeros(bits),p=Board.getSquare(board[0],board[1],board[2],board[3],sq);
                if(f.size==32)throw new IllegalArgumentException("Too many pieces");
                f.nodes[f.size++]=((p&7)-1+6*((p>>>3)^stm))*64+(sq^(stm*56));
            }
            f.norm=f.size>1?1/Math.sqrt(f.size-1):0;
            for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++) {
                int from=f.nodes[j]%64,to=f.nodes[i]%64;
                f.relations[j*f.size+i]=f.nodes[i]/64*225+(to/8-from/8+7)*15+to%8-from%8+7;
            }
            return f;
        }
    }
    static double activate(double x){return x/(1+Math.abs(x));}
    static double derivative(double x){double d=1+Math.abs(x);return 1/(d*d);}
    double unary(Features f){double value=f.material+weights[768];for(int i=0;i<f.size;i++)value+=weights[f.nodes[i]];return value;}
    double logit(Features f,Workspace w) {return logit(f,w,attention);}
    double logit(Features f,Workspace w,int routeMode) {
        Arrays.fill(w.a,0);Arrays.fill(w.b,0);
        for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i) {
            int source=E+f.nodes[j]*WIDTH,rel=R+f.relations[j*f.size+i]*WIDTH,target=i*WIDTH;
            for(int d=0;d<WIDTH;d++)w.a[target+d]+=f.norm*weights[source+d]*weights[rel+d];
        }
        double value=unary(f);
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
            int k=i*WIDTH+d;w.h[k]=activate(w.a[k]);value+=weights[Q+f.nodes[i]*WIDTH+d]*w.h[k]/WIDTH;
        }
        w.baseLogit=value;
        if(routeMode>0) {
            for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++)w.content[i*WIDTH+d]=weights[E+f.nodes[i]*WIDTH+d]+w.h[i*WIDTH+d];
            for(int i=0;i<f.size;i++) {
                double maximum=Double.NEGATIVE_INFINITY,total=0;
                for(int j=0;j<f.size;j++)if(j!=i) {
                    int edge=j*f.size+i;double score=weights[B+f.relations[edge]];
                    if(routeMode==2)for(int d=0;d<WIDTH;d++)score+=weights[K+d]*w.content[i*WIDTH+d]*w.content[j*WIDTH+d]/Math.sqrt(WIDTH);
                    w.alpha[edge]=score;maximum=Math.max(maximum,score);
                }
                for(int j=0;j<f.size;j++)if(j!=i){int edge=j*f.size+i;w.alpha[edge]=Math.exp(w.alpha[edge]-maximum);total+=w.alpha[edge];}
                for(int j=0;j<f.size;j++)if(j!=i)w.alpha[j*f.size+i]/=total;
            }
        }
        for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i) {
            int edge=j*f.size+i,rel=S+f.relations[edge]*WIDTH;
            double scale=routeMode==0?f.norm:(f.size-1)*f.norm*w.alpha[edge];
            for(int d=0;d<WIDTH;d++)w.b[i*WIDTH+d]+=scale*w.h[j*WIDTH+d]*weights[rel+d];
        }
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++)value+=weights[V+f.nodes[i]*WIDTH+d]*activate(w.b[i*WIDTH+d])/WIDTH;
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite contextual piece score");return value;
    }
    public double scoreCp(long[] board,Workspace w){return 400*logit(w.encode(board),w);}
    void add(int i,double g){if(stamps[i]!=serial){stamps[i]=serial;touched[count++]=i;gradient[i]=0;}gradient[i]+=g;}
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
            for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i) {
                int edge=j*f.size+i,rel=S+f.relations[edge]*WIDTH;double scale=attention==0?f.norm:(f.size-1)*f.norm*w.alpha[edge],route=0;
                for(int d=0;d<WIDTH;d++) {
                    double db=w.db[i*WIDTH+d]*scale;add(rel+d,db*w.h[j*WIDTH+d]);w.dh[j*WIDTH+d]+=db*weights[rel+d];
                    if(attention>0)route+=w.db[i*WIDTH+d]*(f.size-1)*f.norm*w.h[j*WIDTH+d]*weights[rel+d];
                }
                w.routeGradient[edge]=route;
            }
            if(attention>0)for(int i=0;i<f.size;i++) {
                double average=0;for(int j=0;j<f.size;j++)if(j!=i)average+=w.alpha[j*f.size+i]*w.routeGradient[j*f.size+i];
                for(int j=0;j<f.size;j++)if(j!=i) {
                    int edge=j*f.size+i;double ds=w.alpha[edge]*(w.routeGradient[edge]-average);add(B+f.relations[edge],ds);
                    if(attention==2)for(int d=0;d<WIDTH;d++) {
                        int target=i*WIDTH+d,source=j*WIDTH+d;double scale=ds/Math.sqrt(WIDTH),di=scale*weights[K+d]*w.content[source],dj=scale*weights[K+d]*w.content[target];
                        add(K+d,scale*w.content[target]*w.content[source]);add(E+f.nodes[i]*WIDTH+d,di);add(E+f.nodes[j]*WIDTH+d,dj);w.dh[target]+=di;w.dh[source]+=dj;
                    }
                }
            }
            for(int k=0;k<f.size*WIDTH;k++)w.dh[k]*=derivative(w.a[k]);
            for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i) {
                int source=E+f.nodes[j]*WIDTH,rel=R+f.relations[j*f.size+i]*WIDTH;
                for(int d=0;d<WIDTH;d++){double da=w.dh[i*WIDTH+d]*f.norm;add(source+d,da*weights[rel+d]);add(rel+d,da*weights[source+d]);}
            }
        }
        return loss/batch.length;
    }
    double train(Features[] batch,double[] targets) {
        double loss=gradients(batch,targets);if(step==Long.MAX_VALUE)throw new ArithmeticException("Step");step++;
        double c1=1-Math.pow(.9,step),c2=1-Math.pow(.999,step);
        for(int i=0;i<weights.length;i++)weights[i]*=1-rate*.01;
        for(int n=0;n<count;n++) {
            int i=touched[n];double g=gradient[i];first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite contextual parameter");
        }
        for(int i=0;i<weights.length;i++)weights[i]=Math.max(-.99,Math.min(.99,weights[i]));return loss;
    }
    void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(0x42524e41504e3031L);out.writeInt(attention);out.writeInt(PARAMETERS);out.writeDouble(rate);out.writeLong(step);
        for(var array:new double[][]{weights,first,second})for(double value:array)out.writeDouble(value);
        out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static AttentivePieceNetwork read(InputStream stream)throws IOException {
        byte[] bytes=stream.readNBytes(41+24*PARAMETERS);if(bytes.length!=40+24*PARAMETERS)throw new IOException("Contextual piece state length");
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readLong()!=0x42524e41504e3031L)throw new IOException("Contextual piece magic");int mode=in.readInt();if(mode<0||mode>2||in.readInt()!=PARAMETERS)throw new IOException("Contextual piece geometry");
        double rate=in.readDouble();long step=in.readLong();if(!Double.isFinite(rate)||rate<=0||step<0)throw new IOException("Contextual piece optimizer");
        var model=new AttentivePieceNetwork(mode,rate,0);model.step=step;
        for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++){array[i]=in.readDouble();if(!Double.isFinite(array[i])||(array==model.second&&array[i]<0))throw new IOException("Contextual piece nonfinite/moment");}
        if(in.readLong()!=crc.getValue())throw new IOException("Contextual piece CRC");return model;
    }
}
