package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;

/** Research-only square-control messages composed back through each attacking piece. */
public final class ComposedAttackFieldNetwork {
    static final int WIDTH=8,E=769,L=E+768*WIDTH,R=L+2925*WIDTH,O=R+450*WIDTH,G=O+832*WIDTH,U=G+26*WIDTH,V=U+2700*WIDTH,PARAMETERS=V+768*WIDTH;
    final boolean compose;
    final double rate;
    final double[] weights=new double[PARAMETERS],first=new double[PARAMETERS],second=new double[PARAMETERS],gradient=new double[PARAMETERS];
    final int[] stamps=new int[PARAMETERS],touched=new int[PARAMETERS];
    int serial,count;
    long step;
    public ComposedAttackFieldNetwork(boolean compose,double rate,long seed) {
        if(!Double.isFinite(rate)||rate<=0)throw new IllegalArgumentException("Rate");this.compose=compose;this.rate=rate;
        var random=new Random(seed);for(int i=E;i<G;i++)weights[i]=.1*random.nextGaussian();
        Arrays.fill(weights,O+768*WIDTH,G,0);
        for(int i=U;i<V;i++)weights[i]=.1*random.nextGaussian(); // V starts zero: exact original initial function.
    }
    static final class Features {
        int size,edges,targetCount;
        final int[] nodes=new int[32],targets=new int[64],from=new int[1024],to=new int[1024],local=new int[1024],anchor=new int[64];
        final double[] scale=new double[64];
        double material,norm;
        void copyFrom(Features f) {
            size=f.size;edges=f.edges;targetCount=f.targetCount;material=f.material;norm=f.norm;
            System.arraycopy(f.nodes,0,nodes,0,size);System.arraycopy(f.scale,0,scale,0,targetCount);System.arraycopy(f.targets,0,targets,0,targetCount);System.arraycopy(f.anchor,0,anchor,0,2*size);
            System.arraycopy(f.from,0,from,0,edges);System.arraycopy(f.to,0,to,0,edges);System.arraycopy(f.local,0,local,0,edges);
        }
        Features frozen(){var f=new Features();f.copyFrom(this);return f;}
    }
    public static final class Workspace {
        final AnchorNeuralNetwork.Workspace anchors=new AnchorNeuralNetwork.Workspace();
        final Features features=new Features();
        final int[] index=new int[64],pieces=new int[64];
        double baseLogit;
        final double[] query=new double[256],queryGradient=new double[256],queryScale=new double[32],fieldGradient=new double[512];
        final double[] a=new double[16],h=new double[16],local=new double[512],gain=new double[512],z=new double[512],localGradient=new double[512],anchorGradient=new double[16];
        Features encode(long[] board,boolean allSquares) {
            var k=anchors.encode(board);var f=features;f.size=k.size;f.edges=f.targetCount=0;f.material=k.material;f.norm=k.norm;Arrays.fill(f.scale,0);Arrays.fill(index,-1);
            System.arraycopy(k.nodes,0,f.nodes,0,f.size);System.arraycopy(k.relations,0,f.anchor,0,2*f.size);
            int stm=Board.player((int)board[4]);long occupied=board[0]|board[1]|board[2];
            for(int s=0;s<64;s++) {
                int piece=Board.getSquare(board[0],board[1],board[2],board[3],s);pieces[s]=piece;
                if(piece==0&&!allSquares)continue;int channel=piece==0?12:(piece&7)-1+6*((piece>>>3)^stm);
                index[s]=f.targetCount;f.targets[f.targetCount++]=channel*64+(s^(stm*56));
            }
            int source=0;
            for(int s=0;s<64;s++)if(pieces[s]!=0) {
                long attacks=RelationalGraphEnergy.attacks(pieces[s],s,occupied);if(!allSquares)attacks&=occupied;
                for(long bits=attacks;bits!=0;bits&=bits-1) {
                    int t=Long.numberOfTrailingZeros(bits),sq=s^(stm*56),tq=t^(stm*56),target=index[t];
                    if(f.edges>=f.from.length||target<0)throw new IllegalStateException("Attack field geometry");
                    f.from[f.edges]=source;f.to[f.edges]=target;f.local[f.edges++]=f.targets[target]/64*225+(tq/8-sq/8+7)*15+tq%8-sq%8+7;f.scale[target]++;
                }
                source++;
            }
            for(int i=0;i<f.targetCount;i++)f.scale[i]=f.scale[i]==0?0:1/Math.sqrt(f.scale[i]);return f;
        }
    }
    static double activate(double x){return x/(1+Math.abs(x));}
    static double derivative(double x){double scale=1+Math.abs(x);return 1/(scale*scale);}
    double unary(Features f){double sum=f.material+weights[768];for(int i=0;i<f.size;i++)sum+=weights[f.nodes[i]];return sum;}
    double logit(Features f,Workspace w) {
        Arrays.fill(w.local,0);Arrays.fill(w.a,0);Arrays.fill(w.h,0);
        for(int edge=0;edge<f.edges;edge++) {
            int source=E+f.nodes[f.from[edge]]*WIDTH,rel=L+f.local[edge]*WIDTH,target=f.to[edge]*WIDTH;double norm=f.scale[f.to[edge]];
            for(int d=0;d<WIDTH;d++)w.local[target+d]+=norm*weights[source+d]*weights[rel+d];
        }
        {
            for(int i=0;i<f.size;i++)for(int k=0;k<2;k++) {
                int rel=f.anchor[2*i+k];if(rel<0)continue;
                for(int d=0;d<WIDTH;d++)w.a[k*WIDTH+d]+=f.norm*weights[E+f.nodes[i]*WIDTH+d]*weights[R+rel*WIDTH+d];
            }
            for(int i=0;i<w.a.length;i++)w.h[i]=activate(w.a[i]);
        }
        double sum=unary(f);
        for(int i=0;i<f.targetCount;i++)for(int d=0;d<WIDTH;d++) {
            int idx=i*WIDTH+d;double gain=1;
            for(int k=0;k<2;k++)gain+=weights[G+(f.targets[i]/64*2+k)*WIDTH+d]*w.h[k*WIDTH+d];
            w.gain[idx]=gain;w.z[idx]=w.local[idx]*gain;sum+=weights[O+f.targets[i]*WIDTH+d]*activate(w.z[idx])/8;
        }
        w.baseLogit=sum;
        if(compose) {
            Arrays.fill(w.query,0);Arrays.fill(w.queryScale,0);
            for(int edge=0;edge<f.edges;edge++)w.queryScale[f.from[edge]]++;
            for(int i=0;i<f.size;i++)w.queryScale[i]=w.queryScale[i]==0?0:1/Math.sqrt(w.queryScale[i]);
            for(int edge=0;edge<f.edges;edge++) {
                int q=f.from[edge],target=f.to[edge]*WIDTH,rel=U+(f.nodes[q]/64*225+f.local[edge]%225)*WIDTH;
                for(int d=0;d<WIDTH;d++)w.query[q*WIDTH+d]+=w.queryScale[q]*weights[rel+d]*activate(w.z[target+d]);
            }
            for(int q=0;q<f.size;q++)for(int d=0;d<WIDTH;d++)sum+=weights[V+f.nodes[q]*WIDTH+d]*activate(w.query[q*WIDTH+d])/8;
        }
        if(!Double.isFinite(sum))throw new ArithmeticException("Nonfinite composed field score");return sum;
    }
    public double scoreCp(long[] board,Workspace w){return 400*logit(w.encode(board,true),w);}
    void add(int i,double g){if(stamps[i]!=serial){stamps[i]=serial;touched[count++]=i;gradient[i]=0;}gradient[i]+=g;}
    double gradients(Features[] batch,double[] targets) {
        if(batch.length==0||batch.length!=targets.length)throw new IllegalArgumentException("Batch");
        if(++serial==0){Arrays.fill(stamps,0);serial=1;}count=0;double loss=0;var w=new Workspace();
        for(int n=0;n<batch.length;n++) {
            if(!Double.isFinite(targets[n])||targets[n]<0||targets[n]>1)throw new IllegalArgumentException("Target");
            var f=batch[n];double p=PairObjectiveDiagnostic.sigmoid(logit(f,w)),error=p-targets[n];loss+=error*error;
            double g=2*error*p*(1-p)/batch.length;add(768,g);for(int i=0;i<f.size;i++)add(f.nodes[i],g);
            Arrays.fill(w.anchorGradient,0);Arrays.fill(w.fieldGradient,0);
            if(compose) {
                for(int q=0;q<f.size;q++)for(int d=0;d<WIDTH;d++) {
                    int idx=q*WIDTH+d,output=V+f.nodes[q]*WIDTH+d;add(output,g*activate(w.query[idx])/8);
                    w.queryGradient[idx]=g*weights[output]*derivative(w.query[idx])/8;
                }
                for(int edge=0;edge<f.edges;edge++) {
                    int q=f.from[edge],target=f.to[edge]*WIDTH,rel=U+(f.nodes[q]/64*225+f.local[edge]%225)*WIDTH;
                    for(int d=0;d<WIDTH;d++) {
                        double db=w.queryGradient[q*WIDTH+d]*w.queryScale[q];add(rel+d,db*activate(w.z[target+d]));
                        w.fieldGradient[target+d]+=db*weights[rel+d]*derivative(w.z[target+d]);
                    }
                }
            }
            for(int i=0;i<f.targetCount;i++)for(int d=0;d<WIDTH;d++) {
                int idx=i*WIDTH+d,output=O+f.targets[i]*WIDTH+d;add(output,g*activate(w.z[idx])/8);
                double dz=g*weights[output]*derivative(w.z[idx])/8+w.fieldGradient[idx];w.localGradient[idx]=dz*w.gain[idx];
                for(int k=0;k<2;k++) {
                    int gate=G+(f.targets[i]/64*2+k)*WIDTH+d,h=k*WIDTH+d;double dg=dz*w.local[idx];
                    add(gate,dg*w.h[h]);w.anchorGradient[h]+=dg*weights[gate];
                }
            }
            for(int edge=0;edge<f.edges;edge++) {
                int source=E+f.nodes[f.from[edge]]*WIDTH,rel=L+f.local[edge]*WIDTH,target=f.to[edge]*WIDTH;double norm=f.scale[f.to[edge]];
                for(int d=0;d<WIDTH;d++) {double da=w.localGradient[target+d]*norm;add(source+d,da*weights[rel+d]);add(rel+d,da*weights[source+d]);}
            }
            {
                for(int h=0;h<w.anchorGradient.length;h++)w.anchorGradient[h]*=derivative(w.a[h])*f.norm;
                for(int i=0;i<f.size;i++)for(int k=0;k<2;k++) {
                    int relation=f.anchor[2*i+k];if(relation<0)continue;
                    for(int d=0;d<WIDTH;d++) {int e=E+f.nodes[i]*WIDTH+d,r=R+relation*WIDTH+d;double da=w.anchorGradient[k*WIDTH+d];add(e,da*weights[r]);add(r,da*weights[e]);}
                }
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
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite attack field parameter");
        }
        for(int i=0;i<weights.length;i++)weights[i]=Math.max(-.99,Math.min(.99,weights[i]));return loss;
    }
    void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(0x42524e4341463031L);out.writeInt(compose?1:0);out.writeInt(PARAMETERS);out.writeDouble(rate);out.writeLong(step);
        for(var array:new double[][]{weights,first,second})for(double value:array)out.writeDouble(value);
        out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static ComposedAttackFieldNetwork read(InputStream stream)throws IOException {
        byte[] bytes=stream.readNBytes(41+24*PARAMETERS);if(bytes.length!=40+24*PARAMETERS)throw new IOException("Attack field state length");
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readLong()!=0x42524e4341463031L)throw new IOException("Attack field magic");int mode=in.readInt();if(mode<0||mode>1||in.readInt()!=PARAMETERS)throw new IOException("Attack field geometry");
        double rate=in.readDouble();long step=in.readLong();if(!Double.isFinite(rate)||rate<=0||step<0)throw new IOException("Attack field optimizer");
        var model=new ComposedAttackFieldNetwork(mode==1,rate,0);model.step=step;
        for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++) {array[i]=in.readDouble();if(!Double.isFinite(array[i])||(array==model.second&&array[i]<0))throw new IOException("Attack field nonfinite/moment");}
        if(in.readLong()!=crc.getValue())throw new IOException("Attack field CRC");return model;
    }
}
