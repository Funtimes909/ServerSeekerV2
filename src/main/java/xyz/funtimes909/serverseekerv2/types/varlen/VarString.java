package xyz.funtimes909.serverseekerv2.types.varlen;

import io.netty.buffer.ByteBuf;
import xyz.funtimes909.serverseekerv2.util.PacketFormatter;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class VarString extends AbstractVarType<String, VarString> {
    public VarString() { super(null, 0); }
    protected VarString(String value, int size) {
        super(value, size);
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }


    public static VarString decode(byte[] in, int index) {
        VarByteArray packet = VarByteArray.decode(in, index);
        return new VarString(
                new String(packet.value),
                packet.size
        );
    }

    public static List<Byte> encode(String in) {
        return PacketFormatter.prefixSize(in.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<Byte> encodeSelf() {
        return encode(this.value);
    }
    @Override
    public List<Byte> encodeValue(Object in) {
        return encode((String) in);
    }




    public static String read(ByteBuf in) {
        int size = VarInt.read(in);
        return in.readCharSequence(size, StandardCharsets.UTF_8).toString();
    }
    public static void write(ByteBuf out, String val) {
        VarByteArray.write(out, val.getBytes(StandardCharsets.UTF_8));
    }
}
