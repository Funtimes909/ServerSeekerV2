package xyz.funtimes909.serverseekerv2.network.protocols;

import com.google.common.primitives.Bytes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import xyz.funtimes909.serverseekerv2.network.Connect;
import xyz.funtimes909.serverseekerv2.network.PacketUtils;
import xyz.funtimes909.serverseekerv2.types.protocols.login.LoginPacketType;
import xyz.funtimes909.serverseekerv2.types.protocols.login.incoming.Compression;
import xyz.funtimes909.serverseekerv2.types.protocols.login.incoming.Disconnect;
import xyz.funtimes909.serverseekerv2.types.protocols.login.incoming.Encryption;
import xyz.funtimes909.serverseekerv2.types.protocols.login.incoming.LoginSuccess;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Login {
    // The default username & uuid to attempt to login to servers if none is given
    public static final String username = "Herobrine";
    public static final UUID uuid = UUID.fromString("f84c6a79-0a4e-45e0-879b-cd49ebd4c4e2");
//    public static final List<Byte> REQUEST = PacketFormatter.encodePacket(0, username, uuid);

    // NOTE: This is only for testing. Once deployed, the BC provider will be added in the main function
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
            Security.addProvider(new BouncyCastleProvider());
    }


    private final InputStream iStream;
    private final OutputStream oStream;
    // The secrets are only generated if encryption is enabled on the server
    public byte[] sharedSecret;
    public Cipher decryptCipher;
    public Cipher encryptCipher;

    public int compressionThreshold = -1;


    public Login(InputStream iStream, OutputStream oStream) {
        this.iStream = iStream;
        this.oStream = oStream;
    }


    /**
     * A test login function. SHOULD NOT BE USED IN PRODUCTION
     */
    public static Login login(String ip, short port) {
        throw new RuntimeException("Not implemented yet");
        /*
        int protocol = 0;

        // First ping the server to get the protocol version
        try (Socket so = Connect.connect(ip, port)) {
            String status = Status.ping(so);
            JsonObject pingJson = JsonParser.parseString(status).getAsJsonObject();
            protocol = pingJson.get("version").getAsJsonObject().get("protocol").getAsInt();
        } catch (Exception ignored) {}

//        System.out.println("Protocol version: " + protocol);

        // Then try to login
        try (Socket so = Connect.connect(ip, port)) {
            try (
                    OutputStream out = so.getOutputStream();
                    InputStream in = so.getInputStream();
            ) {
                Login login = new Login(in, out);
                login.login(protocol);
                return login;
            } catch (Exception e) { e.printStackTrace();}
        } catch (Exception ignored) {}

        return null;
         */
    }


    /*
    public void login(int protocol)
            throws InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException, IOException, BadPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
        login(protocol, REQUEST, "");
    }
    public void login(int protocol, String username, UUID uuid, String accessToken)
            throws InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException, IOException, BadPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
        login(protocol, PacketFormatter.encodePacket(0, username, uuid), accessToken);
    }
    */
    public void login(int protocol, List<Byte> loginRequest, String accessToken)
            throws IllegalBlockSizeException, IOException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException
    {
        throw new RuntimeException("Not implemented yet");
        /*
        // The login request starts off with the Handshake and Login Start
        // TODO: Add a way to give the IP & Port used for the handshake protocol
        List<Byte> request = PacketFormatter.encodePacket(0, // Handshake Protocol
                protocol, // Minecraft Protocol Version
                "", // Server Address
                (short) 0, // Port
                (byte) 2 // Next State (1: status, 2: login, 3: transfer)
        );
        request.addAll(loginRequest);
        // Write the things to the server
        this.oStream.write(Bytes.toArray(request));


        byte[] packet;

        // Try to get a good response within 10 packets
        loop: for (int i = 0; i < 10; i++) {
            // And get its response
            if (sharedSecret == null)
                packet = PacketUtils.readStream(this.iStream, compressionThreshold);
            else
                packet = PacketUtils.readEncryptedStream(this.iStream, decryptCipher, compressionThreshold).getFirst();

            switch (LoginPacketType.getType(VarInt.decode(packet, 0).get())) {
                case DISCONNECT -> {
                    Disconnect disconnectPacket = Disconnect.decode(packet);
                    System.out.println(disconnectPacket.reason);
                    break loop;
                }
                case COMPRESSION -> {
                    Compression compressionPacket = Compression.decode(packet);
                    this.compressionThreshold = compressionPacket.threshold;
                }
                case ENCRYPTION -> {
                    Encryption encryptionPacket = Encryption.decode(packet);

                    this.sharedSecret = new byte[16];
                    SecureRandom.getInstanceStrong().nextBytes(this.sharedSecret);
                    SecretKeySpec sharedSecretKey = new SecretKeySpec(sharedSecret, "AES");
                    IvParameterSpec sharedSecretIv = new IvParameterSpec(sharedSecret);

                    this.decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
                    this.decryptCipher.init(Cipher.DECRYPT_MODE, sharedSecretKey, sharedSecretIv);
                    this.encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
                    this.encryptCipher.init(Cipher.ENCRYPT_MODE, sharedSecretKey, sharedSecretIv);
                    //System.out.println(Bytes.asList(sharedSecretKey.getEncoded()));
                    //System.out.println(Bytes.asList(sharedSecret));

                    Cipher serverEncryptCipher = Cipher.getInstance("RSA/None/PKCS1Padding", BouncyCastleProvider.PROVIDER_NAME);
                    serverEncryptCipher.init(Cipher.ENCRYPT_MODE, encryptionPacket.publicKey);


                    // ========== Create the response packet ========== //
                    // TODO: Move this out of here
                    ArrayList<Byte> encryptionResponse = new ArrayList<>();
                    // Protocol
                    encryptionResponse.add((byte) 1);

                    // Shared secret
                    byte[] encryptedSharedSecret = serverEncryptCipher.doFinal(sharedSecret);

                    encryptionResponse.addAll(VarInt.encode(encryptedSharedSecret.length));
                    encryptionResponse.addAll(Bytes.asList(encryptedSharedSecret));

                    // Token
                    byte[] encryptedVerifyToken = serverEncryptCipher.doFinal(encryptionPacket.verifyToken);

                    encryptionResponse.addAll(VarInt.encode(encryptedVerifyToken.length));
                    encryptionResponse.addAll(Bytes.asList(encryptedVerifyToken));

                    // Prefix with size
                    encryptionResponse.addAll(0, VarInt.encode(encryptionResponse.size()));

                    this.oStream.write(Bytes.toArray(encryptionResponse));
                }
                case LOGIN_SUCCESS -> {
                    LoginSuccess loginPacket = LoginSuccess.decode(packet);
                    System.out.println(Bytes.asList(packet));
                    break loop;
                }
                case null, default -> { }
            }
        }
        */
    }


    public static void main(String[] args) {
        System.out.println(login("127.0.0.1", (short) 25565));
    }
}
