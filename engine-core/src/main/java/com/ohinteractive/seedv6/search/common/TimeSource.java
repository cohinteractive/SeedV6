package com.ohinteractive.seedv6.search.common;

/** Monotonic elapsed-time source. */
@FunctionalInterface
public interface TimeSource {

    TimeSource SYSTEM = System::nanoTime;

    long nanoTime();
}
