package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.nio.file.*;
import java.util.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Bounded research prototype only. No search integration, checkpoint writer or production routing. */
public final class BrnCubicDeltaProbe {
    private record Plan(String id,List<BrnSuccessorMatch.Model> models) {}
    private static volatile double sink;
    static final class Delta {
        final BrnNextModel model;
        final int width=CUBIC.width(),head=CUBIC.head();
        final long[] previous=new long[4];
        final int[][] codes=new int[2][64];
        final double[][] first=new double[2][width],second=new double[2][width],third=new double[2][width];
        final double[] unary=new double[2],last=new double[2],material=new double[2];
        final boolean[] valid=new boolean[2];
        final com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model.Workspace base=BrnNextBase.model().newWorkspace();
        boolean initialized;int changes;long rebuilds,updates;
        Delta(BrnNextModel model) {
            if(model.architecture()!=GEN32_CUBIC)throw new IllegalArgumentException("W3 CUBIC only");
            this.model=model;
        }
        int category(long[] board,int physical,int side) {
            int code=Board.getSquare(board[0],board[1],board[2],board[3],physical),type=code&7;
            if(type==7)throw new IllegalArgumentException("Invalid category");
            return type==0?0:type+(((code>>>3)^side)&1)*6;
        }
        void piece(int side,int square,int code,boolean add) {
            if(code==0)return;
            int feature=(code-1)*64+square;
            unary[side]+=(add?1:-1)*PIECE_NORM*model.weight(UNARY+feature);
            for(int r=0;r<width;r++) {
                double value=model.weight(EMBEDDING+feature*width+r);
                if(add) {
                    third[side][r]+=second[side][r]*value;
                    second[side][r]+=first[side][r]*value;
                    first[side][r]+=value;
                } else {
                    // Invert multiplication by(1+value*t), retaining coefficients through degree3.
                    first[side][r]-=value;
                    second[side][r]-=first[side][r]*value;
                    third[side][r]-=second[side][r]*value;
                }
            }
        }
        void rebuild(long[] board) {
            rebuilds++;changes=0;
            for(int side=0;side<2;side++) {
                unary[side]=model.weight(0);Arrays.fill(first[side],0);Arrays.fill(second[side],0);Arrays.fill(third[side],0);
                for(int square=0;square<64;square++) {
                    int code=category(board,square^(side*56),side);codes[side][square]=code;piece(side,square,code,true);
                }
            }
        }
        double raw(long[] board) {
            long changed=0;
            for(int i=0;i<4;i++)changed|=previous[i]^board[i];
            changed&=previous[0]|previous[1]|previous[2]|board[0]|board[1]|board[2];
            if(!initialized||changed!=0) {
                valid[0]=valid[1]=false;
                if(!initialized||changes>=64||Long.bitCount(changed)>8)rebuild(board);
                else {
                    updates++;changes++;
                    for(int side=0;side<2;side++)for(long bits=changed;bits!=0;bits&=bits-1) {
                        int physical=Long.numberOfTrailingZeros(bits),square=physical^(side*56);
                        int before=codes[side][square],after=category(board,physical,side);
                        piece(side,square,before,false);piece(side,square,after,true);codes[side][square]=after;
                    }
                }
                System.arraycopy(board,0,previous,0,4);initialized=true;
            }
            int side=Board.player((int)board[Board.STATUS]);if(valid[side])return last[side];
            double correction=unary[side];
            for(int r=0;r<width;r++)correction+=model.weight(head+r)*second[side][r]*DEGREE2_NORM
                    +model.weight(head+width+r)*third[side][r]*DEGREE3_NORM;
            double value=base.evaluatePawns(board)+correction;material[side]=base.materialPawns(side);
            if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite CUBIC delta prototype");
            valid[side]=true;return last[side]=value;
        }
        int score(long[] board) {
            double raw=raw(board),m=material[Board.player((int)board[Board.STATUS])];
            return Brn3Model.score(m+.25*(raw-m));
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("MODEL_PLAN MODEL_NAME RECEIPTS CP_JSONL NEW_OUTPUT");
        Path output=Path.of(args[4]);if(Files.exists(output))throw new IllegalArgumentException("Preserve evidence");
        var plan=DataFiles.read(Path.of(args[0]),Plan.class);
        var spec=plan.models().stream().filter(m->m.name().equals(args[1])).findFirst().orElseThrow();
        var snapshot=CheckpointStore.readSnapshot(Path.of(spec.root()),spec.checkpoint());
        if(!snapshot.manifest().networkSha256().equals(spec.networkSha256()))throw new IllegalArgumentException("Model binding");
        var model=((NetworkModel.BrnNext)snapshot.model()).model();
        model=BrnSuccessorVariants.correction(model,spec.correctionScale()==null?1:spec.correctionScale(),null,null);
        var positions=new ArrayList<long[]>();var bindings=new TreeMap<String,String>();
        bindings.put(args[0],DataFiles.hash(Files.readString(Path.of(args[0]))));
        try(var paths=Files.list(Path.of(args[2]))) {
            for(var p:paths.filter(p->p.getFileName().toString().matches("p\\d+-m\\d+-c\\d+\\.json")).sorted().toList()) {
                String text=Files.readString(p);bindings.put(p.toString(),DataFiles.hash(text));
                var receipt=DataFiles.JSON.fromJson(text,BrnSuccessorMatch.Receipt.class);
                positions.add(Board.fromFen(receipt.openingFen()));
                for(var move:receipt.moves())positions.add(Board.fromFen(move.fenAfter()));
            }
        }
        int transitions=positions.size();
        String text=Files.readString(Path.of(args[3]));bindings.put(args[3],DataFiles.hash(text));
        for(String line:text.lines().toList())positions.add(Board.fromFen(JsonParser.parseString(line).getAsJsonObject().get("fen").getAsString()));
        var full=model.newWorkspace();var delta=new Delta(model);double maximum=0;int different=0;
        for(int pass=0;pass<3;pass++)for(int i=0;i<positions.size();i++) {
            var board=positions.get(pass==1?positions.size()-1-i:i);
            maximum=Math.max(maximum,Math.abs(model.rawPawns(board,full)-delta.raw(board)));
            if(model.score(board,full)!=delta.score(board))different++;
        }
        var timings=new ArrayList<Object>();
        var old=new BrnCubicAccumulator(model);double sum=0;
        for(int i=0;i<100000;i++){var b=positions.get(i%transitions);sum+=old.rawPawns(b)+delta.raw(b);}sink=sum;
        for(boolean useDelta:new boolean[]{false,true,true,false,true,false,false,true}) {
            old=new BrnCubicAccumulator(model);delta=new Delta(model);sum=0;long started=System.nanoTime();
            for(int i=0;i<200000;i++){var b=positions.get(i%transitions);sum+=useDelta?delta.raw(b):old.rawPawns(b);}
            long elapsed=System.nanoTime()-started;sink=sum;
            timings.add(Map.of("delta",useDelta,"nanosPerTransition",elapsed/200000.0,"rebuilds",delta.rebuilds,"updates",delta.updates));
        }
        var report=new LinkedHashMap<String,Object>();
        report.put("id","wave3-cubic-delta-prototype-v1");report.put("inputs",bindings);report.put("manifest",snapshot.manifest());
        report.put("correctionScale",spec.correctionScale());report.put("comparisons",positions.size()*3);
        report.put("maximumRawError",maximum);report.put("integerDifferences",different);report.put("timings",timings);
        report.put("scope","Research only; rebuild after64 changed placements or more than8 changed squares. Empirical error check, not a universal rounding proof; no search integration or model publication.");
        DataFiles.write(output,report);
        if(maximum>1e-9||different!=0)throw new IllegalStateException("Delta parity failed; preserve output");
        System.out.println("Delta prototype parity passed; inspect bounded timings: "+output);
    }
    private BrnCubicDeltaProbe() {}
}
