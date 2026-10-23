package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;

/** Research-owned CP64 controls over the unchanged shared kernel. Not a production factory. */
public final class Cp64SelectivityStudy {
    static final List<String> PROFILES=List.of("baseline","no-improving","no-caution",
            "no-history-protection","no-futility","no-reverse","no-continuation","no-refutations");

    public static ConstructionSearch configuration(String profile,int workers) {
        var c=FlatSearchConfig.candidate();
        c=switch(profile) {
            case "baseline" -> c;
            case "no-improving" -> c.with(IMPROVING,false);
            case "no-caution" -> c.with(CAUTION,false);
            case "no-history-protection" -> c.with(HISTORY_PROTECTION,false);
            case "no-futility" -> c.with(FUTILITY,false);
            case "no-reverse" -> c.with(REVERSE_FUTILITY,false);
            case "no-continuation" -> c.with(CONTINUATION,false);
            case "no-refutations" -> c.with(KILLERS,false).with(COUNTERMOVES,false);
            default -> throw new IllegalArgumentException("Unknown selectivity profile: "+profile);
        };
        return new ConstructionSearch(c,workers,64,AspirationSearch.Policy.ADAPTIVE16);
    }

    record Sample(boolean reverse,long[] board,long move,int ply,int depth,int alpha,int beta,int raw,long[] path) {}
    record Observed(SingleDepthSearch owner,List<Map<String,Object>> attempts,List<Sample> samples,long[] events) {}
    static Observed observed(SearchEvaluation evaluation,String profile) throws Exception {
        var config=configuration(profile,1);
        var adapter=ExactSearchAdapter.constructionResearch(evaluation,new TTable(64),config.mechanisms());
        var field=ExactSearchAdapter.class.getDeclaredField("exact");field.setAccessible(true);
        var exact=(ExactSearch)field.get(adapter);
        var samples=new ArrayList<Sample>();long[] events={0,0};
        exact.withConstructionTrace((reverse,board,move,ply,depth,alpha,beta,raw,path)->{
            long n=++events[reverse?0:1];
            if(n==1 || n==8 || n==64 || n==512)
                samples.add(new Sample(reverse,board.clone(),move,ply,depth,alpha,beta,raw,
                        Arrays.copyOfRange(path,1,ply+1)));
        });
        var attempts=new ArrayList<Map<String,Object>>();
        var owner=new AspirationSearch(adapter,config.aspiration(),(depth,alpha,beta,result,nanos)->{
            var row=new LinkedHashMap<String,Object>();
            row.put("depth",depth);row.put("alpha",alpha);row.put("beta",beta);row.put("outcome",result);
            row.put("mechanisms",exact.constructionCounters());attempts.add(row);
        });
        return new Observed(owner,attempts,samples,events);
    }

    /** model corpus NEW-output workers warmups rounds depth nodeBudget roots observe|cost profiles */
    public static void main(String[] args) throws Exception {
        if(args.length!=11) throw new IllegalArgumentException("model corpus NEW-output workers warmups rounds depth budget roots observe|cost profiles");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var all=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),warmups=Integer.parseInt(args[4]),rounds=Integer.parseInt(args[5]);
        int depth=Integer.parseInt(args[6]),count=Integer.parseInt(args[8]);long budget=Long.parseLong(args[7]);
        if(warmups<0 || rounds<1 || count<1 || count>all.size() || depth<1 || depth>ExactSearch.MAX_DEPTH || budget<1)
            throw new IllegalArgumentException("Bounds");
        var profiles=List.of(args[10].split(","));
        if(new HashSet<>(profiles).size()!=profiles.size()) throw new IllegalArgumentException("Duplicate profile");
        var configurations=new LinkedHashMap<String,String>();
        for(String p:profiles) configurations.put(p,configuration(p,workers).identity());
        var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,"configuration",configurations,
                    "productionIdentity",BaselineEngine.load().identity(workers,64),
                    "modelSha256",ConventionalNnueEngine.SHA256,"schema",model.model().schema(),
                    "heads",model.model().outputBuckets(),"scoreMapping",ConventionalNnueEngine.SCORE_MAPPING,
                    "java",System.getProperty("java.runtime.version"),"diagnostics",false)));
            out.newLine();out.flush();
            if(args[9].equals("observe")) {
                if(workers!=1 || warmups!=0 || rounds!=1) throw new IllegalArgumentException("Observation domain");
                for(var root:all.subList(0,count)) for(String profile:profiles) {
                    var observed=observed(model.evaluation(),profile);
                    try(var plain=new SearchDriver(configuration(profile,1).create(model.evaluation()));
                        var owner=new SearchDriver(observed.owner())) {
                        var a=ConstructionTtAdaptiveStudy.request(plain,root,depth,budget);
                        var b=ConstructionTtAdaptiveStudy.request(owner,root,depth,budget);
                        ConstructionTtStudy.same((SearchDriverOutcome)a.get("outcome"),(SearchDriverOutcome)b.get("outcome"));
                        long total=observed.attempts().stream().mapToLong(x->((SearchResult)x.get("outcome")).nodes()).sum();
                        if(total!=((SearchDriverOutcome)b.get("outcome")).nodes()) throw new AssertionError("Attempt accounting");
                        var row=new LinkedHashMap<String,Object>();row.put("type","observation");row.put("id",root.id());
                        row.put("profile",profile);row.put("neutral",true);row.put("historySize",root.history().size());
                        row.put("plain",a);row.put("observed",b);row.put("attempts",observed.attempts());
                        row.put("predictionEvents",observed.events());row.put("samples",observed.samples());
                        out.write(gson.toJson(row));out.newLine();out.flush();
                    }
                }
            } else if(args[9].equals("cost")) {
                var drivers=new SearchDriver[profiles.size()];
                try {
                    for(int i=0;i<drivers.length;i++) drivers[i]=new SearchDriver(configuration(profiles.get(i),workers).create(model.evaluation()));
                    for(int round=-warmups;round<rounds;round++) for(int i=0;i<count;i++) for(int offset=0;offset<drivers.length;offset++) {
                        int p=Math.floorMod(round+i+offset,drivers.length);var root=all.get(i);
                        var row=ConstructionTtAdaptiveStudy.request(drivers[p],root,depth,budget);
                        row.put("type","request");row.put("id",root.id());row.put("round",round);row.put("profile",profiles.get(p));
                        out.write(gson.toJson(row));out.newLine();out.flush();
                    }
                } finally {for(var driver:drivers) if(driver!=null) driver.close();}
            } else throw new IllegalArgumentException("Unknown mode");
            out.write(gson.toJson(Map.of("status","passed")));out.newLine();
        }
    }
    private Cp64SelectivityStudy() {}
}
