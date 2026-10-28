package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;

/** Research-only factorized directed messages and distinct-source wedge energies. */
public final class RelationalMessages {
    static final int WIDTH=8, E=769, R=E+768*WIDTH, Q=R+2700*WIDTH,
            C=Q+768*WIDTH, PARAMETERS=C+768*WIDTH;
    final boolean compose;
    final boolean regularized;
    final double rate;
    final double[] weights=new double[PARAMETERS],first=new double[PARAMETERS],second=new double[PARAMETERS],gradient=new double[PARAMETERS];
    final int[] stamps=new int[PARAMETERS],touched=new int[PARAMETERS];
    int serial,count;
    long step;
    public RelationalMessages(boolean compose,double rate,long seed) {
        this(compose,rate,seed,false);
    }
    public RelationalMessages(boolean compose,double rate,long seed,boolean regularized) {
        if(!Double.isFinite(rate)||rate<=0)throw new IllegalArgumentException("Rate");
        this.compose=compose;this.rate=rate;this.regularized=regularized;
        var random=new Random(seed);for(int i=E;i<C;i++)weights[i]=.1*random.nextGaussian();
    }
    static final class Features {
        int[] nodes=new int[32],from=new int[1024],to=new int[1024],relation=new int[1024];
        int size,edges;double material;
        Features frozen(){var f=new Features();f.nodes=Arrays.copyOf(nodes,size);f.from=Arrays.copyOf(from,edges);f.to=Arrays.copyOf(to,edges);f.relation=Arrays.copyOf(relation,edges);f.size=size;f.edges=edges;f.material=material;return f;}
    }
    public static final class Workspace {
        final RelationalGraphEnergy.Features graph=new RelationalGraphEnergy.Features();
        final Features features=new Features();
        final int[] index=new int[64];
        final double[] aggregate=new double[32*WIDTH],squares=new double[32*WIDTH];
        Features encode(long[] board) {
            graph.encode(board,RelationalGraphEnergy.Mode.EDGE);var f=features;f.size=f.edges=0;f.material=graph.material/400;
            int stm=Board.player((int)board[4]);
            for(int square=0;square<64;square++)if(graph.pieces[square]!=0) {
                int p=graph.pieces[square],channel=(p&7)-1+6*((p>>>3)^stm);
                index[square]=f.size;f.nodes[f.size++]=channel*64+(square^(stm*56));
            }
            for(int s=0;s<64;s++)for(long bits=graph.edges[s];bits!=0;bits&=bits-1) {
                int t=Long.numberOfTrailingZeros(bits),sq=s^(stm*56),tq=t^(stm*56);
                f.from[f.edges]=index[s];f.to[f.edges]=index[t];
                f.relation[f.edges++]=(f.nodes[index[t]]/64)*225+(tq/8-sq/8+7)*15+(tq%8-sq%8+7);
            }
            return f;
        }
    }
    double logit(Features f,Workspace workspace) {
        var a=workspace.aggregate;var b=workspace.squares;Arrays.fill(a,0);Arrays.fill(b,0);
        double sum=f.material+weights[768];for(int i=0;i<f.size;i++)sum+=weights[f.nodes[i]];
        for(int edge=0;edge<f.edges;edge++) {
            int source=E+f.nodes[f.from[edge]]*WIDTH,relation=R+f.relation[edge]*WIDTH,target=f.to[edge]*WIDTH;
            for(int d=0;d<WIDTH;d++){double m=.5*weights[source+d]*weights[relation+d];a[target+d]+=m;b[target+d]+=m*m;}
        }
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
            int address=f.nodes[i]*WIDTH+d,k=i*WIDTH+d;double energy=weights[Q+address]*a[k];
            if(compose)energy+=weights[C+address]*(a[k]*a[k]-b[k]);sum+=energy*.125;
        }
        if(!Double.isFinite(sum))throw new ArithmeticException("Nonfinite message score");return sum;
    }
    public double scoreCp(long[] board,Workspace workspace){return 400*logit(workspace.encode(board),workspace);}
    void add(int i,double g){if(stamps[i]!=serial){stamps[i]=serial;touched[count++]=i;gradient[i]=0;}gradient[i]+=g;}
    double gradients(Features[] batch,double[] targets) {
        if(++serial==0){Arrays.fill(stamps,0);serial=1;}count=0;double loss=0;var workspace=new Workspace();
        for(int n=0;n<batch.length;n++) {
            var f=batch[n];double p=PairObjectiveDiagnostic.sigmoid(logit(f,workspace)),error=p-targets[n];loss+=error*error;
            double derivative=2*error*p*(1-p)/batch.length,scaled=derivative*.125;
            add(768,derivative);for(int i=0;i<f.size;i++)add(f.nodes[i],derivative);
            for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
                int address=f.nodes[i]*WIDTH+d,k=i*WIDTH+d;double a=workspace.aggregate[k];add(Q+address,scaled*a);
                if(compose)add(C+address,scaled*(a*a-workspace.squares[k]));
            }
            for(int edge=0;edge<f.edges;edge++) {
                int source=E+f.nodes[f.from[edge]]*WIDTH,relation=R+f.relation[edge]*WIDTH;
                int target=f.to[edge]*WIDTH,address=f.nodes[f.to[edge]]*WIDTH;
                for(int d=0;d<WIDTH;d++) {
                    double e=weights[source+d],r=weights[relation+d],m=.5*e*r;
                    double factor=weights[Q+address+d];
                    if(compose)factor+=2*weights[C+address+d]*(workspace.aggregate[target+d]-m);
                    add(source+d,scaled*factor*.5*r);add(relation+d,scaled*factor*.5*e);
                }
            }
        }
        return loss/batch.length;
    }
    double train(Features[] batch,double[] targets) {
        double loss=gradients(batch,targets);step++;double c1=1-Math.pow(.9,step),c2=1-Math.pow(.999,step);
        if(regularized)for(int i=0;i<weights.length;i++)weights[i]*=1-rate*.01;
        for(int n=0;n<count;n++) {
            int i=touched[n];double g=gradient[i];first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite message parameter");
        }
        if(regularized)for(int i=0;i<weights.length;i++)weights[i]=Math.max(-.99,Math.min(.99,weights[i]));
        return loss;
    }
    void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeLong(0x42524e4d53473032L);out.writeInt(compose?1:0);out.writeInt(PARAMETERS);out.writeDouble(rate);out.writeLong(step);out.writeInt(regularized?1:0);
        for(var array:new double[][]{weights,first,second})for(double value:array)out.writeDouble(value);
        out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static RelationalMessages read(InputStream stream)throws IOException {
        byte[] bytes=stream.readNBytes(44+24*PARAMETERS+1);if(bytes.length!=40+24*PARAMETERS&&bytes.length!=44+24*PARAMETERS)throw new IOException("Message state length");
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        long magic=in.readLong();if(magic!=0x42524e4d53473031L&&magic!=0x42524e4d53473032L)throw new IOException("Message state magic");
        boolean version2=magic==0x42524e4d53473032L;
        if(bytes.length!=(version2?44:40)+24*PARAMETERS)throw new IOException("Message version length");
        int mode=in.readInt();if(mode<0||mode>1||in.readInt()!=PARAMETERS)throw new IOException("Message geometry");
        double rate=in.readDouble();long step=in.readLong();int regularized=version2?in.readInt():0;
        if(regularized<0||regularized>1||step<0)throw new IOException("Message optimizer");
        var model=new RelationalMessages(mode==1,rate,0,regularized==1);model.step=step;
        for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++){
            array[i]=in.readDouble();if(!Double.isFinite(array[i]))throw new IOException("Message nonfinite");
        }
        if(in.readLong()!=crc.getValue())throw new IOException("Message CRC");return model;
    }
}
