package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import com.ohinteractive.seedv6.training.history.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboard.*;

/** Historical presentation updates only on immutable history identity, range, or local day changes. */
final class TrainingHistory extends JPanel implements Scrollable {
    private final JComboBox<HistoryAnalytics.Range> range=new JComboBox<>(HistoryAnalytics.Range.values());
    private final JTextArea summary=text("",13,SeedTheme.TEXT), coverage=text("",11,SeedTheme.SECONDARY);
    private final JTextArea warning=text("",11,SeedTheme.WARNING), details=text("Select a generation for full identities and measurements.",12,SeedTheme.SECONDARY);
    private final HistoryChart scores=new HistoryChart(true,145), durations=new HistoryChart(false,145);
    private final Records tableModel=new Records();
    private final JTable table=table(tableModel,"trainingHistory");
    private final AbstractTableModel regimes=new AbstractTableModel() {
        public int getRowCount() { return selection==null?0:selection.regimes().size(); }
        public int getColumnCount() { return 2; }
        public String getColumnName(int c) { return c==0?"Gen":"Configuration regime (context, not causation)"; }
        public Object getValueAt(int r,int c) { var s=selection.regimes().get(r); return c==0?s.first()+"–"+s.last():s.regime(); }
    };
    private final AbstractTableModel lineage=new AbstractTableModel() {
        public int getRowCount() { return promoted.size(); }
        public int getColumnCount() { return 3; }
        public String getColumnName(int c) { return new String[]{"Gen","Became Best","Completed (local)"}[c]; }
        public Object getValueAt(int r,int c) { var p=promoted.get(promoted.size()-1-r); return c==0?p.generation():c==1?TrainingDashboardModel.network(p.resultingBest()):timestamp(p.completed()); }
    };
    private List<GenerationRecord> promoted=List.of();
    private HistoryRepository.Snapshot source=HistoryRepository.Snapshot.EMPTY;
    private HistoryAnalytics.Selection selection;
    private LocalDate day;
    private String lastWarning="";
    TrainingHistory() {
        super(new GridBagLayout()); setOpaque(false); setName("trainingHistoryWorkspace");
        range.setName("historyRange");
        JPanel header=padded(new BorderLayout(12,6),12);
        header.add(label("Generation history",20,SeedTheme.TEXT)); header.add(range,BorderLayout.EAST);
        JTextArea note=text("Each Candidate faced its recorded incumbent Best; scores are not absolute strength. Green diamond = promotion; dashed line = new regime; × = unavailable. Large ranges show min/max buckets.",12,SeedTheme.SECONDARY);
        note.setRows(3); header.add(note,BorderLayout.SOUTH); addRow(header,0);
        summary.setRows(2); coverage.setRows(2); summary.setName("historySummary");
        JPanel metrics=padded(new BorderLayout(0,7),12); metrics.add(summary); metrics.add(coverage,BorderLayout.SOUTH); addRow(card("Selected range",null,metrics),1);
        JPanel charts=panel(new GridLayout(1,2,SeedTheme.scale(10),0));
        charts.add(card("Candidate score vs incumbent Best",null,scores)); charts.add(card("Generation duration · active processing",null,durations)); addRow(charts,2);
        JScrollPane rows=scroll(table); rows.setPreferredSize(new Dimension(1,SeedTheme.scale(150))); addRow(card("Completed generations · latest first",null,rows),3);
        details.setRows(6); details.setName("historyDetails"); addRow(card("Selected generation",null,paddedDetails()),5);
        JPanel context=panel(new GridLayout(1,2,SeedTheme.scale(10),0));
        JTable regimeTable=new JTable(regimes); regimeTable.setRowHeight(SeedTheme.scale(25)); regimeTable.getColumnModel().getColumn(0).setPreferredWidth(80); regimeTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        JScrollPane regimeScroll=scroll(regimeTable); regimeScroll.setPreferredSize(new Dimension(1,SeedTheme.scale(95)));
        JTable bestTable=new JTable(lineage); bestTable.setName("historyBestLineage"); bestTable.setRowHeight(SeedTheme.scale(25));
        int[] bestWidths={45,100,210};
        for(int c=0;c<bestWidths.length;c++) bestTable.getColumnModel().getColumn(c).setPreferredWidth(SeedTheme.scale(bestWidths[c]));
        for(JTable contextTable:java.util.List.of(regimeTable,bestTable)) contextTable.setDefaultRenderer(Object.class,new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int column) {
                super.getTableCellRendererComponent(table,value,selected,focus,row,column);
                setToolTipText(optional(value));return this;
            }
        });
        JScrollPane bestScroll=scroll(bestTable); bestScroll.setPreferredSize(new Dimension(1,SeedTheme.scale(95)));
        context.add(card("Configuration regimes",null,regimeScroll)); context.add(card("Best lineage · identity, not strength",null,bestScroll)); addRow(context,4);
        warning.setName("historyWarnings"); warning.setRows(2); addRow(warning,6);
        range.addActionListener(e->rebuild());
        table.getSelectionModel().addListSelectionListener(e->{ if(!e.getValueIsAdjusting()) showDetails(); });
        rebuild();
    }
    private JPanel paddedDetails() {
        JPanel p=padded(new BorderLayout(),10); JScrollPane content=scroll(details);
        content.setPreferredSize(new Dimension(1,SeedTheme.scale(130))); p.add(content); return p;
    }
    private void addRow(JComponent component,int row) {
        GridBagConstraints c=new GridBagConstraints(); c.gridx=0;c.gridy=row;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;
        c.insets=new Insets(0,0,SeedTheme.scale(10),0); component.setMinimumSize(new Dimension(0,component.getPreferredSize().height)); add(component,c);
    }
    void showHistory(HistoryRepository.Snapshot history,String error) {
        LocalDate now=LocalDate.now();
        if(source!=history || !now.equals(day)) { source=history; day=now; rebuild(); }
        String message=String.join("\n",history.warnings())+(error.isBlank()?"":"\n"+error);
        if(!message.equals(lastWarning)) { lastWarning=message; warning.setText(message); warning.setToolTipText(message); }
    }
    private void rebuild() {
        selection=HistoryAnalytics.select(source.records(),(HistoryAnalytics.Range)range.getSelectedItem(),Clock.systemDefaultZone());
        var s=selection.summary();
        summary.setText(s.generations()+" generations   ·   "+s.promotions()+" promotions   ·   "+percent(s.promotionFrequency())+" promoted   ·   "+duration(s.averageSeconds())+" avg   ·   "+number(s.generationsPerHour())+" gen/h");
        coverage.setText(number(s.generationsPerPromotion())+" generations / promotion   ·   "+duration(s.secondsPerPromotion())+" active time / promotion   ·   Duration coverage "+s.timedGenerations()+" / "+s.generations());
        summary.setToolTipText("Promotion frequency = promotions / completed generations; avg duration = recorded total / timed generations; gen/h = timed generations × 3600 / recorded seconds.");
        coverage.setToolTipText("Generations/promotion = all selected completions / promotions. Time/promotion = recorded active duration / promotions; partial coverage is shown. No promotions = unavailable.");
        tableModel.show(selection.records()); scores.showRecords(selection.records()); durations.showRecords(selection.records());
        promoted=selection.records().stream().filter(GenerationRecord::promoted).toList(); regimes.fireTableDataChanged(); lineage.fireTableDataChanged();
        showDetails();
    }
    private void showDetails() {
        int row=table.getSelectedRow();
        if(row<0) {
            details.setText(source.records().isEmpty()
                    ? "No completed generation history yet. History begins with this feature; earlier checkpoints are not backfilled."
                    : selection.records().isEmpty() ? "No completed generations in the selected range."
                    : "Select a generation for full network identities, validation evidence, configuration and timings.");
            return;
        }
        var r=tableModel.record(table.convertRowIndexToModel(row));
        details.setText("Candidate: "+r.candidate()+"\nIncumbent at validation start: "+r.incumbent()+"\nResulting Best: "+r.resultingBest()
                +"\n"+r.regime()+" · valid/incomplete pairs "+r.validPairs()+"/"+r.incompletePairs()+" · score "+optional(r.score())+" · lower "+optional(r.lowerBound())+" · threshold "+optional(r.threshold())+" · assessment "+r.decision()
                +"\nGames completed/aborted "+optional(r.completedGames())+"/"+optional(r.abortedGames())+" · samples "+optional(r.samples())+" · final loss "+optional(r.loss())
                +" · self-play / optimizer / validation "+nanos(r.selfPlayNanos())+" / "+nanos(r.trainingNanos())+" / "+nanos(r.validationNanos())
                +"\nTotal seconds "+(r.totalNanos()==null?"—":Double.toString(r.totalNanos()/1e9))
                +" · started "+optional(r.started())+" · completed "+r.completed());
        details.setToolTipText("Durations exclude trainer downtime and history append; total includes generation resume/checkpoint/decision I/O. Loss is training fit, not strength.");
    }
    static String validationDescription(GenerationRecord r) {
        if (r.bootstrap() == null) return r.regime()+" ? valid/incomplete pairs "+r.validPairs()+"/"+r.incompletePairs()
                +" ? score "+optional(r.score())+" ? lower "+optional(r.lowerBound())+" ? threshold "+optional(r.threshold())+" ? assessment "+r.decision();
        var b = r.bootstrap();
        return r.validationKind() + " (prediction accuracy, not game strength) ? " + r.decision()
                + "\nCandidate / Best held-out loss: " + b.comparison().candidateLoss() + " / " + b.comparison().bestLoss()
                + "\nTraining / held-out samples: " + b.trainingSamples() + " / " + b.comparison().samples()
                + " ? games " + b.trainingGames() + " / " + b.heldOutGames()
                + TrainingProgress.identities(b) + TrainingProgress.componentLosses(b)
                + "\nSplit seed: " + b.splitSeed() + " ? data SHA-256: " + b.dataHash();
    }
    static String optional(Object v) { return v==null?"—":v.toString(); }
    static String percent(Double v) { return v==null?"—":String.format(Locale.ROOT,"%.1f%%",v*100); }
    static String number(Double v) { return v==null?"—":String.format(Locale.ROOT,"%.2f",v); }
    static String nanos(Long v) { return duration(v==null?null:v/1e9); }
    static String duration(Double seconds) {
        if(seconds==null) return "—";
        return seconds<60?String.format(Locale.ROOT,"%.1fs",seconds):seconds<3600?String.format(Locale.ROOT,"%.1fm",seconds/60):String.format(Locale.ROOT,"%.1fh",seconds/3600);
    }
    static String timestamp(Instant time) { return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(time); }
    static final class Records extends AbstractTableModel {
        private List<GenerationRecord> records=List.of();
        void show(List<GenerationRecord> values) { records=values;fireTableDataChanged(); }
        GenerationRecord record(int row) { return records.get(records.size()-1-row); }
        public int getRowCount() { return records.size(); }
        public int getColumnCount() { return 7; }
        public String getColumnName(int c) { return new String[]{"Gen","Incumbent","Score","W–D–L","Outcome","Duration","Completed (local)"}[c]; }
        public Object getValueAt(int row,int c) { var r=record(row);return switch(c) {
            case 0->r.generation();case 1->TrainingDashboardModel.network(r.incumbent());case 2->percent(r.score());case 3->r.bootstrap()==null?r.wins()+"–"+r.draws()+"–"+r.losses():"—";
            case 4->r.outcome().toString().replace('_',' ') + (r.bootstrap() == null ? "" : " (" + r.bootstrap().supervision().mode() + " loss)");case 5->nanos(r.totalNanos());default->timestamp(r.completed());}; }
    }
    static JTable table(Records model,String name) {
        JTable t=new JTable(model);t.setName(name);t.setRowHeight(SeedTheme.scale(26));t.setFillsViewportHeight(true);t.setShowGrid(false);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.getTableHeader().setReorderingAllowed(false);
        t.setFont(SeedTheme.font(11,Font.PLAIN));t.getTableHeader().setFont(SeedTheme.font(11,Font.PLAIN));
        t.setDefaultRenderer(Object.class,new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table,Object v,boolean selected,boolean focus,int row,int col) {
                super.getTableCellRendererComponent(table,v,selected,focus,row,col);
                setBackground(selected?SeedTheme.SELECTED:row%2==0?SeedTheme.INSET:SeedTheme.PANEL);
                setForeground(model.record(row).promoted()?SeedTheme.GREEN:SeedTheme.TEXT);setToolTipText(optional(v));return this;
            }
        });
        int[] widths={45,70,60,65,160,65,125};for(int c=0;c<widths.length;c++) t.getColumnModel().getColumn(c).setPreferredWidth(SeedTheme.scale(widths[c]));
        return t;
    }
    public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
    public int getScrollableUnitIncrement(Rectangle r,int o,int d){return SeedTheme.scale(24);}
    public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(1,r.height-SeedTheme.scale(24));}
    public boolean getScrollableTracksViewportWidth(){return true;}
    public boolean getScrollableTracksViewportHeight(){return false;}
}
