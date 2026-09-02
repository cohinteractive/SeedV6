package com.ohinteractive.seedv6.uci;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.manage.SearchLimits;

public final class UciEngine {

    public UciEngine(InputStream input, OutputStream output, OutputStream error) {
        reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(input, "input"), StandardCharsets.UTF_8));
        uciOutput = new UciOutput(output, error);
        searches = new SearchLifecycleService(rootWorkers);
        diagnosticsEnabled = Boolean.getBoolean("seedv6.searchDiagnostics");
    }

    public void run() throws IOException {
        try {
            String line;
            while((line = reader.readLine()) != null) {
                final String input = line.trim();
                if(input.isEmpty()) continue;
                if(!handle(input)) return;
            }
        } finally {
            searches.close();
        }
    }

    private boolean handle(String input) {
        final String[] tokens = input.split("\\s+");
        if(tokens.length == 1 && tokens[0].equals("uci")) {
            uciOutput.line("id name SeedV6");
            uciOutput.line("id author Charles Clark");
            uciOutput.line(
                "option name Threads type spin default " + RootParallelSearch.DEFAULT_WORKERS
                    + " min " + RootParallelSearch.MIN_WORKERS
                    + " max " + RootParallelSearch.MAX_WORKERS
            );
            uciOutput.line("uciok");
            return true;
        }
        if(tokens.length == 1 && tokens[0].equals("isready")) {
            uciOutput.line("readyok");
            return true;
        }
        if(tokens.length == 1 && tokens[0].equals("ucinewgame")) {
            searches.invalidate(SearchTermination.NEW_GAME);
            session.reset();
            return true;
        }
        if(tokens[0].equals("setoption")) {
            setOption(tokens);
            return true;
        }
        if(tokens[0].equals("position")) {
            try {
                final UciSession.PositionState candidate = session.parsePosition(tokens);
                searches.invalidate(SearchTermination.POSITION_CHANGED);
                session.install(candidate);
            } catch(IllegalArgumentException exception) {
                // Routine protocol rejection is intentionally quiet.
            } catch(RuntimeException exception) {
                uciOutput.error("SeedV6 position failed: " + exception.getMessage());
            }
            return true;
        }
        if(tokens[0].equals("go")) {
            go(tokens);
            return true;
        }
        if(tokens.length == 1 && tokens[0].equals("stop")) {
            searches.stop();
            return true;
        }
        return !(tokens.length == 1 && tokens[0].equals("quit"));
    }

    private void go(String[] tokens) {
        try {
            final UciSession.PositionState position = session.snapshot();
            final SearchLimits limits = GoCommandParser.parse(
                tokens, (int) position.board[com.ohinteractive.seedv6.core.Board.STATUS]
            );
            searches.start(
                position.board, position.history, limits,
                new SearchObserver() {
                    @Override
                    public void onIterationCompleted(IterationSnapshot snapshot) {
                        uciOutput.line(UciInfoFormatter.format(snapshot));
                    }
                },
                diagnosticsEnabled,
                this::publish
            );
        } catch(IllegalArgumentException exception) {
            // Routine protocol rejection is intentionally quiet.
        } catch(RuntimeException exception) {
            uciOutput.error("SeedV6 search start failed: " + exception.getMessage());
        }
    }

    private void setOption(String[] tokens) {
        if(tokens.length != 5 || !tokens[1].equals("name")
            || !tokens[2].equals("Threads") || !tokens[3].equals("value")) {
            return;
        }
        try {
            final int requested = Integer.parseInt(tokens[4]);
            if(requested < RootParallelSearch.MIN_WORKERS
                || requested > RootParallelSearch.MAX_WORKERS
                || requested == rootWorkers) {
                return;
            }
            searches.close();
            rootWorkers = requested;
            searches = new SearchLifecycleService(rootWorkers);
        } catch(NumberFormatException ignored) {
            // Routine protocol rejection is intentionally quiet.
        }
    }

    private void publish(ManagedSearchResult result) {
        if(result.failure() != null) uciOutput.line("info string search failed");
        uciOutput.line(
            "bestmove " + (result.hasMove() ? Move.coordinate(result.bestMove()) : "0000")
        );
    }

    private final BufferedReader reader;
    private final UciOutput uciOutput;
    private final UciSession session = new UciSession();
    private SearchLifecycleService searches;
    private int rootWorkers = RootParallelSearch.DEFAULT_WORKERS;
    /** Silent process-level instrumentation switch; it never adds UCI output. */
    private final boolean diagnosticsEnabled;
}
