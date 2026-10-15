package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Actual fixed-model cumulative policy execution, including every helper.
 * Observation-only sinks; these instrumented requests are not cost evidence. */
public final class Sr021Workers {
    static Object get(Object owner,String name)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    static void set(Object owner,String name,Object value)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);
    }
    public static void main(String[] args)throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("network");
        var evaluation=snapshot.model().evaluation(NnueScoreMapping.V1);
        var board=Board.fromFen(SeePruningTest.MIDDLE);var history=GameHistory.initial(board);
        var gson=new GsonBuilder().create();var policy=FutilityPolicy.valueOf(args[2]);
        try(var out=Files.newBufferedWriter(Path.of(args[1]),StandardOpenOption.CREATE_NEW)) {
            for(int pass=0;pass<2;pass++)for(int count=1;count<=16;count++) {
                if(pass==1&&count!=4&&count!=16)continue;
                SingleDepthSearch facility=count==1?ExactSearchAdapter.futilityResearch(evaluation,policy,null)
                        :new ParallelSearch(count,evaluation,new TTable(1),ConcurrencyPolicy.ROOT_REUSE)
                        .withSeeResearch(SeePolicy.QXRAY,null).withFutilityResearch(policy,null);
                try(facility;var driver=new SearchDriver(facility)) {
                    var searches=new Object[count];
                    if(count==1)searches[0]=get(facility,"exact");
                    else {var workers=(Object[])get(facility,"workers");for(int i=0;i<count;i++)searches[i]=get(workers[i],"search");}
                    var futility=new FutilityPolicy.Diagnostics[count];var see=new SeePolicy.Diagnostics[count];
                    var lmr=new long[count];var lmp=new long[count];
                    var privateHistories=Collections.newSetFromMap(new IdentityHashMap<>());
                    for(int i=0;i<count;i++) {
                        final int worker=i;var search=searches[i];
                        if(get(search,"futilityPolicy")!=policy||get(search,"seePolicy")!=SeePolicy.QXRAY
                                ||!get(search,"lmrPolicy").equals(ExactSearch.LMR_QUIET)
                                ||!get(search,"lmpPolicy").equals(ExactSearch.LMP_WINDOW)
                                ||get(search,"iterationReuse")==null||!privateHistories.add(get(search,"quietHistory")))
                            throw new AssertionError("composition/private history");
                        set(search,"futilityDiagnostics",futility[i]=new FutilityPolicy.Diagnostics());
                        set(search,"seeDiagnostics",see[i]=new SeePolicy.Diagnostics());
                        set(search,"lmrDiagnostics",new LmrDiagnostics(){
                            @Override public void probe(long[] child,int ply,int depth,int reduction,int ordinal,int alpha,int beta,int score,boolean retry){lmr[worker]++;}
                        });
                        set(search,"lmpDiagnostics",new LmpDiagnostics(){
                            @Override public void omitted(long[] parent,long[] child,long move,int ply,int depth,int searched,int quiets,int alpha,int beta){lmp[worker]++;}
                        });
                    }
                    var control=SearchControl.controlled(pass==0?500_000:2_000_000,0,-1,TimeSource.SYSTEM);
                    var result=driver.search(new SearchRequest(board,history,pass==0?7:9,SearchObserver.NONE,control,true));
                    if(result.lastCompletedResult()==null||driver.activeSearchThreads()!=0||result.nodes()!=control.nodes()
                            ||result.nodes()!=result.diagnostics().totalEnteredNodes())throw new AssertionError("completion/accounting/drain");
                    Phase2QuiescenceTest.legalPv(board,result.lastCompletedResult().principalVariation());
                    long[] omissions=Arrays.stream(futility).mapToLong(d->d.omissions).toArray();
                    long[] qOmissions=Arrays.stream(see).mapToLong(d->d.qOmitted).toArray();
                    if(omissions[0]==0||qOmissions[0]==0)throw new AssertionError("owner execution");
                    if(count>1&&Arrays.stream(omissions).skip(1).sum()==0)throw new AssertionError("helper execution");
                    if(pass==1)for(int i=0;i<count;i++)if(omissions[i]==0||qOmissions[i]==0||lmr[i]==0||lmp[i]==0)
                        throw new AssertionError("deep cumulative execution worker "+i+"/"+count);
                    driver.newGame();if(!driver.search(new SearchRequest(board,history,2)).targetDepthCompleted()
                            ||driver.activeSearchThreads()!=0)throw new AssertionError("reuse");
                    out.write(gson.toJson(Map.of("status","passed","policy",policy,"workers",count,"deep",pass==1,
                            "nodes",result.nodes(),"futility",omissions,"qxray",qOmissions,"lmr",lmr,"lmp",lmp)));
                    out.newLine();out.flush();
                }
            }
        }
    }
}
