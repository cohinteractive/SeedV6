package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Follow-up audits on frozen invocations, without changing their selection. */
public final class Cp64SelectivityAudit {
    static final Gson GSON=new GsonBuilder().create();
    static final class Pending {
        final int index;final Cp64SelectivityLab.Sample sample;
        boolean entered,completed;long start,nodes;int score;
        Pending(int i,Cp64SelectivityLab.Sample s){index=i;sample=s;}
        boolean matches(long[] b,long[] path,int ply,int depth) {
            return sample.ply()==ply && sample.depth()==depth && Arrays.equals(sample.board(),b)
                    && Arrays.equals(sample.path(),Arrays.copyOfRange(path,1,ply+1));
        }
    }
    static void nullCosts(ConventionalNnueEngine model,List<P3HistoryStudy.Root> roots,Path input,java.io.BufferedWriter out) throws Exception {
        var rows=Files.readAllLines(input).stream().map(x->JsonParser.parseString(x).getAsJsonObject()).toList();
        for(var root:roots.subList(0,12)) {
            var targets=new ArrayList<Pending>();
            for(var row:rows)if(row.has("type") && row.get("type").getAsString().equals("probe") && row.get("id").getAsString().equals(root.id())) {
                var sample=GSON.fromJson(row.get("sample"),Cp64SelectivityLab.Sample.class);
                if(sample.kind().equals("null-opportunity"))targets.add(new Pending(row.get("sampleIndex").getAsInt(),sample));
            }
            if(targets.size()!=4)throw new AssertionError("Frozen target selection");
            var cfg=Cp64SelectivityLab.configuration("baseline",1,64);
            var adapter=ExactSearchAdapter.constructionResearch(model.evaluation(),new TTable(64),cfg.mechanisms());
            var exact=Cp64SelectivityLab.exact(adapter);
            exact.withSelectivityTrace(new SelectivityTrace(){
                @Override public void survivor(long[] b,long[] path,int ply,int depth,int a,int beta,boolean scout,boolean checked,int raw) {
                    for(var t:targets)if(!t.entered && t.matches(b,path,ply,depth) && t.sample.alpha()==a && t.sample.beta()==beta) {
                        if(raw!=t.sample.raw())throw new AssertionError("Raw mismatch");t.entered=true;t.start=exact.visitedNodes();
                    }
                }
                @Override public void completed(long[] b,long[] path,int ply,int depth,int score,long nodes) {
                    for(var t:targets)if(t.entered && !t.completed && t.matches(b,path,ply,depth)) {
                        t.completed=true;t.nodes=nodes-t.start+1;t.score=score;
                    }
                }
            });
            try(var driver=new SearchDriver(new AspirationSearch(adapter,cfg.aspiration()))) {
                var result=ConstructionTtAdaptiveStudy.request(driver,root,8,500000);
                out.write(GSON.toJson(Map.of("type","null-cost","id",root.id(),"request",result,"targets",targets)));out.newLine();out.flush();
            }
        }
    }
    static void omissions(ConventionalNnueEngine model,List<P3HistoryStudy.Root> roots,Path input,java.io.BufferedWriter out) throws Exception {
        int count=0;
        for(String line:Files.readAllLines(input)) {
            var row=JsonParser.parseString(line).getAsJsonObject();
            if(!row.has("type") || !row.get("type").getAsString().equals("observation"))continue;
            String id=row.get("id").getAsString(),profile=row.get("profile").getAsString();
            var root=roots.stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
            for(var element:row.getAsJsonObject("opportunities").getAsJsonArray("omittedSamples")) {
                var e=element.getAsJsonObject();if(e.get("event").getAsLong()!=1)continue;
                var sample=GSON.fromJson(e.get("sample"),Cp64SelectivityLab.Sample.class);
                var context=Cp64SelectivityProbe.context(root,sample);
                var child=ExhaustiveOracle.child(context.board(),sample.move());
                var h=GameHistory.builder(context.history());h.appendPosition(child);
                var reduced=Cp64SelectivityProbe.reference(model,child,h.snapshot(),sample.depth()-1-sample.reduction(),
                        -sample.alpha()-1,-sample.alpha(),250000,true);
                var nominal=Cp64SelectivityProbe.reference(model,child,h.snapshot(),sample.depth()-1,
                        -sample.alpha()-1,-sample.alpha(),1000000,true);
                out.write(GSON.toJson(Map.of("type","omission-audit","id",id,"profile",profile,"omission",e,
                        "reduced",reduced,"nominal",nominal)));out.newLine();out.flush();count++;
            }
        }
        if(count==0)throw new AssertionError("No omissions selected");
    }
    /** mode model corpus input NEW-output */
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("mode model corpus input NEW-output");
        var model=ConventionalNnueEngine.load(Path.of(args[1]));var roots=P3HistoryStudy.roots(Path.of(args[2]));
        try(var out=Files.newBufferedWriter(Path.of(args[4]),StandardOpenOption.CREATE_NEW)) {
            out.write(GSON.toJson(Map.of("type","identity","arguments",args,"networkSha256",ConventionalNnueEngine.SHA256)));out.newLine();
            if(args[0].equals("null-cost"))nullCosts(model,roots,Path.of(args[3]),out);
            else if(args[0].equals("omissions"))omissions(model,roots,Path.of(args[3]),out);
            else throw new IllegalArgumentException("Unknown mode");
            out.write(GSON.toJson(Map.of("status","passed")));out.newLine();
        }
    }
    private Cp64SelectivityAudit() {}
}
