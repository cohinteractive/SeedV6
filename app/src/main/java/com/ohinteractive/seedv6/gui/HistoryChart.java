package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import com.ohinteractive.seedv6.training.history.GenerationRecord;

/** Native Swing plot. Large series use at most 600 min/max buckets; promotions and regime changes survive. */
final class HistoryChart extends JComponent {
    private record Point(long first, long last, double low, double high, boolean promoted, boolean boundary,
                         GenerationRecord exact) {}
    private final boolean score;
    private List<Point> points = List.of();
    private long first, last;
    private double maximum = 1;
    HistoryChart(boolean score, int height) {
        this.score = score; setPreferredSize(new Dimension(1, SeedTheme.scale(height)));
        setMinimumSize(new Dimension(0, SeedTheme.scale(90))); setToolTipText("");
        setFont(SeedTheme.font(10, Font.PLAIN));
    }
    void showRecords(List<GenerationRecord> records) {
        var next = new ArrayList<Point>(); maximum = score ? 1 : 0;
        int stride = Math.max(1,(records.size()+599)/600);
        GenerationRecord.Regime previous = null;
        for (int offset=0; offset<records.size(); offset+=stride) {
            int end=Math.min(records.size(),offset+stride); double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            boolean promoted=false,boundary=false;
            for (int i=offset;i<end;i++) {
                var r=records.get(i); Double value=score?r.score():r.totalNanos()==null?null:r.totalNanos()/1e9;
                if (value!=null) { min=Math.min(min,value); max=Math.max(max,value); }
                promoted |= r.promoted(); boundary |= previous!=null && !previous.equals(r.regime()); previous=r.regime();
            }
            if (Double.isFinite(max)) maximum=Math.max(maximum,max);
            next.add(new Point(records.get(offset).generation(),records.get(end-1).generation(),min,max,promoted,boundary,
                    end-offset==1?records.get(offset):null));
        }
        points=List.copyOf(next); maximum=score?1:Math.max(1,maximum*1.1);
        if (!records.isEmpty()) { first=records.getFirst().generation(); last=records.getLast().generation(); }
        getAccessibleContext().setAccessibleName(score?"Candidate score versus that generation's incumbent Best":"Recorded generation duration");
        repaint();
    }
    @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext==null) accessibleContext=new AccessibleJComponent() {};
        return accessibleContext;
    }
    private int left() { return SeedTheme.scale(51); }
    private int right() { return Math.max(left()+1,getWidth()-SeedTheme.scale(17)); }
    private int top() { return SeedTheme.scale(13); }
    private int bottom() { return Math.max(top()+1,getHeight()-SeedTheme.scale(27)); }
    private int x(double generation) { return left()+(int)((generation-first)/Math.max(1,last-first)*(right()-left())); }
    private int y(double value) { return bottom()-(int)(value/maximum*(bottom()-top())); }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g.setFont(getFont());
        if(points.isEmpty()) {
            // A grid through the empty-state caption looks like a struck-out message at preview height.
            String message="No completed history"; g.setColor(SeedTheme.SECONDARY);
            g.drawString(message,Math.max(SeedTheme.scale(6),(getWidth()-g.getFontMetrics().stringWidth(message))/2),getHeight()/2);
            g.dispose(); return;
        }
        for(int tick=0;tick<=2;tick++) {
            double value=maximum*tick/2; int y=y(value);
            g.setColor(score&&tick==1?SeedTheme.MUTED:SeedTheme.LINE); g.drawLine(left(),y,right(),y);
            g.setColor(SeedTheme.SECONDARY); g.drawString(score?(int)(value*100)+"%":TrainingHistory.duration(value),SeedTheme.scale(3),y+SeedTheme.scale(4));
        }
        {
            Point previous = null;
            for(var p:points) {
                int x=x(p.first()+(p.last()-p.first())/2.0);
                if(p.boundary()) { g.setColor(SeedTheme.MUTED); g.setStroke(new BasicStroke(1,0,0,1,new float[]{3,4},0)); g.drawLine(x,top(),x,bottom()); g.setStroke(new BasicStroke(1)); }
                if(!Double.isFinite(p.low())) { g.setColor(SeedTheme.WARNING); g.drawString("×",x-3,bottom()-3); previous = null; continue; }
                // Connect exact consecutive scores only; never bridge missing values, regimes or buckets.
                if(score && previous != null && previous.exact()!=null && p.exact()!=null
                        && !p.boundary() && p.first()==previous.last()+1) {
                    g.setColor(SeedTheme.GREEN); g.drawLine(x(previous.first()),y(previous.high()),x,y(p.high()));
                }
                g.setColor(score && p.exact()!=null ? SeedTheme.GREEN : SeedTheme.SECONDARY);
                int y=y(p.high()); g.drawLine(x,y,x,y(p.low()));
                int radius=SeedTheme.scale(p.promoted()?4:2);
                if(p.promoted()) {
                    // An aggregated bucket's maximum need not be the promoted Candidate's score.
                    int marker=p.exact()==null?top()-SeedTheme.scale(7):y;
                    g.setColor(SeedTheme.GREEN);
                    g.fillPolygon(new int[]{x,x+radius,x,x-radius},new int[]{marker-radius,marker,marker+radius,marker},4);
                }
                else g.fillOval(x-radius,y-radius,2*radius,2*radius);
                previous = p;
            }
            g.setColor(SeedTheme.SECONDARY); g.drawString("Gen "+first,left(),getHeight()-SeedTheme.scale(6));
            String end=""+last; g.drawString(end,right()-g.getFontMetrics().stringWidth(end),getHeight()-SeedTheme.scale(6));
        }
        g.dispose();
    }
    @Override public String getToolTipText(MouseEvent event) {
        if(points.isEmpty()) return null;
        Point closest=points.getFirst(); double distance=Double.MAX_VALUE;
        for(var p:points) { double d=Math.abs(event.getX()-x(p.first()+(p.last()-p.first())/2.0)); if(d<distance) { distance=d; closest=p; } }
        var r=closest.exact();
        if(r==null) return "Generations "+closest.first()+"–"+closest.last()+": min/max bucket; "
                +(closest.promoted()?"includes promotion; ":"")+"select table rows for exact measurements";
        return "Generation "+r.generation()+" · "+(score?TrainingHistory.percent(r.score()):TrainingHistory.duration(r.totalNanos()==null?null:r.totalNanos()/1e9))
                +" · "+r.outcome()+" · incumbent "+TrainingDashboardModel.network(r.incumbent());
    }
}
