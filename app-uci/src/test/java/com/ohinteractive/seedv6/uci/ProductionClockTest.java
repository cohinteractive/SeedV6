package com.ohinteractive.seedv6.uci;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.manage.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class ProductionClockTest {
    private static List<UciConfiguration> profiles() throws Exception {
        return List.of(BaselineUciConfiguration.load(),
                ProductionUciConfiguration.load(Path.of("../build-assets/production/bre-pair2-g32.brn-pair2")),
                ConventionalUciConfiguration.load(Path.of("../build-assets/nnue-candidate/conventional-cp64.cnnue")));
    }
    @Test void allNormalProfilesCoupleAllocatorAndOwnerAtEveryWorkerCount() throws Exception {
        for (var original : profiles()) for (var policy : ClockPolicy.values()) {
            var profile = original.withClockPolicy(policy);
            assertEquals(ClockPolicy.BOUNDARY2, original.clockPolicy());
            assertEquals(policy, profile.clockPolicy());
            assertTrue(profile.identity(1).contains("clockPolicy=" + policy.identity()));
            assertEquals(policy == ClockPolicy.FACTUAL ? 330 : 660, profile.clockBudgetMillis(7959, 100, 30));
            for (int workers = 1; workers <= 16; workers++) try (var service = profile.service(workers, 1)) {
                assertTrue(profile.acceptsWorkers(workers));
                var owner = field(field(service, "search"), "exact");
                assertEquals(policy == ClockPolicy.BOUNDARY2, owner instanceof Boundary2ClockPolicy);
            }
        }
    }
    @Test void parserSelectsActualSideIncrementAndPreservesExplicitLimits() throws Exception {
        int white = (int)Board.startingPosition()[Board.STATUS];
        for (var original : profiles()) for (var policy : ClockPolicy.values()) {
            var profile = original.withClockPolicy(policy);
            for (long remaining : new long[]{0, 49, 50, 51, 150, 7959, Long.MAX_VALUE}) {
                var request = GoCommandParser.parse(("go wtime " + remaining + " btime 3000 winc 100 binc 400 movestogo 10").split(" "), white, profile::clockBudgetMillis);
                assertTrue(request.clockManaged());
                assertEquals(policy.allocateClockMillis(remaining, 100, 10), request.timeMillis());
                var black = GoCommandParser.parse("go wtime 7959 btime 3000 winc 100 binc 400 movestogo 10".split(" "), white | Board.PLAYER_BIT, profile::clockBudgetMillis);
                assertEquals(policy.allocateClockMillis(3000, 400, 10), black.timeMillis());
            }
            for (String command : List.of("go depth 3", "go nodes 100", "go movetime 100", "go infinite", "go wtime 1 movetime 100")) {
                var actual = GoCommandParser.parse(command.split(" "), white, profile::clockBudgetMillis);
                assertEquals(GoCommandParser.parse(command.split(" "), white), actual);
                assertFalse(actual.clockManaged());
            }
            var mixed = GoCommandParser.parse("go wtime 7959 winc 100 depth 4 nodes 100".split(" "), white, profile::clockBudgetMillis);
            assertEquals(4, mixed.depth()); assertEquals(100, mixed.nodes()); assertTrue(mixed.clockManaged());
        }
    }
    @Test void protocolOptionsReconfigureIndependentlyAndRejectInvalidSelectionAtomically() throws Exception {
        for (var original : profiles()) {
            var output = new ByteArrayOutputStream(); var errors = new ByteArrayOutputStream();
            String commands = "uci\nsetoption name Hash value 1\nsetoption name Threads value 4\n"
                    + "setoption name ClockPolicy value FACTUAL\nuci\nsetoption name ClockPolicy value typo\nuci\n"
                    + "setoption name Threads value 16\nsetoption name ClockPolicy value BOUNDARY2\nuci\n"
                    + "setoption name Threads value 1\nsetoption name ClockPolicy value FACTUAL\nuci\nquit\n";
            var engine = new UciEngine(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)), output, errors, original);
            engine.run();
            assertTrue(((SearchLifecycleService)field(engine, "searches")).isTerminated());
            assertEquals(ClockPolicy.BOUNDARY2, original.clockPolicy());
            String transcript = output.toString(StandardCharsets.UTF_8);
            assertEquals(5, transcript.lines().filter(x -> x.equals("uciok")).count());
            assertEquals(3, transcript.lines().filter(x -> x.startsWith("option name ClockPolicy type combo default FACTUAL ")).count());
            assertEquals(2, transcript.lines().filter(x -> x.startsWith("option name ClockPolicy type combo default BOUNDARY2 ")).count());
            assertTrue(transcript.contains("clockPolicy=factual"));
            assertTrue(transcript.contains("clockPolicy=boundary2-d6-v1"));
            assertTrue(errors.toString(StandardCharsets.UTF_8).contains("ClockPolicy must be BOUNDARY2 or FACTUAL"));
        }
    }
    @Test void explicitHistoricalAndResearchEntrypointsDoNotInheritProductionPolicy() throws Exception {
        assertNull(ConstructionUciMain.configuration(new String[0]).clockPolicy());
        assertNull(NnueCandidateUciMain.configuration(new String[0]).clockPolicy());
        assertEquals(330, ConstructionUciMain.configuration(new String[0]).clockBudgetMillis(7959, 100, 30));
    }
    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
}
