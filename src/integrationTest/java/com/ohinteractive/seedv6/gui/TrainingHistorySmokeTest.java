package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.history.*;
import static com.ohinteractive.seedv6.training.history.HistoryFixtures.*;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class TrainingHistorySmokeTest {
    @TempDir Path temp;
    private ChessFrame frame;
    @AfterEach void close() throws Exception {
        if(frame!=null) { edt(()->frame.dispatchEvent(new WindowEvent(frame,WindowEvent.WINDOW_CLOSING)));until(()->!edt(frame::isDisplayable)); }
    }
    @Test void persistedFixturesPopulateRangesDashboardRegimesLineageAndResize() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var repository=new HistoryRepository(temp);Instant end=Instant.now();
        String incumbent="g000000-s000000000-"+"b".repeat(64);
        for(int g=1;g<=120;g++) {
            var row=record(g,g%17==0,g<=90?4:5,end.minusSeconds((120-g)*3600L),(1200L+g*8)*1_000_000_000,incumbent);
            repository.append(row);incumbent=row.resultingBest();
        }
        frame=edt(()->new ChessFrame(TrainingDashboardTest.settings(temp),new TrainingController.Backend(),ignored->{}));
        if("150%".equals(System.getProperty("flatlaf.uiScale"))) assertEquals(150,SeedTheme.scale(100));
        edt(()->{frame.setSize(SeedTheme.scale(1586),SeedTheme.scale(992));frame.setVisible(true);named(frame,"workspaces",JTabbedPane.class).setSelectedIndex(1);});
        until(()->edt(()->named(frame,"recentTrainingHistory",JTable.class).getRowCount()==5));
        edt(()->{
            capture("history-dashboard-reference.png");
            JScrollPane dashboard=named(frame,"trainingDashboardScroll",JScrollPane.class);
            dashboard.getVerticalScrollBar().setValue(10000);frame.validate();
            assertTrue(named(frame,"recentTrainingHistory",JTable.class).getVisibleRect().height>0);
            capture("history-dashboard-previews.png");dashboard.getVerticalScrollBar().setValue(0);
            named(frame,"trainingViews",JTabbedPane.class).setSelectedIndex(1);frame.validate();
            var table=named(frame,"trainingHistory",JTable.class);var range=named(frame,"historyRange",JComboBox.class);
            assertEquals(25,table.getRowCount());assertEquals(120L,table.getValueAt(0,0));
            range.setSelectedItem(HistoryAnalytics.Range.LAST_50);assertEquals(50,table.getRowCount());
            table.setRowSelectionInterval(1,1);capture("history-reference.png");
            assertEquals(3,named(frame,"historyBestLineage",JTable.class).getRowCount());
            assertTrue(named(frame,"historyDetails",JTextArea.class).getText().contains("Incumbent at validation start"));
            range.setSelectedItem(HistoryAnalytics.Range.LAST_100);assertEquals(100,table.getRowCount());
            range.setSelectedItem(HistoryAnalytics.Range.ALL);assertEquals(120,table.getRowCount());
            range.setSelectedItem(HistoryAnalytics.Range.TODAY);assertTrue(table.getRowCount()>0);
            range.setSelectedItem(HistoryAnalytics.Range.LAST_50);
            table.setRowSelectionInterval(1,1);
            JScrollPane historyScroll=(JScrollPane)named(frame,"trainingViews",JTabbedPane.class).getComponentAt(1);
            historyScroll.getVerticalScrollBar().setValue(10000);frame.validate();capture("history-regimes-lineage-details.png");
            historyScroll.getVerticalScrollBar().setValue(0);
            for(Dimension size:new Dimension[]{new Dimension(1100,760),new Dimension(1920,1080)}) {
                frame.setSize(SeedTheme.scale(size.width),SeedTheme.scale(size.height));frame.validate();
                assertTrue(range.isShowing());assertTrue(range.getWidth()>=range.getPreferredSize().width);
                assertTrue(named(frame,"historySummary",JTextArea.class).getHeight()>=SeedTheme.scale(30),"Summary must retain its height on resize");
                capture("history-"+size.width+"x"+size.height+".png");
            }
            for(int view:new int[]{2,3}) {named(frame,"trainingViews",JTabbedPane.class).setSelectedIndex(view);frame.validate();capture("history-existing-view-"+view+".png");}
            assertTrue(named(frame,"trainingProgress",JTextArea.class).getText().contains(HistoryRepository.FILE.replace('/','\\'))
                    ||named(frame,"trainingProgress",JTextArea.class).getText().contains("generations-v1.tsv"));
            named(frame,"workspaces",JTabbedPane.class).setSelectedIndex(0);frame.validate();assertTrue(named(frame,"chessBoard",BoardPanel.class).isShowing());capture("history-play-regression.png");
        });
    }
    @Test void largeHistoryPreparationAndPaintAreBoundedAndEmptyRenders() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var records=new ArrayList<GenerationRecord>();
        for(int g=1;g<=100_000;g++) records.add(record(g,g%1000==0,g<50000?4:5,Instant.EPOCH.plusSeconds(g*60L),60_000_000_000L));
        edt(()->{
            SeedTheme.initialize();var history=new TrainingHistory();history.setSize(900,900);
            history.showHistory(HistoryRepository.Snapshot.EMPTY,"");
            history.showHistory(new HistoryRepository.Snapshot(java.util.List.of(),java.util.List.of("Damaged history line 3")),"History write failure");
            assertTrue(named(history,"historyWarnings",JTextArea.class).getText().contains("Damaged history line 3"));
            assertTrue(named(history,"historyWarnings",JTextArea.class).getText().contains("History write failure"));
            named(history,"historyRange",JComboBox.class).setSelectedItem(HistoryAnalytics.Range.ALL);
            long start=System.nanoTime();history.showHistory(new HistoryRepository.Snapshot(records,java.util.List.of()),"");
            long prepare=System.nanoTime()-start;
            var chart=new HistoryChart(true,170);chart.setSize(800,170);chart.showRecords(records);
            assertTrue(chart.getToolTipText(new java.awt.event.MouseEvent(chart,java.awt.event.MouseEvent.MOUSE_MOVED,0,0,300,80,0,false)).contains("min/max bucket"));
            var image=new BufferedImage(800,170,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();start=System.nanoTime();
            for(int i=0;i<100;i++) chart.paint(g);long paint=System.nanoTime()-start;g.dispose();
            System.out.println("PHASE3_LARGE_HISTORY records=100000 prepareMillis="+prepare/1e6+" paintMeanMillis="+paint/1e8+" maxChartBuckets=600");
            assertEquals(100000,named(history,"trainingHistory",JTable.class).getRowCount());
        });
    }
    private void capture(String file) {
        try {
            frame.validate();Path directory=Path.of("build","gui-smoke",System.getProperty("flatlaf.uiScale","100%"));Files.createDirectories(directory);
            BufferedImage image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();frame.paintAll(g);g.dispose();ImageIO.write(image,"png",directory.resolve(file).toFile());
        } catch(Exception failure) {throw new AssertionError(failure);}
    }
}
