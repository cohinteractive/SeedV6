package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** TC-R001 bounded tactical verification observations and clean request costs. No strength inference. */
public final class P4TacticalStudy {
    public static FlatSearchConfig flags(String profile,int workers) {
        if(!Set.of("guided","probcut").contains(profile)) throw new IllegalArgumentException("Unknown tactical profile: "+profile);
        return P4LearningStudy.flags("guided",workers)
                .with(FlatSearchConfig.Feature.TACTICAL_VERIFICATION,profile.equals("probcut"));
    }
    static final class Trace implements SelectivityTrace {
        // Signed magnitude buckets: <-4096,-4096..-1025,-1024..-257,-256..-65,-64..-1,
        // zero,1..64,65..256,257..1024,1025..4096,>4096.
        final long[] history=new long[11],continuation=new long[11],combined=new long[11];
        final long[] eligibleHistory=new long[11],eligibleCombined=new long[11],reductions=new long[16];
        long decisions,eligible,omissions;
        static int bucket(int v) {
            if(v==0)return 5;
            int a=Math.abs(v),b=a<=64?1:a<=256?2:a<=1024?3:a<=4096?4:5;
            return 5+Integer.signum(v)*b;
        }
        @Override public void reduction(long[] board,long move,long[] path,int ply,int depth,int ordinal,int quiets,
                int alpha,int beta,int h,int c,int raw,int previousRaw,int base,int assigned,boolean caution) {
            decisions++;history[bucket(h)]++;continuation[bucket(c)]++;combined[bucket(h+c/2)]++;
            reductions[Math.min(15,assigned)]++;
            if(base>0) {eligible++;eligibleHistory[bucket(h)]++;eligibleCombined[bucket(h+c/2)]++;}
        }
        @Override public void omission(long[] board,long move,long[] path,int ply,int depth,int alpha,int beta,
                int raw,int history,int continuation,int assigned,int margin) {omissions++;}
    }
    static List<ExactSearch> kernels(SingleDepthSearch owner,int workers) throws Exception {
        if(workers==1)return List.of((ExactSearch)P4TtFoundationStudy.field(owner,"exact"));
        var result=new ArrayList<ExactSearch>();
        for(Object worker:(Object[])P4TtFoundationStudy.field(owner,"workers"))
            result.add((ExactSearch)P4TtFoundationStudy.field(worker,"search"));
        return result;
    }
    static Map<String,Object> histories(ExactSearch search) throws Exception {
        var s=(FlatSearchState)P4TtFoundationStudy.field(search,"constructionState");
        var quiet=(int[])P4TtFoundationStudy.field(search,"quietHistory");
        return Map.of("quiet",distribution(quiet),"capture",distribution(s.captures),
                "continuation",distribution(s.continuation),"mechanisms",search.constructionCounters());
    }
    static long[] distribution(Object array) {
        long[] bins=new long[11];
        if(array instanceof int[] a)for(int n:a)bins[Trace.bucket(n)]++;
        if(array instanceof short[] a)for(short n:a)bins[Trace.bucket(n)]++;
        return bins;
    }
    static void same(SearchDriverOutcome a,SearchDriverOutcome b) {
        if(a.nodes()!=b.nodes() || a.attemptedDepth()!=b.attemptedDepth()
                || a.iterationIncomplete()!=b.iterationIncomplete() || a.targetDepthCompleted()!=b.targetDepthCompleted())
            throw new AssertionError("Observation changed request work/completion");
        var x=a.lastCompletedResult();var y=b.lastCompletedResult();
        if((x==null)!=(y==null) || x!=null && (x.score()!=y.score()
                || !Arrays.equals(x.principalVariation(),y.principalVariation())))
            throw new AssertionError("Observation changed score/PV");
    }
    /** model corpus output workers observe|cost rounds depth budget profiles */
    public static void main(String[] args) throws Exception {
        if(args.length!=9)throw new IllegalArgumentException("model corpus output workers observe|cost rounds depth budget profiles");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),rounds=Integer.parseInt(args[5]),depth=Integer.parseInt(args[6]);
        long budget=Long.parseLong(args[7]);boolean observe=args[4].equals("observe");
        if(!observe&&!args[4].equals("cost"))throw new IllegalArgumentException("mode");
        var profiles=List.of(args[8].split(","));var gson=new GsonBuilder().create();int requests=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","source",ApplicationVersion.load().sourceRevision(),
                    "build",ApplicationVersion.load().build(),"modelSha256",ConventionalNnueEngine.SHA256,
                    "args",args,"java",System.getProperty("java.runtime.version"))));out.newLine();out.flush();
            for(int round=observe?0:-1;round<rounds;round++) {
                int rootIndex=0;
                for(var root:roots) {
                    for(int offset=0;offset<profiles.size();offset++) {
                        String profile=profiles.get(Math.floorMod(round+rootIndex+offset,profiles.size()));
                        var config=flags(profile,workers);var table=new TTable(64);
                        SingleDepthSearch owner=workers==1?ExactSearchAdapter.constructionResearch(model.evaluation(),table,config):
                                ParallelSearch.constructionResearch(workers,model.evaluation(),table,config);
                        var kernels=kernels(owner,workers);var traces=new ArrayList<Trace>();
                        if(observe)for(var kernel:kernels) {var trace=new Trace();kernel.withSelectivityTrace(trace);traces.add(trace);}
                        var attempts=new ArrayList<Map<String,Object>>();
                        AspirationSearch.AttemptObserver observer=observe?(d,a,b,r,n)-> {
                            var snapshots=new ArrayList<Map<String,Object>>();
                            try {for(var kernel:kernels)snapshots.add(histories(kernel));}
                            catch(Exception e) {throw new IllegalStateException(e);}
                            attempts.add(Map.of("depth",d,"alpha",a,"beta",b,"result",r,"histories",snapshots));
                        }:null;
                        var aspiration=workers==1?new AspirationSearch((ExactSearchAdapter)owner,AspirationSearch.Policy.ADAPTIVE16,observer):
                                new AspirationSearch((ParallelSearch)owner,AspirationSearch.Policy.ADAPTIVE16,observer);
                        var saved=root.board().clone();
                        try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(aspiration))) {
                            var measurement=ConstructionTtStudy.request(driver,root,depth,budget);
                            var outcome=(SearchDriverOutcome)measurement.get("outcome");
                            if(!Arrays.equals(saved,root.board()))throw new AssertionError("Board changed");
                            if(observe&&workers==1)try(var clean=new SearchDriver(ClockPolicy.BOUNDARY2.apply(
                                    new ConstructionSearch(config,1,64,AspirationSearch.Policy.ADAPTIVE16).create(model.evaluation())))) {
                                same(outcome,(SearchDriverOutcome)ConstructionTtStudy.request(clean,root,depth,budget).get("outcome"));
                            }
                            var row=new LinkedHashMap<String,Object>();row.put("type","request");row.put("profile",profile);
                            row.put("root",root.id());row.put("round",round);row.put("order",offset);
                            row.put("flags",config.flags());row.put("measurement",measurement);
                            if(observe) {row.put("traces",traces);row.put("attempts",attempts);row.put("w1Neutral",workers==1);}
                            out.write(gson.toJson(row));out.newLine();out.flush();requests++;
                        }
                    }
                    rootIndex++;
                }
                System.out.println("completed "+args[4]+" W"+workers+" round="+round);
            }
            out.write(gson.toJson(Map.of("status","passed","requests",requests)));out.newLine();
        }
    }
    private P4TacticalStudy() {}
}
