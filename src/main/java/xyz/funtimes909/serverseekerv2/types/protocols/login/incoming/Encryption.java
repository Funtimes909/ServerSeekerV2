package xyz.funtimes909.serverseekerv2.types.protocols.login.incoming;

import io.netty.buffer.ByteBuf;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import xyz.funtimes909.serverseekerv2.types.PacketTypes;
import xyz.funtimes909.serverseekerv2.types.protocols.AbstractProtocol;

import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

public class Encryption implements AbstractProtocol<Encryption> {
    public static final AbstractProtocol<?> INSTANCE = new Encryption();
    @Override
    public int PROTOCOL() { return 1; }

    public String serverID = "";
    public BCRSAPublicKey publicKey = null;
    public byte[] verifyToken = new byte[0];
    public boolean shouldAuth = false;

    public Encryption() {}
    private Encryption(String serverID, BCRSAPublicKey publicKey, byte[] verifyToken, boolean shouldAuth) {
        this.serverID = serverID;
        this.publicKey = publicKey;
        this.verifyToken = verifyToken;
        this.shouldAuth = shouldAuth;
    }


    public static Encryption decode(ByteBuf in) {
        try {
            String serverID = PacketTypes.String.read(in);
            byte[] publicKey = PacketTypes.ByteArray.read(in).array();
            byte[] verifyToken = PacketTypes.ByteArray.read(in).array();

            KeyFactory keyFactory = KeyFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
            BCRSAPublicKey serverPublicKey = (BCRSAPublicKey) keyFactory.generatePublic(keySpec);

            return new Encryption(
                    serverID,
                    serverPublicKey,
                    verifyToken,
                    in.readByte() == 1
            );
        } catch (Exception ignored) { }
        return new Encryption();
    }
}
