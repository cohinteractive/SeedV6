package com.ohinteractive.seedv6.book;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.core.util.Zobrist;

/**
 * Immutable, shared repertoire compiled with Seed's legal mechanics. Only the
 * resource is canonical: keys, packed moves and board guards are derived data.
 * Lookup uses binary search and primitive arrays, with no parsing or allocation.
 * Randomness and legal-move buffers belong to the caller, never to this object.
 */
public final class Book {
    public static final Book EMPTY = new Book(new ArrayList<>(), 0);

    public static Book bundled() { return Bundled.BOOK; }

    private static final class Bundled {
        static final Book BOOK = loadBundled();
    }

    private static Book loadBundled() {
        var stream = Book.class.getResourceAsStream("opening-book.txt");
        if (stream == null) throw new IllegalStateException("Missing opening repertoire.");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return compile(reader);
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("Invalid opening repertoire.", error);
        }
    }

    /** Zero means miss or inconsistent data. Every stored candidate must still be legal. */
    public long choose(long[] board, long[] legalMoves, int count, RandomGenerator random) {
        if (count < 0 || count > legalMoves.length) throw new IllegalArgumentException("Invalid legal move count.");
        int status = normalizedStatus(board, legalMoves, count);
        long key = normalizedKey(board, status);
        int entry = Arrays.binarySearch(keys, key);
        if (entry < 0) return 0L;
        int guard = entry * 5;
        // Exact board guard also protects against a hash collision/stale Board.KEY.
        for (int i = 0; i < 4; i++) if (positions[guard + i] != board[i]) return 0L;
        if (positions[guard + 4] != status) return 0L;
        int from = offsets[entry], to = offsets[entry + 1];
        for (int i = from; i < to; i++) {
            boolean legal = false;
            for (int j = 0; j < count; j++) if (moves[i] == legalMoves[j]) { legal = true; break; }
            if (!legal) return 0L;
        }
        int ticket = random.nextInt(cumulativeWeights[to - 1]);
        for (int i = from; i < to; i++) if (ticket < cumulativeWeights[i]) return moves[i];
        return 0L;
    }

    public int positionCount() { return positionCount; }
    public int entryCount() { return keys.length; }
    public int moveCount() { return moves.length; }

    private final long[] keys, positions, moves;
    private final int[] offsets, cumulativeWeights;
    private final int positionCount;

    private Book(List<Entry> entries, int positionCount) {
        entries.sort(Comparator.comparingLong(Entry::key));
        this.positionCount = positionCount;
        keys = new long[entries.size()];
        positions = new long[entries.size() * 5];
        offsets = new int[entries.size() + 1];
        int total = entries.stream().mapToInt(e -> e.moves.length).sum();
        moves = new long[total];
        cumulativeWeights = new int[total];
        int offset = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (i > 0 && keys[i - 1] == e.key) throw new IllegalArgumentException("Opening position hash collision.");
            keys[i] = e.key;
            System.arraycopy(e.board, 0, positions, i * 5, 4);
            positions[i * 5 + 4] = e.status;
            offsets[i] = offset;
            int cumulative = 0;
            for (int j = 0; j < e.moves.length; j++) {
                moves[offset] = e.moves[j];
                cumulative = Math.addExact(cumulative, e.weights[j]);
                cumulativeWeights[offset++] = cumulative;
            }
        }
        offsets[entries.size()] = offset;
    }

    /** Complete directed graph, including leaf rows; rejects illegal, missing and unreachable edges/rows. */
    static Book compile(Reader source) throws IOException {
        return compile(source, Board.startingPosition());
    }

    // Package seam for small special-move fixtures; the bundled book always starts at the initial position.
    static Book compile(Reader source, long[] initial) throws IOException {
        Map<String, Row> rows = new HashMap<>();
        var reader = new BufferedReader(source);
        String line;
        int number = 0;
        while ((line = reader.readLine()) != null) {
            number++;
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split(" \\|", -1);
            if (fields.length != 2 || fields[0].split(" ", -1).length != 4)
                throw invalid(number, "Expected four FEN fields | move:weight ...");
            String[] tokens = fields[1].isBlank() ? new String[0] : fields[1].trim().split(" +");
            MoveIntent[] intents = new MoveIntent[tokens.length];
            int[] weights = new int[tokens.length];
            var unique = new HashSet<String>();
            long total = 0;
            for (int i = 0; i < tokens.length; i++) {
                if (!tokens[i].matches("[a-h][1-8][a-h][1-8][qrbn]?:[1-9][0-9]*"))
                    throw invalid(number, "Invalid move or weight: " + tokens[i]);
                String[] parts = tokens[i].split(":");
                if (!unique.add(parts[0])) throw invalid(number, "Duplicate move: " + parts[0]);
                try { weights[i] = Integer.parseInt(parts[1]); }
                catch (NumberFormatException error) { throw invalid(number, "Weight overflow."); }
                total += weights[i];
                if (total > Integer.MAX_VALUE) throw invalid(number, "Weight total overflow.");
                intents[i] = intent(parts[0]);
            }
            if (rows.putIfAbsent(fields[0], new Row(number, intents, weights)) != null)
                throw invalid(number, "Duplicate position.");
        }
        var resolver = new LegalMoveResolver();
        var queue = new ArrayDeque<long[]>();
        var visited = new HashSet<String>();
        var entries = new ArrayList<Entry>();
        long[] legal = new long[256], scratch = new long[Board.MAX_BITBOARDS];
        queue.add(initial.clone());
        while (!queue.isEmpty()) {
            long[] board = queue.remove();
            int count = Gen.genAll(board[0], board[1], board[2], board[3],
                    (int) board[Board.STATUS], board[Board.KEY], true, legal, scratch);
            int status = normalizedStatus(board, legal, count);
            String position = position(board, status);
            if (!visited.add(position)) continue;
            Row row = rows.get(position);
            if (row == null) throw new IllegalArgumentException("Missing opening position: " + position);
            long[] moves = new long[row.intents.length];
            for (int i = 0; i < moves.length; i++) {
                try { moves[i] = resolver.resolve(board, row.intents[i]); }
                catch (IllegalArgumentException | IllegalStateException error) {
                    throw invalid(row.line, "Illegal or ambiguous move: " + row.intents[i]);
                }
                long[] child = new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(board[0], board[1], board[2], board[3],
                        (int) board[Board.STATUS], board[Board.KEY], moves[i], child);
                queue.add(child);
            }
            if (moves.length > 0)
                entries.add(new Entry(normalizedKey(board, status), board, status, moves, row.weights));
        }
        if (visited.size() != rows.size()) throw new IllegalArgumentException("Unreachable/noncanonical opening positions.");
        return new Book(entries, visited.size());
    }

    private static MoveIntent intent(String uci) {
        var promotion = uci.length() == 4 ? MoveIntent.Promotion.NONE : switch (uci.charAt(4)) {
            case 'q' -> MoveIntent.Promotion.QUEEN;
            case 'r' -> MoveIntent.Promotion.ROOK;
            case 'b' -> MoveIntent.Promotion.BISHOP;
            case 'n' -> MoveIntent.Promotion.KNIGHT;
            default -> throw new IllegalArgumentException("Invalid promotion.");
        };
        return new MoveIntent((uci.charAt(1) - '1') * 8 + uci.charAt(0) - 'a',
                (uci.charAt(3) - '1') * 8 + uci.charAt(2) - 'a', promotion);
    }

    private static int normalizedStatus(long[] board, long[] legal, int count) {
        int status = (int) board[Board.STATUS] & ((1 << Board.HALF_MOVE_CLOCK_SHIFT) - 1);
        int ep = Board.enPassantSquare(status);
        if (ep != Value.INVALID) {
            for (int i = 0; i < count; i++) {
                long move = legal[i];
                if (Move.toSquare(move) == ep
                        && ((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN
                        && ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) == Value.NONE)
                    return status;
            }
            status &= (int) Board.ENPASSANT_RESET_BITS;
        }
        return status;
    }

    private static long normalizedKey(long[] board, int status) {
        int original = Board.enPassantSquare((int) board[Board.STATUS]);
        return original != Value.INVALID && Board.enPassantSquare(status) == Value.INVALID
                ? board[Board.KEY] ^ Zobrist.ENPASSANT_FILE[original & Value.FILE] : board[Board.KEY];
    }

    private static String position(long[] board, int status) {
        String[] fields = Fen.fromBoard(board).split(" ");
        if (Board.enPassantSquare(status) == Value.INVALID) fields[3] = "-";
        return String.join(" ", Arrays.copyOf(fields, 4));
    }

    private static IllegalArgumentException invalid(int line, String reason) {
        return new IllegalArgumentException("Opening repertoire line " + line + ": " + reason);
    }

    private record Row(int line, MoveIntent[] intents, int[] weights) {}
    private record Entry(long key, long[] board, int status, long[] moves, int[] weights) {}
}
