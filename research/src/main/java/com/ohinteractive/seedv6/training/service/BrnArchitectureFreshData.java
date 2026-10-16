package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import java.util.*;

/** Prospective geometry-disjoint source acquisition; no test-label evaluation. */
public final class BrnArchitectureFreshData {
    public static void main(String[] args)throws Exception {
        if(args.length<7)throw new IllegalArgumentException("SOURCE NEW_OUT TRAIN_COUNT HELD_COUNT RAW_START SEEK EARLIER_DATA...");
        var earlier=new HashSet<String>();var provenance=new ArrayList<Object>();
        for(int i=6;i<args.length;i++)provenance.add(BrnArchitectureDataAudit.scan(Path.of(args[i]),
                (board,partition,index)->earlier.add(BrnResearchData.groupKey(board))));
        BrnResearchData.prepare(Path.of(args[0]),Path.of(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]),
                Long.parseLong(args[4]),Path.of(args[5]),true,new BrnResearchData.GeometryAdmission(earlier,provenance));
    }
    private BrnArchitectureFreshData(){}
}
