package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.nio.*;
import java.util.*;

/** R057 configurable ONE only: the R056 pooled equations and touched-coordinate Adam. */
public final class TrainableOne {
    final int width,rank,E=769,Q,KEY,QUERY,parameters;
    final double[] weights,first,second,gradient;
    final int[] stamps,touched;
    final Workspace workspace;
    double rate;
    long step;
    int serial,count;

    public TrainableOne(int width,int rank,double rate,long seed) {
        if(!((width==8&&rank==4)||(width==16&&rank==4)||(width==32&&rank==8)))throw new IllegalArgumentException("R057 capacity allocation");
        if(!Double.isFinite(rate)||rate<=0||rate>.1)throw new IllegalArgumentException("Rate");
        this.width=width;this.rank=rank;this.rate=rate;Q=E+768*width;KEY=Q+768*width;QUERY=KEY+768*rank;parameters=QUERY+768*rank;
        weights=new double[parameters];first=new double[parameters];second=new double[parameters];gradient=new double[parameters];
        stamps=new int[parameters];touched=new int[parameters];workspace=new Workspace();var random=new Random(seed);
        for(int i=E;i<parameters;i++)weights[i]=.1*random.nextGaussian();
    }
    static final class Features {
        final int[] nodes=new int[32];int size;double material,norm;
    }
    final class Workspace {
        final double[] pool=new double[rank*width],poolDelta=new double[rank*width],a=new double[32*width],h=new double[32*width],delta=new double[32*width];
    }
    Features encode(long[] board,Features f) {
        f.size=0;f.material=BrnPair2Features.material(board)/4;int stm=Board.player((int)board[4]);
        for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1){int s=Long.numberOfTrailingZeros(bits),piece=Board.getSquare(board[0],board[1],board[2],board[3],s);f.nodes[f.size++]=IncrementalOne.node(piece,s,stm);}
        f.norm=f.size>1?1/Math.sqrt(rank*(f.size-1)):0;return f;
    }
    double decode(ByteBuffer b,int offset,Features f) {
        long bits=b.getLong(offset);f.size=0;f.material=0;
        for(;bits!=0;bits&=bits-1){int s=Long.numberOfTrailingZeros(bits),index=f.size,code=(b.get(offset+8+index/2)>>>((index&1)*4))&15;
            if((code&7)>5||index>=32)throw new IllegalArgumentException("Packed piece");
            int piece=6-(code&7)|(code&8);f.nodes[f.size++]=((piece&7)-1+6*(piece>>>3))*64+s;
            f.material+=((piece&8)==0?1:-1)*BrnPair2Features.pieceMaterial(piece)/4;
        }
        if(f.size<2)throw new IllegalArgumentException("Packed count");f.norm=1/Math.sqrt(rank*(f.size-1));
        return PairObjectiveDiagnostic.sigmoid(b.getShort(offset+24)/400.0);
    }
    double logit(Features f,Workspace w,boolean relational) {
        double value=f.material+weights[768];for(int i=0;i<f.size;i++)value+=weights[f.nodes[i]];
        if(!relational)return value;
        Arrays.fill(w.pool,0);
        for(int j=0;j<f.size;j++){int node=f.nodes[j];for(int r=0;r<rank;r++){double k=weights[KEY+node*rank+r];for(int d=0;d<width;d++)w.pool[r*width+d]+=k*weights[E+node*width+d];}}
        for(int i=0;i<f.size;i++){int node=f.nodes[i];double self=0;for(int r=0;r<rank;r++)self+=weights[QUERY+node*rank+r]*weights[KEY+node*rank+r];
            for(int d=0;d<width;d++){double a=0;for(int r=0;r<rank;r++)a+=weights[QUERY+node*rank+r]*w.pool[r*width+d];
                int k=i*width+d;w.a[k]=f.norm*(a-self*weights[E+node*width+d]);w.h[k]=LowRankRelationNetwork.activate(w.a[k]);value+=weights[Q+node*width+d]*w.h[k]/width;
            }
        }
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite ONE");return value;
    }
    public double scoreCp(long[] board){return 400*logit(encode(board,new Features()),workspace,true);}
    void add(int i,double g){if(stamps[i]!=serial){stamps[i]=serial;touched[count++]=i;gradient[i]=0;}gradient[i]+=g;}
    double gradients(Features[] batch,double[] target) {
        if(batch.length==0||batch.length!=target.length)throw new IllegalArgumentException("Batch");
        if(++serial==0){Arrays.fill(stamps,0);serial=1;}count=0;double loss=0;var w=workspace;
        for(int row=0;row<batch.length;row++){
            if(!Double.isFinite(target[row])||target[row]<0||target[row]>1)throw new IllegalArgumentException("Target");
            var f=batch[row];double p=PairObjectiveDiagnostic.sigmoid(logit(f,w,true)),error=p-target[row];loss+=error*error;
            double g=2*error*p*(1-p)/batch.length;add(768,g);Arrays.fill(w.poolDelta,0);
            for(int i=0;i<f.size;i++){int node=f.nodes[i];add(node,g);
                for(int d=0;d<width;d++){int k=i*width+d;add(Q+node*width+d,g*w.h[k]/width);w.delta[k]=g*weights[Q+node*width+d]*LowRankRelationNetwork.derivative(w.a[k])/width;}
                for(int r=0;r<rank;r++){double query=weights[QUERY+node*rank+r];for(int d=0;d<width;d++)w.poolDelta[r*width+d]+=f.norm*w.delta[i*width+d]*query;}
            }
            for(int i=0;i<f.size;i++){int node=f.nodes[i];double self=0;for(int r=0;r<rank;r++)self+=weights[QUERY+node*rank+r]*weights[KEY+node*rank+r];
                for(int d=0;d<width;d++){double dx=0;for(int r=0;r<rank;r++)dx+=weights[KEY+node*rank+r]*w.poolDelta[r*width+d];add(E+node*width+d,dx-f.norm*w.delta[i*width+d]*self);}
                for(int r=0;r<rank;r++){int key=KEY+node*rank+r,query=QUERY+node*rank+r;double dk=0,dq=0;
                    for(int d=0;d<width;d++){double t=f.norm*w.delta[i*width+d],x=weights[E+node*width+d];dk+=x*(w.poolDelta[r*width+d]-t*weights[query]);dq+=t*(w.pool[r*width+d]-weights[key]*x);}
                    add(key,dk);add(query,dq);
                }
            }
        }
        return loss/batch.length;
    }
    double train(Features[] batch,double[] target) {
        double loss=gradients(batch,target);if(step==Long.MAX_VALUE)throw new ArithmeticException("Step");step++;
        double c1=1-Math.pow(.9,step),c2=1-Math.pow(.999,step);
        for(int i=0;i<parameters;i++)weights[i]*=1-rate*.01;
        for(int n=0;n<count;n++){int i=touched[n];double g=gradient[i];first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i])||!Double.isFinite(first[i])||!Double.isFinite(second[i]))throw new ArithmeticException("Nonfinite optimizer");
        }
        for(int i=0;i<parameters;i++)weights[i]=Math.max(-.99,Math.min(.99,weights[i]));return loss;
    }
    void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeLong(0x42524e4f4e453031L);out.writeInt(width);out.writeInt(rank);out.writeInt(parameters);out.writeDouble(rate);out.writeLong(step);
        for(var array:new double[][]{weights,first,second})for(double v:array)out.writeDouble(v);
        out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static TrainableOne read(InputStream stream)throws IOException {
        byte[] bytes=stream.readNBytes(2_000_001);if(bytes.length<44||bytes.length>2_000_000)throw new IOException("ONE state size");
        var in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readLong()!=0x42524e4f4e453031L)throw new IOException("ONE magic");
        int width=in.readInt(),rank=in.readInt(),parameters=in.readInt();double rate=in.readDouble();long step=in.readLong();TrainableOne m;
        try{m=new TrainableOne(width,rank,rate,0);}catch(IllegalArgumentException e){throw new IOException("ONE dimensions/rate",e);}
        if(parameters!=m.parameters||bytes.length!=44+24*parameters||step<0)throw new IOException("ONE schema");m.step=step;
        for(var a:new double[][]{m.weights,m.first,m.second})for(int i=0;i<a.length;i++){a[i]=in.readDouble();if(!Double.isFinite(a[i])||(a==m.weights&&Math.abs(a[i])>.99)||(a==m.second&&a[i]<0))throw new IOException("ONE state bounds");}
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);if(in.readLong()!=crc.getValue())throw new IOException("ONE CRC");return m;
    }
}
