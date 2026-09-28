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
import static com.ohinteractive.seedv6.search.exact.QuiescenceDeltaCorpus.*;

/** H's same fixtures/windows/budget with child-static observations, exported outside recursion. */
public final class QuiescencePostMoveCorpus {
    static final String PREFIX="SEARCH_QUIESCENCE_POSTMOVE_SR001I_2026-09-28";
    static List<QuiescenceForcingCorpus.Fixture> fixtures() {
        var list=new ArrayList<>(QuiescenceDeltaCorpus.fixtures());
        list.add(new QuiescenceForcingCorpus.Fixture("capture-to-stalemate",
                "k7/8/1p2Q3/4K3/8/8/8/8 w - - 0 1","immediate-stalemate",true,true));
        return list;
    }
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args.length==0?".":args[0]);
        try(var raw=writer(output,"OBSERVATIONS");var runs=writer(output,"RUNS");var exact=writer(output,"ORACLE")) {
            raw.println("run,fixture,node,observation,qply,path_ply,node_fen,move,captured_piece,gain,see_sign,en_passant,previous_qmove,same_target_recapture,alpha,beta,stand_pat,raw_delta_bound,alpha_gap,move_score,move_bound,raised_alpha,beta_cutoff,best_changed,final_best_move,selected_line_cause,node_final_score,move_completed,node_completed,invocation_completed,child_kind,primary_eligible,post_move_static,static_gap,continuation_gain,child_nodes,child_stand_pat_cutoff");
            runs.println("run,fixture,fen,kind,depth,completed_depth,completed,score,best,pv,normal_nodes,qnodes,total_nodes,max_qply,diagnostic_ms,diagnostic_nps,observations,primary_observations,terminal_children,identity,oracle,oracle_nodes");
            exact.println("fixture,node_fen,qply,path_ply,move,gain,see_sign,en_passant,stand_pat,raw_delta_bound,move_exact_score,node_exact_best,relation_to_best,selected_line_cause,unique_mate,unique_draw,child_kind,primary_eligible,post_move_static,continuation_gain");
            var evidence=new QuiescencePostMoveEvidence(300_000);
            for(var f:fixtures()) {
                long[] b=Board.fromFen(f.fen());var oracle=new QuiescenceOracle(HCE);List<String> rows=new ArrayList<>();
                oracle.staticEvidence=(board,ply,qply,stand,best,moves,values,causes,post,kinds)->
                    oracleRows(rows,f.name(),board,ply,qply,stand,best,moves,values,causes,post,kinds);
                String status="matched";int value=0;
                try{value=oracle.score(b,new SearchLineHistory(GameHistory.initial(b)),0,0);}
                catch(AssertionError ex){if(!"Unbounded oracle fixture".equals(ex.getMessage()))throw ex;status="guard-rejected-not-exact";rows.clear();}
                var full=run(raw,runs,evidence,"corpus:"+f.name()+":full",f.name(),b,"corpus-full",0,-32769,32769,status,oracle.nodes);
                if(status.equals("matched")){require(full.completed()&&full.score()==value,"Oracle mismatch "+f.name());rows.forEach(exact::println);}
                if(full.completed())for(int i=0;i<2;i++){
                    String side=i==0?"lower":"upper";int alpha=full.score()+(i==0?-1:0);
                    run(raw,runs,evidence,"corpus:"+f.name()+":"+side,f.name(),b,"corpus-"+side,0,alpha,alpha+1,"not-repeated",0);
                }
            }
            var positions=new ArrayList<>(ExactSearchHarness.orderingPositions());
            positions.add(ExactSearchHarness.positions().stream().filter(p->p.name().equals("endgame")).findFirst().orElseThrow());
            for(int depth:new int[]{2,3})for(var p:positions)
                run(raw,runs,evidence,"benchmark:"+p.name()+":"+depth,p.name(),Board.fromFen(p.fen()),"benchmark",depth,-32769,32769,"not-attempted",0);
        }
    }
    static ExactSearchResult run(PrintWriter raw,PrintWriter runs,QuiescencePostMoveEvidence evidence,
            String run,String fixture,long[] b,String kind,int depth,int alpha,int beta,String oracle,long oracleNodes) {
        var plain=ExactSearch.quiescenceResearch(HCE);var traced=QuiescencePostMoveEvidence.search(HCE,evidence);
        var game=GameHistory.initial(b);
        var a=plain.searchWindow(b,game,depth,alpha,beta,()->plain.visitedNodes()>=BUDGET);
        var r=traced.searchWindow(b,game,depth,alpha,beta,()->traced.visitedNodes()>=BUDGET);
        same(a,r);int primary=0,terminal=0;
        // Reuse H's exact row representation and stable observation ordering for alignment.
        StringWriter buffer=new StringWriter();PrintWriter h=new PrintWriter(buffer);
        QuiescenceDeltaCorpus.export(h,run,fixture,evidence.base,r.completed());h.flush();
        String[] lines=buffer.toString().split("\\R");
        for(int id=0;id<evidence.base.size;id++) {
            boolean eligible=evidence.childKind[id]==0;
            if(eligible)primary++;else if(evidence.childKind[id]>0)terminal++;
            long moveAlpha=evidence.base.get(id,ALPHA),score=evidence.base.get(id,SCORE);
            raw.println(lines[id]+","+csv(kind(evidence.childKind[id]),eligible,
                    eligible?evidence.postStatic[id]:"",eligible?moveAlpha-evidence.postStatic[id]:"",eligible?score-evidence.postStatic[id]:"",
                    evidence.childKind[id]>=0?evidence.childNodes[id]:"",evidence.childStandPatCutoff[id]));
            if(eligible){
                require(score<=evidence.postStatic[id],"Move exceeded child stand-pat bound");
                if(evidence.postStatic[id]<=moveAlpha) require(score==evidence.postStatic[id]&&evidence.childNodes[id]==1&&evidence.childStandPatCutoff[id],"Existing stand-pat cutoff identity failed");
            }
        }
        runs.println(csv(run,fixture,Fen.fromBoard(b),kind,depth,r.completedDepth(),r.completed(),r.completed()?r.score():"",
                r.hasMove()?Move.coordinate(r.bestMove()):"none",QuiescenceSeeCorpus.pv(r),r.normalNodes(),r.qnodes(),r.nodes(),r.maximumQply(),
                r.elapsedNanos()/1e6,r.nps(),evidence.base.size,primary,terminal,"identical",oracle,oracleNodes));
        System.err.println(run+" nodes="+r.nodes()+" primary="+primary+" terminals="+terminal+" "+oracle);
        return r;
    }
    static void oracleRows(List<String> rows,String fixture,long[] b,int ply,int qply,int stand,int best,
            long[] moves,int[] values,int[] causes,int[] post,int[] kinds) {
        int bestCount=stand==best?1:0;for(int v:values)if(v==best)bestCount++;
        for(int i=0;i<moves.length;i++){
            long m=moves[i];if(values[i]==Integer.MIN_VALUE||!eligible(b,m))continue;
            boolean primary=kinds[i]==0,unique=values[i]==best&&bestCount==1;
            int gain=ExactSearch.tacticalMaterialValue(m,Board.enPassantSquare((int)b[Board.STATUS]));
            if(primary)require(values[i]<=post[i],"Exact move exceeded child stand-pat bound");
            rows.add(csv(fixture,Fen.fromBoard(b),qply,ply,Move.coordinate(m),gain,See.atLeastGeneratedLegal(b,m,0)?1:-1,
                    ep(b,m),stand,stand+gain,values[i],best,values[i]<best?"below":unique?"uniquely-best":"equal-best",cause(causes[i]),
                    unique&&Math.abs(best)>=32512,unique&&best==0&&causes[i]>=2,kind(kinds[i]),primary,primary?post[i]:"",primary?values[i]-post[i]:""));
        }
    }
    static String kind(int kind){return kind<0?"incomplete-unresolved":kind==0?"nonterminal":kind==6?"checked":cause(kind);}
    static PrintWriter writer(Path p,String suffix)throws IOException {
        if(suffix.equals("OBSERVATIONS"))return new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                new java.util.zip.GZIPOutputStream(Files.newOutputStream(p.resolve(PREFIX+"_"+suffix+".csv.gz"))),StandardCharsets.UTF_8)));
        return new PrintWriter(Files.newBufferedWriter(p.resolve(PREFIX+"_"+suffix+".csv"),StandardCharsets.UTF_8));
    }
}
