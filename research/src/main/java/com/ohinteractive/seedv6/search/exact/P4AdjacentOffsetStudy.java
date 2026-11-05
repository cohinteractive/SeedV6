package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** IC-R001 bounded observation of the initial-depth-offset owner, not worker voting.
 * Exact96 core and C4 memory flags; the control remains the ordinary C4 factory. */
public final class P4AdjacentOffsetStudy {
    public static void main(String[] args) throws Exception {
        if(args.length!=8) throw new IllegalArgumentException("model roots output rounds depth cap count formulation");
        int rounds=Integer.parseInt(args[3]), depth=Integer.parseInt(args[4]), count=Integer.parseInt(args[6]);
        long cap=Long.parseLong(args[5]);
        boolean independent=args[7].equals("ADJACENT_OFFSET_MEMORY");
        if(!independent && !args[7].equals("CONTROL")) throw new IllegalArgumentException("Formulation");
        if(rounds<1 || rounds>4 || depth<3 || depth>16 || cap<1 || cap>1_000_000 || count<1 || count>40)
            throw new IllegalArgumentException("Bounded daytime study only");
        var roots=P3HistoryStudy.roots(Path.of(args[1]));
        if(count>roots.size()) throw new IllegalArgumentException("Root count");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));
        var configuration=BaselineEngine.configuration(4,64,BaselineEngine.SearchProfile.MW_C4_W4);
        if(configuration.mechanisms().flags()!=1182007295) throw new AssertionError("Exact C4 memory flags required");
        var gson=new GsonBuilder().create();long total=0;int requests=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","coreSource",ApplicationVersion.load().sourceRevision(),
                    "coreBuild",ApplicationVersion.load().build(),"formulation",args[7],"flags",configuration.mechanisms().flags(),
                    "configuration","W4/CP64/Hash64/PVS/ADAPTIVE16/BOUNDARY2; "+(independent?"ADJACENT_OFFSET_OWNER":"EAGER_ROOT"),
                    "networkSha256",ConventionalNnueEngine.SHA256,"args",args,
                    "scope","Fixed-work observations; helper progress is correlated and not a selected result or confidence.")));
            out.newLine();out.flush();
            for(int round=0;round<rounds;round++) for(int i=0;i<count;i++) {
                var root=roots.get(round%2==0?i:count-1-i);var saved=root.board().clone();
                IterativeSearch search=independent
                        ?P4AdjacentOffsetSearch.constructionResearch(4,model.evaluation(),new TTable(64),configuration.mechanisms(),
                                AspirationSearch.Policy.ADAPTIVE16,ClockPolicy.BOUNDARY2)
                        :new SearchDriver(ClockPolicy.BOUNDARY2.apply(configuration.create(model.evaluation())));
                try(search) {
                    var measurement=ConstructionTtStudy.request(search,root,depth,cap);
                    var outcome=(SearchDriverOutcome)measurement.get("outcome");
                    if(!Arrays.equals(saved,root.board())) throw new AssertionError("Root restoration");
                    var row=new LinkedHashMap<String,Object>();
                    row.put("type","request");row.put("root",root.id());row.put("round",round);row.put("measurement",measurement);
                    if(search instanceof P4AdjacentOffsetSearch owner) {
                        var progress=owner.workerProgress();
                        for(var worker:progress) if(worker.firstAttemptedDepth()!=0
                                && worker.firstAttemptedDepth()!=Math.min(depth,worker.worker()+1))
                            throw new AssertionError("Actual initial depth provenance");
                        if(Arrays.stream(progress).mapToLong(P4AdjacentOffsetSearch.WorkerProgress::nodes).sum()!=outcome.nodes())
                            throw new AssertionError("Private/global admissions");
                        for(var worker:progress) if(worker.completedResult()!=null) {
                            var result=worker.completedResult();
                            if(!result.completed() || result.depth()!=worker.completedDepth() || result.nodes()>worker.nodes())
                                throw new AssertionError("Worker completion provenance");
                            Phase2Oracle.legalPv(root.board(),result.principalVariation());
                        }
                        var result=outcome.lastCompletedResult();
                        if(result!=null && (result.depth()!=progress[0].completedDepth()
                                || result.bestMove()!=progress[0].completedResult().bestMove()
                                || result.score()!=progress[0].completedResult().score()))
                            throw new AssertionError("Owner-only publication");
                        row.put("workerProgress",progress);
                    }
                    total+=outcome.nodes();requests++;out.write(gson.toJson(row));out.newLine();out.flush();
                }
            }
            long helpers=Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive()
                    && (t.getName().equals("seedv6-search-helper") || t.getName().equals("seedv6-independent-search"))).count();
            if(helpers!=0) throw new AssertionError("Helpers after close");
            out.write(gson.toJson(Map.of("type","summary","status","passed","requests",requests,"nodes",total,
                    "activeHelpersAfterClose",helpers)));out.newLine();
        }
    }
    private P4AdjacentOffsetStudy() {}
}
