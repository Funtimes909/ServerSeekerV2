package xyz.funtimes909.serverseekerv2.networking.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import xyz.funtimes909.serverseekerv2.types.PacketTypes;

import java.security.Security;

// TODO: Make it's input an abstract packet
public class PacketEncoder extends MessageToByteEncoder<ByteBuf> {
    // NOTE: This is only for testing. Once deployed, the BC provider will be added in the main function
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
            Security.addProvider(new BouncyCastleProvider());
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        // Prefix with size
        PacketTypes.VarInt.write(out, msg.writerIndex());
        // Then write the rest of the packet
        out.writeBytes(msg);
    }
}
