import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local diagnostic only. Labels are White-relative Bullet text; scores are STM-relative. */
public final class SeedNnueValidationDiagnostics {
    private static final class Stats {
        long n;
        double mse, zero, mae, outcomeMse, teacherOutcomeMse, prediction, target, outcome;
        void add(int score, double link, double p, double q, double result) {
            n++; mse+=(p-q)*(p-q); zero+=(.5-q)*(.5-q); mae+=Math.abs(score-link);
            outcomeMse+=(p-result)*(p-result); teacherOutcomeMse+=(q-result)*(q-result);
            prediction+=p; target+=q; outcome+=result;
        }
        Map<String,Object> summary() {
            var r=new LinkedHashMap<String,Object>(); r.put("positions",n);
            if(n>0) {
                r.put("sigmoid_mse",mse/n); r.put("zero_score_mse",zero/n); r.put("link_mae",mae/n);
                r.put("outcome_mse",outcomeMse/n); r.put("teacher_outcome_mse",teacherOutcomeMse/n);
                r.put("mean_prediction",prediction/n); r.put("mean_teacher",target/n); r.put("mean_outcome",outcome/n);
            }
            return r;
        }
    }
    private static Stats[] stats(int n) {
        Stats[] result=new Stats[n]; for(int i=0;i<n;i++)result[i]=new Stats(); return result;
    }
    private static Object[] summaries(Stats[] values) {
        Object[] r=new Object[values.length];for(int i=0;i<r.length;i++)r[i]=values[i].summary();return r;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("NETWORK WHITE_RELATIVE_BULLET_TEXT");
        if(System.getenv("SEED_NNUE_OPERATION")==null)throw new IllegalStateException("Use nnue-control.cmd run");
        var net=ConventionalNetwork.load(Path.of(args[0]));var acc=net.newAccumulator();
        var all=new Stats();var material=stats(8);var targetBins=stats(10);var outcomes=stats(3);
        try(var reader=Files.newBufferedReader(Path.of(args[1]))) {
            for(String line;(line=reader.readLine())!=null;) {
                String[] fields=line.split("\\|");if(fields.length!=3)throw new IllegalArgumentException("Expected three fields");
                long[] board=Board.fromFen(fields[0].trim());boolean white=Board.player((int)board[4])==0;
                double link=Double.parseDouble(fields[1].trim())*(white?1:-1);
                double result=Double.parseDouble(fields[2].trim());if(!white)result=1-result;
                if(result!=0 && result!=.5 && result!=1)throw new IllegalArgumentException("Invalid result");
                double q=1/(1+Math.exp(-link/400));acc.rebuild(board);int score=acc.evaluate(board);
                double p=1/(1+Math.exp(-score/400.0));
                int pieces=Long.bitCount(board[0]|board[1]|board[2]);
                if(pieces<2 || pieces>32)throw new IllegalArgumentException("Invalid material count");
                for(Stats s:new Stats[]{all,material[(pieces-2)/4],targetBins[Math.min(9,(int)(q*10))],outcomes[(int)(result*2)]})
                    s.add(score,link,p,q,result);
            }
        }
        if(all.n==0)throw new IllegalArgumentException("Empty input");
        var r=new LinkedHashMap<String,Object>();r.put("payload_sha256",net.payloadSha256());r.put("overall",all.summary());
        r.put("material_buckets_2_to_5_through_30_to_32",summaries(material));
        r.put("teacher_probability_deciles",summaries(targetBins));r.put("stm_loss_draw_win",summaries(outcomes));
        r.put("limits","Diagnostic reused holdout; game independence unproven. Outcome MSE is not playing strength. Link scores are not source centipawns.");
        System.out.println(new Gson().toJson(r));
    }
}
