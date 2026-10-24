package com.ohinteractive.seedv6.research.timing;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/** Research-only boundary observation around unchanged frozen core/UCI JARs.
 * No engine reflection, Search hook, deadline change or outcome calculation.
 * Custom JFR events include actual owner CPU and complete delivery boundaries.
 */
public final class Boundary2TimingMain {
    @Name("seedv6.ClockRequest") @Label("SeedV6 complete UCI request")
    @Enabled(true) @StackTrace(false)
    public static final class ClockRequest extends Event {
        public String profile, command, position, termination, bestmove;
        public long ordinal, receivedNanos, reportedAtNanos, deliveredAtNanos;
        public long reportedElapsedNanos, accountedNodes, ownerCpuNanos, processCpuNanos;
        public long ownerThreadId;
        public boolean completedDelivery, telemetryValid;
    }

    private static final Object LOCK = new Object();
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final com.sun.management.OperatingSystemMXBean OS =
        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final Pattern REPORT = Pattern.compile(
        "info string request nodes=(\\d+) elapsedNanos=(\\d+) termination=(\\w+)");
    private static String profile, position = "unknown";
    private static long ordinal, ownerId = -1, ownerStart, processStart;
    private static ClockRequest pending;

    private static long owner() {
        if(ownerId >= 0 && THREADS.getThreadCpuTime(ownerId) >= 0) return ownerId;
        long found = -1;
        for(long id : THREADS.getAllThreadIds()) {
            var info = THREADS.getThreadInfo(id);
            if(info != null && info.getThreadName().equals("seedv6-search-worker")) {
                if(found >= 0) return -1;
                found = id;
            }
        }
        return ownerId = found;
    }

    private static void inputLine(String line) {
        synchronized(LOCK) {
            if(line.startsWith("position ")) position = line;
            if(!line.startsWith("go ") && !line.equals("go")) return;
            if(pending != null) finish(false, "superseded");
            long id = owner();
            ownerStart = id < 0 ? -1 : THREADS.getThreadCpuTime(id);
            processStart = OS.getProcessCpuTime();
            var event = new ClockRequest();
            event.profile = profile; event.command = line; event.position = position;
            event.ordinal = ++ordinal; event.ownerThreadId = id;
            event.receivedNanos = System.nanoTime(); event.telemetryValid = ownerStart >= 0 && processStart >= 0;
            event.begin(); pending = event;
        }
    }

    private static void outputLine(String line) {
        synchronized(LOCK) {
            if(pending == null) return;
            if(line.startsWith("info string request ")) {
                var match = REPORT.matcher(line);
                if(!match.matches()) { pending.telemetryValid = false; return; }
                pending.accountedNodes = Long.parseLong(match.group(1));
                pending.reportedElapsedNanos = Long.parseLong(match.group(2));
                pending.termination = match.group(3);
                pending.reportedAtNanos = System.nanoTime();
                pending.telemetryValid &= Thread.currentThread().threadId() == pending.ownerThreadId;
            } else if(line.startsWith("bestmove ")) finish(true, line.substring(9));
        }
    }

    // Called after bestmove bytes are flushed to the real output. Recording cost
    // cannot delay delivery of those bytes; remaining callback CPU is disclosed.
    private static void finish(boolean completed, String move) {
        if(pending == null) return;
        pending.deliveredAtNanos = System.nanoTime();
        pending.ownerCpuNanos = pending.ownerThreadId < 0 ? -1 : THREADS.getThreadCpuTime(pending.ownerThreadId) - ownerStart;
        pending.processCpuNanos = OS.getProcessCpuTime() - processStart;
        pending.bestmove = move; pending.completedDelivery = completed;
        pending.telemetryValid &= completed && pending.reportedAtNanos > 0 && pending.ownerCpuNanos >= 0;
        pending.end(); pending.commit(); pending = null;
    }

    private static final class Input extends FilterInputStream {
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();
        Input(InputStream owner) { super(owner); }
        private void observed(int value) {
            if(value == '\n') {
                inputLine(line.toString(StandardCharsets.UTF_8).stripTrailing()); line.reset();
            } else if(value >= 0) line.write(value);
        }
        @Override public int read() throws IOException { int value=in.read(); observed(value); return value; }
        @Override public int read(byte[] bytes, int offset, int count) throws IOException {
            int n=in.read(bytes,offset,count);
            for(int i=0;i<n;i++) observed(bytes[offset+i]&255);
            return n;
        }
    }

    private static final class Output extends OutputStream {
        private final PrintStream owner;
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();
        Output(PrintStream owner) { this.owner=owner; }
        private void observed(int value) {
            if(value == '\n') {
                owner.flush();
                outputLine(line.toString(StandardCharsets.UTF_8).stripTrailing()); line.reset();
            } else line.write(value);
        }
        @Override public synchronized void write(int value) { owner.write(value); observed(value&255); }
        @Override public synchronized void write(byte[] bytes,int offset,int count) {
            owner.write(bytes,offset,count);
            for(int i=0;i<count;i++) observed(bytes[offset+i]&255);
        }
        @Override public synchronized void flush() { owner.flush(); }
    }

    public static void main(String[] args) throws Exception {
        if(args.length != 4 || !args[0].equals("--network") || !args[2].equals("--profile")
            || !(args[3].equals("baseline") || args[3].equals("boundary2")))
            throw new IllegalArgumentException("Bounded baseline/BOUNDARY2 diagnostics only");
        if(!THREADS.isThreadCpuTimeSupported()) throw new IllegalStateException("Thread CPU unsupported");
        if(!THREADS.isThreadCpuTimeEnabled()) THREADS.setThreadCpuTimeEnabled(true);
        profile=args[3];
        System.setIn(new Input(System.in));
        System.setOut(new PrintStream(new Output(System.out), true, StandardCharsets.UTF_8));
        try {
            Class.forName("com.ohinteractive.seedv6.integration.Cp64SpendingUciMain")
                .getMethod("main",String[].class).invoke(null,(Object)args);
        } finally {
            synchronized(LOCK) { if(pending != null) finish(false,"shutdown-without-delivery"); }
        }
    }
    private Boundary2TimingMain() {}
}
