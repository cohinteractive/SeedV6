package com.ohinteractive.seedv6.core.brnpair2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Pinned runtime input; no checkpoint-store access, discovery, import or fallback. */
public final class ProductionModel {
    public static final String SHA256 = "5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7";
    public static final String CHECKPOINT = "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304";
    public static final String LINEAGE = "235d0688-e060-44e3-8f1e-119c39bc8017";
    public static final String ENGINE_SOURCE = "35294146bb7e44975b516005b2cc196439666e09";

    public static BrnPair2Model load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new IOException("Required BRE-Pair 2 model missing: " + file
                + "; supply --model <file>; expected SHA-256 " + SHA256);
        if (Files.size(file) != BrnPair2Codec.MODEL_BYTES) throw new IOException("Incompatible BRE-Pair 2 model length: " + file);
        byte[] bytes;
        try (var in = Files.newInputStream(file)) { bytes = in.readNBytes(BrnPair2Codec.MODEL_BYTES + 1); }
        try {
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!actual.equals(SHA256)) throw new IOException("Corrupt or different BRE-Pair 2 model: " + file
                    + "; expected SHA-256 " + SHA256 + ", got " + actual);
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        // Decode exactly the bounded bytes whose checksum was verified, as in immutable UE ModelLoader.
        return BrnPair2Codec.readModel(new ByteArrayInputStream(bytes));
    }
    private ProductionModel() {}
}
