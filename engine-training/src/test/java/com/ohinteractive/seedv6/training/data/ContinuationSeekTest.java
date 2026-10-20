package com.ohinteractive.seedv6.training.data;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ContinuationSeekTest {
    @TempDir Path temporary;
    @Test void missingInheritedNavigationFailsThenCopiesWithoutResettingOrReplacing() throws Exception {
        Path file=temporary.resolve("data.jsonl");
        try(var out=Files.newBufferedWriter(file)) {
            for(int i=0;i<9000;i++)out.write("{\"fen\":\"4k3/8/8/8/3pP3/8/8/4K3 w - - 0 1\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":"+i+"}]}]}\n");
        }
        var source=DataSource.register("fixture",file,1);
        var sources=new DataSources(1,List.of(source),false);
        var cursors=Map.of(source.identity(),5000L);
        Path parent=temporary.resolve("parent"),child=temporary.resolve("child");
        try(var reader=SourceReaders.open(source,parent,0)) { for(int i=0;i<5000;i++)assertEquals(i,reader.next().position().target()); }
        assertTrue(assertThrows(IOException.class,()->SourceReaders.open(source,child,5000)).getMessage().contains("does not cover position 5000"));
        byte[] before=Files.readAllBytes(parent.resolve(source.identity()+".json"));
        assertEquals(1,ContinuationSeek.inherit(sources,cursors,parent,child).size());
        assertArrayEquals(before,Files.readAllBytes(child.resolve(source.identity()+".json")));
        try(var reader=SourceReaders.open(source,child,4999)) {
            assertTrue(reader.seekRecords()<=4096);
            for(int i=4999;i<8300;i++)assertEquals(i,reader.next().position().target());
        }
        byte[] extended=Files.readAllBytes(child.resolve(source.identity()+".json"));
        assertTrue(ContinuationSeek.inherit(sources,cursors,parent,child).isEmpty());
        assertArrayEquals(extended,Files.readAllBytes(child.resolve(source.identity()+".json")));
        assertArrayEquals(before,Files.readAllBytes(parent.resolve(source.identity()+".json")));
        Files.writeString(child.resolve(source.identity()+".json"),"{}");
        assertThrows(IOException.class,()->ContinuationSeek.inherit(sources,cursors,parent,child));
        assertEquals("{}",Files.readString(child.resolve(source.identity()+".json")));
        assertThrows(IOException.class,()->ContinuationSeek.inherit(sources,Map.of(source.identity(),8500L),parent,temporary.resolve("uncovered")));
        assertFalse(Files.exists(temporary.resolve("uncovered")));
        Files.writeString(file,"changed",StandardOpenOption.APPEND);
        assertThrows(IOException.class,()->ContinuationSeek.inherit(sources,cursors,parent,temporary.resolve("changed")));
        assertFalse(Files.exists(temporary.resolve("changed")));
    }
}
