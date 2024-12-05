package xyz.funtimes909.serverseekerv2.types;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PacketTypes {
    public static class VarInt {
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

        public static void write(ByteBuf out, int v) {
            do {
                out.writeByte((byte) (((v >> 7 == 0) ? 0 : 0b1000_0000) | (v & 0b0111_1111)));
                v >>= 7;
            } while (v != 0);
        }
    }


    public static class String {
        public static java.lang.String read(ByteBuf in) {
            int size = VarInt.read(in);
            return in.readCharSequence(size, StandardCharsets.UTF_8).toString();
        }
        public static void write(ByteBuf out, java.lang.String val) {
            ByteArray.write(out, val.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static class ByteArray {
        public static ByteBuf read(ByteBuf in) {
            int size = VarInt.read(in);
            return in.readBytes(size);
        }

        public static void write(ByteBuf out, byte[] val) {
            VarInt.write(out, val.length);
            out.writeBytes(val);
        }
    }

    public static class UUID {
        public static java.util.UUID read(ByteBuf in) {
            return new java.util.UUID(in.readLong(), in.readLong());
        }

        public static void write(ByteBuf out, java.util.UUID val) {
            out.writeLong(val.getMostSignificantBits());
            out.writeLong(val.getLeastSignificantBits());
        }
    }
}
