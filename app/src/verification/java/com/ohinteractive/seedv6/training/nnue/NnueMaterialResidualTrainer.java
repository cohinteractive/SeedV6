package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.core.nnue.*;
import java.io.*;
import java.util.Arrays;

/** Track B research recipe, deliberately excluded from legacy application checkpoints.
 * HalfKP raw output is a pawn residual; same material+residual CE and masked Adam
 * equations as BRN3. NNUE keeps float32 kernels/parameters/gradients; moments are
 * binary64. Sparse absent rows retain their weights/moments, exactly as BRN3.
 */
public final class NnueMaterialResidualTrainer {
    public static final String ID="cglhw.halfkp64x32.material-pawn-residual.zero-head.masked-adam.ce.v1";
    private static final long MAGIC=0x53364e5253313031L;
    final Parameters parameters;
    final TrainingScratch scratch=new TrainingScratch();
    final BatchGradients gradients=new BatchGradients();
    private final double[][] first=new double[6][],second=new double[6][];
    private final BrnAdamConfig config;
    private long step;

    public NnueMaterialResidualTrainer(long seed,BrnAdamConfig config) {
        this(Parameters.from(NnueNetwork.initialized(seed)),config);
        Arrays.fill(parameters.groups[Parameters.OUTPUT],0);
        Arrays.fill(parameters.groups[Parameters.OUTPUT_BIAS],0);
    }
    private NnueMaterialResidualTrainer(Parameters parameters,BrnAdamConfig config) {
        this.parameters=parameters;this.config=java.util.Objects.requireNonNull(config);
        for(int g=0;g<6;g++){first[g]=new double[parameters.groups[g].length];second[g]=new double[first[g].length];}
    }
    public long step(){return step;}
    public NnueNetwork snapshot(){return parameters.snapshot();}
    public double predictPawns(long[] board) {
        scratch.forward(parameters,board);
        int material=NnueMaterialBootstrap.forSideToMove(board,NnueMaterialBootstrap.whiteScore(board));
        return material/100.0+scratch.raw;
    }
    public double trainBatch(long[][] boards,double[] targets,int count) {
        if(count<1||count>boards.length||count>targets.length)throw new IllegalArgumentException("Batch size");
        for(int i=0;i<count;i++)if(boards[i]==null||!Double.isFinite(targets[i])||Math.abs(targets[i])>1)
            throw new IllegalArgumentException("Training example");
        gradients.reset();double loss=0;
        for(int i=0;i<count;i++) {
            double pawns=predictPawns(boards[i]);
            var objective=Brn3Objective.crossEntropy(pawns,NnueCorpusTargets.material(boards[i]),targets[i]);
            loss+=objective[0];scratch.backwardRaw(parameters,gradients,objective[1]);
        }
        if(step==Long.MAX_VALUE)throw new ArithmeticException("Optimizer exhausted");
        double c1=1-Math.pow(config.beta1(),step+1),c2=1-Math.pow(config.beta2(),step+1);
        // Verify every candidate before any publication; a bad batch cannot partly update.
        for(boolean publish:new boolean[]{false,true}) {
            for(int g=0;g<6;g++) {
                if(g==Parameters.FEATURES)for(int r=0;r<gradients.count;r++)
                    update(g,gradients.rows[r]*64,gradients.rows[r]*64+64,count,c1,c2,publish);
                else update(g,0,parameters.groups[g].length,count,c1,c2,publish);
            }
        }
        step++;return loss/count;
    }
    private void update(int group,int start,int end,int count,double c1,double c2,boolean publish) {
        double b1=config.beta1(),b2=config.beta2(),one1=b1==.9?.1:1-b1,one2=b2==.999?.001:1-b2;
        for(int i=start;i<end;i++) {
            double g=gradients.values.groups[group][i]/(double)count;
            double m=b1*first[group][i]+one1*g,v=b2*second[group][i]+one2*g*g;
            float weight=(float)(parameters.groups[group][i]-config.learningRate()*(m/c1)/(Math.sqrt(v/c2)+config.epsilon()));
            if(!Double.isFinite(m)||!Double.isFinite(v)||v<0||!Float.isFinite(weight))throw new ArithmeticException("Nonfinite Adam candidate");
            if(publish){parameters.groups[group][i]=weight;first[group][i]=m;second[group][i]=v;}
        }
    }
    public void write(OutputStream output)throws IOException {
        var w=new NnueBinaryFormat.Writer(output,MAGIC,1);
        w.data.writeUTF(ID);w.data.writeLong(step);
        w.data.writeDouble(config.learningRate());w.data.writeDouble(config.beta1());w.data.writeDouble(config.beta2());w.data.writeDouble(config.epsilon());
        for(var g:parameters.groups)NnueBinaryFormat.writeFloats(w.data,g);
        for(var block:new double[][][]{first,second})for(var g:block)for(double v:g)w.data.writeDouble(v);
        w.finish();
    }
    public static NnueMaterialResidualTrainer read(InputStream input)throws IOException {
        var r=new NnueBinaryFormat.Reader(input,MAGIC,1);
        try {
            if(!r.data.readUTF().equals(ID))throw new IOException("Residual recipe mismatch");
            long step=r.data.readLong();if(step<0)throw new IOException("Negative optimizer step");
            var config=new BrnAdamConfig(r.data.readDouble(),r.data.readDouble(),r.data.readDouble(),r.data.readDouble());
            var p=new Parameters();for(var g:p.groups)NnueBinaryFormat.readFloats(r.data,g);p.validate(false);
            var trainer=new NnueMaterialResidualTrainer(p,config);trainer.step=step;
            for(var block:new double[][][]{trainer.first,trainer.second})for(var g:block)for(int i=0;i<g.length;i++) {
                double value=r.data.readDouble();
                if(!Double.isFinite(value)||(block==trainer.second&&value<0)||(step==0&&value!=0))throw new IOException("Invalid Adam moment");
                g[i]=value;
            }
            r.finish();return trainer;
        } catch(IllegalArgumentException invalid){throw new IOException("Invalid residual state",invalid);}
    }
}
