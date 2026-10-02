package com.ohinteractive.seedv6.tools.nnue;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brn2.Brn2MaterialPrior;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.util.*;
import com.ohinteractive.seedv6.tools.eval.EvaluationCorpus;
import com.ohinteractive.seedv6.training.checkpoint.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Research only: no store writer, search, training, or shipped dependency. */
public final class NnueCpCalibration {
    private static final int[] VALUES = {0, 0, 900, 500, 330, 320, 100};
    private static final String[] NAMES = {"", "king", "queen", "rook", "bishop", "knight", "pawn"};
    private final NnueEvaluator evaluator, oracle;
    private final Map<String, Long> counts = new TreeMap<>();
    private double roundTripMax, oracleMax, reflectionMax;
    private long evaluations;

    private NnueCpCalibration(NnueNetwork network) {
        evaluator = new NnueEvaluator(network);
        oracle = NnueEvaluator.scalarOracle(network);
    }

    private void count(String reason) { counts.merge(reason, 1L, Long::sum); }

    private double raw(long[] board) {
        float r = evaluator.evaluate(board);
        if (!Float.isFinite(r) || r != evaluator.raw()) throw new AssertionError("Raw extraction");
        double bounded = evaluator.boundedValue();
        if (bounded != StrictMath.tanh(r)) throw new AssertionError("Bounded extraction");
        if (Math.abs(bounded) == 1) count("evaluation_saturated");
        else roundTripMax = Math.max(roundTripMax,
                Math.abs(r - .5 * (StrictMath.log1p(bounded) - StrictMath.log1p(-bounded))));
        if (evaluations++ % 97 == 0) {
            double other = oracle.evaluate(board);
            oracleMax = Math.max(oracleMax, Math.abs(r - other));
            if (r != other) throw new AssertionError("Scalar oracle mismatch");
            if (r != evaluator.evaluate(board)) throw new AssertionError("Repeat mismatch");
        }
        return r;
    }

    private static int code(long[] b, int s) { return Board.getSquare(b[0], b[1], b[2], b[3], s); }
    private static boolean check(long[] b, int side) {
        return Board.isPlayerInCheck(b[0], b[1], b[2], b[3], side);
    }

    static long[] flip(long[] b) {
        long[] result = b.clone(); result[Board.STATUS] ^= Board.PLAYER_BIT;
        return Board.fromFen(Fen.fromBoard(result));
    }

    static long[] reflect(long[] b) {
        long[] result = b.clone();
        for (int i = 0; i < 4; i++) result[i] = Long.reverseBytes(b[i]);
        result[3] ^= result[0] | result[1] | result[2];
        int status = (int) b[Board.STATUS], rights = (status >>> Board.CASTLING_SHIFT) & 15;
        int swapped = ((rights & 3) << 2) | ((rights >>> 2) & 3);
        result[Board.STATUS] = ((status ^ 1) & ~(15 << Board.CASTLING_SHIFT)) | (swapped << Board.CASTLING_SHIFT);
        return Board.fromFen(Fen.fromBoard(result)); // Sources with EP are excluded before this call.
    }

    static int phase(long[] b) {
        int remaining = 0;
        for (int s = 0; s < 64; s++) remaining += switch (code(b, s) & 7) {
            case Piece.QUEEN -> 4; case Piece.ROOK -> 2;
            case Piece.BISHOP, Piece.KNIGHT -> 1; default -> 0;
        };
        return Math.max(0, Math.min(24, 24 - remaining)); // Existing Eval/EvaluationCorpus measure.
    }

    static int material(long[] b) {
        int total = 0, stm = Board.player((int) b[Board.STATUS]);
        for (int s = 0; s < 64; s++) {
            int c = code(b, s), type = c & 7;
            if (type > 6) throw new AssertionError("Invalid piece");
            total += ((c >>> 3) == stm ? 1 : -1) * VALUES[type];
        }
        if (total != Brn2MaterialPrior.BASIC_V1.score(b)) throw new AssertionError("Canonical material");
        return total;
    }

