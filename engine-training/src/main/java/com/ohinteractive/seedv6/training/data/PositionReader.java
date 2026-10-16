package com.ohinteractive.seedv6.training.data;

import java.io.IOException;

/** Deterministic raw-record traversal. Invalid source records still own an ordinal. */
public interface PositionReader extends AutoCloseable {
    record Entry(long ordinal, TrainingPosition position, String rejection) {}
    Entry next() throws IOException;
    long nextPosition();
    long decodedRecords();
    long seekRecords();
    @Override void close() throws IOException;
}
