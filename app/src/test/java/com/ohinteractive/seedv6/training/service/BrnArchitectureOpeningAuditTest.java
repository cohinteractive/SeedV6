package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureOpeningAuditTest {
    @Test @SuppressWarnings("unchecked") void freshInventoryIsDeterministicAndRejectsStmGeometryAlias() {
        long seed=840170;int first=800;
        var a=BrnArchitectureOpeningAudit.audit(seed,first,4,Set.of());var b=BrnArchitectureOpeningAudit.audit(seed,first,4,Set.of());
        assertEquals(a,b);assertEquals(true,a.get("fresh"));
        var board=BrnArchitectureOpeningAudit.opening(seed,first).board();board[4]^=Board.PLAYER_BIT;
        var collision=BrnArchitectureOpeningAudit.audit(seed,first,4,Set.of(BrnResearchData.groupKey(board)));
        assertEquals(false,collision.get("fresh"));assertTrue(((List<Integer>)collision.get("earlierOverlapIndices")).contains(first));
        var warm=BrnArchitectureOpeningAudit.audit(190413,0,1,Set.of());assertEquals(false,warm.get("fresh"));
        assertThrows(IllegalArgumentException.class,()->BrnArchitectureOpeningAudit.audit(seed,Integer.MAX_VALUE,4,Set.of()));
    }
}
