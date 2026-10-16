package com.ohinteractive.seedv6;

import java.io.IOException;

import com.ohinteractive.seedv6.gui.SwingLauncher;
import com.ohinteractive.seedv6.uci.UciEngine;
import com.ohinteractive.seedv6.uci.LegacyUciEvaluation;

public class Main {

    public static void main(String[] args) {
        if(args.length == 1 && args[0].equalsIgnoreCase("gui")) {
            SwingLauncher.launch();
            return;
        }
        try {
            new UciEngine(System.in, System.out, System.err, LegacyUciEvaluation.load(System.getProperties())).run();
        } catch(IOException exception) {
            System.err.println("SeedV6 UCI input failed: " + exception.getMessage());
        }
    }

}
