package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Fixed affine/degree heads folded into existing guarded polynomial caches.
 * The source model and frozen base never change. Near a rounding boundary the
 * independent, unfurled canonical formula decides the integer score. */
public final class BrnCalibratedAccumulator {
    private final BrnNextModel model;
    private final BrnNextModel.Workspace oracle;
    private final BrnQuadraticAccumulator quadratic;
    private final BrnCubicDeltaAccumulator cubic;
    private final double a0,a1,a2,foldError;
    private double material,magnitude;
    public long roundingFallbacks;

    public BrnCalibratedAccumulator(BrnNextModel model) {
        var kind=model.architecture();
        if(!kind.calibratedHeads())throw new IllegalArgumentException("Calibrated fixed heads required");
        this.model=model;oracle=model.newWorkspace();
        int h=kind.head(),a=kind.affineHead();double scale=1+model.weight(a+3);
        a0=model.weight(a);a1=model.weight(a+1);a2=model.weight(a+2);
        var effectiveKind=kind==GEN32_CAL_CUBIC_R4?GEN32_FIXED_CUBIC_R4:GEN32_FIXED_QUADRATIC_R4;
        double[] weights=new double[effectiveKind.parameters()];
        double unary=0,embedding=0,head=0,extra=0;
        for(int i=0;i<a;i++) {
            double w=model.weight(i);weights[i]=i<EMBEDDING||i>=h?w*scale:w;
            if(i<EMBEDDING)unary=Math.max(unary,Math.abs(w));
            else if(i<h)embedding=Math.max(embedding,Math.abs(w));
            else head+=Math.abs(w);
        }
        if(kind!=GEN32_CAL_AFFINE_R4)for(int r=0;r<kind.width();r++) {
            double w=model.weight(kind.extraHead()+r);extra+=Math.abs(w);
            if(kind==GEN32_CAL_CUBIC_R4)weights[a+r]=w;
            else weights[h+r]+=w;
        }
        // Covers distribution/combination rounding relative to the independent
        // anchor formula, including cancellation of large opposing coefficients.
        double two=2016*embedding*embedding*DEGREE2_NORM;
        double anchor=65*unary+head*two;
        foldError=256*Math.ulp(Math.max(1,anchor*(1+Math.abs(model.weight(a+3)))+extra*Math.max(1,two)));
        var folded=new BrnNextModel(effectiveKind,weights);
        quadratic=kind==GEN32_CAL_CUBIC_R4?null:new BrnQuadraticAccumulator(folded);
        cubic=kind==GEN32_CAL_CUBIC_R4?new BrnCubicDeltaAccumulator(folded):null;
    }
    public long primitiveBytes() {
        return (quadratic!=null?quadratic.primitiveBytes():cubic.primitiveBytes())+oracle.primitiveBytes()+56;
    }
    public long derivedModelPrimitiveBytes() { return 8L*(quadratic!=null?quadratic.model.architecture().parameters():cubic.model.architecture().parameters()); }
    public double rawPawns(long[] board) {
        double combined=quadratic!=null?quadratic.rawPawns(board):cubic.rawPawns(board);
        var base=quadratic!=null?quadratic.base:cubic.base;
        double reference=base.evaluatePawns(board);material=base.materialPawns(Board.player((int)board[Board.STATUS]));
        double correction=combined-reference;
        double shift=a0+a1*(material/8)+a2*((reference-material)/4);
        double value=reference+.5*(correction+shift);
        magnitude=Math.max(1,Math.abs(reference)+Math.abs(combined)+Math.abs(correction)+Math.abs(a0)
                +Math.abs(a1)*Math.abs(material)/8+Math.abs(a2)*(Math.abs(reference)+Math.abs(material))/4);
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite calibrated cache");
        return value;
    }
    public int score(long[] board) {
        double raw=rawPawns(board),mapped=material+.25*(raw-material),scaled=100*mapped;
        double polynomialError=quadratic!=null?quadratic.correctionErrorBound():cubic.correctionErrorBound();
        double guard=25*(.5*(polynomialError+foldError)+64*Math.ulp(magnitude))
                +32*Math.ulp(Math.max(1,Math.abs(scaled)));
        double absolute=Math.abs(scaled);
        if(!Double.isFinite(guard)||guard>=.25||Math.abs(absolute-Math.floor(absolute)-.5)<=guard) {
            roundingFallbacks++;return model.score(board,oracle);
        }
        return Brn3Model.score(mapped);
    }
}
