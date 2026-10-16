package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Full-window severity and reduction/pass decomposition of captured adverse witnesses. */
public final class Sr018WitnessReplay {
    static Field field(String name) throws Exception {var f=ExactSearch.class.getDeclaredField(name);f.setAccessible(true);return f;}
    public static void main(String[] args) throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("Wrong network");
        var def=snapshot.model().evaluation(NnueScoreMapping.V1);var gson=new Gson();var rows=new ArrayList<Object>();
        var ctor=GameHistory.class.getDeclaredConstructor(long[].class,long.class,int.class);ctor.setAccessible(true);
        var invoke=ExactSearch.class.getDeclaredMethod("negamax",int.class,int.class,int.class,int.class,long.class,boolean.class);invoke.setAccessible(true);
        var pass=ExactSearch.class.getDeclaredMethod("pairSyntheticPass",long[].class,long[].class);pass.setAccessible(true);
        for(String line:Files.readAllLines(Path.of(args[1]))) {
            var row=JsonParser.parseString(line).getAsJsonObject();if(!row.has("falseCutoff")||!row.get("falseCutoff").getAsBoolean())continue;
            var sample=row.getAsJsonObject("sample");var b=gson.fromJson(sample.get("board"),long[].class);
            var keys=gson.fromJson(sample.get("history"),long[].class);var history=ctor.newInstance(keys,b[5],(int)b[4]);history.requireCurrent(b);
            int d=sample.get("depth").getAsInt(),ply=sample.get("ply").getAsInt(),beta=sample.get("beta").getAsInt();
            var record=new LinkedHashMap<String,Object>();record.put("id",row.get("id").getAsString());record.put("captured",sample);
            var ref=ExactSearch.quiescenceResearch(ExactEvaluator.from(def)).search(b,history,d,ExactSearch.NEVER_CANCELLED);
            if(!ref.completed())throw new AssertionError("Incomplete full reference");
            record.put("fullWindowLegal",ref);record.put("originalPlyLegalScore",mate(ref.score(),ply));
            var probes=new ArrayList<Object>();
            for(int reduction=0;reduction<=3;reduction++) {
                var c=new long[6];pass.invoke(null,b,c);var e=ExactSearch.quiescenceResearch(ExactEvaluator.from(def));
                System.arraycopy(c,0,((long[][])field("boards").get(e))[ply+1],0,6);
                var h=new SearchLineHistory(history,256);h.enterSyntheticPosition(c);field("history").set(e,h);
                field("cancelled").set(e,ExactSearch.NEVER_CANCELLED);field("nullProbeActive").set(e,true);
                int value=-(int)invoke.invoke(e,d-1-reduction,ply+1,-ExactSearch.INFINITY,ExactSearch.INFINITY,0L,false);
                if(h.size()!=history.size())throw new AssertionError("Unbalanced history");h.leaveSyntheticPosition();
                probes.add(Map.of("reduction",reduction,"syntheticDepth",d-1-reduction,"fullWindowNullScore",value,"failHigh",value>=beta,"nodes",field("nodes").get(e)));
            }
            record.put("isolatedProbes",probes);rows.add(record);
        }
        Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status","passed","witnesses",rows))+"\n",StandardOpenOption.CREATE_NEW);
        System.out.println("Full-window replay completed for "+rows.size()+" witnesses");
    }
    static int mate(int score,int ply){return score>ExactSearch.MAX_STATIC_SCORE?score-ply:score < -ExactSearch.MAX_STATIC_SCORE?score+ply:score;}
}