    static String invalid(long[] b) {
        long kings = b[0] & ~b[1] & ~b[2];
        if (Long.bitCount(kings & ~b[3]) != 1 || Long.bitCount(kings & b[3]) != 1) return "kings";
        for (int s = 0; s < 64; s++) {
            int c = code(b, s), type = c & 7;
            if (type == 7 || (type == 0 && c != 0)) return "piece_encoding";
            if (type == Piece.PAWN && (s / 8 == 0 || s / 8 == 7)) return "pawn_rank";
        }
        int status = (int) b[Board.STATUS];
        if (Board.enPassantSquare(status) >= 0) return "en_passant";
        int[] squares = {7, 0, 63, 56}, rookCodes = {3, 3, 11, 11};
        for (int i = 0; i < 4; i++) if ((status & (1 << (i + 1))) != 0
                && (code(b, squares[i]) != rookCodes[i] || code(b, i < 2 ? 4 : 60) != (i < 2 ? 1 : 9)))
            return "castling";
        if (check(b, 0) || check(b, 1)) return "check";
        if (Board.halfMoveClock(status) >= 100) return "fifty_move";
        if (!Gen.hasLegalMoveNotInCheck(b[0], b[1], b[2], b[3], status)) return "no_legal_move";
        // Reject dead material using a conservative subset of rule-drawn positions.
        int nonKings = Long.bitCount(b[0] | b[1] | b[2]) - 2;
        if (nonKings <= 1 && (b[1] == 0)) return "dead_material";
        return null;
    }

    /** Cheap tactical classification: profitable legal SEE capture >= a pawn, or promotion, either side. */
    static int tactical(long[] b) {
        int maximum = 0;
        long[] moves = new long[256], scratch = new long[6];
        for (int side = 0; side < 2; side++) {
            long[] oriented = Board.player((int) b[Board.STATUS]) == side ? b : flip(b);
            int n = Gen.genAll(oriented[0], oriented[1], oriented[2], oriented[3], (int) oriented[4], oriented[5], true, moves, scratch);
            for (int i = 0; i < n; i++) {
                long move = moves[i];
                int promotion = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & 15;
                int capture = (int) (move >>> Board.TARGET_PIECE_SHIFT) & 15;
                if (promotion != 0) maximum = Math.max(maximum, 900);
                if (capture != 0) maximum = Math.max(maximum, See.evaluate(oriented, move));
            }
        }
        return maximum;
    }

    /** Bounded legality probe, not a search score: detect immediate mating moves for either side. */
    static boolean mateInOne(long[] b) {
        long[] moves = new long[256], replies = new long[256], child = new long[6], scratch = new long[6];
        for (int side = 0; side < 2; side++) {
            long[] oriented = Board.player((int) b[Board.STATUS]) == side ? b : flip(b);
            int n = Gen.genAll(oriented[0], oriented[1], oriented[2], oriented[3], (int) oriented[4], oriented[5], true, moves, scratch);
            for (int i = 0; i < n; i++) {
                Board.makeMoveInto(oriented[0], oriented[1], oriented[2], oriented[3], (int) oriented[4], oriented[5], moves[i], child);
                if (check(child, 1 ^ side) && Gen.genAll(child[0], child[1], child[2], child[3], (int) child[4], child[5], true, replies, scratch) == 0)
                    return true;
            }
        }
        return false;
    }

    private record Base(String source, String cluster, int index, long[] board) {}

