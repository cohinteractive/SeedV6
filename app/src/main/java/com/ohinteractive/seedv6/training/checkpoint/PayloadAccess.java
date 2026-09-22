package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Short-lived, cross-process exclusion for payload access. Deliberately uses exclusive locks for
 * readers too: shared-lock support/promotion varies by provider. Never held during search/training.
 * Separate from the lifetime store.lock, so Play can load while a trainer owns the store.
 */
final class PayloadAccess implements AutoCloseable {
    static final String FILE = "payload.lock";
    private static final ReentrantLock[] LOCAL = new ReentrantLock[64];
    private static final ThreadLocal<Set<Path>> HELD = ThreadLocal.withInitial(HashSet::new);
    static { Arrays.setAll(LOCAL, i -> new ReentrantLock(true)); }
    private final Path root;
    private final ReentrantLock local;
    private final FileChannel channel;
    private final FileLock lock;

    private PayloadAccess(Path root, ReentrantLock local, FileChannel channel, FileLock lock) {
        this.root = root; this.local = local; this.channel = channel; this.lock = lock;
    }

    static PayloadAccess acquire(Path directory) throws IOException {
        return acquire(directory, true);
    }

    /** Catalogue browsing never creates artifacts in a legacy store. If its coordination file is
     * absent, this is an advisory read only; actual model loading still acquires the normal lock.
     */
    static PayloadAccess browse(Path directory) throws IOException { return acquire(directory, false); }

    private static PayloadAccess acquire(Path directory, boolean create) throws IOException {
        Path root = directory.toRealPath();
        ReentrantLock local = LOCAL[Math.floorMod(root.hashCode(), LOCAL.length)];
        local.lock();
        if (HELD.get().contains(root)) return new PayloadAccess(root, local, null, null);
        FileChannel channel = null;
        try {
            // No directory creation; absent stores stay absent. Refuse a substituted symbolic lock.
            try {
                channel = create ? FileChannel.open(root.resolve(FILE), StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)
                        : FileChannel.open(root.resolve(FILE), StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            } catch (NoSuchFileException absent) {
                if (create) throw absent;
                return new PayloadAccess(root, local, null, null);
            }
            FileLock lock = channel.lock();
            HELD.get().add(root);
            return new PayloadAccess(root, local, channel, lock);
        } catch (IOException | RuntimeException failure) {
            try { if (channel != null) channel.close(); } finally { local.unlock(); }
            throw failure;
        }
    }

    @Override public void close() throws IOException {
        try {
            if (channel != null) {
                HELD.get().remove(root);
                try { lock.release(); } finally { channel.close(); }
            }
        } finally { local.unlock(); }
    }
}
