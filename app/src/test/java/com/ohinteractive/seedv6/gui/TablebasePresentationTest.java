package com.ohinteractive.seedv6.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.*;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.search.tablebase.TablebaseWin;
import java.awt.*;
import javax.swing.*;

class TablebasePresentationTest {
    @Test void engineCardLabelsOutcomeWithoutEvaluationUnitsOrASearchedPv() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SeedTheme.initialize();
            var card=new EngineCard();
            var view=new GameController.SearchInfo("Tablebase win",0,"tablebase win",0,-1,"b1g1","TABLEBASE",-1,Value.WHITE);
            var bindings=java.util.List.of(PlayEvaluator.handcrafted(),new PlayEvaluator(PlayEvaluator.Mode.BEST_NNUE,"test","test",com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.handcrafted()));
            for(var binding:bindings){
                card.showSearch(view,binding,1);String labels=labels(card);
                assertTrue(labels.contains("Tablebase"));assertTrue(labels.contains("Winning move"));assertTrue(labels.contains("White wins"));
                assertFalse(labels.contains("Centipawns"));assertFalse(labels.contains("uncalibrated"));assertFalse(labels.contains("Principal variation"));
                card.setSize(560,260);layout(card);assertScoreFits(card);
            }
            card.showSearch(new GameController.SearchInfo("Thinking",3,"cp 120",999,1000,"b1g1","NONE"),PlayEvaluator.handcrafted(),1);
            assertTrue(labels(card).contains("Principal variation"));assertFalse(labels(card).contains("Tablebase"));
        });
    }
    private static void layout(Container parent){parent.doLayout();for(Component c:parent.getComponents())if(c instanceof Container child)layout(child);}
    private static void assertScoreFits(Container parent){for(Component c:parent.getComponents()){
        if(c instanceof JLabel label&&("engineScore".equals(label.getName())||label.getText().equals("Tablebase")))
            assertTrue(label.getPreferredSize().width<=label.getWidth(),label.getText());
        if(c instanceof Container child)assertScoreFits(child);
    }}
    private static String labels(Container parent){
        StringBuilder out=new StringBuilder();for(Component c:parent.getComponents()){
            if(c instanceof JLabel label)out.append(label.getText()).append(' ');
            if(c instanceof Container container)out.append(labels(container));
        }return out.toString();
    }
    @Test void winningOutcomeHasNoInventedCpMateDistanceOrDepthForEitherEvaluator() {
        long[] b=Board.fromFen("7k/8/8/8/8/8/8/KQ6 w - - 0 1");
        long move=new LegalMoveResolver().resolve(b,new MoveIntent(1,6,MoveIntent.Promotion.NONE));
        var result=new ManagedSearchResult(1,move,true,null,SearchTermination.TABLEBASE,0,null,
                SearchDiagnosticsSnapshot.disabled(),new TablebaseWin(move,13));
        var old=new GameController.SearchInfo("Thinking",7,"cp 800",999,555,"a1a2","NONE");
        for(int side:new int[]{Value.WHITE,Value.BLACK}) {
            var view=GameController.SearchInfo.fromFinal(result,old,side);
            assertEquals("Tablebase win",view.state());assertEquals("tablebase win",view.score());
            assertEquals(0,view.depth());assertEquals("b1g1",view.pv());assertEquals(-1,view.nps());
            for(boolean neural:new boolean[]{false,true}) {
                var display=PlayScore.from(view,neural);assertTrue(display.available());
                assertEquals(side==Value.WHITE?"White wins":"Black wins",display.text());
                assertEquals(side==Value.WHITE?1.0:0.0,display.whiteFraction());
            }
        }
    }
}
