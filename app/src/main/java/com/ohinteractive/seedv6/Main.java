package com.ohinteractive.seedv6;

import java.io.IOException;

import com.ohinteractive.seedv6.gui.SwingLauncher;
import com.ohinteractive.seedv6.uci.UciEngine;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("brn-diagnostic")) {
            try {
                com.ohinteractive.seedv6.tools.search.BrnDiagnostic.main(java.util.Arrays.copyOfRange(args, 1, args.length));
            } catch (Exception failure) {
                System.err.println("BRN diagnostic failed: " + failure.getMessage());
                failure.printStackTrace(System.err); System.exit(1);
            }
            return;
        }
        if (args.length > 0 && args[0].equals("frozen-wdl")) {
            try {
                com.ohinteractive.seedv6.training.service.FrozenWdlReplay.main(java.util.Arrays.copyOfRange(args, 1, args.length));
            } catch (Exception failure) {
                System.err.println("Frozen WDL replay failed: " + failure.getMessage());
                failure.printStackTrace(System.err); System.exit(1);
            }
            return;
        }
        if(args.length == 1 && args[0].equalsIgnoreCase("gui")) {
            SwingLauncher.launch();
            return;
        }
        try {
            new UciEngine(System.in, System.out, System.err).run();
        } catch(IOException exception) {
            System.err.println("SeedV6 UCI input failed: " + exception.getMessage());
        }
    }

}
