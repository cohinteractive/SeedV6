import java.nio.file.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;

/** Bounded headless Arena binding/lifecycle fixtures on actual packaged GUI JARs.
 * Four plies at depth four, never a timed competition or strength measurement. */
public final class PackagedArenaProbe {
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[0]);Files.createDirectory(output);
        int games=0,snapshots=0;
        String property=BaselineEngine.SearchProfile.PROPERTY,previous=System.getProperty(property);
        try {
            for(String profile:new String[]{"CLV_C1","MW_C4"}) for(int workers:new int[]{1,2,4,8,16}) {
                if(profile.equals("CLV_C1")) System.clearProperty(property);
                else System.setProperty(property,profile);
                var a=Competitor.competitive(BaselineEngine.Evaluator.CONVENTIONAL_CP64);
                var config=LearningArenaConfig.match("Production binding fixture",a,a,19,
                        new Arena(2,Limit.DEPTH,4,1000,workers,0,0,4,Board.FEN_STARTING_POSITION));
                Path path=output.resolve(profile+"-w"+workers);
                var observed=new ConcurrentLinkedQueue<ActiveGameSnapshot>();
                try(var service=LearningArenaService.create(path,config,update->{
                    if(update.liveGame()!=null && update.liveGame().evaluation()!=null) observed.add(update.liveGame());
                })) {
                    service.run();require(service.state().status()==LearningArenaState.Status.COMPLETE,"completion");
                    require(service.state().current().pairs().size()==1,"one bounded pair");
                    require(!observed.isEmpty(),"actual move telemetry");
                    for(var game:observed) for(var actor:java.util.List.of(game.white(),game.black())) {
                        String id=actor.searchProfile();
                        require(id.contains("searchProfile="+profile+" "),"profile");
                        require(id.contains("flags="+(profile.equals("CLV_C1")?3329490943L:1182007295L)+" "),"flags");
                        require(id.contains("parallel="+(workers==1?"OWNER":"EAGER_ROOT")),"schedule");
                        require(id.contains("clockPolicy=boundary2-d6-v1"),"clock");
                        require(id.contains("workers="+workers+";hashMiB=64;aspiration=ADAPTIVE16"),"resources");
                        require(id.contains("scoreMapping=BT4_Q_LOGIT400_V1"),"evaluator mapping");
                    }
                    snapshots+=observed.size();games+=2;
                }
                try(var resumed=LearningArenaService.resume(path,ignored->{})) {
                    resumed.run();require(resumed.state().status()==LearningArenaState.Status.COMPLETE,"resume");
                }
                require(!Files.exists(path.resolve("A/checkpoints")),"no training store");
                System.out.println("PASS Arena "+profile+" W"+workers+" depth4 four-ply pair, complete/resume/close");
            }
        } finally {
            if(previous==null) System.clearProperty(property);else System.setProperty(property,previous);
        }
        require(Thread.getAllStackTraces().keySet().stream().noneMatch(t->t.isAlive()
                && t.getName().equals("seedv6-search-helper")),"helper drain");
        System.out.println("PASS games="+games+" telemetry="+snapshots+"; technical fixtures only");
    }
    private static void require(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
