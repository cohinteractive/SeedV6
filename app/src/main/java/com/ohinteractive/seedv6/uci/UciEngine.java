package com.ohinteractive.seedv6.uci;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;

public final class UciEngine {

    public UciEngine(InputStream input, OutputStream output, OutputStream error) {
        reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(input, "input"), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(Objects.requireNonNull(output, "output"), StandardCharsets.UTF_8), true);
        errorWriter = new PrintWriter(new OutputStreamWriter(Objects.requireNonNull(error, "error"), StandardCharsets.UTF_8), true);
    }

    public void run() throws IOException {
        String line;
        while((line = reader.readLine()) != null) {
            final String input = line.trim();
            if(input.isEmpty()) continue;
            if(!handle(input)) return;
        }
    }

    private boolean handle(String input) {
        final String[] tokens = input.split("\\s+");
        if(tokens.length == 1 && tokens[0].equals("uci")) {
            writer.println("id name SeedV6");
            writer.println("id author Charles Clark");
            writer.println("uciok");
            return true;
        }
        if(tokens.length == 1 && tokens[0].equals("isready")) {
            writer.println("readyok");
            return true;
        }
        if(tokens.length == 1 && tokens[0].equals("ucinewgame")) {
            session.reset();
            return true;
        }
        if(tokens[0].equals("position")) {
            try {
                session.setPosition(tokens);
            } catch(IllegalArgumentException exception) {
                // Routine protocol rejection is intentionally quiet.
            } catch(RuntimeException exception) {
                errorWriter.println("SeedV6 position failed: " + exception.getMessage());
            }
            return true;
        }
        if(tokens[0].equals("go")) {
            go(tokens);
            return true;
        }
        return !(tokens.length == 1 && tokens[0].equals("quit"));
    }

    private void go(String[] tokens) {
        if(tokens.length != 3 || !tokens[1].equals("depth")) return;
        final int depth;
        try {
            depth = Integer.parseInt(tokens[2]);
        } catch(NumberFormatException exception) {
            return;
        }
        if(depth < 1 || depth > FlatNegamax.MAX_SUPPORTED_DEPTH) return;
        try {
            final SearchResult result = session.search(depth);
            writer.println("bestmove " + (result.hasMove() ? Move.coordinate(result.bestMove()) : "0000"));
        } catch(RuntimeException exception) {
            errorWriter.println("SeedV6 search failed: " + exception.getMessage());
        }
    }

    private final BufferedReader reader;
    private final PrintWriter writer;
    private final PrintWriter errorWriter;
    private final UciSession session = new UciSession();
}
