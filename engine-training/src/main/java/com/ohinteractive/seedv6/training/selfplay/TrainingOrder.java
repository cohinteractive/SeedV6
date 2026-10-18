package com.ohinteractive.seedv6.training.selfplay;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Exact historical Fisher-Yates order, with a fixed-size cache above 64K positions. */
final class TrainingOrder implements AutoCloseable {
    private static final int LIMIT = 65536, PAGE = 1024, PAGES = 256;
    final int length;
    private final int[] small;
    private final Path path;
    private final FileChannel file;
    private final int[][] pages = new int[PAGES][];
    private final int[] keys = new int[PAGES];
    private final boolean[] dirty = new boolean[PAGES];
    private boolean identity;
    private final ByteBuffer bytes = ByteBuffer.allocate(PAGE * Integer.BYTES);

    TrainingOrder(int length) {
        this.length = length; Arrays.fill(keys, -1);
        try {
            if (length <= LIMIT) { small = new int[length]; path = null; file = null; }
            else {
                small = null; path = Files.createTempFile("seedv6-training-", ".order");
                FileChannel opened;
                try { opened = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE); }
                catch (Throwable e) { Files.deleteIfExists(path); throw e; }
                file = opened;
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    boolean reset(boolean shuffle, SplittableRandom random, SelfPlayControl control) {
        try {
            identity = !shuffle;
            if (identity) return !control.cancelled();
            Arrays.fill(keys, -1); Arrays.fill(dirty, false);
            for (int start = 0; start < length;) {
                if (control.cancelled()) return false;
                int count = Math.min(PAGE, length - start);
                if (small != null) for (int i = 0; i < count; i++) small[start + i] = start + i;
                else {
                    bytes.clear(); for (int i = 0; i < count; i++) bytes.putInt(start + i); bytes.flip();
                    write(bytes, (long) start * 4);
                }
                start += count;
            }
            if (shuffle) for (int i = length - 1; i > 0; i--) {
                if ((i & 1023) == 0 && control.cancelled()) return false;
                int other = random.nextInt(i + 1), swap = get(i);
                set(i, get(other)); set(other, swap);
            }
            return !control.cancelled();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    synchronized int get(int index) {
        Objects.checkIndex(index, length);
        if (identity) return index;
        if (small != null) return small[index];
        return page(index / PAGE)[index % PAGE];
    }
    private void set(int index, int value) {
        if (small != null) small[index] = value;
        else { page(index / PAGE)[index % PAGE] = value; dirty[(index / PAGE) % PAGES] = true; }
    }
    private int[] page(int number) {
        int slot = number % PAGES;
        if (keys[slot] == number) return pages[slot];
        try {
            if (dirty[slot]) flush(keys[slot], pages[slot]);
            int[] values = pages[slot];
            if (values == null) pages[slot] = values = new int[PAGE];
            int count = Math.min(PAGE, length - number * PAGE);
            bytes.clear().limit(count * 4);
            while (bytes.hasRemaining()) if (file.read(bytes, (long) number * PAGE * 4 + bytes.position()) < 0)
                throw new EOFException("Truncated training order");
            bytes.flip(); for (int i = 0; i < count; i++) values[i] = bytes.getInt();
            keys[slot] = number; dirty[slot] = false; return values;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    private void flush(int number, int[] values) throws IOException {
        int count = Math.min(PAGE, length - number * PAGE);
        bytes.clear(); for (int i = 0; i < count; i++) bytes.putInt(values[i]); bytes.flip();
        write(bytes, (long) number * PAGE * 4);
    }
    private void write(ByteBuffer buffer, long offset) throws IOException {
        while (buffer.hasRemaining()) file.write(buffer, offset + buffer.position());
    }
    public void close() {
        if (file == null) return;
        try { try { file.close(); } finally { Files.deleteIfExists(path); } }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
