package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Isolated allocation measurement, not a Search benchmark. Keep the tested table
 * reachable; thread allocation includes object/array/stripe headers and padding. */
public final class P4TtMemoryAllocation {
    private static volatile TTable retained;
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("rawLane output.json");
        boolean raw=Boolean.parseBoolean(args[0]);var rows=new ArrayList<Map<String,Object>>();
        var mx=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        mx.setThreadAllocatedMemoryEnabled(true);long id=Thread.currentThread().threadId();
        retained=new TTable(1,raw); // resolve constructor/class initialization before measurements
        for(int i=0;i<4;i++) {
            retained=null;System.gc();
            long heap=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            long before=mx.getThreadAllocatedBytes(id),start=System.nanoTime();
            retained=new TTable(64,raw);
            long elapsed=System.nanoTime()-start,allocated=mx.getThreadAllocatedBytes(id)-before;
            System.gc();
            rows.add(Map.of("entries",retained.entryCapacity(),"payloadBytes",retained.entryPayloadBytes(),
                    "constructorAllocatedBytes",allocated,"constructorNanos",elapsed,
                    "retainedHeapDelta",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()-heap));
        }
        var clears=new ArrayList<Long>();
        for(int i=0;i<36;i++) {
            long start=System.nanoTime();retained.clear();long elapsed=System.nanoTime()-start;
            if(i>=4)clears.add(elapsed);
        }
        Files.writeString(Path.of(args[1]),new GsonBuilder().create().toJson(Map.of("rawLane",raw,"rows",rows,
                "clearNanos",clears,
                "java",System.getProperty("java.runtime.version"),"jvm",ManagementFactory.getRuntimeMXBean().getInputArguments(),
                "scope","TT constructor allocation and approximate retained heap; excludes Search/evaluator/process runtime")),StandardOpenOption.CREATE_NEW);
    }
    private P4TtMemoryAllocation() {}
}
