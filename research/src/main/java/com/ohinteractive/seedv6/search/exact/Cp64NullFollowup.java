package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Fixed all-target late-position challenge before any flattened null integration. */
public final class Cp64NullFollowup {
    static final Gson GSON=new GsonBuilder().create();
    static Cp64SelectivityProbe.Reference selective(ConventionalNnueEngine model,Cp64SelectivityProbe.Context c,Cp64SelectivityLab.Sample s) {
        var owner=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),new TTable(64),FlatSearchConfig.candidate());
        long start=System.nanoTime();var r=owner.searchRootMove(c.parent(),c.parentHistory(),s.depth()-1,c.lastMove(),-s.beta(),1-s.beta(),true,
                ()->owner.visitedNodes()>=200000);long elapsed=System.nanoTime()-start;
        Phase2Oracle.legalPv(c.parent(),r.principalVariation());
        if(!owner.search(c.parent(),c.parentHistory(),1,()->false).completed())throw new AssertionError("Verifier reuse");
        return new Cp64SelectivityProbe.Reference(r,-r.score(),elapsed);
    }
    /** model played-corpus original-corpus original-probes NEW-output */
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("model played-corpus original-corpus original-probes NEW-output");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        try(var out=Files.newBufferedWriter(Path.of(args[4]),StandardOpenOption.CREATE_NEW)) {
            out.write(GSON.toJson(Map.of("type","identity","arguments",args,"networkSha256",ConventionalNnueEngine.SHA256)));out.newLine();
            int samples=0;
            for(var root:roots.subList(0,12)) {
                var cfg=Cp64SelectivityLab.configuration("baseline",1,64);var obs=new Cp64SelectivityLab.Observation();
                var targets=new ArrayList<Cp64SelectivityAudit.Pending>();var depths=new HashSet<Integer>();
                var adapter=ExactSearchAdapter.constructionResearch(model.evaluation(),new TTable(64),cfg.mechanisms());
                var exact=Cp64SelectivityLab.exact(adapter);
                exact.withSelectivityTrace(new SelectivityTrace(){
                    @Override public void survivor(long[] b,long[] path,int ply,int depth,int a,int beta,boolean scout,boolean checked,int raw) {
                        int before=obs.samples.size();obs.survivor(b,path,ply,depth,a,beta,scout,checked,raw);
                        if(obs.samples.size()>before && depth>=3 && depth<=6 && depths.add(depth)) {
                            var p=new Cp64SelectivityAudit.Pending(before,obs.samples.get(before));p.entered=true;p.start=exact.visitedNodes();targets.add(p);
                        }
                    }
                    @Override public void completed(long[] b,long[] path,int ply,int depth,int score,long nodes) {
                        for(var t:targets)if(!t.completed && t.matches(b,path,ply,depth)){t.completed=true;t.nodes=nodes-t.start+1;t.score=score;}
                    }
                });
                try(var plain=new SearchDriver(cfg.create(model.evaluation()));var traced=new SearchDriver(new AspirationSearch(adapter,cfg.aspiration()))) {
                    var a=ConstructionTtAdaptiveStudy.request(plain,root,8,500000);
                    var b=ConstructionTtAdaptiveStudy.request(traced,root,8,500000);
                    ConstructionTtStudy.same((SearchDriverOutcome)a.get("outcome"),(SearchDriverOutcome)b.get("outcome"));
                    out.write(GSON.toJson(Map.of("type","observation","id",root.id(),"plain",a,"traced",b,"neutral",true,
                            "opportunities",obs,"targets",targets)));out.newLine();out.flush();
                }
                for(var t:targets) {
                    var s=t.sample;var c=Cp64SelectivityProbe.context(root,s);var r=new LinkedHashMap<String,Object>();
                    r.put("type","late-null-probe");r.put("id",root.id());r.put("target",t);
                    r.put("null",Cp64SelectivityProbe.probe(model,c,s,200000));
                    r.put("unprunedVerification",Cp64SelectivityProbe.reference(model,c.board(),c.history(),s.depth()-2,s.beta()-1,s.beta(),200000,false));
                    r.put("selectiveVerification",selective(model,c,s));
                    r.put("nominal",Cp64SelectivityProbe.reference(model,c.board(),c.history(),s.depth(),s.beta()-1,s.beta(),5000000,false));
                    out.write(GSON.toJson(r));out.newLine();out.flush();samples++;
                }
            }
            var originalRoots=P3HistoryStudy.roots(Path.of(args[2]));int unresolved=0;
            for(String line:Files.readAllLines(Path.of(args[3]))) {
                var r=JsonParser.parseString(line).getAsJsonObject();
                if(!r.has("type") || !r.get("type").getAsString().equals("probe"))continue;
                var s=GSON.fromJson(r.get("sample"),Cp64SelectivityLab.Sample.class);
                if(!s.kind().equals("null-opportunity") || r.getAsJsonObject("nominal").getAsJsonObject("result").get("completed").getAsBoolean()
                        || r.getAsJsonObject("null").get("prediction").getAsInt()<s.beta())continue;
                String id=r.get("id").getAsString();var root=originalRoots.stream().filter(x->x.id().equals(id)).findFirst().orElseThrow();
                var c=Cp64SelectivityProbe.context(root,s);
                var nominal=Cp64SelectivityProbe.reference(model,c.board(),c.history(),s.depth(),s.beta()-1,s.beta(),5000000,false);
                out.write(GSON.toJson(Map.of("type","prior-unknown-followup","id",id,"sampleIndex",r.get("sampleIndex"),"sample",s,"nominal",nominal)));
                out.newLine();out.flush();unresolved++;
            }
            if(unresolved!=1)throw new AssertionError("Expected one prior accepted unknown");
            out.write(GSON.toJson(Map.of("status","passed","lateSamples",samples,"priorUnknownFollowups",unresolved)));out.newLine();
        }
    }
    private Cp64NullFollowup() {}
}
