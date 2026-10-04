package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.training.validation.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.IntToDoubleFunction;

/** Explicit fresh-model bootstrap, excluded from application distributions.
 * No training, checkpoint loading, score adjudication, or parameter selection. */
public final class BootstrapAudit {
    private static final Gson JSON = new GsonBuilder().serializeNulls().create();
    private static volatile double sink;
    public static final long CORPUS_SEED = 2026100501L, OPENING_SEED = 2026100502L;
    public record Edge(long[] parent, long[] child, String kind, int changedSquares) {}

    /** Legal uniform trajectories, independent of model seed and evaluation. */
    public static List<Edge> corpus(int games, int plies) {
        var edges = new ArrayList<Edge>();
        var random = new SplittableRandom(CORPUS_SEED);
        for (int g = 0; g < games; g++) {
            var game = new HeadlessGame(Board.startingPosition(), plies);
            while (game.active()) {
                var parent = game.boardSnapshot();
                long[] moves = game.legalMoves();
                game.play(moves[random.nextInt(moves.length)]);
                var child = game.boardSnapshot();
                long changed = 0;
                for (int i = 0; i < 4; i++) changed |= parent[i] ^ child[i];
                long kings = parent[0] & ~parent[1] & ~parent[2];
                String kind = count(child) < count(parent) ? "capture" : (changed & kings) != 0 ? "king" : "quiet";
                edges.add(new Edge(parent, child, kind, Long.bitCount(changed)));
            }
        }
        return List.copyOf(edges);
    }
    private static int count(long[] b) { return Long.bitCount(b[0] | b[1] | b[2]); }
    private static int side(long[] b) { return Board.player((int)b[4]); }
    private static double white(double score, long[] b) { return side(b) == 0 ? score : -score; }

    /** Legal chess symmetry: colour swap + rank reflection; file reflection only without castling rights. */
    public static long[] symmetry(long[] board, boolean colourInversion) {
        String[] fields=Fen.fromBoard(board).split(" ");
        if(!colourInversion&&!fields[2].equals("-"))throw new IllegalArgumentException("File reflection with castling rights is not a chess symmetry");
        var fen=new StringBuilder();
        for(int rank=7;rank>=0;rank--) {
            int empty=0;
            for(int file=0;file<8;file++) {
                int square=(rank*8+file)^(colourInversion?56:7);
                int piece=Board.getSquare(board[0],board[1],board[2],board[3],square);
                if(piece==0){empty++;continue;}
                if(empty>0){fen.append(empty);empty=0;}
                fen.append(Piece.SHORT_STRING[colourInversion?piece^8:piece]);
            }
            if(empty>0)fen.append(empty);
            if(rank>0)fen.append('/');
        }
        String rights=fields[2];
        if(colourInversion&&!rights.equals("-")) {
            var flipped=new StringBuilder();
            for(char c:"KQkq".toCharArray())if(rights.indexOf(Character.isUpperCase(c)?Character.toLowerCase(c):Character.toUpperCase(c))>=0)flipped.append(c);
            rights=flipped.toString();
        }
        String ep=fields[3];
        if(!ep.equals("-"))ep=colourInversion?""+ep.charAt(0)+(char)('9'-ep.charAt(1)+'0'):""+(char)('h'-ep.charAt(0)+'a')+ep.charAt(1);
        fen.append(' ').append((side(board)^(colourInversion?1:0))==0?'w':'b');
        fen.append(' ').append(rights).append(' ').append(ep).append(' ').append(fields[4]).append(' ').append(fields[5]);
        return Board.fromFen(fen.toString());
    }

    /** Independent audit oracle only: never injected into NNUE or its search. */
    public static double materialOracle(long[] b) {
        double value = 0;
        for (int s = 0; s < 64; s++) {
            int p = Board.getSquare(b[0], b[1], b[2], b[3], s);
            double v = switch (p & Piece.TYPE) {
                case Piece.PAWN -> 1; case Piece.KNIGHT -> 3.2; case Piece.BISHOP -> 3.3;
                case Piece.ROOK -> 5; case Piece.QUEEN -> 9; default -> 0;
            };
            value += (p & 8) == 0 ? v : -v;
        }
        return side(b) == 0 ? value : -value;
    }