    private void run(List<Base> bases, Path output) throws IOException {
        Set<String> seen = new HashSet<>();
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            out.println("source\tcluster\tindex\tphase\tstm\tremoved_side\tpiece\tsquare\tcp\tmaterial\tr0\tr1\treverse_r0\treverse_r1\tmax_see0\tmax_see1\tmate_in_one0\tmate_in_one1\tquiet\tfen");
            for (Base base : bases) {
                count("base_input_" + base.source);
                long[] b = base.board;
                String reason = invalid(b);
                if (reason != null) { count("base_reject_" + reason); continue; }
                String identity = Arrays.toString(Arrays.copyOf(b, 4)) + ":" + Board.player((int) b[4]);
                if (!seen.add(identity)) { count("base_duplicate"); continue; }
                count("base_valid_" + base.source);
                double r0 = raw(b), reverse0 = raw(flip(b));
                double reflected = raw(reflect(b));
                reflectionMax = Math.max(reflectionMax, Math.abs(reflected - r0));
                if (material(reflect(b)) != material(b) || material(flip(b)) != -material(b))
                    throw new AssertionError("Perspective transform");
                int tactic0 = tactical(b), m0 = material(b);
                boolean mate0 = mateInOne(b);
                for (int square = 0; square < 64; square++) {
                    int c = code(b, square), type = c & 7;
                    if (type < 2 || type > 6) continue;
                    count("pair_attempt");
                    long[] changed = b.clone();
                    for (int plane = 0; plane < 4; plane++) changed[plane] &= ~(1L << square);
                    // Canonical parser rebuilds the key, preserves all original status fields.
                    changed = Board.fromFen(Fen.fromBoard(changed));
                    reason = invalid(changed);
                    if (reason != null) { count("pair_reject_" + reason); continue; }
                    int delta = ((c >>> 3) == Board.player((int) b[4]) ? -1 : 1) * VALUES[type];
                    if (material(changed) - m0 != delta || changed[4] != b[4]
                            || material(flip(changed)) + m0 != -delta) throw new AssertionError("Perturbation delta");
                    double r1 = raw(changed), reverse1 = raw(flip(changed));
                    int tactic1 = tactical(changed);
                    boolean mate1 = mateInOne(changed);
                    boolean quiet = tactic0 < 100 && tactic1 < 100 && !mate0 && !mate1;
                    if (mate0 || mate1) count("pair_mate_in_one");
                    count("pair_valid"); count(quiet ? "pair_quiet" : "pair_tactical");
                    out.printf(Locale.ROOT, "%s\t%s\t%d\t%d\t%d\t%d\t%s\t%d\t%d\t%d\t%.17g\t%.17g\t%.17g\t%.17g\t%d\t%d\t%b\t%b\t%b\t%s%n",
                            base.source, base.cluster, base.index, phase(b), Board.player((int) b[4]), c >>> 3,
                            NAMES[type], square, delta, m0, r0, r1, reverse0, reverse1, tactic0, tactic1, mate0, mate1, quiet, Fen.fromBoard(b));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("<NNUE store> <best|checkpoint ID> <4096 sample binary> <new output directory>");
        Path root = Path.of(args[0]), samples = Path.of(args[2]), output = Path.of(args[3]);
        Files.createDirectory(output); // Never overwrite a previous run.
        var checkpoint = args[1].equals("best") ? CheckpointStore.readBestSnapshot(root) : CheckpointStore.readSnapshot(root, args[1]);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                Files.readAllBytes(root.resolve("checkpoints").resolve(checkpoint.manifest().id()).resolve("network.nnue"))));
        if (!hash.equals(checkpoint.manifest().networkSha256())) throw new AssertionError("Teacher hash");
        List<Base> bases = new ArrayList<>();
        try (var in = new DataInputStream(new BufferedInputStream(Files.newInputStream(samples)))) {
            int n = in.readInt();
            if (n != 4096) throw new IllegalArgumentException("This reproduction uses the accepted 128 x 32 diagnostic dataset");
            for (int i = 0; i < n; i++) {
                long[] b = new long[6]; for (int j = 0; j < 6; j++) b[j] = in.readLong(); in.readDouble();
                bases.add(new Base("handcrafted", "game-" + (i / 32), i, b));
            }
            if (in.read() != -1) throw new AssertionError("Sample trailing bytes");
        }
        int ordinal = 0;
        for (var entry : EvaluationCorpus.entries())
            bases.add(new Base("search_corpus", "root-" + entry.rootIndex(), ordinal++, entry.board()));
        var study = new NnueCpCalibration(checkpoint.model().nnue());
        study.run(bases, output.resolve("pairs.tsv"));
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(output.resolve("measurement.txt"), StandardCharsets.UTF_8))) {
            out.println("store=" + root.toAbsolutePath()); out.println("manifest=" + checkpoint.manifest());
            out.println("lineage=" + TrainingLineage.read(root)); out.println("sha256=" + hash);
            out.println("schema=" + NnueFeatureSchema.ID + "; dimensions=64x2,32,1");
            out.println("samples_sha256=" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(samples))));
            out.println("corpus_sha256=" + EvaluationCorpus.rawSha256());
            out.println("evaluations=" + study.evaluations); out.println("round_trip_max=" + study.roundTripMax);
            out.println("oracle_max=" + study.oracleMax); out.println("reflection_max=" + study.reflectionMax);
            study.counts.forEach((k,v) -> out.println(k + "=" + v));
        }
        System.out.println(Files.readString(output.resolve("measurement.txt")));
    }
}
