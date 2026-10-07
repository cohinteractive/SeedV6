package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureMaterialTest {
    @Test void cheapPriorExactlyMatchesBrnGen0AcrossSpecialAndStmTransitions()throws Exception {
        var material=BrnArchitectureControls.loadView(java.nio.file.Path.of("material-fast")).evaluator().get();
        assertEquals(BrnArchitectureMaterial.ID,BrnArchitectureControls.modelIdentity(java.nio.file.Path.of("material-fast")));
        assertEquals("material-only",BrnArchitectureControls.modelIdentity(java.nio.file.Path.of("material")));
        var gen0=new Brn3Trainer(71).snapshot().newWorkspace();
        var requests=BrnArchitectureTransitions.requests(List.of(Board.startingPosition()));
        for(var request:requests) {
            var parent=request.parent();var child=request.child();var before=parent.clone();var childBefore=child.clone();
            material.initialize(parent);int original=material.evaluate(parent,0);
            assertEquals(Brn3Model.score(gen0.evaluatePawns(parent)),original);
            material.child(parent,child,0);assertEquals(Brn3Model.score(gen0.evaluatePawns(child)),material.evaluate(child,1));
            var nullChild=child.clone();nullChild[4]^=Board.PLAYER_BIT;
            material.child(child,nullChild,1);assertEquals(Brn3Model.score(gen0.evaluatePawns(nullChild)),material.evaluate(nullChild,2));
            assertEquals(original,material.evaluate(parent,0));assertArrayEquals(before,parent);assertArrayEquals(childBefore,child);
        }
    }
}
