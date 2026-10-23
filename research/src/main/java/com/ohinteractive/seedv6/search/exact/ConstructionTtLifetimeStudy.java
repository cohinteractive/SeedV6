package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.*;

/** Actual-history CP64 replay. Existing request retention versus explicit newGame
 * control; independent white/black owners. No alternative TT retention policy. */
public final class ConstructionTtLifetimeStudy {
    static List<P3HistoryStudy.Root> prefixes(String line) {
        var fields=line.split("\t",-1);
        long[] board=Board.fromFen(fields[1]);var history=GameHistory.builder(board);
        var roots=new ArrayList<P3HistoryStudy.Root>();
        for(String uci:fields[2].split(" ")) {
            roots.add(new P3HistoryStudy.Root(fields[0]+"-ply"+roots.size(),board,history.snapshot()));
            long move=Arrays.stream(ExhaustiveOracle.legalMoves(board))
                    .filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow(()->new IllegalArgumentException("Illegal fixture move "+uci));
            board=ExhaustiveOracle.child(board,move);history.appendPosition(board);
        }
        return roots;
    }

    /** model lines.tsv new-output depth nodeBudget startPly endPly */
    public static void main(String[] args) throws Exception {
        if(args.length!=7) throw new IllegalArgumentException("model lines.tsv new-output depth nodeBudget startPly endPly");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var e=model.evaluation();
        int depth=Integer.parseInt(args[3]),start=Integer.parseInt(args[5]),end=Integer.parseInt(args[6]);
        long budget=Long.parseLong(args[4]);
        if(depth<1 || depth>ExactSearch.MAX_DEPTH || budget<1 || start<0 || end<=start) throw new IllegalArgumentException("Invalid bounds");
        var games=new ArrayList<List<P3HistoryStudy.Root>>();
        for(String line:Files.readAllLines(Path.of(args[1]))) if(!line.isBlank() && !line.startsWith("#")) games.add(prefixes(line));
        var gson=new GsonBuilder().create();int count=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,"modelSha256",ConventionalNnueEngine.SHA256,
                    "config",ConstructionTtStudy.configuration("baseline",1).identity(),"games",games.size(),"allFixtureMovesLegallyReplayed",true)));
            out.newLine();out.flush();
            for(var game:games) {
                SearchDriver[] plain=new SearchDriver[2],observed=new SearchDriver[2],cleared=new SearchDriver[2];
                var tables=new ConstructionTtStudy.ObservedTable[2];
                try {
                    for(int side=0;side<2;side++) {
                        tables[side]=new ConstructionTtStudy.ObservedTable(64);
                        var adapter=ExactSearchAdapter.constructionResearch(e,tables[side],FlatSearchConfig.candidate());
                        observed[side]=new SearchDriver(new AspirationSearch(adapter,AspirationSearch.Policy.WIDEN16));
                        plain[side]=new SearchDriver(ConstructionTtStudy.configuration("baseline",1).create(e));
                        cleared[side]=new SearchDriver(ConstructionTtStudy.configuration("baseline",1).create(e));
                    }
                    for(int ply=start;ply<Math.min(end,game.size());ply++) {
                        var root=game.get(ply);int side=Board.player((int)root.board()[Board.STATUS]);
                        tables[side].resetCounters();
                        long reset=System.nanoTime();cleared[side].newGame();reset=System.nanoTime()-reset;
                        Map<String,Object> a,c;
                        if((ply/2&1)==0) {
                            a=ConstructionTtStudy.request(plain[side],root,depth,budget);
                            c=ConstructionTtStudy.request(cleared[side],root,depth,budget);
                        } else {
                            c=ConstructionTtStudy.request(cleared[side],root,depth,budget);
                            a=ConstructionTtStudy.request(plain[side],root,depth,budget);
                        }
                        var b=ConstructionTtStudy.request(observed[side],root,depth,budget);
                        ConstructionTtStudy.same((SearchDriverOutcome)a.get("outcome"),(SearchDriverOutcome)b.get("outcome"));
                        if(tables[side].proofStores!=0 || tables[side].proofHits!=0) throw new AssertionError("Unexpected ordinary proof domain");
                        var row=new LinkedHashMap<String,Object>();row.put("type","lifetime");row.put("id",root.id());row.put("ply",ply);
                        row.put("side",side);row.put("neutral",true);row.put("retained",a);row.put("cleared",c);
                        row.put("clearNanos",reset);row.put("observed",b);row.put("table",tables[side].snapshot(true));
                        out.write(gson.toJson(row));out.newLine();out.flush();count++;
                    }
                } finally {
                    for(var group:List.of(plain,observed,cleared)) for(var driver:group) if(driver!=null) driver.close();
                }
            }
            out.write(gson.toJson(Map.of("status","passed","requests",count)));out.newLine();
        }
    }
    private ConstructionTtLifetimeStudy() {}
}
