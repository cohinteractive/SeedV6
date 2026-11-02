package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Three successive requests from existing real histories, without new-game between
 * requests. FEN-only roots repeat their position; no candidate-selected continuations. */
public final class P4CorrectionSequenceStudy {
    static List<List<P3HistoryStudy.Root>> sequences(Path path) throws Exception {
        var result=new ArrayList<List<P3HistoryStudy.Root>>();
        for(String line:Files.readAllLines(path)) {
            if(line.isBlank()||line.startsWith("#"))continue;
            var f=line.split("\t",-1);var moves=f.length>2&&!f[2].isBlank()?f[2].split(" "):new String[0];
            var sequence=new ArrayList<P3HistoryStudy.Root>();
            for(int step=0;step<3;step++) {
                int count=Math.max(0,moves.length-4+2*step);var board=Board.fromFen(f[1]);var history=GameHistory.builder(board);
                for(int i=0;i<count;i++) {
                    String uci=moves[i];long move=Arrays.stream(ExhaustiveOracle.legalMoves(board))
                            .filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);history.appendPosition(board);
                }
                sequence.add(new P3HistoryStudy.Root(f[0]+"-step"+step,board,history.snapshot()));
            }
            result.add(sequence);
        }
        return result;
    }
    static List<Long> savedCounts(List<ExactSearch> kernels) throws Exception {
        var counts=new ArrayList<Long>();
        for(var k:kernels) {
            var s=(FlatSearchState)P4TtFoundationStudy.field(k,"constructionState");long n=0;
            if(s.correction!=null&&s.correction.saved!=null)for(short v:s.correction.saved)if(v!=0)n++;
            counts.add(n);
        }
        return counts;
    }
    /** model corpus output workers observe|cost rounds depth budget profiles */
    public static void main(String[] args) throws Exception {
        if(args.length!=9)throw new IllegalArgumentException("model corpus output workers observe|cost rounds depth budget profiles");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var sequences=sequences(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),rounds=Integer.parseInt(args[5]),depth=Integer.parseInt(args[6]);
        long budget=Long.parseLong(args[7]);boolean observe=args[4].equals("observe");
        if(!observe&&!args[4].equals("cost"))throw new IllegalArgumentException("mode");
        var profiles=List.of(args[8].split(","));var gson=new GsonBuilder().create();int requests=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","source",ApplicationVersion.load().sourceRevision(),
                    "build",ApplicationVersion.load().build(),"modelSha256",ConventionalNnueEngine.SHA256,"args",args)));out.newLine();
            for(int round=observe?0:-1;round<rounds;round++) {
                int index=0;
                for(var sequence:sequences) {
                    for(int offset=0;offset<profiles.size();offset++) {
                        String profile=profiles.get(Math.floorMod(round+index+offset,profiles.size()));
                        var config=P4CorrectionStudy.flags(profile,workers);var table=new TTable(64);
                        SingleDepthSearch owner=workers==1?ExactSearchAdapter.constructionResearch(model.evaluation(),table,config):
                                ParallelSearch.constructionResearch(workers,model.evaluation(),table,config);
                        var kernels=P4CorrectionStudy.kernels(owner,workers);var diagnostics=new ArrayList<CorrectionHistory.Diagnostics>();
                        if(observe)for(var k:kernels) {
                            var s=(FlatSearchState)P4TtFoundationStudy.field(k,"constructionState");
                            if(s.correction!=null) {var d=new CorrectionHistory.Diagnostics();s.correction.diagnostics=d;diagnostics.add(d);}
                        }
                        SingleDepthSearch aspiration=workers==1?new AspirationSearch((ExactSearchAdapter)owner,AspirationSearch.Policy.ADAPTIVE16):
                                new AspirationSearch((ParallelSearch)owner,AspirationSearch.Policy.ADAPTIVE16);
                        try(var driver=new SearchDriver(ClockPolicy.BOUNDARY2.apply(aspiration));
                                var clean=observe&&workers==1?new SearchDriver(ClockPolicy.BOUNDARY2.apply(
                                        new ConstructionSearch(config,1,64,AspirationSearch.Policy.ADAPTIVE16).create(model.evaluation()))):null) {
                            int step=0;
                            for(var root:sequence) {
                                var before=root.board().clone();var measurement=ConstructionTtStudy.request(driver,root,depth,budget);
                                if(!Arrays.equals(before,root.board()))throw new AssertionError("Board changed");
                                if(clean!=null)P4CorrectionStudy.same((SearchDriverOutcome)measurement.get("outcome"),
                                        (SearchDriverOutcome)ConstructionTtStudy.request(clean,root,depth,budget).get("outcome"));
                                var row=new LinkedHashMap<String,Object>();row.put("type","request");row.put("profile",profile);
                                row.put("sequence",index);row.put("root",root.id());row.put("step",step++);row.put("round",round);
                                row.put("measurement",measurement);
                                if(observe) {row.put("correctionCumulative",diagnostics);row.put("savedNonzero",savedCounts(kernels));row.put("w1Neutral",workers==1);}
                                out.write(gson.toJson(row));out.newLine();out.flush();requests++;
                            }
                            driver.newGame();
                            if(observe&&savedCounts(kernels).stream().anyMatch(n->n!=0))throw new AssertionError("New-game retained correction");
                        }
                    }
                    index++;
                }
                System.out.println("completed sequence "+args[4]+" W"+workers+" round="+round);
            }
            out.write(gson.toJson(Map.of("status","passed","requests",requests)));out.newLine();
        }
    }
    private P4CorrectionSequenceStudy() {}
}
