package xyz.funtimes909.serverseekerv2.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.funtimes909.serverseekerv2.types.PacketTypes;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.InflaterOutputStream;

public class PacketUtils {
    private static final Logger log = LoggerFactory.getLogger(PacketUtils.class);

    public static ByteBuf read(ByteBuf in) throws IOException {
        return read(in, -1);
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


    /**
     * Decrypts an encrypted input stream
     * @param cipher The Cipher object already initialized with key & algorithm (used to decode)
     * @return List of packets. While normal servers *should* only ever return one packet per request,
     * it is possible to send back multiple. So this returns a list of the byte arrays which contains each packet.
     */
    public static List<byte[]> readEncryptedStream(InputStream io, Cipher cipher)
            throws IllegalBlockSizeException, IOException, BadPaddingException {
        return readEncryptedStream(io, cipher, -1);
    }
    /**
     * Decrypts an encrypted input stream
     * @param cipher The Cipher object already initialized with key & algorithm (used to decode)
     * @return List of packets. While normal servers *should* only ever return one packet per request,
     * it is possible to send back multiple. So this returns a list of the byte arrays which contains each packet.
     */
    public static List<byte[]> readEncryptedStream(InputStream io, Cipher cipher, int compressionThreshold)
            throws IOException, IllegalBlockSizeException, BadPaddingException
    {
//        List<byte[]> result = new ArrayList<>();
//        byte[] encryptedPacket = io.readAllBytes();
//        byte[] packet = cipher.doFinal(encryptedPacket);
//
//        int packetStartIndex = 0;
//
//        while (packetStartIndex < packet.length) {
//            VarInt packetSize = VarInt.decode(packet, packetStartIndex);
//            packetStartIndex += packetSize.get() + packetSize.getSize() + 1;
//            result.add(
//                    decompressPacket(
//                            Arrays.copyOfRange(
//                                    packet,
//                                    packetStartIndex - packetSize.get() - 1,
//                                    packetStartIndex - 1
//                            ),
//                            compressionThreshold
//                    )
//            );
//        }
//
//        return result;
        throw new RuntimeException("Reading encrypted streams not done yet");
    }
}