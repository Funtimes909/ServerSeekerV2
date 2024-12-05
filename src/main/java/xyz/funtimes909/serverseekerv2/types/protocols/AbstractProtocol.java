package xyz.funtimes909.serverseekerv2.types.protocols;

import io.netty.buffer.ByteBuf;

public interface AbstractProtocol<S> {
    static final AbstractProtocol<?> INSTANCE = new Error();
    int PROTOCOL();


    static <S> S decode(ByteBuf in) {
        throw new RuntimeException("Decode packet not implemented");
    };
    default ByteBuf encode() {
        throw new RuntimeException("Encode packet not implemented");
    };
}
