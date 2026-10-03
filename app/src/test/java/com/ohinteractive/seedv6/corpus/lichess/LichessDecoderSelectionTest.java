package com.ohinteractive.seedv6.corpus.lichess;

import com.ohinteractive.seedv6.corpus.CorpusRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LichessDecoderSelectionTest {
    static final String FEN="4k3/8/8/8/8/8/8/4K3 w - -";
    static String root(String evals,String extra) {
        return "{\"fen\":\""+FEN+"\",\"evals\":"+evals+extra+"}";
    }
    @Test void unusedTreesAndLaterPvsDoNotChangeSelection() {
        String ignored="{\"deep\":[null,true,false,1.25e30,{\"string\":\"escaped \\\" quote \\u1234\"}]}";
        String selected="{\"depth\":20,\"knodes\":15,\"pvs\":[{\"cp\":42,\"line\":\""+"a2a4 ".repeat(4000)+"\"},{\"mate\":1}]}";
        String invalid="{\"depth\":99,\"pvs\":[{\"cp\":12,\"mate\":1},{\"cp\":999}]}";
        var result=LichessDecoder.decode(root("[null,[],"+selected+","+invalid+"]",",\"ignored\":"+ignored),3,7);
        assertEquals(42,result.target());assertEquals(CorpusRecord.CP,result.targetKind());
        assertEquals(20,result.depth());assertEquals(15,result.work());assertEquals(3,result.sourceId());
    }
    @Test void duplicateRelevantKeysRetainLastValueAndNumericPolicy() {
        String evaluation="{\"depth\":100,\"depth\":20,\"knodes\":5,\"pvs\":[{\"cp\":999}],\"pvs\":[{\"cp\":5,\"cp\":42.0}]}";
        String json=root("[]",",\"fen\":\""+FEN+"\",\"evals\":["+evaluation+"]");
        assertEquals(42,LichessDecoder.decode(json,1,1).target());
        assertThrows(IllegalArgumentException.class,()->LichessDecoder.decode(json.replace("42.0","42.5"),1,1));
        assertThrows(IllegalArgumentException.class,()->LichessDecoder.decode(json.replace("42.0","\"42\""),1,1));
    }
    @Test void skippedContentStillRequiresStrictCompleteJson() {
        String valid="[{\"depth\":20,\"pvs\":[{\"cp\":42}]}]";
        for(String malformed:new String[]{"NaN","[1,]","{\"x\":}","\"bad\\u123z\"","/*comment*/0","01","\"bad\nline\""})
            assertThrows(IllegalArgumentException.class,()->LichessDecoder.decode(root(valid,",\"unused\":"+malformed),1,1),malformed);
        assertThrows(IllegalArgumentException.class,()->LichessDecoder.decode(root(valid,"")+" {}",1,1));
    }
}
