package com.ohinteractive.seedv6.uci;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.Main;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class UciProcessTest {

    @Test
    void actualMainProvidesHandshakeReadinessAndCleanQuitWithoutStartupNoise() throws Exception {
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("uci");
            assertEquals("id name SeedV6", engine.readLine());
            assertEquals("id author Charles Clark", engine.readLine());
            assertEquals("uciok", engine.readLine());
            engine.send("isready");
            assertEquals("readyok", engine.readLine());
            engine.send("quit");
            engine.awaitExit();
            assertEquals(0, engine.exitCode());
            assertEquals("", engine.stderr());
            assertEquals(4, engine.lines().size());
        }
    }

    @Test
    void actualMainExitsCleanlyOnEofWithoutQuit() throws Exception {
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("uci");
            assertEquals("id name SeedV6", engine.readLine());
            assertEquals("id author Charles Clark", engine.readLine());
            assertEquals("uciok", engine.readLine());
            engine.eof();
            engine.awaitExit();
            assertEquals(0, engine.exitCode());
            assertEquals("", engine.stderr());
            assertEquals(3, engine.lines().size());
        }
    }

    @Test
    void invalidGoFormsAreQuietAndDoNotBreakLaterCommands() throws Exception {
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("go depth 0");
            engine.send("go depth -1");
            engine.send("go depth nope");
            engine.send("go depth " + (FlatNegamax.MAX_SUPPORTED_DEPTH + 1));
            engine.send("go depth");
            engine.send("go");
            engine.send("go depth 1 extra");
            engine.send("go infinite nodes 1");
            engine.send("go nodes -1");
            engine.send("go movetime 9223372036854775808");
            engine.send("isready");
            assertEquals("readyok", engine.readLine());
            engine.send("go depth 1");
            assertLegalBestMove(Board.startingPosition(), engine.readLine());
            engine.send("quit");
            engine.awaitExit();
            assertEquals(2, engine.lines().size());
            assertEquals("", engine.stderr());
        }
    }

    @Test
    void sequentialAsynchronousDepthSearchPreservesPositionAndExactChessSemantics() throws Exception {
        final String fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1";
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("position startpos");
            engine.send("go depth 1");
            assertLegalBestMove(Board.startingPosition(), engine.readLine());

            engine.send("position fen " + fen + " moves e2e3 e8e7");
            final UciSession expected = new UciSession();
            expected.setPosition(("position fen " + fen + " moves e2e3 e8e7").split("\\s+"));
            engine.send("go depth 2");
            assertLegalBestMove(expected.boardSnapshot(), engine.readLine());
            engine.send("quit");
            engine.awaitExit();
            assertEquals(2, engine.lines().size());
            assertEquals("", engine.stderr());
        }
    }

    @Test
    void terminalRootsCompleteImmediatelyUnderInfiniteAndTimedSearch() throws Exception {
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("position fen 7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
            engine.send("go infinite");
            assertEquals("bestmove 0000", engine.readLine());
            engine.send("position fen 7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
            engine.send("go movetime 100");
            assertEquals("bestmove 0000", engine.readLine());
            engine.send("quit");
            engine.awaitExit();
            assertEquals(List.of("bestmove 0000", "bestmove 0000"), engine.lines());
            assertEquals("", engine.stderr());
        }
    }

    @Test
    void goInfiniteKeepsInputResponsiveAndStopPublishesExactlyOnce() throws Exception {
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("go infinite");
            engine.send("isready");
            assertEquals("readyok", engine.readLine());
            final long start = System.nanoTime();
            engine.send("stop");
            assertLegalBestMove(Board.startingPosition(), engine.readLine());
            final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsedMillis < 5_000L, "stop took " + elapsedMillis + " ms");
            engine.send("stop");
            engine.send("quit");
            engine.awaitExit();
            assertEquals(2, engine.lines().size());
            assertEquals("", engine.stderr());
        }
    }

    @Test
    void nodesMovetimeAndClockGoCommandsAllTerminateWithLegalMoves() throws Exception {
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("go nodes 20");
            assertLegalBestMove(Board.startingPosition(), engine.readLine());

            final long movetimeStart = System.nanoTime();
            engine.send("go movetime 100");
            assertLegalBestMove(Board.startingPosition(), engine.readLine());
            final long movetimeElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - movetimeStart);
            assertTrue(movetimeElapsed < 5_000L, "movetime search took " + movetimeElapsed + " ms");

            engine.send("go wtime 3000 btime 9000 winc 100 binc 900 movestogo 30");
            assertLegalBestMove(Board.startingPosition(), engine.readLine());
            engine.send("quit");
            engine.awaitExit();
            assertEquals(3, engine.lines().size());
            assertEquals("", engine.stderr());
        }
    }

    @Test
    void backToBackGoReplacesAndSuppressesOldGeneration() throws Exception {
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("go infinite");
            engine.send("go nodes 0");
            assertLegalBestMove(Board.startingPosition(), engine.readLine());
            engine.send("quit");
            engine.awaitExit();
            assertEquals(1, engine.lines().size());
            assertEquals("", engine.stderr());
        }
    }

    @Test
    void positionReplacementSuppressesActiveOldPositionResult() throws Exception {
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("go infinite");
            engine.send("position fen 7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
            engine.send("go infinite");
            assertEquals("bestmove 0000", engine.readLine());
            engine.send("quit");
            engine.awaitExit();
            assertEquals(List.of("bestmove 0000"), engine.lines());
            assertEquals("", engine.stderr());
        }
    }

    @Test
    void newGameInvalidatesActiveSearchAndResetsPositionHistory() throws Exception {
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("go infinite");
            engine.send("ucinewgame");
            engine.send("go nodes 0");
            assertLegalBestMove(Board.startingPosition(), engine.readLine());
            engine.send("quit");
            engine.awaitExit();
            assertEquals(1, engine.lines().size());
            assertEquals("", engine.stderr());
        }
    }

    @Test
    void quitAndEofDuringSearchAreBoundedAndSuppressBestmove() throws Exception {
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("go infinite");
            engine.send("isready");
            assertEquals("readyok", engine.readLine());
            final long start = System.nanoTime();
            engine.send("quit");
            engine.awaitExit();
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 5_000L);
            assertEquals(List.of("readyok"), engine.lines());
            assertEquals("", engine.stderr());
        }

        try(EngineSession engine = EngineSession.launch()) {
            engine.send("go infinite");
            engine.send("isready");
            assertEquals("readyok", engine.readLine());
            final long start = System.nanoTime();
            engine.eof();
            engine.awaitExit();
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 5_000L);
            assertEquals(List.of("readyok"), engine.lines());
            assertEquals("", engine.stderr());
        }
    }

    @Test
    void malformedPositionDoesNotReplacePreviouslyAcceptedProcessState() throws Exception {
        final String accepted = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1";
        try(EngineSession engine = EngineSession.launch()) {
            engine.send("position fen " + accepted);
            engine.send("position startpos moves e2e4 e7e5 e1e5");
            engine.send("go depth 1");
            assertLegalBestMove(Board.fromFen(accepted), engine.readLine());
            engine.send("quit");
            engine.awaitExit();
            assertEquals(1, engine.lines().size());
            assertEquals("", engine.stderr());
        }
    }

    private static void assertLegalBestMove(long[] board, String output) {
        assertTrue(output.startsWith("bestmove "), output);
        final String coordinate = output.substring("bestmove ".length());
        assertFalse(coordinate.equals("0000"), output);
        final long resolved = new LegalMoveResolver().resolve(board, UciMoveParser.parse(coordinate));
        assertEquals(coordinate, Move.coordinate(resolved));
    }

    private static final class EngineSession implements AutoCloseable {
        static EngineSession launch() throws Exception {
            final Path java = Path.of(
                System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java"
            );
            final Path mainClasses = Path.of(
                Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            return new EngineSession(new ProcessBuilder(
                java.toString(), "-cp", mainClasses.toString(), Main.class.getName()
            ).start());
        }

        private EngineSession(Process process) {
            this.process = process;
            input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            error = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
        }

        void send(String command) throws IOException {
            input.write(command);
            input.newLine();
            input.flush();
        }

        String readLine() throws Exception {
            final Future<String> future = readers.submit(output::readLine);
            try {
                final String line = future.get(5L, TimeUnit.SECONDS);
                if(line == null) fail("SeedV6 exited before producing the expected UCI line.");
                lines.add(line);
                return line;
            } catch(TimeoutException exception) {
                future.cancel(true);
                fail("SeedV6 did not produce the expected UCI line within five seconds.");
                return null;
            } catch(ExecutionException exception) {
                throw new IOException(exception.getCause());
            }
        }

        void eof() throws IOException {
            input.close();
        }

        void awaitExit() throws Exception {
            if(!process.waitFor(10L, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("SeedV6 UCI process did not terminate within ten seconds.");
            }
            String line;
            while((line = output.readLine()) != null) lines.add(line);
            final StringBuilder errors = new StringBuilder();
            while((line = error.readLine()) != null) {
                if(!errors.isEmpty()) errors.append(System.lineSeparator());
                errors.append(line);
            }
            stderr = errors.toString();
        }

        int exitCode() {
            return process.exitValue();
        }

        List<String> lines() {
            return List.copyOf(lines);
        }

        String stderr() {
            return stderr;
        }

        @Override
        public void close() {
            readers.shutdownNow();
            if(process.isAlive()) process.destroyForcibly();
        }

        private final Process process;
        private final BufferedWriter input;
        private final BufferedReader output;
        private final BufferedReader error;
        private final ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor();
        private final List<String> lines = new ArrayList<>();
        private String stderr = "";
    }
}
