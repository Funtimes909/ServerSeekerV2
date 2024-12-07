package xyz.funtimes909.serverseekerv2.networking.protocols;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import xyz.funtimes909.serverseekerv2.types.LoginAttempt;
import xyz.funtimes909.serverseekerv2.types.PacketTypes;

import java.net.Socket;
import java.util.Arrays;

public class QuickLogin extends ChannelInboundHandlerAdapter {
    private static final ByteBuf loginPacketSuffix;
    private static final ByteBuf idAndUsername;
    static {
        ByteBuf loginPacketSuffixBuilder = Unpooled.buffer();
        PacketTypes.String.write(loginPacketSuffixBuilder, ":3"); // Server Address
        loginPacketSuffixBuilder.writeShort(0); // Port
        loginPacketSuffixBuilder.writeByte(2);  // Next State (1: status, 2: login, 3: transfer)
        loginPacketSuffix = loginPacketSuffixBuilder.asReadOnly();
//        loginPacketSuffixBuilder.release(); // TODO: Do I need to release?

        ByteBuf idAndUsernameBuilder = Unpooled.buffer();
        idAndUsernameBuilder.writeByte(0); // Packet ID (all versions use 0)
        PacketTypes.String.write(idAndUsernameBuilder, Login.username); // Username
        idAndUsername = idAndUsernameBuilder.asReadOnly();
//        idAndUsernameBuilder.release(); // TODO: Do I need to release?
    }

    private final ByteBuf REQUEST_HANDSHAKE;
    private final ByteBuf REQUEST_LOGIN_START;

    public QuickLogin(int protocol) {
        // Handshake Packet //
        ByteBuf handshake = Unpooled.buffer();
        handshake.writeByte(0); // Protocol ID
        PacketTypes.VarInt.write(handshake, protocol); // Minecraft Protocol
        handshake.writeBytes(loginPacketSuffix); // Rest of the packet
        this.REQUEST_HANDSHAKE = handshake.asReadOnly();

        // Login Packet //
        ByteBuf login = Unpooled.buffer();
        if (protocol >= 764) { // 1.20.2 (to latest)
            login.writeBytes(Login.REQUEST); // Modern login request
        } else if (protocol >= 761) { // 1.19.3 (to 1.20.1)
            PacketTypes.VarInt.write(login, idAndUsername.capacity() + 1); // Size
            login.writeBytes(idAndUsername); // ID & Username
            login.writeByte(0x00); // Weather the uuid is encoded (we get a smaller packet if we don't ;)
        } else if (protocol >= 759) { // 1.19 (to 1.19.2)
            PacketTypes.VarInt.write(login, idAndUsername.capacity() + 1 + (protocol == 760? 1: 0)); // Size
            login.writeBytes(idAndUsername); // ID & Username
            login.writeByte(0x00); // Weather we should encode lots of random stuff (don't bother)

            if (protocol == 760) // 1.19.2
                login.writeByte(0x00); // Weather we should encode the uuid (smaller if we don't)
        } else {
            PacketTypes.VarInt.write(login, idAndUsername.capacity()); // Size
            login.writeBytes(idAndUsername); // ID & Username
        }
        this.REQUEST_LOGIN_START = login.asReadOnly();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
//        System.out.println(Arrays.toString(REQUEST.copy().array()));
        ctx.write(REQUEST_HANDSHAKE.copy());
        ctx.writeAndFlush(REQUEST_LOGIN_START.copy());
//        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        int protocol = PacketTypes.VarInt.read(in);
        System.out.println(protocol);
        if (protocol == 0 || protocol == 1 || protocol == 2)
            ctx.close();
    }

    /** A really rudimentary login method that can check some basic stats about the server */
    public static LoginAttempt quickLogin(Socket so, int protocol) {
        throw new RuntimeException("Not implemented yet");

        /*
        try (
                OutputStream out = so.getOutputStream();
                InputStream in = so.getInputStream();
        ) {
            // The login request starts off with the Handshake and Login Start
            List<Byte> request = VarInt.encode(protocol); // Minecraft Protocol Version
            request.addAll(loginPacketSuffix); // The rest of the packet
            request.addFirst((byte) 0); // Protocol ID
            request.addAll(0, VarInt.encode(request.size())); // Packet size

            if (protocol >= 764) { // 1.20.2 (to latest)
                request.addAll(Login.REQUEST); // Modern login request
            } else if (protocol >= 761) { // 1.19.3 (to 1.20.1)
                request.addAll(VarInt.encode(idAndUsername.size() + 1)); // Size
                request.addAll(idAndUsername); // ID & Username
                request.add((byte) 0x00); // Weather the uuid is encoded (we get a smaller packet if we don't ;)
            } else if (protocol >= 759) { // 1.19 (to 1.19.2)
                request.addAll(VarInt.encode(idAndUsername.size() + 1 + (protocol == 760? 1: 0))); // Size
                request.addAll(idAndUsername); // ID & Username
                request.add((byte) 0x00); // Weather we should encode lots of random stuff (don't bother)

                if (protocol == 760) // 1.19.2
                    request.add((byte) 0x00); // Weather we should encode the uuid (smaller if we don't)
            } else {
                request.addAll(VarInt.encode(idAndUsername.size())); // Size
                request.addAll(idAndUsername); // ID & Username
            }
            // Write the things to the server
            out.write(Bytes.toArray(request));


            int compressionThreshold = -1;
            byte[] packet;

            // Try to get a good response within 5 packets
            loop: for (int i = 0; i < 5; i++) {
                // And get its response
                packet = PacketUtils.readStream(in, compressionThreshold);

                switch (packet[0]) {
                    // Disconnect
                    case 0: return LoginAttempt.ONLY_WHITELIST;
                    // Encryption
                    case 1: {
                        if (protocol >= 766 && packet[packet.length - 1] != 1) {
                            // In 1.20.5 they allowed having encryption and online status to be separate things,
                            // so now the final byte (bool) represents weather it's in online or offline mode (client auth?)
                            return LoginAttempt.OFFLINE;
                        }
                        // Else it is expected that encryption means the server is in online mode
                        return LoginAttempt.ONLINE;
                    }
                    // Success
                    case 2: return LoginAttempt.INSECURE;
                    // Compression
                    case 3: compressionThreshold = VarInt.decode(packet, 1).get();
                }
            }
        } catch (Exception ignored) { }
        return LoginAttempt.UNKNOWN;
        */
    }



    public static void main(String[] args) {
        throw new RuntimeException("Not implemented yet");
        /*
        String ip = "127.0.0.1";
        int port = 25565;
        int protocol = 0;

        // First ping the server to get the protocol version
        try (Socket so = Connect.connect(ip, port)) {
            String status = Handshake.ping(so);
            JsonObject pingJson = JsonParser.parseString(status).getAsJsonObject();
            protocol = pingJson.get("version").getAsJsonObject().get("protocol").getAsInt();
        } catch (Exception ignored) {}
        // Then try to login
        try (Socket so = Connect.connect(ip, port)) {
            System.out.println(quickLogin(so, protocol));
        } catch (Exception ignored) {}
         */
    }
}
