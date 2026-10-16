package com.ohinteractive.seedv6.tools.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.util.*;
import com.ohinteractive.seedv6.tools.eval.EvaluationCorpus;
import com.ohinteractive.seedv6.training.checkpoint.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Final bounded material-anchor falsification; verification source set only. */
public final class NnueCpReplacementCalibration {
    static final String TEACHER = "g000121-s000011724-604847b03dc1d1866a5f95a3fa9d1aba51fd86d012cfae2db5ef041518993dc1";
    static final String HASH = "712b905928b5235bc5618621a39de2abcc4b3ef70eef41ee6166b2ea4bc28cab";
    private static final int[] VALUES = {0, 0, 900, 500, 330, 320, 100};
    private static final String[] NAMES = {"", "K", "Q", "R", "B", "N", "P"};
    static final int[][] CLASSES = {{5,4}, {5,3}, {4,3}, {3,2}, {6,5}, {6,4}, {6,3}, {6,2}};
    private final NnueEvaluator evaluator, oracle;
    private final Map<String, Long> counts = new TreeMap<>();
    private long evaluations;
    private double oracleMax, reflectionMax, roundTripMax, restorationMax;

    private NnueCpReplacementCalibration(NnueNetwork network) {
        evaluator = new NnueEvaluator(network); oracle = NnueEvaluator.scalarOracle(network);
    }
    private void count(String reason) { counts.merge(reason, 1L, Long::sum); }
    static int code(long[] b, int s) { return Board.getSquare(b[0], b[1], b[2], b[3], s); }

    static long[] replace(long[] b, int square, int type) {
        int old = code(b, square);
        if ((old & 7) < 2 || (old & 7) > 6 || type < 2 || type > 6 || type == (old & 7))
            throw new IllegalArgumentException("Replace an occupied non-king by a different non-king type");
        long[] changed = b.clone();
        int piece = type | (old & 8);
        for (int plane = 0; plane < 4; plane++)
            changed[plane] = (changed[plane] & ~(1L << square)) | (((piece >>> plane) & 1L) << square);
        changed = Board.fromFen(Fen.fromBoard(changed)); // Rebuild key; retain all FEN state.
        if (changed[Board.STATUS] != b[Board.STATUS]
                || (changed[0] | changed[1] | changed[2]) != (b[0] | b[1] | b[2]))
            throw new AssertionError("State or occupancy changed");
        for (int s = 0; s < 64; s++)
            if (code(changed, s) != (s == square ? piece : code(b, s)))
                throw new AssertionError("Unrelated piece changed");
        return changed;
    }

    static int delta(long[] b, int square, int type) {
        int old = code(b, square);
        return ((old >>> 3) == Board.player((int) b[4]) ? 1 : -1) * (VALUES[type] - VALUES[old & 7]);
    }

    private double raw(long[] b) {
        float r = evaluator.evaluate(b);
        if (!Float.isFinite(r) || r != evaluator.raw() || evaluator.boundedValue() != StrictMath.tanh(r))
            throw new AssertionError("Pre-tanh extraction");
        double bounded = evaluator.boundedValue();
        if (Math.abs(bounded) == 1) count("evaluation_saturated");
        else roundTripMax = Math.max(roundTripMax,
                Math.abs(r - .5 * (StrictMath.log1p(bounded) - StrictMath.log1p(-bounded))));
        if (evaluations++ % 97 == 0) {
            double other = oracle.evaluate(b);
            oracleMax = Math.max(oracleMax, Math.abs(r - other));
            if (r != other || r != evaluator.evaluate(b)) throw new AssertionError("Oracle/repeat mismatch");
        }
        return r;
    }

