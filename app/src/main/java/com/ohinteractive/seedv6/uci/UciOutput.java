package com.ohinteractive.seedv6.uci;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Serializes complete protocol lines produced by command and worker threads. */
final class UciOutput {

    UciOutput(OutputStream output, OutputStream error) {
        writer = new PrintWriter(new OutputStreamWriter(
            Objects.requireNonNull(output, "output"), StandardCharsets.UTF_8
        ), true);
        errorWriter = new PrintWriter(new OutputStreamWriter(
            Objects.requireNonNull(error, "error"), StandardCharsets.UTF_8
        ), true);
    }

    synchronized void line(String line) {
        writer.println(line);
    }

    synchronized void error(String line) {
        errorWriter.println(line);
    }

    private final PrintWriter writer;
    private final PrintWriter errorWriter;
}
