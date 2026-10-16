package com.ohinteractive.seedv6.uci;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.exact.ParallelSearch;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;

/** One protocol implementation; search execution and cancellation belong to engine-core. */
public final class UciEngine implements AutoCloseable {
    public UciEngine(InputStream input, OutputStream output, OutputStream error, UciConfiguration configuration) {
        reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(input), StandardCharsets.UTF_8));
        uciOutput = new UciOutput(output, error);
        this.configuration = Objects.requireNonNull(configuration);
        ownBook = !configuration.standalone();
        searches = configuration.service(rootWorkers, hashMiB);
        searches.setBookEnabled(ownBook);
        diagnosticsEnabled = Boolean.getBoolean("seedv6.searchDiagnostics");
    }

    public void run() throws IOException {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String input = line.trim();
                if (input.isEmpty()) continue;
                if (!handle(input)) return;
            }
        } finally { close(); }
    }

    private boolean handle(String input) {
        String[] tokens = input.split("\\s+");
        try {
            if (tokens.length == 1 && tokens[0].equals("uci")) {
                uciOutput.line("id name " + configuration.name());
                uciOutput.line("id author Charles Clark");
                uciOutput.line("option name Threads type spin default 1 min 1 max 16");
                if (configuration.standalone()) {
                    uciOutput.line("option name Hash type spin default 64 min 1 max 1024");
                    uciOutput.line("option name Clear Hash type button");
                } else uciOutput.line("option name OwnBook type check default true");
                identify();
                uciOutput.line("uciok");
            } else if (tokens.length == 1 && tokens[0].equals("isready")) {
                uciOutput.line("readyok");
            } else if (tokens.length == 1 && tokens[0].equals("ucinewgame")) {
                discardPublication();
                searches.invalidate(SearchTermination.NEW_GAME);
                session.reset();
            } else if (tokens[0].equals("setoption")) {
                setOption(tokens);
            } else if (tokens[0].equals("position")) {
                var candidate = session.parsePosition(tokens);
                discardPublication();
                searches.invalidate(SearchTermination.POSITION_CHANGED);
                session.install(candidate);
            } else if (tokens[0].equals("go")) {
                go(tokens);
            } else if (tokens.length == 1 && tokens[0].equals("stop")) {
                synchronized (publicationLock) {
                    if (request != null) {
                        request.stopped = true;
                        if (request.completed != null) publishCompleted(request);
                    }
                }
                searches.stop();
            } else if (tokens.length == 1 && tokens[0].equals("quit")) return false;
        } catch (IllegalArgumentException rejected) {
            // Preserve quiet legacy rejection; standalone diagnoses invalid client input on stderr.
            if (configuration.standalone()) uciOutput.error("SeedV6 UCI rejected command: " + rejected.getMessage());
        } catch (RuntimeException failure) {
            uciOutput.error("SeedV6 UCI command failed: " + failure.getMessage());
        }
        return true;
    }

    private void identify() {
        String identity = configuration.identity(rootWorkers);
        if (!identity.isEmpty()) uciOutput.line("info string " + identity
                + (configuration.standalone() ? " ttMiB=" + hashMiB : ""));
    }

    private void go(String[] tokens) {
        var position = session.snapshot();
        var limits = GoCommandParser.parse(tokens, (int) position.board[Board.STATUS]);
        long fallback = 0;
        if (configuration.standalone()) {
            long[] moves = new long[256];
            int count = Gen.genAll(position.board[0], position.board[1], position.board[2], position.board[3],
                    (int) position.board[Board.STATUS], position.board[Board.KEY], true,
                    moves, new long[Board.MAX_BITBOARDS]);
            if (count > 0) fallback = moves[0];
        }
        var next = new Publication(limits.infinite() && configuration.standalone(), fallback);
        synchronized (publicationLock) { request = next; }
        try {
            searches.start(position.board, position.history, limits, new SearchObserver() {
                @Override public void onIterationCompleted(IterationSnapshot snapshot) {
                    synchronized (publicationLock) {
                        if (request == next) uciOutput.line(UciInfoFormatter.format(snapshot));
                    }
                }
            }, diagnosticsEnabled, result -> {
                synchronized (publicationLock) {
                    if (request != next) return;
                    next.completed = result;
                    if (!next.waitForStop || next.stopped) publishCompleted(next);
                }
            });
        } catch (RuntimeException failure) {
            synchronized (publicationLock) { if (request == next) request = null; }
            throw failure;
        }
    }

    // Never hold publicationLock while entering core: callbacks hold core's generation gate.
    private synchronized void setOption(String[] tokens) {
        if (closed) return;
        if (!configuration.standalone() && tokens.length == 5 && tokens[1].equals("name")
                && tokens[2].equals("OwnBook") && tokens[3].equals("value")
                && (tokens[4].equals("true") || tokens[4].equals("false"))) {
            ownBook = Boolean.parseBoolean(tokens[4]);
            searches.setBookEnabled(ownBook);
            return;
        }
        if (configuration.standalone() && tokens.length == 4 && tokens[1].equals("name")
                && tokens[2].equalsIgnoreCase("Clear") && tokens[3].equalsIgnoreCase("Hash")) {
            discardPublication();
            searches.invalidate(SearchTermination.NEW_GAME);
            return;
        }
        if (tokens.length != 5 || !tokens[1].equals("name") || !tokens[3].equals("value")) return;
        int workers = rootWorkers, hash = hashMiB;
        if (tokens[2].equalsIgnoreCase("Threads")) workers = Integer.parseInt(tokens[4]);
        else if (configuration.standalone() && tokens[2].equalsIgnoreCase("Hash")) hash = Integer.parseInt(tokens[4]);
        else return;
        if (workers < ParallelSearch.MIN_WORKERS || workers > ParallelSearch.MAX_WORKERS
                || !configuration.acceptsWorkers(workers) || hash < 1 || hash > 1024)
            throw new IllegalArgumentException("Unsupported Threads/Hash setting (Threads 1..16, Hash 1..1024 MiB)");
        if (workers == rootWorkers && hash == hashMiB) return;
        final SearchLifecycleService replacement;
        try {
            // Allocate first so failure retains the old settings and live service.
            replacement = configuration.service(workers, hash);
        } catch (OutOfMemoryError exhausted) {
            uciOutput.error("SeedV6 UCI could not allocate Hash/Threads; retained previous settings. Reduce Hash or increase -Xmx.");
            return;
        }
        discardPublication();
        try { searches.close(); }
        catch (RuntimeException | Error failure) { replacement.close(); throw failure; }
        searches = replacement;
        rootWorkers = workers;
        hashMiB = hash;
        searches.setBookEnabled(ownBook);
        identify();
    }

    private void publishCompleted(Publication publication) {
        var result = publication.completed;
        if (!configuration.identity(rootWorkers).isEmpty()) uciOutput.line("info string request nodes="
                + result.nodes() + " elapsedNanos=" + (System.nanoTime() - publication.started)
                + " termination=" + result.termination());
        if (result.termination() == SearchTermination.BOOK) uciOutput.line("info string opening book");
        if (result.failure() != null) uciOutput.error("SeedV6 UCI search failed: " + result.failure());
        if (result.tablebaseWin() != null) uciOutput.line("info string tablebase win dtz "
                + result.tablebaseWin().distanceToZero());
        // UCI cannot claim a draw; fallback supplies only a legal move, never a score/depth/PV.
        long move = result.hasMove() ? result.bestMove() : publication.fallback;
        uciOutput.line("bestmove " + (move == 0 ? "0000" : Move.coordinate(move)));
        request = null;
    }

    private void discardPublication() { synchronized (publicationLock) { request = null; } }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        discardPublication();
        searches.close();
    }

    private static final class Publication {
        final boolean waitForStop;
        final long fallback, started = System.nanoTime();
        boolean stopped;
        ManagedSearchResult completed;
        Publication(boolean waitForStop, long fallback) { this.waitForStop = waitForStop; this.fallback = fallback; }
    }
    private final BufferedReader reader;
    private final UciOutput uciOutput;
    private final UciSession session = new UciSession();
    private final UciConfiguration configuration;
    private final Object publicationLock = new Object();
    private Publication request;
    private SearchLifecycleService searches;
    private boolean ownBook, closed;
    private int rootWorkers = 1, hashMiB = 64;
    private final boolean diagnosticsEnabled;
}
