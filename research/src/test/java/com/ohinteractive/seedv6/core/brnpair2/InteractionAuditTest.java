package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InteractionAuditTest {
    @Test void remoteKingAndKnightCancelButMissingCornerIsRejected() {
        long[][] boards={Board.fromFen("6k1/5ppp/8/8/8/8/5PPP/1N4K1 w - - 0 1"),
            Board.fromFen("6k1/5ppp/8/8/8/8/5PPP/1N3K2 w - - 0 1"),
            Board.fromFen("6k1/5ppp/8/8/8/2N5/5PPP/6K1 w - - 0 1"),
            Board.fromFen("6k1/5ppp/8/8/8/2N5/5PPP/5K2 w - - 0 1")};
        assertDoesNotThrow(()->InteractionAudit.requireCancellation(boards));
        boards[3]=boards[2];assertThrows(IllegalArgumentException.class,()->InteractionAudit.requireCancellation(boards));
    }
}
