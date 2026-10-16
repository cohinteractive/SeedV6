package com.ohinteractive.seedv6.search.exact;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.*;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;
import static com.ohinteractive.seedv6.search.exact.QuiescenceDeltaEvidence.*;

/** SR-001H diagnostic companion to the existing fixed-depth headless harness/corpus.
 * CSV formatting and exhaustive-oracle allocations occur outside production recursion. */
public final class QuiescenceDeltaCorpus {
    static final String PREFIX="SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28";
    static final ExactEvaluator HCE=QuiescenceSeeCorpus.HCE;
    static final long BUDGET=1_000_000;
    static List<QuiescenceForcingCorpus.Fixture> fixtures() {
        var list=new ArrayList<>(QuiescenceForcingCorpus.fixtures());
        list.add(new QuiescenceForcingCorpus.Fixture("nonchecking-capture-dead-draw",
                "4k3/8/8/8/8/8/3r4/4K3 w - - 0 1", "capture-to-insufficient-material",true,true));
        return list;
    }
    public static void main(String[] args) throws Exception {
        if(args.length>0 && args[0].equals("--witnesses")) { witnesses(Path.of(args[1]));return; }
        Path output=Path.of(args.length==0 ? "." : args[0]);
        try(var raw=writer(output,"OBSERVATIONS");var runs=writer(output,"RUNS");var exact=writer(output,"ORACLE")) {
            raw.println("run,fixture,node,observation,qply,path_ply,node_fen,move,captured_piece,gain,see_sign,en_passant,previous_qmove,same_target_recapture,alpha,beta,stand_pat,raw_delta_bound,alpha_gap,move_score,move_bound,raised_alpha,beta_cutoff,best_changed,final_best_move,selected_line_cause,node_final_score,move_completed,node_completed,invocation_completed");
            runs.println("run,fixture,fen,kind,depth,completed_depth,completed,score,best,pv,normal_nodes,qnodes,total_nodes,max_qply,diagnostic_ms,diagnostic_nps,observations,identity,oracle,oracle_nodes");
            exact.println("fixture,node_fen,qply,path_ply,move,gain,see_sign,en_passant,stand_pat,raw_delta_bound,move_exact_score,node_exact_best,relation_to_best,selected_line_cause,unique_mate,unique_draw,excess_over_raw_bound");
            var evidence=new QuiescenceDeltaEvidence(300_000);
            for(var f:fixtures()) {
                long[] b=Board.fromFen(f.fen());
                // Full enumeration is guarded by the established oracle's 200k/64 limits.
                var oracle=new QuiescenceOracle(HCE); List<String> rows=new ArrayList<>();
                oracle.evidence=(board,ply,qply,stand,best,moves,values,causes)->oracleRows(rows,f.name(),board,ply,qply,stand,best,moves,values,causes);
                String status="matched"; int value=0;
                try { value=oracle.score(b,new SearchLineHistory(GameHistory.initial(b)),0,0); }
                catch(AssertionError ex) {
                    if(!"Unbounded oracle fixture".equals(ex.getMessage())) throw ex;
                    status="guard-rejected-not-exact"; rows.clear();
                }
                var full=run(raw,runs,evidence,"corpus:"+f.name()+":full",f.name(),b,"corpus-full",0,-32769,32769,status,oracle.nodes);
                if(status.equals("matched")) {
                    require(full.completed() && full.score()==value,"Oracle mismatch "+f.name());
                    rows.forEach(exact::println);
                }
                // Windows around the independently completed value expose ordinary bound visitation.
                if(full.completed()) {
                    int score=full.score();
                    run(raw,runs,evidence,"corpus:"+f.name()+":lower",f.name(),b,"corpus-lower",0,score-1,score,"not-repeated",0);
                    run(raw,runs,evidence,"corpus:"+f.name()+":upper",f.name(),b,"corpus-upper",0,score,score+1,"not-repeated",0);
                }
            }
            List<ExactSearchHarness.Position> positions=new ArrayList<>(ExactSearchHarness.orderingPositions());
            positions.add(ExactSearchHarness.positions().stream().filter(p->p.name().equals("endgame")).findFirst().orElseThrow());
            for(int depth:new int[]{2,3}) for(var p:positions)
                run(raw,runs,evidence,"benchmark:"+p.name()+":"+depth,p.name(),Board.fromFen(p.fen()),"benchmark",depth,-32769,32769,"not-attempted",0);
        }
    }
    /** Re-evaluate selected actual observations at full window and enumerate when bounded.
     * These nodes have just-capture/irreversible contexts; the captured child cannot repeat
     * a pre-capture position. No trace score is relabelled exact without enumeration. */
    static void witnesses(Path selected) throws Exception {
        try(var out=writer(Path.of("."),"WITNESS_VALUES")) {
            out.println("run,fixture,observation,observed_qply,path_ply,node_fen,move,alpha,beta,alpha_gap,observed_move_score,stand_pat,gain,immediate_child_value,immediate_eval_gain,material_change,piece_square_change,passed_pawn_change,other_eval_change,full_node_score,full_pv,oracle_status,oracle_nodes,exact_move_score,exact_node_best,relation_to_best,selected_line_cause,move_oracle_excess");
            for(String line:Files.readAllLines(selected)) {
                String[] s=line.split("\t");long[] b=Board.fromFen(s[5]);long m=QuiescenceDepthCorpus.move(b,s[6]);long[] c=ExhaustiveOracle.child(b,m);
                require(eligible(b,m),"Ineligible witness");
                int before=Eval.evaluate(b),after=-Eval.evaluate(c),gain=ExactSearch.tacticalMaterialValue(m,Board.enPassantSquare((int)b[Board.STATUS]));
                var a=Eval.breakdown(b);var d=Eval.breakdown(c);int side=Board.player((int)b[Board.STATUS]);
                var aw=side==0?a.white():a.black();var ab=side==0?a.black():a.white();
                var dw=side==0?d.white():d.black();var db=side==0?d.black():d.white();
                int material=dw.material()-db.material()-aw.material()+ab.material();
                int pst=dw.pieceSquare()-db.pieceSquare()-aw.pieceSquare()+ab.pieceSquare();
                int passed=dw.passedPawns()-db.passedPawns()-aw.passedPawns()+ab.passedPawns();
                var q=ExactSearch.quiescenceResearch(HCE);var r=q.search(b,GameHistory.initial(b),0,()->q.visitedNodes()>=BUDGET);
                int ply=Integer.parseInt(s[4]);var o=new QuiescenceOracle(HCE);List<String> evidence=new ArrayList<>();
                o.evidence=(board,p,qp,stand,best,moves,values,causes)->{
                    if(p!=ply)return;int count=stand==best?1:0;for(int v:values)if(v==best)count++;
                    for(int i=0;i<moves.length;i++)if(moves[i]==m){evidence.add(""+values[i]);evidence.add(""+best);
                        evidence.add(values[i]<best?"below":count==1?"uniquely-best":"equal-best");evidence.add(cause(causes[i]));evidence.add(""+(values[i]-stand-gain));}
                };
                String status="exact";
                try { int value=o.score(b,new SearchLineHistory(GameHistory.initial(b)),0,ply);
                    int normalized=r.score();if(normalized>=32512)normalized-=ply;else if(normalized<=-32512)normalized+=ply;
                    require(r.completed() && normalized==value,"Witness full-window oracle mismatch");
                } catch(AssertionError ex) {if(!"Unbounded oracle fixture".equals(ex.getMessage()))throw ex;status="guard-rejected";evidence.clear();}
                while(evidence.size()<5)evidence.add("");
                out.println(csv(s[0],s[1],s[2],s[3],s[4],s[5],s[6],s[7],s[8],s[9],s[10],before,gain,after,after-before,
                        material,pst,passed,after-before-material-pst-passed,r.completed()?r.score():"",QuiescenceSeeCorpus.pv(r),status,o.nodes,
                        evidence.get(0),evidence.get(1),evidence.get(2),evidence.get(3),evidence.get(4)));
            }
        }
    }
    static ExactSearchResult run(PrintWriter raw, PrintWriter runs, QuiescenceDeltaEvidence evidence,
            String run,String fixture,long[] b,String kind,int depth,int alpha,int beta,String oracle,long oracleNodes) {
        var plain=ExactSearch.quiescenceResearch(HCE);
        var q=QuiescenceDeltaEvidence.search(HCE,evidence);
        var game=GameHistory.initial(b);
        var reference=plain.searchWindow(b,game,depth,alpha,beta,()->plain.visitedNodes()>=BUDGET);
        var r=q.searchWindow(b,game,depth,alpha,beta,()->q.visitedNodes()>=BUDGET);
        same(reference,r);
        export(raw,run,fixture,evidence,r.completed());
        runs.println(csv(run,fixture,Fen.fromBoard(b),kind,depth,r.completedDepth(),r.completed(),r.completed()?r.score():"",
                r.hasMove()?Move.coordinate(r.bestMove()):"none",QuiescenceSeeCorpus.pv(r),r.normalNodes(),r.qnodes(),r.nodes(),r.maximumQply(),
                r.elapsedNanos()/1e6,r.nps(),evidence.size,"identical",oracle,oracleNodes));
        System.err.println(run+" nodes="+r.nodes()+" observations="+evidence.size+" "+oracle);
        return r;
    }
    static void same(ExactSearchResult a,ExactSearchResult b) {
        require(a.completed()==b.completed() && a.completedDepth()==b.completedDepth() && a.score()==b.score()
                && a.bestMove()==b.bestMove() && Arrays.equals(a.principalVariation(),b.principalVariation())
                && a.nodes()==b.nodes() && a.qnodes()==b.qnodes() && a.maximumQply()==b.maximumQply(),"Instrumentation changed Search");
    }
    static void export(PrintWriter out,String run,String fixture,QuiescenceDeltaEvidence e,boolean complete) {
        long[] b=new long[Board.MAX_BITBOARDS];
        for(int id=0;id<e.size;id++) {
            System.arraycopy(e.data,id*WIDTH+BOARD,b,0,b.length);
            long m=e.get(id,MOVE), previous=e.get(id,PREVIOUS);int flags=(int)e.get(id,FLAGS);
            boolean done=(flags&MOVE_DONE)!=0,nodeDone=(flags&NODE_DONE)!=0;
            long gain=e.get(id,GAIN),stand=e.get(id,STAND),alpha=e.get(id,ALPHA),beta=e.get(id,BETA),score=e.get(id,SCORE);
            boolean ep=ep(b,m);int captured=(int)(m>>>Board.TARGET_PIECE_SHIFT)&Piece.TYPE;
            out.println(csv(run,fixture,e.get(id,NODE),id,e.get(id,QPLY),e.get(id,PLY),Fen.fromBoard(b),Move.coordinate(m),
                    Piece.SHORT_STRING[ep?Piece.PAWN:captured],gain,e.get(id,SEE),ep,previous==0?"":Move.coordinate(previous),
                    previous!=0 && capture(previous) && Move.toSquare(previous)==Move.toSquare(m),alpha,beta,stand,stand+gain,alpha-stand-gain,
                    done?score:"",done?(score<=alpha?"UPPER":score>=beta?"LOWER":"EXACT"):"incomplete",
                    (flags&RAISED)!=0,(flags&CUTOFF)!=0,(flags&BEST_CHANGED)!=0,(flags&FINAL_BEST)!=0,done?cause((int)e.get(id,CAUSE)):"",
                    nodeDone?e.get(id,FINAL):"",done,nodeDone,complete));
        }
    }
    static void oracleRows(List<String> rows,String fixture,long[] b,int ply,int qply,int stand,int best,
            long[] moves,int[] scores,int[] causes) {
        int bestCount=stand==best?1:0;
        for(int s:scores) if(s==best) bestCount++;
        for(int i=0;i<moves.length;i++) {
            long m=moves[i];
            if(scores[i]==Integer.MIN_VALUE || !eligible(b,m)) continue;
            int gain=ExactSearch.tacticalMaterialValue(m,Board.enPassantSquare((int)b[Board.STATUS]));
            boolean unique=scores[i]==best && bestCount==1;
            rows.add(csv(fixture,Fen.fromBoard(b),qply,ply,Move.coordinate(m),gain,See.atLeastGeneratedLegal(b,m,0)?1:-1,
                    ep(b,m),stand,stand+gain,scores[i],best,scores[i]<best?"below":unique?"uniquely-best":"equal-best",cause(causes[i]),
                    unique && Math.abs(best)>=32512,unique && best==0 && causes[i]>=2,scores[i]-stand-gain));
        }
    }
    static boolean eligible(long[] b,long m) {
        return !QuiescenceOracle.inCheck(b) && QuiescenceOracle.tactical(b,m) && !QuiescenceOracle.promotion(m)
                && !QuiescenceOracle.inCheck(ExhaustiveOracle.child(b,m));
    }
    static boolean ep(long[] b,long m) {
        return ((m>>>Board.TARGET_PIECE_SHIFT)&Piece.TYPE)==0
                && ((m>>>Board.START_PIECE_SHIFT)&Piece.TYPE)==Piece.PAWN
                && Move.toSquare(m)==Board.enPassantSquare((int)b[Board.STATUS]);
    }
    static boolean capture(long m) {
        return ((m>>>Board.TARGET_PIECE_SHIFT)&Piece.TYPE)!=0
                || ((m>>>Board.START_PIECE_SHIFT)&Piece.TYPE)==Piece.PAWN
                    && (Move.fromSquare(m)&7)!=(Move.toSquare(m)&7); // Includes EP's empty packed victim.
    }
    static String cause(int c) { return new String[]{"static","mate","stalemate","rule50","repetition","insufficient"}[c]; }
    static PrintWriter writer(Path p,String suffix)throws IOException {
        if(suffix.equals("OBSERVATIONS")) return new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                new java.util.zip.GZIPOutputStream(Files.newOutputStream(p.resolve(PREFIX+"_"+suffix+".csv.gz"))),StandardCharsets.UTF_8)));
        return new PrintWriter(Files.newBufferedWriter(p.resolve(PREFIX+"_"+suffix+".csv"),StandardCharsets.UTF_8));
    }
    static String csv(Object... items) {
        StringJoiner s=new StringJoiner(",");for(Object x:items)s.add("\""+x.toString().replace("\"","\"\"")+"\"");return s.toString();
    }
    static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
