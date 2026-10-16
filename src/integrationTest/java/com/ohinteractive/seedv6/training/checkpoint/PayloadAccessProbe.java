package com.ohinteractive.seedv6.training.checkpoint;

import java.nio.file.Path;

/** Separate JVM used by coordination tests, with no dependency on a test runner. */
public final class PayloadAccessProbe {
    public static void main(String[] args) throws Exception {
        System.out.println("waiting"); System.out.flush();
        try (var access = PayloadAccess.acquire(Path.of(args[0]))) {
            System.out.println("acquired"); System.out.flush();
            if (args.length > 1) System.in.read();
        }
    }
}
