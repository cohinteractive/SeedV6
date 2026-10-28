package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import java.util.*;

/** Exact symbolic support of factorized PAIR and COMPOSE as board functions. */
public final class MessageCapacityAudit {
    static void add(Map<List<Integer>,Integer> terms,int sign,Integer... indices) {
        Arrays.sort(indices);terms.merge(List.of(indices),sign,Integer::sum);
    }
    static Map<List<Integer>,Integer> terms(RelationalMessages.Features f,boolean compose) {
        var result=new HashMap<List<Integer>,Integer>();add(result,1,768);
        for(int i=0;i<f.size;i++)add(result,1,f.nodes[i]);
        for(int e=0;e<f.edges;e++) {
            int a=RelationalMessages.E+f.nodes[f.from[e]]*8,b=RelationalMessages.R+f.relation[e]*8,t=f.to[e];
            add(result,1,a,b,RelationalMessages.Q+f.nodes[t]*8);
            if(compose)for(int other=0;other<e;other++)if(f.to[other]==t) {
                if(f.from[other]==f.from[e])throw new IllegalArgumentException("Duplicate directed source");
                add(result,1,a,b,RelationalMessages.E+f.nodes[f.from[other]]*8,RelationalMessages.R+f.relation[other]*8,RelationalMessages.C+f.nodes[t]*8);
            }
        }
        return result;
    }
    static double evaluate(Map<List<Integer>,Integer> terms,double[] weights) {
        double value=0;
        for(var entry:terms.entrySet()) {
            var ids=entry.getKey();if(ids.size()==1){value+=entry.getValue()*weights[ids.getFirst()];continue;}
            for(int d=0;d<8;d++){double product=entry.getValue()/16.0;for(int i:ids)product*=weights[i+d];value+=product;}
        }
        return value;
    }
    static int unmatched(long[][] boards,boolean compose) {
        var combined=new HashMap<List<Integer>,Integer>();var workspace=new RelationalMessages.Workspace();double material=0;
        for(int n=0;n<4;n++) {
            var features=workspace.encode(boards[n]);material+=InteractionAudit.SIGNS[n]*features.material;
            for(var entry:terms(features,compose).entrySet())combined.merge(entry.getKey(),InteractionAudit.SIGNS[n]*entry.getValue(),Integer::sum);
        }
        if(Math.abs(material)>1e-10)throw new IllegalArgumentException("Material contrast changed");
        return (int)combined.values().stream().filter(x->x!=0).count();
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();var config=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();Path cases=Path.of(config.get("cases").getAsString());
        if(!PairObjectiveDiagnostic.sha(cases).equals(config.get("cases_sha256").getAsString()))throw new IllegalArgumentException("Fixture identity");
        var result=new ArrayList<Object>();
        for(var item:JsonParser.parseString(Files.readString(cases)).getAsJsonArray()) {
            var row=item.getAsJsonObject();long[][] boards=new long[4][];for(int i=0;i<4;i++)boards[i]=Board.fromFen(row.getAsJsonArray("fens").get(i).getAsString());
            result.add(Map.of("source_row",row.get("source_row").getAsInt(),"context_changed",row.get("context_changed").getAsBoolean(),
                "pair_unmatched_monomials",unmatched(boards,false),"compose_unmatched_monomials",unmatched(boards,true)));
        }
        Path out=Path.of(config.get("output").getAsString());Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("cases",result,
            "config_sha256",PairObjectiveDiagnostic.sha(Path.of(args[0])),"scope","Exact board-function support,not learned response quality or playing strength"))+"\n",StandardOpenOption.CREATE_NEW);
        System.out.println("Completed "+result.size()+" symbolic message-support quartets");
    }
    private MessageCapacityAudit(){}
}
