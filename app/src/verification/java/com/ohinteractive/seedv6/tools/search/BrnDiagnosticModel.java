package com.ohinteractive.seedv6.tools.search;

import java.io.*;
import java.security.*;
import java.util.HexFormat;
import com.ohinteractive.seedv6.core.brn2.*;

/** Stable parameter identity, independent of optimizer, timestamps, paths and checkpoint IDs. */
final class BrnDiagnosticModel {
    static String fingerprint(Brn2Model model) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var out = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
            // Big endian, versioned preamble, then exact ascending parameter index order.
            out.writeUTF("seedv6-brn-parameter-fingerprint-v1");
            out.writeUTF("BRN-2");
            out.writeInt(Brn2Codec.VERSION);
            out.writeInt(Brn2Features.VERSION);
            for (int dimension : new int[]{Brn2Model.NODE_ROWS, Brn2Model.RELATION_ROWS, Brn2Model.STATUS_ROWS,
                    Brn2Model.ENDPOINT_COUNT, Brn2Model.HIDDEN_WIDTH, Brn2Model.PARAMETER_COUNT}) out.writeInt(dimension);
            for (int i = 0; i < Brn2Model.PARAMETER_COUNT; i++) out.writeLong(Double.doubleToLongBits(model.weight(i)));
            out.flush();
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private BrnDiagnosticModel() {}
}
