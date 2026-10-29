package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;

/** Learned target-state modulation of mixed nonlinear relational recurrence. */
public final class ContextGatedPieceNetwork {
    static final int WIDTH=8,INPUTS=12,E=769,Q=E+768*WIDTH,A=Q+768*WIDTH,V=A+12*WIDTH*INPUTS,M=V+768*WIDTH,C=M+WIDTH*WIDTH,PARAMETERS=C+WIDTH*WIDTH;
    static final double[][] GEOMETRY=new double[225][10];
    static final long[][] BETWEEN=new long[64][64];
    static {
        for(int dy=-7;dy<=7;dy++)for(int dx=-7;dx<=7;dx++) {
            int ax=Math.abs(dx),ay=Math.abs(dy),r=(dy+7)*15+dx+7;
            GEOMETRY[r]=new double[]{1,dx/7.0,dy/7.0,ax/7.0,ay/7.0,dx*dy/49.0,(dx*dx+dy*dy)/98.0,
                (dx==0||dy==0)&&(ax+ay>0)?1:0,ax==ay&&ax>0?1:0,ax*ay==2?1:0};
        }
        for(int from=0;from<64;from++)for(int to=0;to<64;to++)if(from!=to) {
            int dx=to%8-from%8,dy=to/8-from/8;
            if(dx==0||dy==0||Math.abs(dx)==Math.abs(dy)) {
                int step=Integer.signum(dx)+8*Integer.signum(dy);
                for(int square=from+step;square!=to;square+=step)BETWEEN[from][to]|=1L<<square;
            }
        }
    }
    final boolean context=true,compose=true,mix=true,gate;final double rate;
    final double[] weights=new double[PARAMETERS],first=new double[PARAMETERS],second=new double[PARAMETERS],gradient=new double[PARAMETERS];
    final int[] stamps=new int[PARAMETERS],touched=new int[PARAMETERS];int serial,count;long step;long projectionVersion;
    private record Projection(long version,double[] linear,double[] gates,int states){}
    private volatile Projection projectionCache;
    public ContextGatedPieceNetwork(boolean gate,double rate,long seed) {
        if(!Double.isFinite(rate)||rate<=0)throw new IllegalArgumentException("Rate");
        this.gate=gate;this.rate=rate;var random=new Random(seed);
        for(int i=E;i<A;i++)weights[i]=.1*random.nextGaussian();
        for(int i=A;i<V;i++)if((i-A)%INPUTS<10)weights[i]=.1*random.nextGaussian();
    }
    static final class Features {
        final int[] nodes=new int[32],relations=new int[1024];final byte[] blockers=new byte[1024];
        int size;double norm,material;
        void copyFrom(Features f){size=f.size;norm=f.norm;material=f.material;System.arraycopy(f.nodes,0,nodes,0,size);System.arraycopy(f.relations,0,relations,0,size*size);System.arraycopy(f.blockers,0,blockers,0,size*size);}
        Features frozen(){var result=new Features();result.copyFrom(this);return result;}
    }
    public static final class Workspace {
        final Features features=new Features();
        final double[] a=new double[256],h=new double[256],b=new double[256],raw=new double[256],condition=new double[256],message=new double[256],dm=new double[256],db=new double[256],dh=new double[256],edgePre=new double[8192],edgeGate=new double[8192];
        double[] projection,gates;int gateStates;
        final double[] projectionGradient=new double[2700*WIDTH];
        final boolean[] projectionTouched=new boolean[2700];
        ContextGatedPieceNetwork projectionOwner;long projectionVersion;
        double baseLogit;
        Features encode(long[] board) {
            var f=features;f.size=0;f.material=BrnPair2Features.material(board)/4;int stm=Board.player((int)board[4]);long occupied=0;
            for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1) {
                int sq=Long.numberOfTrailingZeros(bits),p=Board.getSquare(board[0],board[1],board[2],board[3],sq);
                if(f.size==32)throw new IllegalArgumentException("Too many pieces");int canonical=sq^(stm*56);occupied|=1L<<canonical;
                f.nodes[f.size++]=((p&7)-1+6*((p>>>3)^stm))*64+canonical;
            }
            f.norm=f.size>1?1/Math.sqrt(f.size-1):0;
            for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++) {
                int from=f.nodes[j]%64,to=f.nodes[i]%64,k=j*f.size+i;
                f.relations[k]=f.nodes[i]/64*225+(to/8-from/8+7)*15+to%8-from%8+7;
                f.blockers[k]=(byte)Long.bitCount(BETWEEN[from][to]&occupied);
            }
            return f;
        }
    }
    static double activate(double x){return x/(1+Math.abs(x));}
    static double derivative(double x){double d=1+Math.abs(x);return 1/(d*d);}
    double unary(Features f){double value=f.material+weights[768];for(int i=0;i<f.size;i++)value+=weights[f.nodes[i]];return value;}
    double finish(Features f,Workspace w,double value) {
        w.baseLogit=value;if(!compose)return value;Arrays.fill(w.b,0);
        for(int j=0;j<f.size;j++)for(int d=0;d<WIDTH;d++) {
            double message=w.h[j*WIDTH+d];if(mix)for(int e=0;e<WIDTH;e++)message+=weights[M+d*WIDTH+e]*w.h[j*WIDTH+e]/Math.sqrt(WIDTH);
            w.message[j*WIDTH+d]=message;
        }
        for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i)for(int d=0;d<WIDTH;d++)w.b[i*WIDTH+d]+=f.norm*w.message[j*WIDTH+d]*w.edgeGate[(j*f.size+i)*WIDTH+d];
        if(gate)for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
            int k=i*WIDTH+d;double c=0;for(int e=0;e<WIDTH;e++)c+=weights[C+d*WIDTH+e]*w.h[i*WIDTH+e]/Math.sqrt(WIDTH);
            w.condition[k]=c;w.raw[k]=w.b[k];w.b[k]*=1+activate(c);
        }
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++)value+=weights[V+f.nodes[i]*WIDTH+d]*activate(w.b[i*WIDTH+d])/WIDTH;
        return value;
    }
    double logit(Features f,Workspace w) {
        Arrays.fill(w.a,0);
        for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i) {
            int edge=j*f.size+i,relation=f.relations[edge],base=A+(relation/225)*WIDTH*INPUTS,source=E+f.nodes[j]*WIDTH;
            double[] geometry=GEOMETRY[relation%225];double clear=(geometry[7]+geometry[8]>0&&f.blockers[edge]==0)?1:0,blocked=f.blockers[edge]/7.0;
            for(int d=0;d<WIDTH;d++) {
                int parameter=base+d*INPUTS,k=edge*WIDTH+d;double value=0;
                for(int x=0;x<10;x++)value+=weights[parameter+x]*geometry[x];
                if(context)value+=weights[parameter+10]*clear+weights[parameter+11]*blocked;
                w.edgePre[k]=value;w.edgeGate[k]=activate(value);w.a[i*WIDTH+d]+=f.norm*weights[source+d]*w.edgeGate[k];
            }
        }
        double value=unary(f);
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
            int k=i*WIDTH+d;w.h[k]=activate(w.a[k]);value+=weights[Q+f.nodes[i]*WIDTH+d]*w.h[k]/WIDTH;
        }
        value=finish(f,w,value);
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite shared geometry score");return value;
    }
    /** Required after package-local research ablations; training invalidates automatically. */
    void invalidateProjections(){if(projectionVersion==Long.MAX_VALUE)throw new ArithmeticException("Projection version");projectionVersion++;}
    private Projection projections() {
        var cached=projectionCache;
        if(cached!=null&&cached.version==projectionVersion)return cached;
        synchronized(this) {
            cached=projectionCache;if(cached!=null&&cached.version==projectionVersion)return cached;
            int states=context?7:1;double[] linear=new double[2700*WIDTH],gates=new double[2700*states*WIDTH];
            for(int relation=0;relation<2700;relation++) {
                double[] geometry=GEOMETRY[relation%225];int base=A+(relation/225)*WIDTH*INPUTS;
                for(int d=0;d<WIDTH;d++) {
                    double value=0;for(int x=0;x<10;x++)value+=weights[base+d*INPUTS+x]*geometry[x];linear[relation*WIDTH+d]=value;
                    for(int blocks=0;blocks<states;blocks++) {
                        double pre=value,clear=(geometry[7]+geometry[8]>0&&blocks==0)?1:0;
                        if(context)pre+=weights[base+d*INPUTS+10]*clear+weights[base+d*INPUTS+11]*(blocks/7.0);
                        gates[(relation*states+blocks)*WIDTH+d]=activate(pre);
                    }
                }
            }
            cached=new Projection(projectionVersion,linear,gates,states);projectionCache=cached;return cached;
        }
    }
    void prepareProjection(Workspace w) {
        if(w.projectionOwner==this&&w.projectionVersion==projectionVersion)return;
        var cached=projections();w.projection=cached.linear;w.gates=cached.gates;w.gateStates=cached.states;
        w.projectionOwner=this;w.projectionVersion=projectionVersion;
    }
    double projectionLogit(Features f,Workspace w) {
        prepareProjection(w);Arrays.fill(w.a,0);
        for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i) {
            int edge=j*f.size+i,relation=f.relations[edge],base=A+(relation/225)*WIDTH*INPUTS,source=E+f.nodes[j]*WIDTH;
            double[] geometry=GEOMETRY[relation%225];double clear=(geometry[7]+geometry[8]>0&&f.blockers[edge]==0)?1:0,blocked=f.blockers[edge]/7.0;
            for(int d=0;d<WIDTH;d++) {
                int k=edge*WIDTH+d;double value=w.projection[relation*WIDTH+d];
                if(context)value+=weights[base+d*INPUTS+10]*clear+weights[base+d*INPUTS+11]*blocked;
                w.edgePre[k]=value;w.edgeGate[k]=activate(value);w.a[i*WIDTH+d]+=f.norm*weights[source+d]*w.edgeGate[k];
            }
        }
        double value=unary(f);
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++){int k=i*WIDTH+d;w.h[k]=activate(w.a[k]);value+=weights[Q+f.nodes[i]*WIDTH+d]*w.h[k]/WIDTH;}
        value=finish(f,w,value);
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite compiled shared geometry score");return value;
    }
    double compiledLogit(Features f,Workspace w) {
        prepareProjection(w);Arrays.fill(w.a,0);
        for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i) {
            int edge=j*f.size+i,gate=(f.relations[edge]*w.gateStates+(context?f.blockers[edge]:0))*WIDTH,source=E+f.nodes[j]*WIDTH;
            for(int d=0;d<WIDTH;d++){double value=w.gates[gate+d];w.edgeGate[edge*WIDTH+d]=value;w.a[i*WIDTH+d]+=f.norm*weights[source+d]*value;}
        }
        double value=unary(f);
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++){int k=i*WIDTH+d;w.h[k]=activate(w.a[k]);value+=weights[Q+f.nodes[i]*WIDTH+d]*w.h[k]/WIDTH;}
        value=finish(f,w,value);
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite compiled shared geometry score");return value;
    }
    public double scoreCp(long[] board,Workspace w){return 400*compiledLogit(w.encode(board),w);}
    double scoreCpProjection(long[] board,Workspace w){return 400*projectionLogit(w.encode(board),w);}
    double scoreCpUncached(long[] board,Workspace w){return 400*logit(w.encode(board),w);}
    void add(int i,double g){if(stamps[i]!=serial){stamps[i]=serial;touched[count++]=i;gradient[i]=0;}gradient[i]+=g;}
    double gradients(Features[] batch,double[] targets){return gradients(batch,targets,true);}
    double gradientsUncached(Features[] batch,double[] targets){return gradients(batch,targets,false);}
    private double gradients(Features[] batch,double[] targets,boolean compiled) {
        if(batch.length==0||batch.length!=targets.length)throw new IllegalArgumentException("Batch");
        if(++serial==0){Arrays.fill(stamps,0);serial=1;}count=0;double loss=0;var w=new Workspace();
        for(int row=0;row<batch.length;row++) {
            if(!Double.isFinite(targets[row])||targets[row]<0||targets[row]>1)throw new IllegalArgumentException("Target");
            var f=batch[row];double p=PairObjectiveDiagnostic.sigmoid(compiled?compiledLogit(f,w):logit(f,w)),error=p-targets[row];loss+=error*error;
            double g=2*error*p*(1-p)/batch.length;add(768,g);for(int i=0;i<f.size;i++)add(f.nodes[i],g);
            for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
                int k=i*WIDTH+d,q=Q+f.nodes[i]*WIDTH+d;add(q,g*w.h[k]/WIDTH);
                w.dh[k]=compose?g*weights[q]/WIDTH:g*weights[q]*derivative(w.a[k])/WIDTH;
                if(compose){int v=V+f.nodes[i]*WIDTH+d;add(v,g*activate(w.b[k])/WIDTH);w.db[k]=g*weights[v]*derivative(w.b[k])/WIDTH;}
            }
            if(gate)for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
                int k=i*WIDTH+d;double dc=w.db[k]*w.raw[k]*derivative(w.condition[k]);
                for(int e=0;e<WIDTH;e++){int c=C+d*WIDTH+e;add(c,dc*w.h[i*WIDTH+e]/Math.sqrt(WIDTH));w.dh[i*WIDTH+e]+=dc*weights[c]/Math.sqrt(WIDTH);}
                w.db[k]*=1+activate(w.condition[k]);
            }
            if(compose) {
                if(!mix){for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i)for(int d=0;d<WIDTH;d++)w.dh[j*WIDTH+d]+=w.db[i*WIDTH+d]*f.norm*w.edgeGate[(j*f.size+i)*WIDTH+d];}
                else {
                    Arrays.fill(w.dm,0);
                    for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i)for(int d=0;d<WIDTH;d++)w.dm[j*WIDTH+d]+=w.db[i*WIDTH+d]*f.norm*w.edgeGate[(j*f.size+i)*WIDTH+d];
                    for(int j=0;j<f.size;j++) {
                        for(int e=0;e<WIDTH;e++)w.dh[j*WIDTH+e]+=w.dm[j*WIDTH+e];
                        for(int d=0;d<WIDTH;d++)for(int e=0;e<WIDTH;e++) {
                            add(M+d*WIDTH+e,w.dm[j*WIDTH+d]*w.h[j*WIDTH+e]/Math.sqrt(WIDTH));
                            w.dh[j*WIDTH+e]+=w.dm[j*WIDTH+d]*weights[M+d*WIDTH+e]/Math.sqrt(WIDTH);
                        }
                    }
                }
                for(int k=0;k<f.size*WIDTH;k++)w.dh[k]*=derivative(w.a[k]);
            }
            for(int j=0;j<f.size;j++)for(int i=0;i<f.size;i++)if(j!=i) {
                int edge=j*f.size+i,relation=f.relations[edge],base=A+(relation/225)*WIDTH*INPUTS,source=E+f.nodes[j]*WIDTH;
                double[] geometry=GEOMETRY[relation%225];double clear=(geometry[7]+geometry[8]>0&&f.blockers[edge]==0)?1:0,blocked=f.blockers[edge]/7.0;
                for(int d=0;d<WIDTH;d++) {
                    int parameter=base+d*INPUTS,k=edge*WIDTH+d;double da=w.dh[i*WIDTH+d]*f.norm;
                    add(source+d,da*w.edgeGate[k]);double remaining=1-Math.abs(w.edgeGate[k]);double edgeDerivative=compiled?remaining*remaining:derivative(w.edgePre[k]);double route=da*weights[source+d];if(compose)route+=w.db[i*WIDTH+d]*f.norm*w.message[j*WIDTH+d];double dg=route*edgeDerivative;
                    if(compiled){w.projectionTouched[relation]=true;w.projectionGradient[relation*WIDTH+d]+=dg;}
                    else for(int x=0;x<10;x++)if(geometry[x]!=0)add(parameter+x,dg*geometry[x]);
                    if(context){if(clear!=0)add(parameter+10,dg*clear);if(blocked!=0)add(parameter+11,dg*blocked);}
                }
            }
        }
        if(compiled)for(int relation=0;relation<2700;relation++)if(w.projectionTouched[relation]) {
            double[] geometry=GEOMETRY[relation%225];int base=A+(relation/225)*WIDTH*INPUTS;
            for(int d=0;d<WIDTH;d++)for(int x=0;x<10;x++)if(geometry[x]!=0)add(base+d*INPUTS+x,w.projectionGradient[relation*WIDTH+d]*geometry[x]);
        }
        return loss/batch.length;
    }
    double train(Features[] batch,double[] targets) {
        double loss=gradients(batch,targets);updateFromGradients();return loss;
    }
    void updateFromGradients() {
        if(step==Long.MAX_VALUE)throw new ArithmeticException("Step");invalidateProjections();step++;
        double c1=1-Math.pow(.9,step),c2=1-Math.pow(.999,step);
        for(int i=0;i<weights.length;i++)weights[i]*=1-rate*.01;
        for(int n=0;n<count;n++) {
            int i=touched[n];double g=gradient[i];first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite shared geometry parameter");
        }
        for(int i=0;i<weights.length;i++)weights[i]=Math.max(-.99,Math.min(.99,weights[i]));
    }
    void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(0x42524e4347543031L);out.writeInt(gate?1:0);out.writeInt(PARAMETERS);out.writeDouble(rate);out.writeLong(step);
        for(var array:new double[][]{weights,first,second})for(double value:array)out.writeDouble(value);
        out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static ContextGatedPieceNetwork read(InputStream stream)throws IOException {
        byte[] bytes=stream.readNBytes(41+24*PARAMETERS);if(bytes.length!=40+24*PARAMETERS)throw new IOException("Context gated state length");
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readLong()!=0x42524e4347543031L)throw new IOException("Context gated magic");int mode=in.readInt();if(mode<0||mode>1||in.readInt()!=PARAMETERS)throw new IOException("Context gated geometry");
        double rate=in.readDouble();long step=in.readLong();if(!Double.isFinite(rate)||rate<=0||step<0)throw new IOException("Context gated optimizer");
        var model=new ContextGatedPieceNetwork(mode==1,rate,0);model.step=step;
        for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++){array[i]=in.readDouble();if(!Double.isFinite(array[i])||(array==model.second&&array[i]<0))throw new IOException("Context gated nonfinite/moment");}
        if(in.readLong()!=crc.getValue())throw new IOException("Context gated CRC");return model;
    }
}
