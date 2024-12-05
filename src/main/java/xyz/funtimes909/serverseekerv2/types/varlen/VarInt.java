package xyz.funtimes909.serverseekerv2.types.varlen;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class VarInt extends AbstractVarType<Integer, VarInt> {
    public VarInt() { super(null, 0); }
    protected VarInt(Integer value, int size) {
        super(value, size);
    }

    @Override
    public Class<?> getType() {
        return Integer.class;
    }


    /**
     * Attempts to decode the varint at the start of a byte array
     * @param i Starting index to the byte array
     * @return A Pair containing the value and the n bytes needed to get it in the byte array
     */
    public static VarInt decode(byte[] in, int i) {
        int val = 0;
        byte count = 0;

        for (; i < in.length; i++) {
            byte b = in[i];
            val |= (b & 0b0111_1111) << count;
            if (((b >> 7) != -1) || (count > (7 /* size */ * 5 /* max size */)))
                break;
            count += 7;
        }

        return new VarInt(val, ((count/7) + 1));
    }


    /**
     * Attempts to decode the varint from an input stream
     * @return The decoded input stream
     */
    public static Integer decode(InputStream in) throws IOException {
        int val = 0;

        for (byte count = 0; count < (7 /* size */ * 5 /* max size */); count += 7) {
            byte b = (byte) in.read();
            val |= (b & 0b0111_1111) << count;
            if (((b >> 7) != -1))
                break;
        }

        return val;
    }



    public static List<Byte> encode(int v) {
        ArrayList<Byte> res = new ArrayList<>();
        do {
            res.add((byte) (((v >> 7 == 0)? 0: 0b1000_0000) | (v & 0b0111_1111)));
            v >>= 7;
        } while (v != 0);
        return res;
    }

    @Override
    public List<Byte> encodeSelf() {
        return encode(this.value);
    }
    @Override
    public List<Byte> encodeValue(Object in) {
        return encode((int) in);
    }










    public static void write(ByteBuf out, int v) {
        do {
            out.writeByte((byte) (((v >> 7 == 0)? 0: 0b1000_0000) | (v & 0b0111_1111)));
            v >>= 7;
        } while (v != 0);
    }
    public static int read(ByteBuf in) {
        int val = 0;

        for (byte count = 0; count < (7 /* size */ * 5 /* max size */); count += 7) {
            byte b = in.readByte();
            val |= (b & 0b0111_1111) << count;
            if (((b >> 7) != -1))
                break;
        }

        return val;
    }
}