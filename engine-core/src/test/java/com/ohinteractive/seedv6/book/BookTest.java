package com.ohinteractive.seedv6.book;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.PositionIdentity;

class BookTest {
    @Test void completeRecoveredGraphCompilesAndEveryMoveHasExactlyItsSourceWeight() throws Exception {
        Book book = Book.bundled();
        assertEquals(4161, book.positionCount());
        assertEquals(3013, book.entryCount());
        assertEquals(4580, book.moveCount());
        String source = resource();
        String body = source.lines().filter(l -> !l.startsWith("#")).collect(java.util.stream.Collectors.joining("\n", "", "\n"));
        // Frozen result of comparing all 4590 historical edges against Seed's legal
        // resolution and resulting FEN, then pooling rank support at normalized positions.
        assertEquals("a74106d100c364f39fb99c5d6698bb0d4fd5b7def127c031b66044ac7a8af15e",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8))));
        int edges = 0;
        for (String line : body.lines().toList()) {
            String[] f = line.split(" \\|", -1);
            long[] board = Board.fromFen(f[0] + " 0 1"), before = board.clone();
            long[] legal = legal(board);
            String[] tokens = f[1].isBlank() ? new String[0] : f[1].trim().split(" ");
            int total = Arrays.stream(tokens).mapToInt(t -> Integer.parseInt(t.split(":")[1])).sum();
            int ticket = 0;
            for (String token : tokens) {
                String[] m = token.split(":");
                int weight = Integer.parseInt(m[1]);
                for (int i = 0; i < weight; i++)
                    assertEquals(m[0], Move.coordinate(book.choose(board, legal, legal.length, new Ticket(ticket++, total))), line);
                edges++;
            }
            if (tokens.length == 0) assertEquals(0, book.choose(board, legal, legal.length, new Ticket(-1, 0)));
            assertArrayEquals(before, board);
        }
        assertEquals(4580, edges);
        assertSame(book, Book.bundled());
    }

    @Test void knownOpeningsHaveOrderedPreferenceAndSeededReproducibility() {
        var book = Book.bundled();
        long[] board = Board.startingPosition(), legal = legal(board);
        String[] expected = {"c2c4", "d2d4", "d2d4", "d2d4", "d2d4", "e2e4", "e2e4", "e2e4", "g1f3", "g1f3"};
        for (int i = 0; i < expected.length; i++)
            assertEquals(expected[i], Move.coordinate(book.choose(board, legal, legal.length, new Ticket(i, 10))));
        var a = new Random(82); var b = new Random(82);
        for (int i = 0; i < 100; i++) assertEquals(book.choose(board, legal, legal.length, a), book.choose(board, legal, legal.length, b));
        board = play(board, "e2e4"); legal = legal(board);
        assertNotEquals(0, book.choose(board, legal, legal.length, a));
        long[] noEp = Board.fromFen(position(board) + " 19 30");
        assertEquals(book.choose(board, legal, legal.length, new Random(9)),
                book.choose(noEp, legal(noEp), legal(noEp).length, new Random(9)));
        board = play(Board.startingPosition(), "a2a3"); legal = legal(board);
        assertEquals(0, book.choose(board, legal, legal.length, a));
    }

    @Test void moveOrderTranspositionsShareAllChoicesAndWeights() {
        long[] a = Board.startingPosition(), b = Board.startingPosition();
        for (String move : new String[]{"g1f3", "g8f6", "d2d4", "d7d5"}) a = play(a, move);
        for (String move : new String[]{"d2d4", "d7d5", "g1f3", "g8f6"}) b = play(b, move);
        assertEquals(position(a), position(b));
        var first = new Random(2); var second = new Random(2);
        for (int i = 0; i < 100; i++)
            assertEquals(Book.bundled().choose(a, legal(a), legal(a).length, first),
                    Book.bundled().choose(b, legal(b), legal(b).length, second));
    }

    @Test void unsafeOrStaleEntryFallsBackWithoutSubstitutingAnotherMove() {
        long[] board = Board.startingPosition(), legal = legal(board);
        long forbidden = playMove(board, "d2d4");
        long[] incomplete = Arrays.stream(legal).filter(m -> m != forbidden).toArray();
        assertEquals(0, Book.bundled().choose(board, incomplete, incomplete.length, new Ticket(-1, 0)));
        long[] collision = play(board, "a2a3"); collision[Board.KEY] = board[Board.KEY];
        assertEquals(0, Book.bundled().choose(collision, legal, legal.length, new Ticket(-1, 0)));
        assertEquals(0, Book.EMPTY.choose(board, legal, legal.length, new Ticket(-1, 0)));
    }

    @Test void compilerRejectsBadSyntaxWeightsMovesMissingTargetsAndOrphans() throws Exception {
        String graph = graph(Board.startingPosition(), "e2e4:3", "d2d4:1");
        assertEquals(2, Book.compile(new StringReader(graph)).moveCount());
        for (String bad : new String[]{
                graph.replace("e2e4:3", "e2e5:3"), graph.replace("e2e4:3", "e2e4:0"),
                graph.replace("e2e4:3", "e2e4:-1"), graph.replace("e2e4:3", "e2e4:2147483648"),
                graph.replace("e2e4:3", "e2e4:2147483647"), graph.replace("e2e4:3", "e2e4:3 e2e4:1"),
                graph.replace("e2e4:3", "e2e4x:3"), graph.replace("e2e4:3", "i2e4:3"),
                graph.replace("e2e4:3", "e2e4q:3"), graph.lines().findFirst().orElseThrow(),
                graph + graph, graph + position(play(Board.startingPosition(), "a2a3")) + " |\n",
                "not a position", "", graph.replace("KQkq", "QQkq")})
            assertThrows(IllegalArgumentException.class, () -> Book.compile(new StringReader(bad)), bad);
        assertThrows(IOException.class, () -> Book.compile(new java.io.Reader() {
            public int read(char[] b, int o, int l) throws IOException { throw new IOException("fixture"); }
            public void close() {}
        }));
    }

    @Test void castlingEnPassantAndEveryPromotionResolveToExactNativeLegalMoves() throws Exception {
        checkSpecial("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1", "e1c1");
        checkSpecial("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1", "e8g8", "e8c8");
        checkSpecial("7k/8/8/3pP3/8/8/8/7K w - d6 0 1", "e5d6");
        checkSpecial("7k/8/8/8/3Pp3/8/8/7K b - d3 0 1", "e4d3");
        checkSpecial("7k/P7/8/8/8/8/8/7K w - - 0 1", "a7a8q", "a7a8r", "a7a8b", "a7a8n");
        checkSpecial("7k/8/8/8/8/8/p7/7K b - - 0 1", "a2a1q", "a2a1r", "a2a1b", "a2a1n");
        long[] pinned = Board.fromFen("k3r3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        String graph = graph(pinned, "e5e6:1");
        Book book = Book.compile(new StringReader(graph), pinned);
        long[] normalized = Board.fromFen(position(pinned) + " 0 1");
        assertEquals(pick(book, pinned, 0, 1), pick(book, normalized, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Book.compile(new StringReader(graph.replace("e5e6", "e5d6")), pinned));
    }

    private static void checkSpecial(String fen, String... moves) throws Exception {
        long[] board = Board.fromFen(fen);
        String[] tokens = Arrays.stream(moves).map(m -> m + ":1").toArray(String[]::new);
        Book book = Book.compile(new StringReader(graph(board, tokens)), board);
        for (int i = 0; i < moves.length; i++) assertEquals(playMove(board, moves[i]), pick(book, board, i, moves.length));
    }
    private static long pick(Book book, long[] board, int ticket, int total) {
        long[] legal = legal(board); return book.choose(board, legal, legal.length, new Ticket(ticket, total));
    }
    private static String graph(long[] root, String... tokens) {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put(position(root), String.join(" ", tokens));
        for (String token : tokens) rows.put(position(play(root, token.split(":")[0])), "");
        StringBuilder text = new StringBuilder();
        rows.forEach((p, m) -> text.append(p).append(" | ").append(m).append('\n'));
        return text.toString();
    }
    private static String position(long[] board) {
        String[] f = Fen.fromBoard(board).split(" ");
        if (PositionIdentity.repetitionKey(board) != board[Board.KEY]) f[3] = "-";
        return String.join(" ", Arrays.copyOf(f, 4));
    }
    private static long[] play(long[] b, String uci) {
        long[] child = new long[6];
        Board.makeMoveInto(b[0], b[1], b[2], b[3], (int)b[4], b[5], playMove(b, uci), child); return child;
    }
    private static long playMove(long[] board, String uci) {
        return Arrays.stream(legal(board)).filter(m -> Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
    }
    private static long[] legal(long[] b) {
        long[] moves = new long[256];
        int n = Gen.genAll(b[0], b[1], b[2], b[3], (int)b[4], b[5], true, moves, new long[6]);
        return Arrays.copyOf(moves, n);
    }
    private static String resource() throws IOException {
        try (var stream = Book.class.getResourceAsStream("opening-book.txt")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static final class Ticket extends Random {
        private final int ticket, total;
        Ticket(int ticket, int total) { this.ticket = ticket; this.total = total; }
        @Override public int nextInt(int bound) { assertEquals(total, bound); assertTrue(ticket >= 0 && ticket < bound); return ticket; }
    }
}
