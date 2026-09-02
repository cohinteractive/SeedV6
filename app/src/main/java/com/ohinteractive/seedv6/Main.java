package com.ohinteractive.seedv6;

import java.io.IOException;

import com.ohinteractive.seedv6.uci.UciEngine;

public class Main {

    public static void main(String[] args) {
        try {
            new UciEngine(System.in, System.out, System.err).run();
        } catch(IOException exception) {
            System.err.println("SeedV6 UCI input failed: " + exception.getMessage());
        }
    }

}
