package com.ohinteractive.seedv6.core.brnpair2;

import java.io.*;
import java.util.*;

/** Research-only neural attack/defense messages conditioned by learned global king anchors. */
public final class ContextualAttackNetwork {
    static final int WIDTH=8,E=769,L=E+768*WIDTH,R=L+2700*WIDTH,O=R+450*WIDTH,G=O+768*WIDTH,PARAMETERS=G+24*WIDTH;
    final boolean global;
    final double rate;
    final double[] weights=new double[PARAMETERS],first=new double[PARAMETERS],second=new double[PARAMETERS],gradient=new double[PARAMETERS];
    final int[] stamps=new int[PARAMETERS],touched=new int[PARAMETERS];
    int serial,count;
    long step;
    public ContextualAttackNetwork(boolean global,double rate,long seed) {
        if(!Double.isFinite(rate)||rate<=0)throw new IllegalArgumentException("Rate");this.global=global;this.rate=rate;
        var random=new Random(seed);for(int i=E;i<G;i++)weights[i]=.1*random.nextGaussian();
    }
    static final class Features {
        int size,edges;
        final int[] nodes=new int[32],from=new int[1024],to=new int[1024],local=new int[1024],anchor=new int[64];
        final double[] scale=new double[32];
        double material,norm;
        void copyFrom(Features f) {
            size=f.size;edges=f.edges;material=f.material;norm=f.norm;
            System.arraycopy(f.nodes,0,nodes,0,size);System.arraycopy(f.scale,0,scale,0,size);System.arraycopy(f.anchor,0,anchor,0,2*size);
            System.arraycopy(f.from,0,from,0,edges);System.arraycopy(f.to,0,to,0,edges);System.arraycopy(f.local,0,local,0,edges);
        }
        Features frozen(){var f=new Features();f.copyFrom(this);return f;}
    }
    public static final class Workspace {
        final RelationalMessages.Workspace graph=new RelationalMessages.Workspace();
        final AnchorNeuralNetwork.Workspace anchors=new AnchorNeuralNetwork.Workspace();
        final Features features=new Features();
        final double[] a=new double[16],h=new double[16],local=new double[256],gain=new double[256],z=new double[256],localGradient=new double[256],anchorGradient=new double[16];
        Features encode(long[] board) {
            var g=graph.encode(board);var k=anchors.encode(board);var f=features;
            if(g.size!=k.size)throw new IllegalStateException("Node geometry");
            f.size=g.size;f.edges=g.edges;f.material=g.material;f.norm=k.norm;Arrays.fill(f.scale,0);
            for(int i=0;i<f.size;i++){if(g.nodes[i]!=k.nodes[i])throw new IllegalStateException("Node order");f.nodes[i]=g.nodes[i];}
            System.arraycopy(k.relations,0,f.anchor,0,2*f.size);
            for(int e=0;e<f.edges;e++){f.from[e]=g.from[e];f.to[e]=g.to[e];f.local[e]=g.relation[e];f.scale[f.to[e]]++;}
            for(int i=0;i<f.size;i++)f.scale[i]=f.scale[i]==0?0:1/Math.sqrt(f.scale[i]);return f;
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
        if(global) {
            for(int i=0;i<f.size;i++)for(int k=0;k<2;k++) {
                int rel=f.anchor[2*i+k];if(rel<0)continue;
                for(int d=0;d<WIDTH;d++)w.a[k*WIDTH+d]+=f.norm*weights[E+f.nodes[i]*WIDTH+d]*weights[R+rel*WIDTH+d];
            }
            for(int i=0;i<w.a.length;i++)w.h[i]=activate(w.a[i]);
        }
        double sum=unary(f);
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
            int idx=i*WIDTH+d;double gain=1;
            if(global)for(int k=0;k<2;k++)gain+=weights[G+(f.nodes[i]/64*2+k)*WIDTH+d]*w.h[k*WIDTH+d];
            w.gain[idx]=gain;w.z[idx]=w.local[idx]*gain;sum+=weights[O+f.nodes[i]*WIDTH+d]*activate(w.z[idx])/8;
        }
        if(!Double.isFinite(sum))throw new ArithmeticException("Nonfinite contextual attack score");return sum;
    }
    public double scoreCp(long[] board,Workspace w){return 400*logit(w.encode(board),w);}
    void add(int i,double g){if(stamps[i]!=serial){stamps[i]=serial;touched[count++]=i;gradient[i]=0;}gradient[i]+=g;}
    double gradients(Features[] batch,double[] targets) {
        if(batch.length==0||batch.length!=targets.length)throw new IllegalArgumentException("Batch");
        if(++serial==0){Arrays.fill(stamps,0);serial=1;}count=0;double loss=0;var w=new Workspace();
        for(int n=0;n<batch.length;n++) {
            if(!Double.isFinite(targets[n])||targets[n]<0||targets[n]>1)throw new IllegalArgumentException("Target");
            var f=batch[n];double p=PairObjectiveDiagnostic.sigmoid(logit(f,w)),error=p-targets[n];loss+=error*error;
            double g=2*error*p*(1-p)/batch.length;add(768,g);for(int i=0;i<f.size;i++)add(f.nodes[i],g);
            Arrays.fill(w.anchorGradient,0);
            for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
                int idx=i*WIDTH+d,output=O+f.nodes[i]*WIDTH+d;add(output,g*activate(w.z[idx])/8);
                double dz=g*weights[output]*derivative(w.z[idx])/8;w.localGradient[idx]=dz*w.gain[idx];
                if(global)for(int k=0;k<2;k++) {
                    int gate=G+(f.nodes[i]/64*2+k)*WIDTH+d,h=k*WIDTH+d;double dg=dz*w.local[idx];
                    add(gate,dg*w.h[h]);w.anchorGradient[h]+=dg*weights[gate];
                }
            }
            for(int edge=0;edge<f.edges;edge++) {
                int source=E+f.nodes[f.from[edge]]*WIDTH,rel=L+f.local[edge]*WIDTH,target=f.to[edge]*WIDTH;double norm=f.scale[f.to[edge]];
                for(int d=0;d<WIDTH;d++) {double da=w.localGradient[target+d]*norm;add(source+d,da*weights[rel+d]);add(rel+d,da*weights[source+d]);}
            }
            if(global) {
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
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite contextual attack parameter");
        }
        for(int i=0;i<weights.length;i++)weights[i]=Math.max(-.99,Math.min(.99,weights[i]));return loss;
    }
    void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(0x42524e4341543031L);out.writeInt(global?1:0);out.writeInt(PARAMETERS);out.writeDouble(rate);out.writeLong(step);
        for(var array:new double[][]{weights,first,second})for(double value:array)out.writeDouble(value);
        out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static ContextualAttackNetwork read(InputStream stream)throws IOException {
        byte[] bytes=stream.readNBytes(41+24*PARAMETERS);if(bytes.length!=40+24*PARAMETERS)throw new IOException("Contextual attack state length");
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readLong()!=0x42524e4341543031L)throw new IOException("Contextual attack magic");int mode=in.readInt();if(mode<0||mode>1||in.readInt()!=PARAMETERS)throw new IOException("Contextual attack geometry");
        double rate=in.readDouble();long step=in.readLong();if(!Double.isFinite(rate)||rate<=0||step<0)throw new IOException("Contextual attack optimizer");
        var model=new ContextualAttackNetwork(mode==1,rate,0);model.step=step;
        for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++) {array[i]=in.readDouble();if(!Double.isFinite(array[i])||(array==model.second&&array[i]<0))throw new IOException("Contextual attack nonfinite/moment");}
        if(in.readLong()!=crc.getValue())throw new IOException("Contextual attack CRC");return model;
    }
}
