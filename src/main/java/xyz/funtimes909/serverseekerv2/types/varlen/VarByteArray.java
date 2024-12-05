package xyz.funtimes909.serverseekerv2.types.varlen;

import io.netty.buffer.ByteBuf;
import xyz.funtimes909.serverseekerv2.util.PacketFormatter;

import java.util.Arrays;
import java.util.List;

public class VarByteArray extends AbstractVarType<byte[], VarByteArray> {
    public VarByteArray() { super(null, 0); }
    protected VarByteArray(byte[] value, int size) {
        super(value, size);
    }

    @Override
    public Class<?> getType() {
        return byte[].class;
    }


    public static VarByteArray decode(byte[] in, int index) {
        VarInt packetSize = VarInt.decode(in, index);
        return new VarByteArray(
                Arrays.copyOfRange(in, packetSize.size + index, packetSize.value + packetSize.size + index),
                packetSize.value + packetSize.size
        );
    }

    public static List<Byte> encode(byte[] in) {
        return PacketFormatter.prefixSize(in);
    }

    @Override
    public List<Byte> encodeSelf() {
        return encode(this.value);
    }
    @Override
    public List<Byte> encodeValue(Object in) {
        return encode((byte[]) in);
    }




    public static ByteBuf read(ByteBuf in) {
        int size = VarInt.read(in);
        return in.readBytes(size);
    }
    public static void write(ByteBuf out, byte[] val) {
        VarInt.write(out, val.length);
        out.writeBytes(val);
    }
}
