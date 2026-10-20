package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.nio.file.*;
import java.util.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Label-independent mean-mixture ablation; all source positions were already used for development. */
public final class BrnGatedContextProbe {
    record Row(long[] board,double target,double[] gates) {}
    static final class Stats {
        long n;double entropy;final double[] sum=new double[4],square=new double[4],min={1,1,1,1},max=new double[4];
        final long[] largest=new long[4];
        void add(double[] g) {
            n++;int arg=0;
            for(int i=0;i<4;i++) {
                sum[i]+=g[i];square[i]+=g[i]*g[i];min[i]=Math.min(min[i],g[i]);max[i]=Math.max(max[i],g[i]);
                if(g[i]>0)entropy-=g[i]*Math.log(g[i]);if(g[i]>g[arg])arg=i;
            }
            largest[arg]++;
        }
        Object report() {
            double[] mean=new double[4],sd=new double[4];
            for(int i=0;i<4;i++){mean[i]=sum[i]/n;sd[i]=Math.sqrt(Math.max(0,square[i]/n-mean[i]*mean[i]));}
            return Map.of("positions",n,"mean",mean,"standardDeviation",sd,"min",min,"max",max,"argmaxCounts",largest,"meanEntropyNats",entropy/n);
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("MODEL_PLAN BT4_JSONL CP_JSONL NEW_OUTPUT");
        Path output=Path.of(args[3]);if(Files.exists(output))throw new IllegalArgumentException("Preserve prior evidence");
        var plan=DataFiles.read(Path.of(args[0]),BrnSuccessorAudit.Plan.class);
        var spec=plan.models().stream().filter(m->m.name().equals("W1-GATE")).findFirst().orElseThrow();
        var snapshot=CheckpointStore.readSnapshot(Path.of(spec.root()),spec.checkpoint());
        var model=((NetworkModel.BrnNext)snapshot.model()).model();
        var datasets=new ArrayList<List<Row>>();var inputs=new TreeMap<String,String>();
        var statistics=new TreeMap<String,Stats>();double[] mean=new double[4];
        for(int source=1;source<=2;source++) {
            Path input=Path.of(args[source]);String text=Files.readString(input);inputs.put(input.toString(),DataFiles.hash(text));
            var rows=new ArrayList<Row>();
            for(String line:text.lines().toList()) {
                var json=JsonParser.parseString(line).getAsJsonObject();var board=Board.fromFen(json.get("fen").getAsString());
                double[] g=gates(model,board);rows.add(new Row(board,json.get("target").getAsDouble(),g));
                int material=NnueCorpusTargets.material(board);
                statistics.computeIfAbsent((source==1?"BT4":"CP")+":"+(material<=30?"material<=30":material<=55?"material31..55":"material>55"),k->new Stats()).add(g);
            }
            if(rows.size()!=2048)throw new IllegalArgumentException("Expected2048 frozen rows per source");
            for(int i=0;i<1024;i++)for(int r=0;r<4;r++)mean[r]+=rows.get(i).gates()[r]/2048;
            datasets.add(rows);
        }
        double[] weights=new double[GATED_PAIRS.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=model.weight(i);
        Arrays.fill(weights,EMBEDDING,GATED_PAIRS.head(),0);
        for(int r=0;r<4;r++)weights[GATED_PAIRS.head()+r]=mean[r]==0?-1000:Math.log(mean[r]);
        var constant=new BrnNextModel(GATED_PAIRS,weights);
        var fullWorkspace=model.newWorkspace();var constantWorkspace=constant.newWorkspace();
        var results=new ArrayList<Object>();
        for(int s=0;s<2;s++) {
            var rows=datasets.get(s);
            for(String variant:List.of("full","learned-mean"))for(double gain:new double[]{.25,1}) {
                double ce=0,mse=0;
                for(int i=1024;i<rows.size();i++) {
                    var row=rows.get(i);double material=BrnPair2Features.material(row.board());
                    double raw=variant.equals("full")?model.rawPawns(row.board(),fullWorkspace):constant.rawPawns(row.board(),constantWorkspace);
                    raw=material+gain*(raw-material);int amount=NnueCorpusTargets.material(row.board());
                    ce+=Brn3Objective.crossEntropy(raw,amount,row.target())[0];
                    double error=Brn3Objective.smoothOutcome(raw,amount)[0]-row.target();mse+=error*error;
                }
                results.add(Map.of("source",s==0?"BT4":"CP","variant",variant,"gain",gain,"positions",1024,"crossEntropy",ce/1024,"smoothOutcomeMse",mse/1024));
            }
        }
        var distribution=new TreeMap<String,Object>();statistics.forEach((k,v)->distribution.put(k,v.report()));
        DataFiles.write(output,Map.of("plan","brn-successor-wave2-gate-context-v1","manifest",snapshot.manifest(),
                "inputs",inputs,"meanFitPositions",2048,"meanGates",mean,"gateDistribution",distribution,"evaluation",results));
        System.out.println("Completed constant learned-mean gate control: "+output);
    }
    private static double[] gates(BrnNextModel model,long[] board) {
        int side=Board.player((int)board[4]),head=GATED_PAIRS.head();double[] g=new double[4];
        for(int square=0;square<64;square++) {
            int code=Board.getSquare(board[0],board[1],board[2],board[3],square^(side*56));
            if((code&7)==0)continue;
            int category=(code&7)+(((code>>>3)^side)&1)*6,feature=(category-1)*64+square;
            for(int r=0;r<4;r++)g[r]+=model.weight(EMBEDDING+feature*4+r);
        }
        double maximum=Double.NEGATIVE_INFINITY,total=0;
        for(int r=0;r<4;r++){g[r]=g[r]*PIECE_NORM+model.weight(head+r);maximum=Math.max(maximum,g[r]);}
        for(int r=0;r<4;r++){g[r]=Math.exp(g[r]-maximum);total+=g[r];}
        for(int r=0;r<4;r++)g[r]/=total;
        return g;
    }
    private BrnGatedContextProbe() {}
}