    private static final class Moments {
        long n; double sx, sy, xx, yy, xy, difference, maxDifference;
        void add(double x, double y) {
            n++; sx += x; sy += y; xx += x*x; yy += y*y; xy += x*y;
            difference += Math.abs(x-y); maxDifference = Math.max(maxDifference, Math.abs(x-y));
        }
        Map<String,Object> report() {
            double vx = xx - sx*sx/n, vy = yy - sy*sy/n;
            var m = new LinkedHashMap<String,Object>();
            m.put("n", n); m.put("meanX", sx/n); m.put("meanY", sy/n);
            m.put("sdX", Math.sqrt(Math.max(0, vx/n))); m.put("sdY", Math.sqrt(Math.max(0, vy/n)));
            m.put("correlation", vx > 1e-24 && vy > 1e-24 ? (xy-sx*sy/n)/Math.sqrt(vx*vy) : null);
            m.put("meanAbsoluteDifference", difference/n); m.put("maxAbsoluteDifference", maxDifference);
            return m;
        }
    }

    public static Map<String,Object> diagnostics(long seed, List<Edge> edges, NnueNetwork network, Brn3Trainer trainer) throws Exception {
        var evaluator = new NnueEvaluator(network);
        var parentAcc = new NnueAccumulator(network); var childAcc = new NnueAccumulator(network);
        var model = trainer.snapshot(); var brn = model.newWorkspace();
        var mapped = new TreeSet<Integer>(); var stats = new TreeMap<String,Moments>();
        double materialError = 0, incrementalError = 0, trainerError = 0;
        long zeroAcc = 0, saturatedAcc = 0, zeroHidden = 0, saturatedHidden = 0;
        for (var edge : edges) {
            long[] p = edge.parent(), c = edge.child();
            parentAcc.rebuild(p); float pn = evaluator.evaluate(p, parentAcc);
            for (int role = 0; role < 2; role++) for (int h = 0; h < 64; h++) {
                float x = evaluator.accumulator(role,h); if (x == 0) zeroAcc++; if (x == 1) saturatedAcc++;
            }
            for (int h = 0; h < 32; h++) { float x = evaluator.hidden(h); if (x == 0) zeroHidden++; if (x == 1) saturatedHidden++; }
            int score = NnueScoreMapping.V1.map(evaluator.boundedValue()); mapped.add(score);
            childAcc.update(p,c,parentAcc); float cnIncremental = evaluator.evaluate(c,childAcc);
            float cn = evaluator.evaluate(c); incrementalError = Math.max(incrementalError,Math.abs(cn-cnIncremental));
            double pb = brn.evaluatePawns(p,.25), cb = brn.evaluatePawns(c,.25);
            materialError = Math.max(materialError,Math.max(Math.abs(pb-materialOracle(p)),Math.abs(cb-materialOracle(c))));
            trainerError = Math.max(trainerError,Math.abs(trainer.predictPawns(p)-pb));
            for (String group : List.of("all",edge.kind(),"changedSquares-"+edge.changedSquares())) {
                stats.computeIfAbsent("nnueRawWhite/"+group,k->new Moments()).add(white(pn,p),white(cn,c));
                stats.computeIfAbsent("brnPawnsWhite/"+group,k->new Moments()).add(white(pb,p),white(cb,c));
            }
            stats.computeIfAbsent("nnueSearchScoreSTM",k->new Moments()).add(score,score);
            var inverted=symmetry(p,true);
            stats.computeIfAbsent("nnueColourInversionSTM",k->new Moments()).add(pn,evaluator.evaluate(inverted));
            stats.computeIfAbsent("brnColourInversionSTM",k->new Moments()).add(pb,brn.evaluatePawns(inverted));
            if(Fen.fromBoard(p).split(" ")[2].equals("-")) {
                var mirrored=symmetry(p,false);
                stats.computeIfAbsent("nnueFileReflectionSTM",k->new Moments()).add(pn,evaluator.evaluate(mirrored));
                stats.computeIfAbsent("brnFileReflectionSTM",k->new Moments()).add(pb,brn.evaluatePawns(mirrored));
            }
            // Isolate placement changes from perspective-slot swapping: a legal null probe after the child.
            if(!Board.isPlayerInCheckPext(c[0],c[1],c[2],c[3],side(c))) {
                var sameSide=new long[6];Board.nullMoveInto(c[0],c[1],c[2],c[3],(int)c[4],c[5],sameSide);
                stats.computeIfAbsent("nnueSameSTMAfterNull/"+edge.kind(),k->new Moments()).add(pn,evaluator.evaluate(sameSide));
            }
            // Null-move probe only where the mover is not in check; EP cleared by engine.
            if (!Board.isPlayerInCheckPext(p[0],p[1],p[2],p[3],side(p))) {
                var flipped = new long[6]; Board.nullMoveInto(p[0],p[1],p[2],p[3],(int)p[4],p[5],flipped);
                stats.computeIfAbsent("nnueNullWhite",k->new Moments()).add(white(pn,p),white(evaluator.evaluate(flipped),flipped));
                stats.computeIfAbsent("brnNullWhite",k->new Moments()).add(white(pb,p),white(brn.evaluatePawns(flipped),flipped));
            }
        }
        double headMax = 0;
        for (int i = Brn3Layout.HEAD; i <= Brn3Layout.OUTPUT_BIAS; i++) headMax = Math.max(headMax,Math.abs(trainer.weight(i)));
        if (materialError > 1e-12 || trainerError > 1e-12 || headMax != 0 || trainer.step() != 0 || incrementalError > 1e-8)
            throw new AssertionError("Fresh baseline identity/incremental audit failed");
        var out = new LinkedHashMap<String,Object>();
        out.put("seed",seed); out.put("positions",edges.size()); out.put("optimizerUpdates",trainer.step());
        out.put("brnReadoutMaxAbs",headMax); out.put("brnMaterialMaxErrorPawns",materialError);
        out.put("brnTrainerInferenceMaxErrorPawns",trainerError); out.put("nnueIncrementalMaxRawError",incrementalError);
        out.put("nnueModelSha256",sha(NnueNetworkCodec.encode(network)));
        out.put("brnModelSha256",sha(Brn3Codec.encodeModel(model)));
        out.put("nnueParameters",NnueNetwork.PARAMETER_COUNT); out.put("brnInferenceParameters",Brn3Model.PARAMETER_COUNT);
        out.put("brnTrainingParameters",Brn3Layout.TRAINING_PARAMETERS);
        out.put("nnueMappedScores",mapped);
        out.put("nnueAccumulatorZeroFraction",zeroAcc/(128.0*edges.size())); out.put("nnueAccumulatorSaturationFraction",saturatedAcc/(128.0*edges.size()));
        out.put("nnueHiddenZeroFraction",zeroHidden/(32.0*edges.size())); out.put("nnueHiddenSaturationFraction",saturatedHidden/(32.0*edges.size()));
        var reports = new TreeMap<String,Object>(); stats.forEach((k,v)->reports.put(k,v.report())); out.put("topology",reports);
        return out;
    }

