package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Magic;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;

/** Read-only feasibility oracle. No trained model, optimizer, search integration or codec. */
public final class BrnVisibilityFeatureProbe {
    static final int PARAMETERS=2*16*12*7;
    static final int[] DX={1,-1,0,0,1,1,-1,-1},DY={0,0,1,-1,1,-1,1,-1};
    private static volatile double sink;

    static long[] canonical(long[] board) {
        long[] result=Arrays.copyOf(board,4);
        if(Board.player((int)board[Board.STATUS])!=0) {
            for(int i=0;i<4;i++)result[i]=Long.reverseBytes(result[i]);
            result[3]^=result[0]|result[1]|result[2];
        }
        return result;
    }
    static int code(long[] b,int square) { return Board.getSquare(b[0],b[1],b[2],b[3],square); }
    static int relation(int type,int direction) {
        return switch(type) {
            case Piece.BISHOP -> direction>=4?direction-4:-1;
            case Piece.ROOK -> direction<4?4+direction:-1;
            case Piece.QUEEN -> 8+direction;
            default -> -1;
        };
    }
    static int address(int attacker,int target,int direction,int distance) {
        int slot=relation(attacker&7,direction);
        if(slot<0||distance<1||distance>7||(target&7)<1||(target&7)>6)
            throw new IllegalArgumentException("Unsupported ray feature");
        int category=(target&7)-1+((target>>>3)&1)*6;
        return (((attacker>>>3)&1)*16+slot)*84+category*7+distance-1;
    }
    /** Independent coordinate walker; sorted IDs retain multiplicities. */
    static int[] oracle(long[] b,boolean visible) {
        var features=new ArrayList<Integer>();
        for(int square=0;square<64;square++) {
            int attacker=code(b,square);
            for(int d=0;d<8;d++)if(relation(attacker&7,d)>=0) {
                for(int distance=1;distance<=7;distance++) {
                    int x=(square&7)+distance*DX[d],y=(square>>>3)+distance*DY[d];
                    if(x<0||x>7||y<0||y>7)break;
                    int target=code(b,y*8+x);
                    if((target&7)==0)continue;
                    features.add(address(attacker,target,d,distance));
                    if(visible)break;
                }
            }
        }
        return features.stream().mapToInt(Integer::intValue).sorted().toArray();
    }
    static int direction(int from,int to) {
        int x=(to&7)-(from&7),y=(to>>>3)-(from>>>3);
        if(y==0)return x>0?0:1;
        if(x==0)return y>0?2:3;
        if(Math.abs(x)!=Math.abs(y))throw new IllegalArgumentException("Non-ray target");
        return x>0?(y>0?4:5):(y>0?6:7);
    }
    /** Allocation-free extraction into caller memory; no ordering dependence in feature identity. */
    static int extract(long[] b,boolean visible,int[] output) {
        long occupied=b[0]|b[1]|b[2];int count=0;
        long sliders=((b[1]&~b[2])|(~b[0]&~b[1]&b[2]))&occupied;
        for(long bits=sliders;bits!=0;bits&=bits-1) {
            int square=Long.numberOfTrailingZeros(bits),attacker=code(b,square),type=attacker&7;
            long blockers=visible?occupied:0;
            long targets=switch(type) {
                case Piece.BISHOP -> Magic.bishopMoves(square,blockers);
                case Piece.ROOK -> Magic.rookMoves(square,blockers);
                case Piece.QUEEN -> Magic.queenMoves(square,blockers);
                default -> throw new IllegalStateException("Slider bit mask differs");
            }&occupied;
            for(long targetBits=targets;targetBits!=0;targetBits&=targetBits-1) {
                int target=Long.numberOfTrailingZeros(targetBits);
                output[count++]=address(attacker,code(b,target),direction(square,target),
                        Math.max(Math.abs((target&7)-(square&7)),Math.abs((target>>>3)-(square>>>3))));
            }
        }
        return count;
    }
    static double run(List<long[]> boards,boolean visible,double[] weights,int repetitions) {
        int[] features=new int[4096];double total=0;
        for(int i=0;i<repetitions;i++) {
            long[] board=boards.get(i%boards.size());
            int n=extract(board,visible,features);
            for(int j=0;j<n;j++)total+=weights[features[j]];
        }
        sink=total;return total;
    }
    static int[] witness(boolean visible) {
        int[] values=new int[8];
        int feature=address(Piece.ROOK,Piece.BISHOP|8,2,3);
        for(int mask=0;mask<8;mask++) {
            String rook=(mask&1)!=0?"R6K":"7K";
            String blocker=(mask&2)!=0?"P7":"8";
            String target=(mask&4)!=0?"b7":"8";
            long[] board=canonical(Board.fromFen("7k/8/8/8/"+target+"/8/"+blocker+"/"+rook+" w - - 0 1"));
            for(int f:oracle(board,visible))if(f==feature)values[mask]++;
        }
        return values;
    }
    static int thirdDifference(int[] values) {
        int result=0;
        for(int mask=0;mask<8;mask++)result+=((3-Integer.bitCount(mask))%2==0?1:-1)*values[mask];
        return result;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("PREDICTIONS_JSONL NEW_REPORT NEW_FEATURES_JSONL");
        Path source=Path.of(args[0]),report=Path.of(args[1]),featuresFile=Path.of(args[2]);
        if(Files.exists(report)||Files.exists(featuresFile))throw new IllegalArgumentException("Preserve prior evidence");
        var boards=new ArrayList<long[]>();var fens=new ArrayList<String>();
        try(var lines=Files.lines(source)) {
            for(String line:lines.toList()) {
                String fen=JsonParser.parseString(line).getAsJsonObject().get("fen").getAsString();
                boards.add(canonical(Board.fromFen(fen)));fens.add(fen);
            }
        }
        if(boards.isEmpty()||boards.size()>4096)throw new IllegalArgumentException("Bounded nonempty panel required");
        long[] totals=new long[2];int[] maxima=new int[2];
        try(var writer=Files.newBufferedWriter(featuresFile,StandardOpenOption.CREATE_NEW)) {
            for(int i=0;i<boards.size();i++) {
                var row=new LinkedHashMap<String,Object>();row.put("fen",fens.get(i));
                for(int mode=0;mode<2;mode++) {
                    int[] actual=new int[4096];int count=extract(boards.get(i),mode==0,actual);
                    actual=Arrays.copyOf(actual,count);Arrays.sort(actual);
                    if(!Arrays.equals(actual,oracle(boards.get(i),mode==0)))throw new IllegalStateException("Independent extraction differs");
                    totals[mode]+=count;maxima[mode]=Math.max(maxima[mode],count);
                    row.put(mode==0?"visible":"geometry",actual);
                }
                // Gson is pretty-printing globally; each emitted record must remain one physical line.
                writer.write(DataFiles.JSON.toJson(row).replace("\n","").replace("\r",""));writer.newLine();
            }
        }
        int[] visibleWitness=witness(true),geometryWitness=witness(false);
        if(thirdDifference(visibleWitness)!=-1||thirdDifference(geometryWitness)!=0)
            throw new IllegalStateException("Higher-order witness differs");
        double[] weights=new double[PARAMETERS];var random=new SplittableRandom(2026102107);
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-.01,.01);
        for(int mode=0;mode<2;mode++)run(boards,mode==0,weights,100000);
        long[][] timings=new long[2][7];
        for(int pass=0;pass<7;pass++)for(int offset=0;offset<2;offset++) {
            int mode=(pass+offset)%2;long start=System.nanoTime();run(boards,mode==0,weights,100000);
            timings[mode][pass]=System.nanoTime()-start;
        }
        var latency=new ArrayList<Object>();
        for(int mode=0;mode<2;mode++) {
            long[] sorted=timings[mode].clone();Arrays.sort(sorted);
            latency.add(Map.of("mode",mode==0?"visible":"geometry","passNanos",timings[mode],
                    "medianNanos",sorted[3]/100000.0,"evaluationsPerPass",100000,
                    "meanActiveFeatures",totals[mode]/(double)boards.size(),"maximumActiveFeatures",maxima[mode]));
        }
        DataFiles.write(report,Map.of("id","brn-visibility-feasibility-v1","source",source.toString(),
                "sourceSha256",HexFormat.of().formatHex(DataFiles.digest().digest(Files.readAllBytes(source))),
                "positions",boards.size(),"parameters",PARAMETERS,"latency",latency,
                "visibleWitness",visibleWitness,"geometryWitness",geometryWitness,
                "limitations",List.of("Untrained random-weight extraction cost on canonical preconverted boards; excludes canonicalization, BRE base, search and incremental maintenance.",
                        "Both friendly and opposing nearest ray occupants included; geometric control ignores intervening occupancy. No legal-move, pin or teacher feature.",
                        "No fitting, model checkpoint, training gate or strength claim.")));
        System.out.println("Verified "+boards.size()+" positions; third differences -1/0; "+latency);
    }
    private BrnVisibilityFeatureProbe() {}
}
