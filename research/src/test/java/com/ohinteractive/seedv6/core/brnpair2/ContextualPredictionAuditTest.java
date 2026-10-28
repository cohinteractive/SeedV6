package com.ohinteractive.seedv6.core.brnpair2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContextualPredictionAuditTest {
    @Test void probabilityErrorUsesAlignedPerspectiveAndAllRows() {
        double[] logits={0,Math.log(3),-Math.log(3)},targets={0,.5,1};
        assertEquals((.25+.0625+.5625)/3,ContextualPredictionAudit.mse(logits,targets),1e-15);
        assertThrows(IllegalArgumentException.class,()->ContextualPredictionAudit.mse(new double[0],new double[0]));
        assertThrows(IllegalArgumentException.class,()->ContextualPredictionAudit.mse(logits,new double[2]));
    }
}
