package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;

/** Isolated counterfactual probes of frozen opportunities, not a flat-kernel policy.
 * Uses retained synthetic-domain machinery and independent unpruned legal references. */
public final class Cp64SelectivityProbe {
    record Context(long[] board,GameHistory history,long[] parent,GameHistory parentHistory,long lastMove) {}
    record Reference(ExactSearchResult result,int parentRelativeScore,long elapsedNanos) {}
    static Context context(P3HistoryStudy.Root root,Cp64SelectivityLab.Sample sample) {
        long[] b=root.board(),parent=null;var h=GameHistory.builder(root.history());GameHistory prior=null;long last=0;
        if(sample.path().length!=sample.ply())throw new AssertionError("Path length");
        for(long move:sample.path()){parent=b;prior=h.snapshot();last=move;b=ExhaustiveOracle.child(b,move);h.appendPosition(b);}
        if(!Arrays.equals(b,sample.board()) || parent==null)throw new AssertionError("Real path replay");
        return new Context(b,h.snapshot(),parent,prior,last);
    }
    static Reference reference(ConventionalNnueEngine model,long[] b,GameHistory h,int depth,int alpha,int beta,long budget,boolean negate) {
        var q=ExactSearch.quiescenceResearch(ExactEvaluator.from(model.evaluation()));long start=System.nanoTime();
        var r=q.searchWindow(b,h,depth,alpha,beta,()->q.visitedNodes()>=budget);
        Phase2Oracle.legalPv(b,r.principalVariation());
        return new Reference(r,negate?-r.score():r.score(),System.nanoTime()-start);
    }
    static Map<String,Object> probe(ConventionalNnueEngine model,Context c,Cp64SelectivityLab.Sample s,long budget) {
        boolean[] stop={false};int[] calls={0},prediction={0},reduction={0},evaluation={0};
        var diagnostics=new NullMovePolicy.Diagnostics(){
            @Override public void probe(long[] b,int ply,int depth,int beta,int raw,int r,int score,boolean accepted) {
                if(ply!=1 || depth!=s.depth() || beta!=s.beta() || !Arrays.equals(b,s.board()))
                    throw new AssertionError("Unexpected null target");
                calls[0]++;prediction[0]=score;reduction[0]=r;evaluation[0]=raw;stop[0]=true;
            }
        };
        var policy=s.depth()==3?NullMovePolicy.SHALLOW:NullMovePolicy.ADAPTIVE;
        var owner=ExactSearch.nullMoveResearch(ExactEvaluator.from(model.evaluation()),null,policy,diagnostics);
        long start=System.nanoTime();
        var result=owner.searchRootMove(c.parent(),c.parentHistory(),s.depth()+1,c.lastMove(),-s.beta(),1-s.beta(),true,
                ()->stop[0] || owner.visitedNodes()>=budget);
        long elapsed=System.nanoTime()-start;
        // Intentional cancellation after the isolated probe prevents an experimental
        // cutoff, completed result or learning publication; only its observation escapes.
        if(result.completed() || calls[0]>1)throw new AssertionError("Probe publication");
        var row=new LinkedHashMap<String,Object>();row.put("policy",policy);row.put("captured",calls[0]==1);
        row.put("prediction",prediction[0]);row.put("reduction",reduction[0]);row.put("raw",evaluation[0]);
        row.put("nodes",result.nodes());row.put("probeNodes",diagnostics.probeNodes);row.put("probeQnodes",diagnostics.probeQnodes);
        row.put("elapsedNanos",elapsed);row.put("incompleteByDesign",calls[0]==1);
        if(!owner.search(c.parent(),c.parentHistory(),1,()->false).completed())throw new AssertionError("Probe owner reuse");
        row.put("drainReuse",true);return row;
    }
    /** model corpus opportunities NEW-output nominalBudget probeBudget */
    public static void main(String[] args) throws Exception {
        if(args.length!=6)throw new IllegalArgumentException("model corpus opportunities NEW-output nominalBudget probeBudget");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        long nominalBudget=Long.parseLong(args[4]),probeBudget=Long.parseLong(args[5]);
        if(nominalBudget<1 || probeBudget<1)throw new IllegalArgumentException("Budgets");
        var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[3]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,"networkSha256",ConventionalNnueEngine.SHA256,
                "reference","independent recursive TT-off unpruned nominal qtree; original history, rerooted mate distance",
                "limits","Ordinary thresholds only; selective extension/history/TT context is not recreated. Probes are not completed search results or strength evidence.")));
            out.newLine();out.flush();int count=0;
            for(String line:Files.readAllLines(Path.of(args[2]))) {
                var row=JsonParser.parseString(line).getAsJsonObject();
                if(!row.has("type") || !row.get("type").getAsString().equals("observation")
                        || !row.get("profile").getAsString().equals("baseline"))continue;
                String id=row.get("id").getAsString();var root=roots.stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
                var selected=new HashSet<String>();int index=0;
                for(var element:row.getAsJsonObject("opportunities").getAsJsonArray("samples")) {
                    int sampleIndex=index++;var sample=gson.fromJson(element,Cp64SelectivityLab.Sample.class);
                    boolean nullProbe=sample.kind().equals("null-opportunity");
                    if(sample.depth()<3 || sample.depth()>(nullProbe?6:5) || !selected.add(sample.kind()+sample.depth()))continue;
                    var c=context(root,sample);var result=new LinkedHashMap<String,Object>();
                    result.put("type","probe");result.put("id",id);result.put("sampleIndex",sampleIndex);result.put("sample",sample);
                    if(nullProbe) {
                        result.put("null",probe(model,c,sample,probeBudget));
                        result.put("verification",reference(model,c.board(),c.history(),sample.depth()-2,
                                sample.beta()-1,sample.beta(),probeBudget,false));
                        result.put("nominal",reference(model,c.board(),c.history(),sample.depth(),
                                sample.beta()-1,sample.beta(),nominalBudget,false));
                    } else {
                        var child=ExhaustiveOracle.child(c.board(),sample.move());var history=GameHistory.builder(c.history());history.appendPosition(child);
                        result.put("reduced",reference(model,child,history.snapshot(),sample.depth()-1-sample.reduction(),
                                -sample.alpha()-1,-sample.alpha(),probeBudget,true));
                        result.put("nominal",reference(model,child,history.snapshot(),sample.depth()-1,
                                -sample.alpha()-1,-sample.alpha(),nominalBudget,true));
                    }
                    out.write(gson.toJson(result));out.newLine();out.flush();count++;
                }
            }
            if(count==0)throw new AssertionError("No eligible samples");
            out.write(gson.toJson(Map.of("status","passed","samples",count)));out.newLine();
        }
    }
    private Cp64SelectivityProbe() {}
}
