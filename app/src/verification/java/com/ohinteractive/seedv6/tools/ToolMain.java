package com.ohinteractive.seedv6.tools;

import java.util.Arrays;

/** Explicit developer entry point, excluded from application distributions. */
public final class ToolMain {
    public static void main(String[] args) throws Exception {
        if(args.length == 0) throw new IllegalArgumentException("Expected brn-diagnostic or frozen-wdl.");
        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        switch(args[0]) {
            case "brn-diagnostic" -> com.ohinteractive.seedv6.tools.search.BrnDiagnostic.main(remaining);
            case "pair2-parity" -> com.ohinteractive.seedv6.core.brn3.BrnPair2ResearchParity.main(remaining);
            case "frozen-wdl" -> com.ohinteractive.seedv6.training.service.FrozenWdlReplay.main(remaining);
            default -> throw new IllegalArgumentException("Unknown developer command: " + args[0]);
        }
    }
    private ToolMain() {}
}