    private record Base(String source, String cluster, int index, long[] board) {}
    private void run(List<Base> bases, Path output) throws IOException {
        Set<String> seen = new HashSet<>();
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            out.println("source\tcluster\tindex\tphase\tstm\treplaced_side\tpiece\tdirection\tfrom_piece\tto_piece\tsquare\tcp\tmaterial\tr0\tr1\treverse_r0\treverse_r1\tmax_see0\tmax_see1\tmate_in_one0\tmate_in_one1\tquiet\tfen");
            for (Base base : bases) {
                count("base_input_" + base.source);
                long[] b = base.board;
                String reason = NnueCpCalibration.invalid(b);
                if (reason != null) { count("base_reject_" + reason); continue; }
                String identity = Arrays.toString(Arrays.copyOf(b, 4)) + ":" + Board.player((int) b[4]);
                if (!seen.add(identity)) { count("base_duplicate"); continue; }
                count("base_valid_" + base.source);
                double r0 = raw(b), reverse0 = raw(NnueCpCalibration.flip(b));
                double reflected = raw(NnueCpCalibration.reflect(b));
                reflectionMax = Math.max(reflectionMax, Math.abs(r0 - reflected));
                int material = NnueCpCalibration.material(b), tactic0 = NnueCpCalibration.tactical(b);
                boolean mate0 = NnueCpCalibration.mateInOne(b);
                for (int square = 0; square < 64; square++) {
                    int c = code(b, square), from = c & 7;
                    for (int[] anchor : CLASSES) {
                        if (from != anchor[0] && from != anchor[1]) continue;
                        int to = from == anchor[0] ? anchor[1] : anchor[0];
                        String label = NAMES[anchor[0]] + "-" + NAMES[anchor[1]];
                        count("pair_attempt"); count("class_" + label + "_attempt");
                        long[] changed = replace(b, square, to);
                        reason = NnueCpCalibration.invalid(changed);
                        if (reason != null) { count("pair_reject_" + reason); count("class_" + label + "_reject_" + reason); continue; }
                        int cp = delta(b, square, to);
                        if (NnueCpCalibration.material(changed) - material != cp
                                || NnueCpCalibration.material(NnueCpCalibration.flip(changed)) != -material - cp
                                || NnueCpCalibration.material(NnueCpCalibration.reflect(changed)) != material + cp)
                            throw new AssertionError("Material/perspective delta");
                        double r1 = raw(changed), reverse1 = raw(NnueCpCalibration.flip(changed));
                        reflectionMax = Math.max(reflectionMax, Math.abs(r1 - raw(NnueCpCalibration.reflect(changed))));
                        long[] restored = replace(changed, square, from);
                        if (!Arrays.equals(restored, b)) throw new AssertionError("Exact restoration");
                        if (evaluations % 101 == 0) {
                            restorationMax = Math.max(restorationMax, Math.abs(raw(restored) - r0));
                            if (restorationMax != 0) throw new AssertionError("Restored raw");
                        }
                        int tactic1 = NnueCpCalibration.tactical(changed);
                        boolean mate1 = NnueCpCalibration.mateInOne(changed);
                        boolean quiet = tactic0 < 100 && tactic1 < 100 && !mate0 && !mate1;
                        count("pair_valid"); count("class_" + label + "_valid");
                        count(quiet ? "pair_quiet" : "pair_tactical");
                        if (quiet) count("class_" + label + "_quiet");
                        if (tactic0 >= 100) count("pair_base_tactical");
                        if (tactic1 >= 100) count("pair_replacement_tactical");
                        if (mate0 || mate1) count("pair_mate_in_one");
                        out.printf(Locale.ROOT, "%s\t%s\t%d\t%d\t%d\t%d\t%s\t%s\t%s\t%s\t%d\t%d\t%d\t%.17g\t%.17g\t%.17g\t%.17g\t%d\t%d\t%b\t%b\t%b\t%s%n",
                                base.source, base.cluster, base.index, NnueCpCalibration.phase(b), Board.player((int) b[4]), c >>> 3,
                                label, VALUES[to] > VALUES[from] ? "increase" : "decrease", NAMES[from], NAMES[to], square, cp, material,
                                r0, r1, reverse0, reverse1, tactic0, tactic1, mate0, mate1, quiet, Fen.fromBoard(b));
                    }
                }
            }
            if (out.checkError()) throw new IOException("Evidence write failed");
        }
        if (reflectionMax > 1e-6) throw new AssertionError("Canonical reflection mismatch");
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    private static CheckpointStore.Checkpoint teacher(Path root) throws Exception {
        var checkpoint = CheckpointStore.readBestSnapshot(root);
        var m = checkpoint.manifest();
        if (!m.id().equals(TEACHER) || m.generation() != 121 || m.optimizerStep() != 11724
                || !m.networkSha256().equals(HASH) || !m.architecture().schemaId().equals(NnueFeatureSchema.ID)
                || m.architecture().schemaVersion() != 1 || NnueNetworkCodec.VERSION != 1
                || !hash(root.resolve("checkpoints").resolve(TEACHER).resolve("network.nnue")).equals(HASH))
            throw new IllegalStateException("Accepted Best differs from the exact prior teacher; stop");
        return checkpoint;
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 3)
            throw new IllegalArgumentException("<store> [<4096 samples.bin> <new output directory>]");
        Path root = Path.of(args[0]);
        var checkpoint = teacher(root); // Verify before reading positions or creating evidence.
        if (args.length == 1) { System.out.println(checkpoint.manifest()); return; }
        Path samples = Path.of(args[1]), output = Path.of(args[2]);
        if (!hash(samples).equals("09aa882d43d2b2f4b198dbe6937e694e80a901e2ecc6515a3f948d3e0c4ebd50")
                || !EvaluationCorpus.rawSha256().equals("dad39174f075e77941e6e4e5d579e0c46a294b6652e22d15d614b9bd0a07f697"))
            throw new IllegalStateException("Retained corpus identity changed");
        List<Base> bases = new ArrayList<>();
        try (var in = new DataInputStream(new BufferedInputStream(Files.newInputStream(samples)))) {
            int n = in.readInt();
            if (n != 4096) throw new AssertionError("Diagnostic count");
            for (int i = 0; i < n; i++) {
                long[] b = new long[6]; for (int j = 0; j < 6; j++) b[j] = in.readLong(); in.readDouble();
                bases.add(new Base("handcrafted", "game-" + i / 32, i, b));
            }
            if (in.read() != -1) throw new AssertionError("Sample trailing bytes");
        }
        int ordinal = 0;
        for (var entry : EvaluationCorpus.entries())
            bases.add(new Base("search_corpus", "root-" + entry.rootIndex(), ordinal++, entry.board()));
        Files.createDirectory(output);
        var study = new NnueCpReplacementCalibration(checkpoint.model().nnue());
        study.run(bases, output.resolve("pairs.tsv"));
        if (!teacher(root).manifest().equals(checkpoint.manifest())) throw new AssertionError("Teacher moved");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(output.resolve("measurement.txt"), StandardCharsets.UTF_8))) {
            out.println("store=" + root.toAbsolutePath()); out.println("manifest=" + checkpoint.manifest());
            out.println("lineage=" + TrainingLineage.read(root)); out.println("sha256=" + HASH);
            out.println("schema=" + NnueFeatureSchema.ID + "; schema_version=1; codec=1; dimensions=64x2,32,1");
            out.println("samples_sha256=" + hash(samples)); out.println("corpus_sha256=" + EvaluationCorpus.rawSha256());
            out.println("pairs_sha256=" + hash(output.resolve("pairs.tsv")));
            out.println("evaluations=" + study.evaluations); out.println("round_trip_max=" + study.roundTripMax);
            out.println("oracle_max=" + study.oracleMax); out.println("reflection_max=" + study.reflectionMax);
            out.println("restoration_max=" + study.restorationMax);
            study.counts.forEach((k,v) -> out.println(k + "=" + v));
            if (out.checkError()) throw new IOException("Measurement write failed");
        }
        System.out.println(Files.readString(output.resolve("measurement.txt")));
    }
}
