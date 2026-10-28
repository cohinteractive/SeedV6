package com.ohinteractive.seedv6.core.brnpair2;

import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TeacherDatasetTest {
    @TempDir Path temporary;
    @Test void interruptionPreservesChunksAndResumeSkipsTeacherCalls()throws Exception {
        String fen="4k3/8/8/8/8/8/8/4K3 w - - 0 1";Path source=temporary.resolve("source.tsv"),out=temporary.resolve("labels");
        Files.writeString(source,(fen+"\t0.2\n").repeat(5));var first=new AtomicInteger();
        assertThrows(IllegalStateException.class,()->TeacherDataset.process(source,out,5,2,b->{if(first.incrementAndGet()==4)throw new IllegalStateException("interruption");return 100;},()->{}));
        assertTrue(Files.exists(out.resolve("chunk-00000/receipt.json")));assertFalse(Files.exists(out.resolve("chunk-00001")));
        var resumed=new AtomicInteger();var result=TeacherDataset.process(source,out,5,2,b->{resumed.incrementAndGet();return 100;},()->{});assertEquals(3,resumed.get());
        var lines=Files.readAllLines(out.resolve("data.tsv"));assertEquals(5,lines.size());
        for(String line:lines){assertEquals(fen,line.split("\t")[0]);assertEquals(1/(1+Math.exp(-.25)),Double.parseDouble(line.split("\t")[1]),0);}
        var repeated=new AtomicInteger();assertEquals(result,TeacherDataset.process(source,out,5,2,b->{repeated.incrementAndGet();return 100;},()->{}));assertEquals(0,repeated.get());
        assertThrows(java.io.IOException.class,()->TeacherDataset.publish(source,temporary.getParent().resolve("escape.tsv"),temporary));
        Files.writeString(out.resolve("chunk-00000/data.tsv"),"corrupt");assertThrows(java.io.IOException.class,()->TeacherDataset.process(source,out,5,2,b->100,()->{}));
    }
}
