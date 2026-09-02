package com.ohinteractive.seedv6.uci;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        final EngineRun run = launch("uci\nisready\nquit\n");

        assertEquals(0, run.exitCode());
        assertEquals(List.of(
            "id name SeedV6",
            "id author Charles Clark",
            "uciok",
            "readyok"
        ), run.lines());
        assertEquals("", run.stderr());
    }

    @Test
    void actualMainExitsCleanlyOnEofWithoutQuit() throws Exception {
        final EngineRun run = launch("uci\n");

        assertEquals(0, run.exitCode());
        assertEquals(List.of(
            "id name SeedV6",
            "id author Charles Clark",
            "uciok"
        ), run.lines());
        assertEquals("", run.stderr());
    }

    @Test
    void invalidGoFormsAreQuietBoundedAndDoNotBreakLaterCommands() throws Exception {
        final EngineRun run = launch(String.join("\n",
            "go depth 0",
            "go depth -1",
            "go depth nope",
            "go depth " + (FlatNegamax.MAX_SUPPORTED_DEPTH + 1),
            "go depth",
            "go",
            "go depth 1 extra",
            "go infinite",
            "isready",
            "go depth 1",
            "quit",
            ""
        ));

        assertEquals(0, run.exitCode());
        assertEquals(2, run.lines().size());
        assertEquals("readyok", run.lines().get(0));
        assertLegalBestMove(Board.startingPosition(), run.lines().get(1));
        assertEquals("", run.stderr());
    }

    @Test
    void actualMainSearchesStartPositionAndFenReplayAtValidDepths() throws Exception {
        final String fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1";
        final EngineRun run = launch(String.join("\n",
            "position startpos",
            "go depth 1",
            "position fen " + fen + " moves e2e3 e8e7",
            "go depth 2",
            "quit",
            ""
        ));

        assertEquals(2, run.lines().size());
        assertLegalBestMove(Board.startingPosition(), run.lines().get(0));

        final UciSession expected = new UciSession();
        expected.setPosition(("position fen " + fen + " moves e2e3 e8e7").split("\\s+"));
        assertLegalBestMove(expected.boardSnapshot(), run.lines().get(1));
        assertEquals("", run.stderr());
    }

    @Test
    void terminalMateAndStalemateEmitAuthoritativeNoMoveToken() throws Exception {
        final EngineRun run = launch(String.join("\n",
            "position fen 7k/6Q1/5K2/8/8/8/8/8 b - - 0 1",
            "go depth 1",
            "position fen 7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
            "go depth 2",
            "quit",
            ""
        ));

        assertEquals(List.of("bestmove 0000", "bestmove 0000"), run.lines());
        assertEquals("", run.stderr());
    }

    @Test
    void newGameDropsAStaleTerminalPositionAndHistory() throws Exception {
        final EngineRun run = launch(String.join("\n",
            "position fen 7k/6Q1/5K2/8/8/8/8/8 b - - 0 1",
            "ucinewgame",
            "go depth 1",
            "quit",
            ""
        ));

        assertEquals(1, run.lines().size());
        assertFalse(run.lines().get(0).equals("bestmove 0000"));
        assertLegalBestMove(Board.startingPosition(), run.lines().get(0));
        assertEquals("", run.stderr());
    }

    @Test
    void malformedPositionDoesNotReplacePreviouslyAcceptedProcessState() throws Exception {
        final String accepted = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1";
        final EngineRun run = launch(String.join("\n",
            "position fen " + accepted,
            "position startpos moves e2e4 e7e5 e1e5",
            "go depth 1",
            "quit",
            ""
        ));

        assertEquals(1, run.lines().size());
        assertLegalBestMove(Board.fromFen(accepted), run.lines().get(0));
        assertEquals("", run.stderr());
    }

    private static void assertLegalBestMove(long[] board, String output) {
        assertTrue(output.startsWith("bestmove "), output);
        final String coordinate = output.substring("bestmove ".length());
        assertFalse(coordinate.equals("0000"), output);
        final long resolved = new LegalMoveResolver().resolve(board, UciMoveParser.parse(coordinate));
        assertEquals(coordinate, Move.coordinate(resolved));
    }

    private static EngineRun launch(String commands) throws Exception {
        final Path java = Path.of(
            System.getProperty("java.home"), "bin",
            System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java"
        );
        final Path mainClasses = Path.of(
            Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        );
        final Process process = new ProcessBuilder(
            java.toString(), "-cp", mainClasses.toString(), Main.class.getName()
        ).start();
        try(var input = process.getOutputStream()) {
            input.write(commands.getBytes(StandardCharsets.UTF_8));
            input.flush();
            input.close();
            if(!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("SeedV6 UCI process did not terminate within ten seconds.");
            }
            final String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new EngineRun(process.exitValue(), stdout.lines().toList(), stderr);
        } catch(IOException exception) {
            process.destroyForcibly();
            throw exception;
        } finally {
            if(process.isAlive()) process.destroyForcibly();
        }
    }

    private record EngineRun(int exitCode, List<String> lines, String stderr) {}
}
