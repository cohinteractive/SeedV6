package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalFeatures;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Explicit research-only cross-language fixture emission; never packaged in UCI. */
public final class ConventionalNnueProbe {
    private record Row(String fen,int score,int rebuilt,List<List<Integer>> features,int[][] accumulators) {}
    public static void main(String[] args) throws Exception {
        if(args.length==3 && args[1].equals("--validation")) { validation(args);return; }
        if(args.length!=2)throw new IllegalArgumentException("network.cnnue positions.fens | network.cnnue --validation data.txt");
        var net=ConventionalNetwork.load(Path.of(args[0]));var acc=net.newAccumulator();var gson=new Gson();
        for(String fen:Files.readAllLines(Path.of(args[1]))) {
            long[] b=Board.fromFen(fen);acc.rebuild(b);
            int stm=Board.player((int)b[4]);var fs=new ArrayList<List<Integer>>();int[][] raw=new int[2][256];
            for(int side=0;side<2;side++) {
                int perspective=stm^side;long kings=b[0]&~b[1]&~b[2];
                int king=Long.numberOfTrailingZeros(kings&(perspective==0?~b[3]:b[3]));
                var indices=new ArrayList<Integer>();
                for(int square=0;square<64;square++) {
                    int piece=Board.getSquare(b[0],b[1],b[2],b[3],square);
                    if(piece!=0)indices.add(ConventionalFeatures.index(perspective,king,piece,square));
                }
                Collections.sort(indices);fs.add(indices);
                for(int i=0;i<256;i++)raw[side][i]=acc.raw(perspective,i);
            }
            System.out.println(gson.toJson(new Row(fen,acc.evaluate(b),net.fullRebuild(b),fs,raw)));
        }
    }
    private static void validation(String[] args) throws Exception {
        var net=ConventionalNetwork.load(Path.of(args[0]));var acc=net.newAccumulator();
        int count=0;double mse=0,zeroMse=0,mae=0;
        try(var reader=Files.newBufferedReader(Path.of(args[2]))) {
            for(String line;(line=reader.readLine())!=null;) {
                String[] fields=line.split("\\|");long[] board=Board.fromFen(fields[0].trim());
                double targetCp=Double.parseDouble(fields[1].trim())*(Board.player((int)board[4])==0?1:-1);
                double target=1/(1+Math.exp(-targetCp/400));acc.rebuild(board);int score=acc.evaluate(board);
                double prediction=1/(1+Math.exp(-score/400.0));
                mse+=(prediction-target)*(prediction-target);zeroMse+=(.5-target)*(.5-target);mae+=Math.abs(score-targetCp);count++;
            }
        }
        if(count==0)throw new IllegalArgumentException("Empty validation");
        System.out.println(new Gson().toJson(java.util.Map.of("positions",count,"sigmoid_mse",mse/count,
            "zero_score_mse",zeroMse/count,"cp_mae",mae/count,"payload_sha256",net.payloadSha256())));
    }
}
