package xyz.funtimes909.serverseekerv2.network;

import xyz.funtimes909.serverseekerv2.types.varlen.VarInt;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.InflaterOutputStream;

public class PacketUtils {
    public static byte[] readStream(InputStream io) throws IOException {
        return readStream(io, -1);
    }
    public static byte[] readStream(InputStream io, int compressionThreshold) throws IOException {
        int packetSize = VarInt.decode(io);
        byte[] packet = io.readNBytes(packetSize);
        return decompressPacket(packet, compressionThreshold);
    }



    public static byte[] decompressPacket(byte[] packet, int compressionThreshold) throws IOException {
        if (compressionThreshold == -1) // If it isn't enabled
            return packet;

        // Then see how large it would be
        VarInt dataSize = VarInt.decode(packet, 0);
        byte[] newPacket = Arrays.copyOfRange(packet, dataSize.getSize(), packet.length);
        // If the packet is too small, then you don't need to decompress
        if (dataSize.get() < compressionThreshold)
            return newPacket;

        // Now we try to de-compress
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (OutputStream ios = new InflaterOutputStream(os)) {
            ios.write(newPacket);
        }
        return os.toByteArray();
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
        List<byte[]> result = new ArrayList<>();
        byte[] encryptedPacket = io.readAllBytes();
        byte[] packet = cipher.doFinal(encryptedPacket);

        int packetStartIndex = 0;

        while (packetStartIndex < packet.length) {
            VarInt packetSize = VarInt.decode(packet, packetStartIndex);
            packetStartIndex += packetSize.get() + packetSize.getSize() + 1;
            result.add(
                    decompressPacket(
                            Arrays.copyOfRange(
                                    packet,
                                    packetStartIndex - packetSize.get() - 1,
                                    packetStartIndex - 1
                            ),
                            compressionThreshold
                    )
            );
        }

        return result;
    }
}