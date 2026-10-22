package com.ohinteractive.seedv6.core.nnue.conventional;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.SplittableRandom;
import static org.junit.jupiter.api.Assertions.*;

class ConventionalNetworkTest {
    private static byte[] fixture(boolean extreme) throws Exception {
        var payload = ByteBuffer.allocate(ConventionalNetwork.PAYLOAD_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        var random = new SplittableRandom(20261022);
        for (int i = 0; i < ConventionalNetwork.PARAMETERS; i++)
            payload.putShort((short) (extreme ? Short.MAX_VALUE : random.nextInt(-32, 33)));
        var file = ByteBuffer.allocate(ConventionalNetwork.HEADER_BYTES + payload.capacity()).order(ByteOrder.LITTLE_ENDIAN);
        file.put(new byte[]{'S','6','C','N','U','0','0','1'});
        for (int n : new int[]{1,7680,256,10,255,64,400,payload.capacity()}) file.putInt(n);
        file.put(MessageDigest.getInstance("SHA-256").digest(payload.array())).put(payload.array());
        return file.array();
    }
    private static ConventionalNetwork network() throws Exception { return ConventionalNetwork.read(new ByteArrayInputStream(fixture(false))); }

    @Test void rejectsTruncationTrailingBytesBadHashAndEverySchemaField() throws Exception {
        byte[] file = fixture(false);
        assertEquals(64, ConventionalNetwork.read(new ByteArrayInputStream(file)).payloadSha256().length());
        for (int index : new int[]{0,8,12,16,20,24,28,32,36,40,72,file.length-1}) {
            byte[] bad = file.clone(); bad[index] ^= 1;
            assertThrows(IOException.class, () -> ConventionalNetwork.read(new ByteArrayInputStream(bad)));
        }
        for (int length : new int[]{0,71,file.length-1,file.length+1}) {
            byte[] bad = Arrays.copyOf(file,length);
            assertThrows(IOException.class, () -> ConventionalNetwork.read(new ByteArrayInputStream(bad)));
        }
    }

    @Test void featureCoordinatesMirrorAndOrientBothKings() {
        // White king e1 mirrors a2 to h2; its bucket is 3.
        assertEquals(3*768+15, ConventionalFeatures.index(0,4,6,8));
        // Black king e8: same friendly pawn geometry after rank+file inversion.
        assertEquals(3*768+15, ConventionalFeatures.index(1,60,14,48));
        assertEquals(3*768+384+15, ConventionalFeatures.index(0,4,14,8));
        assertNotEquals(ConventionalFeatures.context(0,3),ConventionalFeatures.context(0,4));
        assertEquals(ConventionalFeatures.context(0,16),ConventionalFeatures.context(0,17));
        for (int side=0;side<2;side++) for(int king=0;king<64;king++)
            for(int color=0;color<2;color++) for(int type=1;type<=6;type++) for(int sq=0;sq<64;sq++) {
                int index=ConventionalFeatures.index(side,king,type+(color<<3),sq);
                assertTrue(index>=0 && index<7680);
            }
    }

    @Test void specialMovesUndoAndNullAreExactIncludingMirrorAndBucketCrossings() throws Exception {
        var net=network();
        String[][] fixtures={
            {Board.FEN_STARTING_POSITION,"e2e4"},
            {"4k3/8/8/3n4/4P3/8/8/4K3 w - - 0 1","e4d5"},
            {"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","e1g1","e1c1"},
            {"r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1","e8g8","e8c8"},
            {"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","e5d6"},
            {"4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 1","d4e3"},
            {"7k/8/8/8/8/8/8/3K4 w - - 0 1","d1e1","d1d2"},
            {"3k4/8/8/8/8/8/8/7K b - - 0 1","d8e8","d8d7"},
            {"7k/8/8/8/8/8/8/2K5 w - - 0 1","c1b1"},
            {"7k/8/8/8/8/K7/8/8 w - - 0 1","a3b3"},
            {"1r5k/P7/8/8/8/8/8/7K w - - 0 1","a7a8q","a7a8r","a7a8b","a7a8n","a7b8q","a7b8r","a7b8b","a7b8n"},
            {"7k/8/8/8/8/8/p7/1R5K b - - 0 1","a2a1q","a2a1r","a2a1b","a2a1n","a2b1q","a2b1r","a2b1b","a2b1n"}
        };
        int count=0;
        for(var f:fixtures) {
            long[] parent=Board.fromFen(f[0]); var a=net.newAccumulator(); a.rebuild(parent);
            for(int i=1;i<f.length;i++) {
                long[] child=play(parent,f[i]); var b=net.newAccumulator();
                b.update(parent,child,a); compare(net,child,b); compare(net,parent,a);
                var undo=net.newAccumulator(); undo.update(child,parent,b); compare(net,parent,undo); count++;
            }
            long[] child=new long[6];
            Board.nullMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],child);
            var b=net.newAccumulator(); b.update(parent,child,a); compare(net,child,b);
            for(int side=0;side<2;side++) for(int j=0;j<256;j++) assertEquals(a.raw(side,j),b.raw(side,j));
        }
        System.out.println("special move transitions="+count+" plus null/undo");
    }

    @Test void deterministicLegalWalksAndSiblingsMatchEveryRawAccumulator() throws Exception {
        var net=network(); var rng=new SplittableRandom(78123); int transitions=0;
        for(int game=0;game<4;game++) {
            long[] parent=Board.startingPosition(); var a=net.newAccumulator(); a.rebuild(parent);
            for(int ply=0;ply<80;ply++) {
                long[] moves=new long[256]; int n=generate(parent,moves); if(n==0)break;
                int selected=rng.nextInt(n); long[] child=null; ConventionalAccumulator b=null;
                for(int k=0;k<Math.min(n,3);k++) {
                    child=make(parent,moves[(selected+k)%n]); b=net.newAccumulator(); b.update(parent,child,a);
                    compare(net,child,b); compare(net,parent,a); transitions++;
                }
                parent=child;a=b;
            }
        }
        System.out.println("legal/sibling transitions="+transitions);
    }

    @Test void stateOwnershipResetQsearchAbsolutePlyAndMismatches() throws Exception {
        var net=network(); var definition=SearchEvaluation.conventional(net);
        var main=definition.newState(257); var q=definition.newState(257); var worker=definition.newState(257);
        long[] board=Board.startingPosition(),child=play(board,"e2e4");
        main.initialize(board,255);main.child(board,child,255);q.initializeFrom(child,256,main);
        worker.initialize(board,0);
        assertEquals(net.fullRebuild(child),q.evaluate(child,256));
        assertEquals(net.fullRebuild(board),worker.evaluate(board,0));
        assertThrows(IllegalArgumentException.class,()->main.initializeFrom(board,0,main));
        assertThrows(IllegalArgumentException.class,()->q.initializeFrom(child,256,SearchEvaluation.conventional(net).newState(257)));
        assertThrows(IllegalArgumentException.class,()->worker.evaluate(child,0));
        worker.initialize(child,0);assertEquals(net.fullRebuild(child),worker.evaluate(child,0));
        var oracle=SearchEvaluation.conventionalFullRebuild(net).newState(1);
        assertEquals(net.fullRebuild(child),oracle.evaluate(child,0));
        var a=net.newAccumulator(); var b=network().newAccumulator();a.rebuild(board);b.rebuild(board);
        assertThrows(IllegalArgumentException.class,()->a.copyFrom(board,b));
        assertThrows(IllegalArgumentException.class,()->a.update(board,child,a));
        assertThrows(IllegalArgumentException.class,()->a.rebuild(new long[6]));
        compare(net,board,a);
    }

    @Test void hostileQuantizedWeightsCannotOverflowIntoWrongSignOrMateDomain() throws Exception {
        var net=ConventionalNetwork.read(new ByteArrayInputStream(fixture(true)));
        var a=net.newAccumulator();var board=Board.startingPosition();a.rebuild(board);
        assertEquals(32511,a.evaluate(board));assertEquals(32511,net.fullRebuild(board));
        assertEquals(33*32767,a.raw(0,0));
    }

    @Test void headAtIntBoundaryAndHostileSignedWeightsMatchIndependentArithmetic() throws Exception {
        // 33025 * 255^2 fits int; 33026 * 255^2 does not. Cover both sides of the bound,
        // signed-short minimum, cancellation and arbitrary unclipped accumulator values.
        short[][] heads = new short[5][512];
        heads[0][0]=32767; heads[0][256]=258;
        heads[1][0]=32767; heads[1][256]=259;
        heads[2][0]=Short.MIN_VALUE; heads[2][256]=-258;
        heads[3][0]=32767; heads[3][256]=Short.MIN_VALUE;
        Arrays.fill(heads[4],Short.MIN_VALUE);
        var random=new SplittableRandom(88231);
        for(short[] head:heads) {
            byte[] file=fixture(false);
            var bytes=ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
            int offset=ConventionalNetwork.HEADER_BYTES+(7680*256+256)*2;
            for(int i=0;i<head.length;i++)bytes.putShort(offset+2*i,head[i]);
            bytes.putShort(file.length-2,(short)-123);
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(
                    Arrays.copyOfRange(file,ConventionalNetwork.HEADER_BYTES,file.length));
            System.arraycopy(digest,0,file,40,digest.length);
            var net=ConventionalNetwork.read(new ByteArrayInputStream(file));
            for(int trial=0;trial<40;trial++) {
                int[] us=new int[256],them=new int[256];
                java.math.BigInteger dot=java.math.BigInteger.ZERO;
                for(int i=0;i<512;i++) {
                    int raw=trial==0?Integer.MAX_VALUE:trial==1?Integer.MIN_VALUE:
                            trial==2?255:random.nextInt(-1000,1001);
                    if(i<256)us[i]=raw;else them[i-256]=raw;
                    int clipped=Math.max(0,Math.min(255,raw));
                    dot=dot.add(java.math.BigInteger.valueOf(clipped).pow(2)
                            .multiply(java.math.BigInteger.valueOf(head[i])));
                }
                long cp=dot.divide(java.math.BigInteger.valueOf(255))
                        .subtract(java.math.BigInteger.valueOf(123))
                        .multiply(java.math.BigInteger.valueOf(400))
                        .divide(java.math.BigInteger.valueOf(16320)).longValueExact();
                assertEquals(Math.max(-32511,Math.min(32511,cp)),net.score(us,them));
            }
        }
    }

    @Test void completeQsearchAndTwoPrivateWorkersMatchFullRebuildSearch() throws Exception {
        var net=network();
        for(String fen:new String[]{"4k3/8/8/3n4/4P3/8/8/4K3 w - - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                "7k/P7/8/8/8/8/8/7K w - - 0 1"}) {
            var board=Board.fromFen(fen);
            var request=new com.ohinteractive.seedv6.search.common.SearchRequest(board,
                com.ohinteractive.seedv6.rules.GameHistory.initial(board),2,
                com.ohinteractive.seedv6.search.common.SearchObserver.NONE,
                com.ohinteractive.seedv6.search.common.SearchControl.unlimited(),true);
            int reference;
            try(var oracle=com.ohinteractive.seedv6.search.driver.ProductionSearch.create(1,SearchEvaluation.conventionalFullRebuild(net),4)) {
                var result=oracle.search(request);assertTrue(result.completed());reference=result.score();
            }
            for(int workers:new int[]{1,2})try(var search=com.ohinteractive.seedv6.search.driver.ProductionSearch.create(workers,SearchEvaluation.conventional(net),4)) {
                var result=search.search(request);assertTrue(result.completed());assertEquals(reference,result.score());
                assertTrue(result.diagnostics().worker().nodes().qNodes()>0);
                assertArrayEquals(board,Board.fromFen(fen));
            }
        }
    }

    private static void compare(ConventionalNetwork net,long[] board,ConventionalAccumulator actual) {
        var rebuilt=net.newAccumulator();rebuilt.rebuild(board);
        for(int s=0;s<2;s++)for(int j=0;j<256;j++)assertEquals(rebuilt.raw(s,j),actual.raw(s,j));
        assertEquals(net.fullRebuild(board),actual.evaluate(board));
    }
    private static int generate(long[] b,long[] moves) {return Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,moves,new long[6]);}
    private static long[] make(long[] b,long move) {long[] out=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],move,out);return out;}
    private static long[] play(long[] b,String uci) {
        long[] moves=new long[256];int n=generate(b,moves);
        for(int i=0;i<n;i++)if(Move.coordinate(moves[i]).equals(uci))return make(b,moves[i]);
        throw new AssertionError("Illegal test move "+uci);
    }
}
