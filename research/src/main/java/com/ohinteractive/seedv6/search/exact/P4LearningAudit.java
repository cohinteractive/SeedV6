package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Small prospective stratified samples of ordinary LMR decisions, replayed with
 * the independent recursive TT-off/unpruned reference. Selective disagreement
 * is not automatically a defect, and these samples are not a strength endpoint. */
public final class P4LearningAudit {
    static final class Samples implements SelectivityTrace {
        final int[][] selected=new int[6][3];
        final List<Cp64SelectivityLab.Sample> samples=new ArrayList<>();
        @Override public void reduction(long[] b,long move,long[] path,int ply,int depth,int ordinal,int quiets,
                int alpha,int beta,int h,int c,int raw,int previousRaw,int base,int assigned,boolean caution) {
            if(base==0 || assigned==0 || depth<3 || depth>5 || ply==0
                    || alpha < -ExactSearch.MAX_STATIC_SCORE || beta>ExactSearch.MAX_STATIC_SCORE) return;
            int evidence=h+c/2,band=evidence<=-32?0:evidence>=32?2:1;
            if(selected[depth][band]++!=0)return; // First event per root/depth/band, never outcome-selected.
            samples.add(new Cp64SelectivityLab.Sample("history-band-"+band,b.clone(),move,
                    Arrays.copyOfRange(path,1,ply+1),ply,depth,alpha,beta,raw,h,c,assigned));
        }
    }
    /** model corpus NEW-output profile; fixed collection bounds and reference budgets. */
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("model corpus NEW-output profile");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        var config=P4LearningStudy.flags(args[3],1);var gson=new GsonBuilder().create();int count=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","args",args,"flags",config.flags(),
                    "modelSha256",ConventionalNnueEngine.SHA256,"reference","independent recursive TT-off unpruned qtree",
                    "selection","first event per root/depth3..5/evidence band, actual assigned reduction>0, ordinary domain",
                    "limits","Original real history replayed; independent rerooted search lacks selective extension/order/TT context. Disagreement is not automatically a bug.")));
            out.newLine();out.flush();
            for(var root:roots) {
                var samples=new Samples();var adapter=ExactSearchAdapter.constructionResearch(model.evaluation(),new TTable(64),config);
                var exact=Cp64SelectivityLab.exact(adapter);exact.withSelectivityTrace(samples);
                try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(new AspirationSearch(adapter,AspirationSearch.Policy.ADAPTIVE16)))) {
                    var measurement=ConstructionTtStudy.request(driver,root,8,200000);
                    out.write(gson.toJson(Map.of("type","collection","root",root.id(),"measurement",measurement,
                            "samples",samples.samples.size())));out.newLine();out.flush();
                }
                for(var sample:samples.samples) {
                    var context=Cp64SelectivityProbe.context(root,sample);
                    var child=ExhaustiveOracle.child(context.board(),sample.move());
                    var history=GameHistory.builder(context.history());history.appendPosition(child);
                    var reduced=Cp64SelectivityProbe.reference(model,child,history.snapshot(),sample.depth()-1-sample.reduction(),
                            -sample.alpha()-1,-sample.alpha(),50000,true);
                    var nominal=Cp64SelectivityProbe.reference(model,child,history.snapshot(),sample.depth()-1,
                            -sample.alpha()-1,-sample.alpha(),200000,true);
                    out.write(gson.toJson(Map.of("type","sample","root",root.id(),"sample",sample,
                            "reduced",reduced,"nominal",nominal)));out.newLine();out.flush();count++;
                }
            }
            if(count==0)throw new AssertionError("No actual reduced samples");
            out.write(gson.toJson(Map.of("status","passed","samples",count)));out.newLine();
        }
    }
    private P4LearningAudit() {}
}
