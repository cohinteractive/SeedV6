package com.ohinteractive.seedv6.core.nnue.conventional;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CompactNetworkTest {
    static byte[] fixture(int kind) throws Exception {
        int features=7680+(kind==2?4560:0),depth=kind==1?16:0;
        int n=features*128+128+2048+8+depth*(256+1+8)+(kind==1?8:0);
        var payload=ByteBuffer.allocate(n*2).order(ByteOrder.LITTLE_ENDIAN);var rng=new SplittableRandom(73);
        for(int i=0;i<n;i++)payload.putShort((short)rng.nextInt(-20,21));
        var file=ByteBuffer.allocate(76+n*2).order(ByteOrder.LITTLE_ENDIAN);
        file.put("S6P2N002".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        for(int x:new int[]{2,kind,features,128,depth,255,64,400,n*2})file.putInt(x);
        return file.put(MessageDigest.getInstance("SHA-256").digest(payload.array())).put(payload.array()).array();
    }
    static CompactNetwork net(int kind) throws Exception {return CompactNetwork.read(new ByteArrayInputStream(fixture(kind)));}
    static long[] move(long[] board,long move) {
        long[] child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],move,child);return child;
    }
    static void compare(CompactNetwork net,long[] board,CompactAccumulator state) {
        var fresh=net.newAccumulator();fresh.rebuild(board);
        for(int side=0;side<2;side++)for(int i=0;i<net.hidden();i++)assertEquals(fresh.raw(side,i),state.raw(side,i));
        assertEquals(net.fullRebuild(board),state.evaluate(board));
    }
    @Test void architecturesCoverSpecialMovesRandomSiblingsUndoAndLifecycle() throws Exception {
        for(int width:new int[]{0,1,2}) {
            var net=net(width);assertEquals(128,net.hidden());var random=new SplittableRandom(991);
            String[] roots={"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                "1r5k/P7/1p6/8/8/8/3N1N2/4K3 w - - 0 1",
                "4k3/8/8/3p4/4P3/2N5/8/3K4 w - - 0 1",
                "8/8/8/3k4/8/3K4/8/8 b - - 0 1"};
            for(String fen:roots) {
                long[] parent=Board.fromFen(fen);var acc=net.newAccumulator();acc.rebuild(parent);
                for(int ply=0;ply<30;ply++) {
                    long[] moves=new long[256];int count=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
                    if(count==0)break;
                    // All first-ply siblings include every underpromotion/castle/EP option.
                    for(int i=0;i<(ply==0?count:Math.min(count,3));i++) {
                        var child=move(parent,moves[i]);var next=net.newAccumulator();next.update(parent,child,acc);
                        compare(net,child,next);compare(net,parent,acc);
                        var undo=net.newAccumulator();undo.update(child,parent,next);compare(net,parent,undo);
                    }
                    long[] child=move(parent,moves[random.nextInt(count)]);var next=net.newAccumulator();next.update(parent,child,acc);parent=child;acc=next;
                }
            }
            var definition=net.evaluation();var main=definition.newState(257);var q=definition.newState(257);var worker=definition.newState(257);
            long[] board=Board.startingPosition(),child=new long[6];
            Board.nullMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],child);
            main.initialize(board,255);main.child(board,child,255);q.initializeFrom(child,256,main);worker.initialize(board,0);
            assertEquals(net.fullRebuild(child),q.evaluate(child,256));assertEquals(net.fullRebuild(board),worker.evaluate(board,0));
            assertThrows(IllegalArgumentException.class,()->main.initializeFrom(board,0,main));
            assertThrows(IllegalArgumentException.class,()->q.initializeFrom(child,256,net.evaluation().newState(257)));
            var stale=net.newAccumulator();stale.rebuild(board);long[] changed=Board.fromFen(roots[1]);
            assertThrows(IllegalStateException.class,()->stale.evaluate(changed));
        }
    }
    @Test void codecRejectsGeometryCorruptionTruncationAndProductionUse() throws Exception {
        byte[] file=fixture(1);assertThrows(IOException.class,()->ConventionalNetwork.read(new ByteArrayInputStream(file)));
        for(int index:new int[]{0,7,8,12,16,20,24,28,32,36,40,44,76,file.length-1}) {
            byte[] bad=file.clone();bad[index]^=1;assertThrows(IOException.class,()->CompactNetwork.read(new ByteArrayInputStream(bad)));
        }
        for(int size:new int[]{75,file.length-1,file.length+1})
            assertThrows(IOException.class,()->CompactNetwork.read(new ByteArrayInputStream(Arrays.copyOf(file,size))));
    }
    @Test void foreignGeometryIsRejectedInBothDirections() throws Exception {
        byte[] conventional=Files.readAllBytes(Path.of("../build-assets/production/conventional-wdl25.cnnue"));
        assertThrows(IOException.class,()->CompactNetwork.read(new ByteArrayInputStream(conventional)));
        byte[] prior=Phase2NetworkTest.fixture(1);
        assertThrows(IOException.class,()->CompactNetwork.read(new ByteArrayInputStream(prior)));
        byte[] compact=fixture(1);
        assertThrows(IOException.class,()->Phase2Network.read(new ByteArrayInputStream(compact)));
    }
    @Test void boundedVectorDotMatchesUnboundedIntegerReference() {
        if(!ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent())return;
        var rng=new SplittableRandom(17);var kernel=new CompactVectorHead();
        for(int width:new int[]{128})for(int trial=0;trial<40;trial++) {
            int[] us=new int[width],them=new int[width],weights=new int[width*16];int offset=(trial%8)*width*2;
            var expected=java.math.BigInteger.ZERO;
            for(int i=0;i<width*2;i++) {
                int raw=rng.nextInt(-1000000,1000000),w=trial==0?-127:trial==1?127:rng.nextInt(-127,128);
                if(i<width)us[i]=raw;else them[i-width]=raw;weights[offset+i]=w;
                expected=expected.add(java.math.BigInteger.valueOf(Math.clamp(raw,0,255)).pow(2).multiply(java.math.BigInteger.valueOf(w)));
            }
            assertEquals(expected.longValueExact(),kernel.dot(us,them,weights,offset));
            int[] activated=new int[272];
            for(int i=0;i<128;i++) {
                int a=Math.clamp(us[i],0,255),b=Math.clamp(them[i],0,255);
                activated[16+i]=a*a;activated[144+i]=b*b;
            }
            assertEquals(expected.longValueExact(),kernel.dense(activated,16,weights,offset));
        }
    }
}
