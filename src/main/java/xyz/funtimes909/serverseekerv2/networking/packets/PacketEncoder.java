package xyz.funtimes909.serverseekerv2.networking.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import xyz.funtimes909.serverseekerv2.types.PacketTypes;

// TODO: Make it's input an abstract packet
public class PacketEncoder extends MessageToByteEncoder<ByteBuf> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        // Prefix with size
        PacketTypes.VarInt.write(out, msg.writerIndex());
        // Then write the rest of the packet
        out.writeBytes(msg);
    }
}
