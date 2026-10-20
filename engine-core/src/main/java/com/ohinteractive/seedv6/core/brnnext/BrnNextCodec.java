package com.ohinteractive.seedv6.core.brnnext;

import java.io.*;
import java.util.zip.CRC32;

/** Fixed version, feature, numerical and calibration identity; separate model/state magic. */
public final class BrnNextCodec {
    static final long MODEL_MAGIC = 0x53364E5852543031L, STATE_MAGIC = 0x53364E5853543031L;
    private static final int HEADER_BYTES = 36;
    public static int modelBytes(BrnNextArchitecture kind) { return HEADER_BYTES + (kind.frozenBase()?32:0) + kind.parameters() * 8 + 4; }
    public static int trainingBytes(BrnNextArchitecture kind) { return HEADER_BYTES + (kind.frozenBase()?32:0) + 16 + kind.parameters() * 24 + 4; }
    static void header(DataOutputStream out, long magic, BrnNextArchitecture kind) throws IOException {
        out.writeLong(magic);
        for (int value : new int[]{1, kind.wireId(), kind.parameters(), kind.width(), 1, 1, 64}) out.writeInt(value);
        if(kind.frozenBase())out.write(java.util.HexFormat.of().parseHex(BrnNextBase.SHA256));
    }
    static DataInputStream checked(InputStream input, long magic, BrnNextArchitecture kind, int size) throws IOException {
        byte[] bytes = input.readNBytes(size + 1);
        if (bytes.length != size) throw new IOException("BRN-next payload length mismatch");
        var crc = new CRC32(); crc.update(bytes, 0, size - 4);
        var tail = new DataInputStream(new ByteArrayInputStream(bytes, size - 4, 4));
        if (tail.readInt() != (int) crc.getValue()) throw new IOException("BRN-next checksum mismatch");
        var in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readLong() != magic) throw new IOException("Wrong BRN-next artifact/family");
        for (int value : new int[]{1, kind.wireId(), kind.parameters(), kind.width(), 1, 1, 64})
            if (in.readInt() != value) throw new IOException("Incompatible BRN-next architecture/layout/calibration");
        if(kind.frozenBase()&&!java.util.Arrays.equals(in.readNBytes(32),java.util.HexFormat.of().parseHex(BrnNextBase.SHA256)))
            throw new IOException("Incompatible frozen base identity");
        return in;
    }
    static void finish(ByteArrayOutputStream bytes, DataOutputStream out, OutputStream target) throws IOException {
        out.flush(); var crc = new CRC32(); crc.update(bytes.toByteArray());
        out.writeInt((int) crc.getValue()); out.flush(); bytes.writeTo(target);
    }
    static double finite(DataInputStream in) throws IOException {
        double value = in.readDouble();
        if (!Double.isFinite(value)) throw new IOException("Nonfinite BRN-next payload");
        return value;
    }
    public static void writeModel(BrnNextModel model, OutputStream target) throws IOException {
        var bytes = new ByteArrayOutputStream(modelBytes(model.architecture()));
        var out = new DataOutputStream(bytes); header(out, MODEL_MAGIC, model.architecture());
        for (int i = 0; i < model.architecture().parameters(); i++) out.writeDouble(model.weight(i));
        finish(bytes, out, target);
    }
    public static BrnNextModel readModel(BrnNextArchitecture kind, InputStream input) throws IOException {
        var in = checked(input, MODEL_MAGIC, kind, modelBytes(kind));
        var weights = new double[kind.parameters()];
        for (int i = 0; i < weights.length; i++) weights[i] = finite(in);
        return new BrnNextModel(kind, weights);
    }
    private BrnNextCodec() {}
}