    private static String sha(byte[] data) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }

    private static Map<String,Object> measure(int count, IntToDoubleFunction work) {
        int loops = 20000; double[] nanos = new double[7]; long[] bytes = new long[7];
        var mx = ManagementFactory.getThreadMXBean();
        var alloc = mx instanceof com.sun.management.ThreadMXBean a && a.isThreadAllocatedMemorySupported() ? a : null;
        if (alloc != null) alloc.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        for (int repeat = -4; repeat < 7; repeat++) {
            double total = 0; long before = alloc == null ? 0 : alloc.getThreadAllocatedBytes(thread), start = System.nanoTime();
            for (int i = 0; i < loops; i++) total += work.applyAsDouble(i % count);
            long elapsed = System.nanoTime()-start;
            long used = alloc == null ? -1 : alloc.getThreadAllocatedBytes(thread)-before;
            sink = total;
            if (repeat >= 0) { nanos[repeat] = (double)elapsed/loops; bytes[repeat] = used; }
        }
        double[] sorted = nanos.clone(); Arrays.sort(sorted);
        return Map.of("nsPerOperation",nanos,"medianNs",sorted[3],"allocatedBytesPer20000Operations",bytes,"warmupOperations",80000);
    }

    private static Map<String,Object> performance(List<Edge> corpus, NnueNetwork network, Brn3Model model) {
        var edges = corpus.subList(0,Math.min(256,corpus.size())); int size = edges.size();
        var evaluator = new NnueEvaluator(network); var parent = new NnueAccumulator[size]; var child = new NnueAccumulator(network);
        for (int i=0;i<size;i++) { parent[i] = new NnueAccumulator(network); parent[i].rebuild(edges.get(i).parent()); }
        var cached = model.newWorkspace(); var repeated = model.newWorkspace();
        var out = new LinkedHashMap<String,Object>();
        out.put("nnuePrepared",measure(size,i->evaluator.evaluate(edges.get(i).parent(),parent[i])));
        out.put("nnueFullRefreshAndInference",measure(size,i->evaluator.evaluate(edges.get(i).parent())));
        out.put("nnueTransitionOnly",measure(size,i->{var e=edges.get(i);child.update(e.parent(),e.child(),parent[i]);return child.raw(0,0);}));
        out.put("nnueTransitionAndInference",measure(size,i->{var e=edges.get(i);child.update(e.parent(),e.child(),parent[i]);return evaluator.evaluate(e.child(),child);}));
        out.put("brnSequentialTransitionAndInference",measure(size,i->cached.evaluatePawns(edges.get(i).parent(),.25)));
        out.put("brnRepeatedInference",measure(1,i->repeated.evaluatePawns(edges.get(0).parent(),.25)));
        out.put("scope","256 trajectory parents; NNUE update includes copy, king refresh where encountered; BRN stream includes game/wrap refresh; timings are raw values, not integer mapping; four warmups/seven measurements per operation; fixed order, one JVM");
        return out;
    }

    private static Map<String,Object> game(ValidationArena.Opening opening, NnueNetwork network, Brn3Model model, int nnueColor, int depth, int cap) {
        var game = opening.newGame(cap); var moves = new ArrayList<String>(); long[] nodes = new long[2], nanos = new long[2];
        try (var nnue = new SearchDriver(new ExactSearchAdapter(SearchEvaluation.incremental(network),new TTable(4)));
             var brn = new SearchDriver(new ExactSearchAdapter(SearchEvaluation.brn3(model),new TTable(4)))) {
            while (game.active()) {
                int actor = game.sideToMove() == nnueColor ? 0 : 1;
                var control = SearchControl.controlled(-1,System.nanoTime(),-1,TimeSource.SYSTEM);
                long start = System.nanoTime();
                var result = (actor == 0 ? nnue : brn).search(new SearchRequest(game.boardSnapshot(),game.historySnapshot(),depth,control));
                nanos[actor] += System.nanoTime()-start; nodes[actor] += control.nodes();
                if (!result.targetDepthCompleted() || result.lastCompletedResult() == null || !result.lastCompletedResult().hasMove())
                    throw new IllegalStateException("Required search failed; no score assigned");
                long move = result.lastCompletedResult().bestMove(); moves.add(Long.toUnsignedString(move,16)); game.play(move);
            }
        }
        var out = new LinkedHashMap<String,Object>(); out.put("nnueColor",nnueColor);
        out.put("termination",game.termination()); out.put("nnueScore",game.termination().completed() ? (game.termination().result().orElseThrow().target(nnueColor)+1)/2.0 : null);
        out.put("plies",game.playedPlies()); out.put("nodesNnueBrn",nodes); out.put("nanosNnueBrn",nanos);
        out.put("finalFen",Fen.fromBoard(game.boardSnapshot())); out.put("movesHex",moves); return out;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) throw new IllegalArgumentException("NEW_OUTPUT MODE[audit|match] SEEDS_CSV PAIRS DEPTH PLY_CAP");
        Path out = Path.of(args[0]); String mode=args[1];
        if (!mode.equals("audit") && !mode.equals("match")) throw new IllegalArgumentException("Unknown mode");
        int pairs=Integer.parseInt(args[3]),depth=Integer.parseInt(args[4]),cap=Integer.parseInt(args[5]);
        if(pairs<1||depth<1||depth>6||cap<1)throw new IllegalArgumentException("Invalid bounds");
        long[] seeds=Arrays.stream(args[2].split(",")).mapToLong(Long::parseLong).toArray();
        Files.createDirectory(out);
        var metadata=new LinkedHashMap<String,Object>(); metadata.put("args",args); metadata.put("seeds",seeds);
        metadata.put("java",System.getProperty("java.runtime.version")); metadata.put("os",System.getProperty("os.name")); metadata.put("arch",System.getProperty("os.arch"));
        metadata.put("jvmArgs",ManagementFactory.getRuntimeMXBean().getInputArguments()); metadata.put("corpusSeed",CORPUS_SEED); metadata.put("openingSeed",OPENING_SEED);
        metadata.put("auditSourceSha256",sha(Files.readAllBytes(Path.of("app/src/verification/java/com/ohinteractive/seedv6/tools/nnue/cglhw/BootstrapAudit.java"))));
        metadata.put("search","SearchDriver/ExactSearchAdapter/ExactSearch,1 worker,private4MiBTT per side/game,static leaves(no qsearch),no node/time limit,required equal depth,production score mappings,no book/tablebase/training/checkpoint input");
        metadata.put("openingPolicy","uniform legal 6..10 plies,unfiltered,paired colour reversal,same indexed openings every initialization,alternate game order by pair");
        Files.writeString(out.resolve("metadata.json"),JSON.toJson(metadata),StandardOpenOption.CREATE_NEW);
        var edges=corpus(16,80);
        if(mode.equals("audit"))Files.writeString(out.resolve("corpus.fens"),String.join("\n",edges.stream().map(e->Fen.fromBoard(e.parent())).toList())+"\n",StandardOpenOption.CREATE_NEW);
        try(var writer=Files.newBufferedWriter(out.resolve("results.jsonl"),StandardOpenOption.CREATE_NEW)) {
            for(long seed:seeds) {
                var network=NnueNetwork.initialized(seed); var trainer=new Brn3Trainer(seed); var model=trainer.snapshot();
                if(mode.equals("audit")) {
                    var d=diagnostics(seed,edges,network,trainer); write(writer,Map.of("type","audit","data",d));
                    write(writer,Map.of("type","performance","seed",seed,"data",performance(edges,network,model)));
                } else {
                    var config=new ValidationConfig(pairs,OPENING_SEED,6,10,depth,1,NnueScoreMapping.V1,cap);
                    var root=Board.startingPosition();
                    for(int index=0;index<pairs;index++) {
                        var opening=ValidationArena.opening(root,GameHistory.initial(root),config,index);
                        if(!opening.newGame(cap).active())throw new IllegalStateException("Inactive opening; do not silently replace");
                        var first=game(opening,network,model,index%2,depth,cap);
                        var second=game(opening,network,model,1-index%2,depth,cap);
                        write(writer,Map.of("type","pair","seed",seed,"index",index,"openingHash",opening.identity(),"openingFen",Fen.fromBoard(opening.board()),"first",first,"second",second));
                    }
                }
            }
        }
    }
    private static void write(BufferedWriter out,Object row)throws IOException {
        String json=JSON.toJson(row); out.write(json);out.newLine();out.flush();
        System.out.println(json);
    }
}
