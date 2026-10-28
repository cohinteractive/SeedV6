package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import java.nio.file.*;
import java.util.*;
import static com.ohinteractive.seedv6.core.brnpair2.AttentivePieceNetwork.*;

/** Frozen routing decomposition and score ablations; never modifies a trained model. */
public final class RoutingMechanismAudit {
    static final class Summary {
        long count,small;double sum,absolute,squares,minimum=Double.POSITIVE_INFINITY,maximum=Double.NEGATIVE_INFINITY;
        void add(double x){if(!Double.isFinite(x))throw new ArithmeticException("Nonfinite audit metric");count++;sum+=x;absolute+=Math.abs(x);squares+=x*x;minimum=Math.min(minimum,x);maximum=Math.max(maximum,x);if(Math.abs(x)<.1)small++;}
        Map<String,Object> result(){return Map.of("count",count,"mean",sum/count,"mean_abs",absolute/count,"rms",Math.sqrt(squares/count),"minimum",minimum,"maximum",maximum,"fraction_abs_lt_point1",(double)small/count);}
    }
    static final class Report {
        final Map<String,Summary> metrics=new TreeMap<>();long rows;final double[] loss=new double[3];double maximumFullError,maximumGeometryError;
        void add(String name,double x){metrics.computeIfAbsent(name,k->new Summary()).add(x);}
        Map<String,Object> result(){var stats=new TreeMap<String,Object>();metrics.forEach((k,v)->stats.put(k,v.result()));return Map.of("rows",rows,"full_teacher_mse",loss[0]/rows,"geometry_only_teacher_mse",loss[1]/rows,"static_content_teacher_mse",loss[2]/rows,"maximum_full_reconstruction_error",maximumFullError,"maximum_geometry_reconstruction_error",maximumGeometryError,"metrics",stats);}
    }
    static double[] softmax(double[] scores,int skip) {
        var alpha=new double[scores.length];double max=Double.NEGATIVE_INFINITY,total=0;
        for(int j=0;j<scores.length;j++)if(j!=skip)max=Math.max(max,scores[j]);
        for(int j=0;j<scores.length;j++)if(j!=skip){alpha[j]=Math.exp(scores[j]-max);total+=alpha[j];}
        for(int j=0;j<scores.length;j++)if(j!=skip)alpha[j]/=total;return alpha;
    }
    static double centeredRms(double[] a,int skip) {
        double mean=0,squares=0;int count=a.length-(skip>=0?1:0);if(count==0)return 0;
        for(int j=0;j<a.length;j++)if(j!=skip)mean+=a[j];mean/=count;
        for(int j=0;j<a.length;j++)if(j!=skip)squares+=(a[j]-mean)*(a[j]-mean);return Math.sqrt(squares/count);
    }
    static double[] inspect(AttentivePieceNetwork m,long[] board,double target,Report report) {
        if(m.attention!=2)throw new IllegalArgumentException("Frozen DYNAMIC required");
        var w=new Workspace();var f=w.encode(board);double actual=m.logit(f,w),base=w.baseLogit;double[] reconstructed={base,base,base};
        for(int k=0;k<f.size*WIDTH;k++) {
            report.add("first_preactivation",w.a[k]);report.add("first_activation",w.h[k]);report.add("first_softsign_derivative",derivative(w.a[k]));
            report.add("second_preactivation",w.b[k]);report.add("second_activation",activate(w.b[k]));report.add("second_softsign_derivative",derivative(w.b[k]));
        }
        report.add("unary_logit",m.unary(f));report.add("first_pass_logit_contribution",base-m.unary(f));report.add("second_pass_logit_contribution",actual-base);
        for(int i=0;i<f.size;i++) {
            double[] geometry=new double[f.size],staticPart=new double[f.size],context=new double[f.size],full=new double[f.size],staticScore=new double[f.size];
            for(int j=0;j<f.size;j++)if(j!=i) {
                geometry[j]=m.weights[B+f.relations[j*f.size+i]];
                for(int d=0;d<WIDTH;d++) {
                    double ei=m.weights[E+f.nodes[i]*WIDTH+d],ej=m.weights[E+f.nodes[j]*WIDTH+d],hi=w.h[i*WIDTH+d],hj=w.h[j*WIDTH+d],k=m.weights[K+d]/Math.sqrt(WIDTH);
                    staticPart[j]+=k*ei*ej;context[j]+=k*(ei*hj+hi*ej+hi*hj);
                }
                staticScore[j]=geometry[j]+staticPart[j];full[j]=staticScore[j]+context[j];
            }
            report.add("geometry_centered_rms",centeredRms(geometry,i));report.add("static_content_centered_rms",centeredRms(staticPart,i));report.add("context_centered_rms",centeredRms(context,i));
            double[][] alpha={softmax(full,i),softmax(geometry,i),softmax(staticScore,i)};double entropy=0,allL1=0,contextL1=0;
            for(int j=0;j<f.size;j++)if(j!=i){if(alpha[0][j]>0)entropy-=alpha[0][j]*Math.log(alpha[0][j]);allL1+=Math.abs(alpha[0][j]-alpha[1][j]);contextL1+=Math.abs(alpha[0][j]-alpha[2][j]);}
            report.add("normalized_entropy",f.size>2?entropy/Math.log(f.size-1):1);report.add("remove_all_content_attention_l1",allL1);report.add("remove_neighborhood_attention_l1",contextL1);
            for(int mode=0;mode<3;mode++)for(int d=0;d<WIDTH;d++) {
                double z=0;for(int j=0;j<f.size;j++)if(j!=i)z+=Math.sqrt(f.size-1)*alpha[mode][j]*w.h[j*WIDTH+d]*m.weights[S+f.relations[j*f.size+i]*WIDTH+d];
                reconstructed[mode]+=m.weights[V+f.nodes[i]*WIDTH+d]*activate(z)/WIDTH;
            }
        }
        double geometry=m.logit(f,w,1),fullError=Math.abs(actual-reconstructed[0]),geometryError=Math.abs(geometry-reconstructed[1]);
        if(fullError>1e-12||geometryError>1e-12)throw new IllegalStateException("Routing reconstruction mismatch");
        report.maximumFullError=Math.max(report.maximumFullError,fullError);report.maximumGeometryError=Math.max(report.maximumGeometryError,geometryError);report.rows++;
        for(int mode=0;mode<3;mode++){double error=PairObjectiveDiagnostic.sigmoid(reconstructed[mode])-target;report.loss[mode]+=error*error;}return reconstructed;
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        for(String key:List.of("cases","played_cases","played_prediction","teacher"))if(!PairCoverageDiagnostic.sha(Path.of(config.get(key).getAsString())).equals(config.get(key+"_sha256").getAsString()))throw new IllegalArgumentException("Input identity "+key);
        var old=PairCoverageDiagnostic.Data.read(Path.of(config.get("cases").getAsString()),4096);var teacher=ConventionalNetwork.load(Path.of(config.get("teacher").getAsString()));long[] board=new long[PairCoverageDiagnostic.Data.STRIDE];
        for(int i=0;i<old.rows;i++){old.board(i,board);if(Math.abs(old.targets[i]-PairObjectiveDiagnostic.sigmoid(teacher.fullRebuild(board)/400.0))>1e-15)throw new IllegalArgumentException("Old teacher targets");}
        var positions=JsonParser.parseString(Files.readString(Path.of(config.get("played_cases").getAsString()))).getAsJsonObject().getAsJsonArray("positions");
        var played=JsonParser.parseString(Files.readString(Path.of(config.get("played_prediction").getAsString()))).getAsJsonObject();if(positions.size()!=991||played.get("rows").getAsInt()!=991)throw new IllegalArgumentException("Played rows");
        long[][] boards=new long[991][];double[] targets=new double[991];boolean[] check=new boolean[991];
        for(int i=0;i<991;i++){var p=positions.get(i).getAsJsonObject();boards[i]=Board.fromFen(p.get("fen").getAsString());int cp=teacher.fullRebuild(boards[i]);targets[i]=PairObjectiveDiagnostic.sigmoid(cp/400.0);check[i]=p.get("in_check").getAsBoolean();if(cp!=played.getAsJsonArray("teacher_cp").get(i).getAsInt()||targets[i]!=played.getAsJsonArray("teacher_targets").get(i).getAsDouble()||check[i]!=played.getAsJsonArray("in_check").get(i).getAsBoolean())throw new IllegalArgumentException("Played teacher/row parity");}
        var models=new ArrayList<Object>();long started=System.nanoTime();
        for(var element:config.getAsJsonArray("models")) {
            var spec=element.getAsJsonObject();Path state=Path.of(spec.get("state").getAsString());String hash=PairCoverageDiagnostic.sha(state);if(!hash.equals(spec.get("state_sha256").getAsString()))throw new IllegalArgumentException("State identity");
            AttentivePieceNetwork model;try(var input=Files.newInputStream(state)){model=AttentivePieceNetwork.read(input);}var development=new Report();var all=new Report();var noncheck=new Report();
            for(int i=0;i<old.rows;i++){if(i%512==0)PairObjectiveDiagnostic.permission();old.board(i,board);inspect(model,board,old.targets[i],development);}
            for(int i=0;i<991;i++){inspect(model,boards[i],targets[i],all);if(!check[i])inspect(model,boards[i],targets[i],noncheck);}
            if(noncheck.rows!=876||!hash.equals(PairCoverageDiagnostic.sha(state)))throw new IllegalArgumentException("Rows/state changed");
            models.add(Map.of("id",spec.get("id").getAsString(),"state_sha256",hash,"old_development",development.result(),"played_all",all.result(),"played_noncheck",noncheck.result()));
        }
        var result=Map.of("config_sha256",PairCoverageDiagnostic.sha(configPath),"models",models,"teacher_row_parity",true,"seconds",(System.nanoTime()-started)/1e9,"scope","Frozen consumed-panel mechanism and distribution diagnosis;no fit or strength claim");
        Files.writeString(Path.of(config.get("output").getAsString()),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Completed frozen routing diagnosis for "+models.size()+" models");
    }
    private RoutingMechanismAudit(){}
}
