package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.util.Piece;
import java.util.*;

/** A diagnostic oracle, never an evaluator input, initializer, or training target.
 * Synthetic king-safe FENs, not legal single-move transitions or a reachability claim. */
public final class MaterialRemovalProbe {
    public record Removal(int code,int square,long[] board) {}
    public static List<Removal> positions() {
        var root=Board.startingPosition();var positions=new ArrayList<Removal>();
        for(int s=0;s<64;s++) {
            int code=Board.getSquare(root[0],root[1],root[2],root[3],s);
            if(code==0||(code&7)==Piece.KING)continue;
            var b=root.clone();for(int i=0;i<4;i++)b[i]&=~(1L<<s);
            int right=switch(s){case 0->4;case 7->2;case 56->16;case 63->8;default->0;};
            b[4]&=~(long)right;
            positions.add(new Removal(code,s,Board.fromFen(Fen.fromBoard(b))));
        }
        return List.copyOf(positions);
    }
    public static Map<String,Object> report(IncrementalModel model) {
        var worker=model.worker(2);var root=Board.startingPosition();worker.refresh(root,0);double base=worker.raw(root,0);
        int baseMapped=worker.evaluate(root,0),positive=0,negative=0,zero=0;
        var effects=new ArrayList<Map<String,Object>>();
        for(var removal:positions()) {
            worker.refresh(removal.board(),1);double raw=worker.raw(removal.board(),1);int mapped=worker.evaluate(removal.board(),1);
            int sign=(removal.code()&8)==0?1:-1;double aligned=sign*(base-raw);
            if(aligned>0)positive++;else if(aligned<0)negative++;else zero++;
            var row=new LinkedHashMap<String,Object>();row.put("piece",Piece.SHORT_STRING[removal.code()]);row.put("square",removal.square());
            row.put("alignedRawEffect",aligned);row.put("alignedMappedEffect",sign*(baseMapped-mapped));
            row.put("brnMaterialAlignedEffectPawns",sign*(BootstrapAudit.materialOracle(root)-BootstrapAudit.materialOracle(removal.board())));
            effects.add(row);
        }
        return Map.of("baseRaw",base,"baseMapped",baseMapped,"materialDirectionAgreements",positive,
                "materialDirectionDisagreements",negative,"ties",zero,"removals",effects,
                "scope","30 starting placements with one non-king removed; castling rights corrected; both kings safe; no reachability proof; no candidate parameters selected or changed from these results");
    }
}
