package xyz.funtimes909.serverseekerv2.types.protocols.login.incoming;

import io.netty.buffer.ByteBuf;
import xyz.funtimes909.serverseekerv2.types.PacketTypes;
import xyz.funtimes909.serverseekerv2.types.protocols.AbstractProtocol;

public class Compression implements AbstractProtocol<Compression> {
    public static final AbstractProtocol<?> INSTANCE = new Compression();
    @Override
    public int PROTOCOL() { return 3; }

    public int threshold = -1;

    public Compression() {}

    private Compression(int threshold) {
        this.threshold = threshold;
    }


    public static Compression decode(ByteBuf in) {
        return new Compression(
                PacketTypes.VarInt.read(in)
        );
    }
}
