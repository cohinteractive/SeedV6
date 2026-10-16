package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiPredicate;

/** Test-only access to the existing atomic-move seam; every non-failing write uses the real publisher. */
public final class StoreFaults {
    public static CheckpointStore open(Path root, BiPredicate<Path, Path> fail) throws IOException {
        return new CheckpointStore(root, (source, target, replace) -> {
            if (fail.test(source, target)) throw new IOException("Injected atomic publication failure: " + target.getFileName());
            CheckpointStore.atomicMove(source, target, replace);
        });
    }
    private StoreFaults() {}
}
