package xyz.funtimes909.serverseekerv2.networking.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import xyz.funtimes909.serverseekerv2.types.PacketTypes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Security;
import java.util.List;
import java.util.zip.InflaterOutputStream;

// TODO: Make it's output an abstract packet
public class PacketDecoder extends ByteToMessageDecoder {
    public int compressionThreshold;

    // NOTE: This is only for testing. Once deployed, the BC provider will be added in the main function
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
            Security.addProvider(new BouncyCastleProvider());
    }



    public PacketDecoder() {
        this(-1);
    }
    public PacketDecoder(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        out.add(read(msg, this.compressionThreshold));
    }




    public static ByteBuf read(ByteBuf in, int compressionThreshold) throws IOException {
        ByteBuf packet = PacketTypes.ByteArray.read(in);
        return decompressPacket(packet, compressionThreshold);
    }

    public static ByteBuf decompressPacket(ByteBuf in, int compressionThreshold) throws IOException {
        if (compressionThreshold == -1) // If it isn't enabled
            return in;

        // Then see how large it would be
        int size = PacketTypes.VarInt.read(in);
        // If the packet is too small, then you don't need to decompress
        if (size < compressionThreshold)
            return in;

        // Now we try to de-compress
        // TODO: Find a more Netty compatible way of doing this
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (OutputStream ios = new InflaterOutputStream(os)) {
            ios.write(in.readBytes(size).array());
        }
        return Unpooled.copiedBuffer(os.toByteArray());
    }
}
