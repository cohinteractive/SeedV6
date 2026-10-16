package com.ohinteractive.seedv6.uci;

import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Compatibility boundary; canonical model identity and validation live in engine-core. */
public final class ProductionModel {
    public static final String SHA256 = com.ohinteractive.seedv6.core.brnpair2.ProductionModel.SHA256;
    public static final String CHECKPOINT = com.ohinteractive.seedv6.core.brnpair2.ProductionModel.CHECKPOINT;
    public static final String LINEAGE = com.ohinteractive.seedv6.core.brnpair2.ProductionModel.LINEAGE;
    public static final String ENGINE_SOURCE = com.ohinteractive.seedv6.core.brnpair2.ProductionModel.ENGINE_SOURCE;

    /** Relative paths are relative to the installed application, never the client's working directory. */
    public static Path resolve(String configured) throws Exception {
        Path code = Path.of(ProductionModel.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path home = Files.isRegularFile(code) ? code.getParent().getParent() : code;
        Path selected = Path.of(configured == null || configured.isBlank() ? "models/baseline.brn-pair2" : configured);
        return (selected.isAbsolute() ? selected : home.resolve(selected)).normalize();
    }

    public static BrnPair2Model load(Path file) throws IOException {
        return com.ohinteractive.seedv6.core.brnpair2.ProductionModel.load(file);
    }
    private ProductionModel() {}
}
