package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model;
import java.io.*;
import java.security.*;
import java.util.HexFormat;

/** Immutable named component of the new residual schemas, independent of future production selection. */
public final class BrnNextBase {
    public static final String SHA256="5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7";
    private static final class Holder { static final BrnPair2Model MODEL=load(); }
    public static BrnPair2Model model() { return Holder.MODEL; }
    private static BrnPair2Model load() {
        try(var input=BrnNextBase.class.getResourceAsStream("/models/bre-pair2-g32.brn-pair2")) {
            if(input==null)throw new IOException("Required frozen Gen32 residual-model component is missing");
            byte[] bytes=input.readNBytes(BrnPair2Codec.MODEL_BYTES+1);
            if(bytes.length!=BrnPair2Codec.MODEL_BYTES
                    || !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(SHA256))
                throw new IOException("Frozen Gen32 residual-model component identity mismatch");
            return BrnPair2Codec.readModel(new ByteArrayInputStream(bytes));
        } catch(IOException failure) { throw new UncheckedIOException(failure); }
        catch(NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private BrnNextBase() {}
}
