package com.ohinteractive.seedv6.core;

import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Fen;

/** Isolated SEE sign/numeric cost and queen-material sensitivity; no strength claim. */
public final class Sr024ExchangeCost {
    record Sample(long[] board,long move) {}
    static volatile long sink;
    public static void main(String[] args)throws Exception {
        var samples=new ArrayList<Sample>();var random=new Random(2026101424L);
        int positions=0;
        for(int game=0;game<32;game++) {
            long[] board=Board.startingPosition();
            for(int ply=0;ply<80;ply++) {
                long[] list=legal(board);positions++;
                for(long move:list)if(tactical(board,move))samples.add(new Sample(board.clone(),move));
                if(list.length==0)break;
                board=child(board,list[random.nextInt(list.length)]);
            }
        }
        long positive=0,negative=0,zero=0;var differences=new ArrayList<Map<String,Object>>();
        var values=new TreeMap<Integer,Long>();
        for(var s:samples) {
            int exact=SeeLegalOracle.evaluate(s.board,s.move);
            if(exact!=See.evaluate(s.board,s.move)||See.atLeastGeneratedLegal(s.board,s.move,0)!=(exact>=0))
                throw new AssertionError("Independent exchange mismatch");
            if(exact<0)negative++;else if(exact>0)positive++;else zero++;
            values.merge(exact,1L,Long::sum);
            int pairMaterial=exchange(s.board,s.move,900);
            if((exact>=0)!=(pairMaterial>=0))differences.add(Map.of("fen",Fen.fromBoard(s.board),
                    "move",Move.coordinate(s.move),"see975",exact,"see900",pairMaterial));
        }
        long[] scratch=new long[4];var allocation=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        for(int warm=0;warm<100;warm++)for(var s:samples) {
            sink+=See.atLeastGeneratedLegal(s.board,s.move,0)?1:0;
            sink+=See.evaluateGeneratedLegal(s.board,s.move,scratch);
        }
        var rows=new ArrayList<Map<String,Object>>();long thread=Thread.currentThread().threadId();
        for(int round=0;round<6;round++)for(int step=0;step<2;step++) {
            boolean threshold=((round+step)&1)==0;long sum=0,calls=0;
            long allocated=allocation.getThreadAllocatedBytes(thread),start=System.nanoTime();
            for(int repeat=0;repeat<100;repeat++)for(var s:samples) {
                sum+=threshold?(See.atLeastGeneratedLegal(s.board,s.move,0)?1:0):See.evaluateGeneratedLegal(s.board,s.move,scratch);calls++;
            }
            long elapsed=System.nanoTime()-start,bytes=allocation.getThreadAllocatedBytes(thread)-allocated;sink=sum;
            rows.add(Map.of("round",round,"mode",threshold?"threshold":"numeric","calls",calls,
                    "nanos",elapsed,"allocatedBytes",bytes,"checksum",sum));
        }
        var report=new LinkedHashMap<String,Object>();report.put("status","passed");report.put("positions",positions);
        report.put("moves",samples.size());report.put("signCounts",List.of(positive,zero,negative));
        report.put("exchangeValues975",values);
        report.put("queen900SignDifferences",differences);report.put("measurements",rows);
        report.put("limits","Deterministic random legal games are a coverage/microcost corpus, not search-frequency or strength evidence. Alternate queen value changes exchange information only; evaluator remains fixed. Scalar current-thread allocations omit other JVM threads.");
        Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n",StandardOpenOption.CREATE_NEW);
    }
    static long[] legal(long[] b) {
        long[] moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
        int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[Board.STATUS],b[Board.KEY],true,moves,scratch);
        return Arrays.copyOf(moves,n);
    }
    static long[] child(long[] b,long m) {
        long[] result=new long[Board.MAX_BITBOARDS];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[Board.STATUS],b[Board.KEY],m,result);return result;
    }
    static boolean tactical(long[] b,long m) {
        return ((m>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0||((m>>>Board.PROMOTE_PIECE_SHIFT)&Board.PIECE_BITS)!=0
                ||((m>>>Board.START_PIECE_SHIFT)&Piece.TYPE)==Piece.PAWN&&Move.toSquare(m)==Board.enPassantSquare((int)b[Board.STATUS]);
    }
    static int value(int type,int queen) {return type==0?0:type==Piece.QUEEN?queen:Eval.exchangeValue(type);}
    static int gain(long[] b,long m,int queen) {
        int captured=(int)(m>>>Board.TARGET_PIECE_SHIFT)&Piece.TYPE;
        if(captured==0&&((m>>>Board.START_PIECE_SHIFT)&Piece.TYPE)==Piece.PAWN
                &&Move.toSquare(m)==Board.enPassantSquare((int)b[Board.STATUS]))captured=Piece.PAWN;
        int promotion=(int)(m>>>Board.PROMOTE_PIECE_SHIFT)&Piece.TYPE;
        return value(captured,queen)+(promotion==0?0:value(promotion,queen)-value(Piece.PAWN,queen));
    }
    static int exchange(long[] b,long m,int queen) {return gain(b,m,queen)-reply(child(b,m),Move.toSquare(m),queen);}
    static int reply(long[] b,int target,int queen) {
        int best=0;
        for(long m:legal(b))if(Move.toSquare(m)==target&&((m>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0)
            best=Math.max(best,gain(b,m,queen)-reply(child(b,m),target,queen));
        return best;
    }
}
