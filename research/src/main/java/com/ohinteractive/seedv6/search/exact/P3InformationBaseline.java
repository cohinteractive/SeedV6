package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Read-only Phase 3 baseline observations. No new recursive hook or policy.
 * Reflection reads completed private history outside recursion; never mutates it.
 * Instrumented elapsed values are not clean performance measurements. */
public final class P3InformationBaseline {
    private static final Field HISTORY;
    static {
        try { HISTORY = ExactSearch.class.getDeclaredField("quietHistory"); HISTORY.setAccessible(true); }
        catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }

    static final class ObservedTable extends TTable {
        long probes, hits, saves;
        ObservedTable(int mib) { super(mib); }
        @Override public boolean probe(long key, TEntry entry) {
            probes++;
            boolean found = super.probe(key, entry);
            if (found) hits++;
            return found;
        }
        @Override public void save(long key, int depth, int type, int score, long move) {
            saves++;
            super.save(key, depth, type, score, move);
        }
        void resetCounters() { probes=hits=saves=0; }
    }

    static ExactSearch engine(SearchEvaluation evaluation, TTable table, InternalSearchPolicy.Diagnostics d) {
        return engine(ExactEvaluator.from(evaluation),table,d);
    }

    static ExactSearch engine(ExactEvaluator evaluator, TTable table, InternalSearchPolicy.Diagnostics d) {
        return ExactSearch.internalSearchResearch(evaluator, table,
                InternalSearchPolicy.OFF, d).withSeeResearch(SeePolicy.QXRAY, null);
    }

    /** Production admission accounting differs from recursive entry counting on PVS retries. */
    static final class Admissions implements ExactEvaluator {
        final ExactEvaluator delegate;
        long nodes,evaluations;
        Admissions(SearchEvaluation evaluation) { delegate=ExactEvaluator.from(evaluation); }
        @Override public void initialize(long[] board) { nodes=evaluations=0;delegate.initialize(board); }
        @Override public void child(long[] parent,long[] child,int ply,int qply) {
            nodes++;delegate.child(parent,child,ply,qply);
        }
        @Override public void reenter(int ply) { nodes++;delegate.reenter(ply); }
        @Override public int evaluate(long[] board,int ply) { evaluations++;return delegate.evaluate(board,ply); }
    }

    static int[] history(ExactSearch search) {
        try { return ((int[]) HISTORY.get(search)).clone(); }
        catch (IllegalAccessException failure) { throw new AssertionError(failure); }
    }

    static Map<String,Object> historySummary(int[] current, int[] previous) {
        int positive=0,negative=0,saturated=0,overlap=0,sameSign=0;
        long absolute=0;
        for(int i=0;i<current.length;i++) {
            int value=current[i];
            if(value>0) positive++;
            if(value<0) negative++;
            absolute+=Math.abs(value);
            if(Math.abs(value)==QuietHistory.LIMIT) saturated++;
            if(value!=0 && previous!=null && previous[i]!=0) {
                overlap++;
                if(Integer.signum(value)==Integer.signum(previous[i])) sameSign++;
            }
        }
        return Map.of("positive",positive,"negative",negative,"saturated",saturated,
                "absoluteSum",absolute,"overlapPreviousCompleted",overlap,"sameSign",sameSign);
    }

    static void same(ExactSearchResult a, ExactSearchResult b) {
        if(a.completed()!=b.completed() || a.score()!=b.score() || a.bestMove()!=b.bestMove()
                || !Arrays.equals(a.principalVariation(),b.principalVariation())
                || a.nodes()!=b.nodes() || a.qnodes()!=b.qnodes() || a.selective()!=b.selective()
                || a.maximumQply()!=b.maximumQply() || a.maximumExtensionPlies()!=b.maximumExtensionPlies())
            throw new AssertionError("Observation changed Search: " + a + " / " + b);
    }

    /** model corpus new-output max-depth max-nodes-per-invocation max-roots.
     * Both copies use identical node cancellation; aborted observations stay in output. */
    public static void main(String[] args) throws Exception {
        if(args.length!=6) throw new IllegalArgumentException(
                "model corpus.tsv new-output.jsonl max-depth max-nodes-per-invocation max-roots");
        var evaluation=SearchEvaluation.brnPair2(ProductionModel.load(Path.of(args[0])));
        int maxDepth=Integer.parseInt(args[3]),maxRoots=Integer.parseInt(args[5]);
        long budget=Long.parseLong(args[4]);
        if(maxDepth<1 || maxDepth>12 || budget<1 || maxRoots<1) throw new IllegalArgumentException("Invalid bounds");
        var gson=new GsonBuilder().create();
        int roots=0,observations=0,incomplete=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank() || line.startsWith("#")) continue;
                if(roots==maxRoots) break;
                var fields=line.split("\t",-1);
                long[] board=Board.fromFen(fields[1]);
                var builder=GameHistory.builder(board);
                if(fields.length>2 && !fields[2].isBlank()) for(String uci:fields[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board))
                            .filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);
                }
                var game=builder.snapshot();
                var table=new ObservedTable(64);
                var diagnostics=new InternalSearchPolicy.Diagnostics();
                var admissions=new Admissions(evaluation);
                var observed=engine(admissions,table,diagnostics);
                var plain=engine(evaluation,new TTable(64),null);
                observed.beginRequest();plain.beginRequest();
                int[] previous=null;
                try {
                    for(int depth=1;depth<=maxDepth;depth++) {
                        table.resetCounters();
                        var a=plain.search(board,game,depth,()->plain.visitedNodes()>=budget);
                        var b=observed.search(board,game,depth,()->observed.visitedNodes()>=budget);
                        same(a,b);
                        Phase2Oracle.legalPv(board,b.principalVariation());
                        int[] current=history(observed);
                        var row=new LinkedHashMap<String,Object>();
                        row.put("id",fields[0]);row.put("historySize",game.size());row.put("depth",depth);
                        row.put("outcome",b);row.put("neutral",true);
                        row.put("preparedAdmissions",admissions.nodes);row.put("evaluations",admissions.evaluations);
                        row.put("tt",Map.of("probes",table.probes,"hits",table.hits,"saveAttempts",table.saves));
                        row.put("guidance",diagnostics);row.put("history",historySummary(current,previous));
                        out.write(gson.toJson(row));out.newLine();out.flush();observations++;
                        if(!b.completed()) {incomplete++;break;}
                        previous=current;
                    }
                } finally { observed.endRequest();plain.endRequest(); }
                roots++;
                System.out.println("completed root "+fields[0]);
            }
            out.write(gson.toJson(Map.of("status","passed","roots",roots,"observations",observations,
                    "incompleteInvocations",incomplete,"requestedTtMiB",64,"entries",2097152,
                    "primitiveTtBytes",50331648,"historyBytesPerWorker",262144)));
            out.newLine();
        }
    }
}
