package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;

/** Bounded H1 cost separation and H2/H4/H5 opportunity observation, never production activation. */
public final class Cp64SelectivityLab {
    public static ConstructionSearch configuration(String name,int workers,int hash) {
        var base=Cp64SelectivityStudy.configuration(switch(name) {
            case "lazy-improving","reduction-futility","nominal-futility","verified-null","nominal-verified-null" -> "baseline";
            case "reduction-no-improving" -> "no-improving";
            default -> name;
        },workers);
        var mechanisms=switch(name) {
            case "lazy-improving" -> base.mechanisms().with(LAZY_IMPROVING,true);
            case "reduction-futility","reduction-no-improving" -> base.mechanisms().with(REDUCTION_FUTILITY,true);
            case "nominal-futility" -> base.mechanisms().with(NOMINAL_FUTILITY,true);
            case "verified-null" -> base.mechanisms().with(VERIFIED_NULL,true);
            case "nominal-verified-null" -> base.mechanisms().with(VERIFIED_NULL,true).with(NOMINAL_FUTILITY,true);
            default -> base.mechanisms();
        };
        return new ConstructionSearch(mechanisms,
                workers,hash,base.aspiration());
    }
    record Sample(String kind,long[] board,long move,long[] path,int ply,int depth,int alpha,int beta,
                  int raw,int history,int continuation,int reduction) {}
    static final class Observation implements SelectivityTrace {
        final long[][] surviving=new long[17][6]; // depth capped16; absent/belowBeta/0..99/100..299/300..599/600+ surplus
        final long[][][] reductions=new long[17][9][3]; // nominal depth, assigned child depth, main-history sign
        final long[] counts=new long[9];
        final int[][] selected=new int[2][17];
        final List<Sample> samples=new ArrayList<>();
        final long[][] omissions=new long[17][3];
        final List<Map<String,Object>> omittedSamples=new ArrayList<>();
        final long[][] nullEvents=new long[17][2];
        final long[] completedNullWork=new long[2]; // completed probe/verifier nodes; request totals retain all aborts
        final List<Map<String,Object>> nullSamples=new ArrayList<>();
        void nullPrediction(long[] b,long[] path,int ply,int depth,int beta,int raw,int r,int prediction,boolean accepted) {
            long n=++nullEvents[depth][accepted?1:0];
            if(n==1 || n==8 || n==64 || n==512)nullSamples.add(Map.of("sample",
                    new Sample("integrated-null",b.clone(),0,Arrays.copyOfRange(path,1,ply+1),ply,depth,beta-1,beta,raw,0,0,r),
                    "prediction",prediction,"selectedCutoff",accepted,"event",n));
        }
        @Override public void omission(long[] b,long move,long[] path,int ply,int depth,int alpha,int beta,
                int raw,int history,int continuation,int assignedChildDepth,int marginDepth) {
            long n=++omissions[depth][Integer.signum(history)+1];
            if(n==1 || n==8 || n==64 || n==512) omittedSamples.add(Map.of("sample",
                    new Sample("deeper-futility-omission",b.clone(),move,Arrays.copyOfRange(path,1,ply+1),
                            ply,depth,alpha,beta,raw,history,continuation,depth-1-assignedChildDepth),
                    "assignedChildDepth",assignedChildDepth,"marginDepth",marginDepth,"event",n));
        }
        static boolean material(long[] b,int status) {
            long occupied=b[0]|b[1]|b[2],kings=b[0]&~b[1]&~b[2],pawns=~b[0]&b[1]&b[2];
            long side=Board.player(status)==0?~b[3]:b[3];
            return Long.bitCount(occupied)>6 && (occupied&~kings&~pawns&side)!=0;
        }
        @Override public void survivor(long[] b,long[] path,int ply,int depth,int alpha,int beta,boolean scout,boolean checked,int raw) {
            counts[0]++;
            if(ply==0 || checked || depth<3 || !(scout || beta==alpha+1)
                    || alpha < -ExactSearch.MAX_STATIC_SCORE || beta>ExactSearch.MAX_STATIC_SCORE) return;
            int status=(int)b[Board.STATUS];
            if(Board.halfMoveClock(status)+depth>=100 || !material(b,status)) return;
            counts[1]++;
            int delta=raw==Integer.MIN_VALUE?Integer.MIN_VALUE:raw-beta;
            int bucket=raw==Integer.MIN_VALUE?0:delta<0?1:delta<100?2:delta<300?3:delta<600?4:5;
            int d=Math.min(16,depth);surviving[d][bucket]++;
            if(bucket>=2) {
                counts[2]++;
                if(selected[0][d]++<2) samples.add(new Sample("null-opportunity",b.clone(),0,
                        Arrays.copyOfRange(path,1,ply+1),ply,depth,alpha,beta,raw,0,0,0));
            }
        }
        @Override public void reduction(long[] b,long move,long[] path,int ply,int depth,int ordinal,int quiets,
                int alpha,int beta,int history,int continuation,int raw,int prior,int base,int assigned,boolean caution) {
            counts[3]++;
            int status=(int)b[Board.STATUS];
            if(CaptureHistory.isTactical(move,Board.enPassantSquare(status))) return;
            counts[4]++;
            if(history<0) counts[5]++;else if(history==0) counts[6]++;
            if(base==0) return; // Existing LMR guards and extension veto already applied.
            counts[7]++;
            int d=Math.min(16,depth),childDepth=depth-1-assigned;
            reductions[d][Math.min(8,childDepth)][Integer.signum(history)+1]++;
            int piece=(int)((move>>>Board.START_PIECE_SHIFT)&Piece.TYPE);
            if(depth>2 && childDepth<=2 && quiets>=4 && beta==alpha+1 && alpha>=0
                    && raw!=Integer.MIN_VALUE && raw+100*childDepth<=alpha && history<=0
                    && piece!=Piece.PAWN && piece!=Piece.KING && material(b,status)) {
                counts[8]++;
                if(selected[1][d]++<2) samples.add(new Sample("reduction-futility-opportunity",b.clone(),move,
                        Arrays.copyOfRange(path,1,ply+1),ply,depth,alpha,beta,raw,history,continuation,assigned));
            }
        }
    }
    static ExactSearch exact(ExactSearchAdapter adapter) throws Exception {
        var field=ExactSearchAdapter.class.getDeclaredField("exact");field.setAccessible(true);return (ExactSearch)field.get(adapter);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=11) throw new IllegalArgumentException("model corpus NEW-output workers warmups rounds depth budget roots observe|cost profiles");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var all=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),warmups=Integer.parseInt(args[4]),rounds=Integer.parseInt(args[5]);
        int depth=Integer.parseInt(args[6]),count=Integer.parseInt(args[8]);long budget=Long.parseLong(args[7]);
        if(warmups<0 || rounds<1 || count<1 || count>all.size() || depth<1 || depth>ExactSearch.MAX_DEPTH || budget<1)
            throw new IllegalArgumentException("Bounds");
        var profiles=List.of(args[10].split(","));var identities=new LinkedHashMap<String,String>();
        if(new HashSet<>(profiles).size()!=profiles.size()) throw new IllegalArgumentException("Duplicate profile");
        for(String p:profiles) identities.put(p,configuration(p,workers,64).identity());
        var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,"profiles",identities,
                    "productionIdentity",BaselineEngine.load().identity(workers,64),"networkSha256",ConventionalNnueEngine.SHA256)));
            out.newLine();out.flush();
            if(args[9].equals("observe")) {
                if(workers!=1 || warmups!=0 || rounds!=1) throw new IllegalArgumentException("Observation domain");
                for(var root:all.subList(0,count)) for(String profile:profiles) {
                    var config=configuration(profile,1,64);var observation=new Observation();
                    var adapter=ExactSearchAdapter.constructionResearch(model.evaluation(),new TTable(64),config.mechanisms());
                    var exact=exact(adapter);exact.withSelectivityTrace(observation);var attempts=new ArrayList<Map<String,Object>>();
                    if(config.mechanisms().has(VERIFIED_NULL)) {
                        var field=ExactSearch.class.getDeclaredField("constructionState");field.setAccessible(true);
                        var state=(FlatSearchState)field.get(exact);
                        exact.withFlatNullObservation(false,new NullMovePolicy.Diagnostics(){
                            @Override public void probe(long[] b,int ply,int d,int beta,int raw,int r,int prediction,boolean accepted) {
                                observation.nullPrediction(b,state.pathMoves,ply,d,beta,raw,r,prediction,accepted);
                                observation.completedNullWork[0]=probeNodes;observation.completedNullWork[1]=verificationNodes;
                            }
                        });
                    }
                    var owner=new AspirationSearch(adapter,config.aspiration(),(d,a,b,r,n)->attempts.add(
                            Map.of("depth",d,"alpha",a,"beta",b,"outcome",r,"mechanisms",exact.constructionCounters())));
                    try(var plain=new SearchDriver(config.create(model.evaluation()));var observed=new SearchDriver(owner)) {
                        var a=ConstructionTtAdaptiveStudy.request(plain,root,depth,budget);
                        var b=ConstructionTtAdaptiveStudy.request(observed,root,depth,budget);
                        ConstructionTtStudy.same((SearchDriverOutcome)a.get("outcome"),(SearchDriverOutcome)b.get("outcome"));
                        if(attempts.stream().mapToLong(x->((SearchResult)x.get("outcome")).nodes()).sum()!=((SearchDriverOutcome)b.get("outcome")).nodes())
                            throw new AssertionError("Attempt accounting");
                        out.write(gson.toJson(Map.of("type","observation","id",root.id(),"profile",profile,"neutral",true,
                                "plain",a,"observed",b,"attempts",attempts,"opportunities",observation)));
                        out.newLine();out.flush();
                    }
                }
            } else if(args[9].equals("cost")) {
                var drivers=new SearchDriver[profiles.size()];
                try {
                    for(int p=0;p<drivers.length;p++) drivers[p]=new SearchDriver(configuration(profiles.get(p),workers,64).create(model.evaluation()));
                    for(int round=-warmups;round<rounds;round++) for(int i=0;i<count;i++) for(int offset=0;offset<drivers.length;offset++) {
                        int p=Math.floorMod(round+i+offset,drivers.length);var root=all.get(i);
                        var row=ConstructionTtAdaptiveStudy.request(drivers[p],root,depth,budget);
                        row.put("type","request");row.put("id",root.id());row.put("round",round);row.put("profile",profiles.get(p));
                        out.write(gson.toJson(row));out.newLine();out.flush();
                    }
                } finally {for(var d:drivers) if(d!=null)d.close();}
            } else throw new IllegalArgumentException("Unknown mode");
            out.write(gson.toJson(Map.of("status","passed")));out.newLine();
        }
    }
    private Cp64SelectivityLab() {}
}
