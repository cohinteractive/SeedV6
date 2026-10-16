package com.ohinteractive.seedv6.training.service;

/** Opt-in BRN-2 lineage seeds. Only SELF_PLAY uses dataSeed; all other domains use masterSeed. */
public record BrnRunSeeds(long masterSeed, long dataSeed) {
    public String settingsSuffix() { return "|brn-run-seeds-v1:" + masterSeed + ":" + dataSeed; }
}
