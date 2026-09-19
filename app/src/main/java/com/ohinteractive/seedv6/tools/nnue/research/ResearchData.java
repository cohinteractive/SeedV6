package com.ohinteractive.seedv6.tools.nnue.research;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.ValidationArena;

/** Small experimental datasets are distinct from authoritative checkpoint/promotion records. */
final class ResearchData {
    static void append(Path path, Object... fields) throws IOException {
        StringJoiner line = new StringJoiner("\t");
        for (Object field : fields) line.add(String.valueOf(field).replace('\n', ' ').replace('\r', ' ').replace('\t', ' '));
        Files.writeString(path, line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    static String gameHash(GameTrajectory game) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (var position : game.positions()) digest.update(java.nio.ByteBuffer.allocate(8).putLong(position.playedMove()).array());
            digest.update(ValidationArena.stateHash(game.finalBoard(), game.history()).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    static String openingHash(GameTrajectory game, SelfPlayConfig config, int index) {
        int desired = (int) new SplittableRandom(SelfPlayRunner.gameSeed(config.seed(), index))
                .nextLong(config.minimumOpeningPlies(), (long) config.maximumOpeningPlies() + 1);
        long[] start = game.positions().isEmpty() ? game.finalBoard() : game.positions().getFirst().board();
        HeadlessGame opening = new HeadlessGame(start, Integer.MAX_VALUE);
        for (int i = 0; i < Math.min(desired, game.playedPlies()); i++) opening.play(game.positions().get(i).playedMove());
        return ValidationArena.stateHash(opening.boardSnapshot(), opening.historySnapshot());
    }
    static void holdout(DataOutputStream out, long generation, int game, List<TrajectorySampler.Sample> samples) throws IOException {
        out.writeLong(generation); out.writeInt(game); out.writeInt(samples.size());
        for (var sample : samples) { for (long bitboard : sample.board()) out.writeLong(bitboard); out.writeDouble(sample.target()); }
        out.flush();
    }
    record Calibration(int games, int samples, double mse, NetworkHealth.Distribution predictions,
                       NetworkHealth.Distribution targets, long negativeTargets, long drawTargets, long positiveTargets) {}
    static Calibration calibrate(Path path, NnueNetwork network) throws IOException {
        List<Double> predictions = new ArrayList<>(), targets = new ArrayList<>();
        NnueEvaluator evaluator = new NnueEvaluator(network);
        double sum = 0; int games = 0; long negative = 0, draw = 0, positive = 0;
        try (var in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            if (in.readInt() != 0x4A484F31) throw new IOException("Unknown holdout dataset.");
            while (in.available() > 0) {
                in.readLong(); in.readInt(); int count = in.readInt();
                if (count < 1 || count > 32) throw new IOException("Invalid holdout game.");
                games++;
                for (int i = 0; i < count; i++) {
                    long[] board = new long[Board.MAX_BITBOARDS];
                    for (int b = 0; b < board.length; b++) board[b] = in.readLong();
                    double target = in.readDouble();
                    if (target != -1 && target != 0 && target != 1) throw new IOException("Invalid W/D/L target.");
                    evaluator.evaluate(board); double prediction = evaluator.boundedValue();
                    predictions.add(prediction); targets.add(target); sum += (prediction - target) * (prediction - target);
                    if (target < 0) negative++; else if (target > 0) positive++; else draw++;
                }
            }
        }
        return new Calibration(games, predictions.size(), sum / predictions.size(),
                NetworkHealth.distribution(predictions.size(), predictions::get), NetworkHealth.distribution(targets.size(), targets::get), negative, draw, positive);
    }
    static void diversity(Path root) throws IOException {
        int completed = 0, aborted = 0, caps = 0, white = 0, draw = 0, black = 0, min = Integer.MAX_VALUE, max = 0;
        long plies = 0; Set<String> openings = new HashSet<>(), games = new HashSet<>();
        Map<String, Integer> reasons = new TreeMap<>();
        List<String> lines = Files.readAllLines(root.resolve("games.tsv"));
        for (String line : lines) {
            String[] f = line.split("\t");
            GameTermination reason = GameTermination.valueOf(f[3]); int length = Integer.parseInt(f[4]);
            openings.add(f[5]); games.add(f[6]); reasons.merge(reason.name(), 1, Integer::sum);
            if (reason.completed()) {
                completed++; plies += length; min = Math.min(min, length); max = Math.max(max, length);
                switch (reason.result().orElseThrow()) { case WHITE_WIN -> white++; case DRAW -> draw++; case BLACK_WIN -> black++; }
            } else { aborted++; if (reason == GameTermination.PLY_CAP) caps++; }
        }
        ResearchTool.out("DIVERSITY", "attempted", lines.size(), "completed", completed, "white/draw/black", white + "/" + draw + "/" + black,
                "rates", (double) white / completed, (double) draw / completed, (double) black / completed,
                "pliesMean/min/max", (double) plies / completed, completed == 0 ? 0 : min, max,
                "distinctOpenings", openings.size(), "repeatedOpeningRate", 1.0 - (double) openings.size() / lines.size(),
                "repeatedGameRate", 1.0 - (double) games.size() / lines.size(), "capped", caps, "aborted", aborted,
                "abortedRate", (double) aborted / lines.size(), "terminations", reasons);
    }
    private ResearchData() {}
}
