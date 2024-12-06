package xyz.funtimes909.serverseekerv2.networking.protocols;

import xyz.funtimes909.serverseekerv2.types.LoginAttempt;

import java.net.Socket;

public class QuickLogin {
//    private static final List<Byte> loginPacketSuffix;
    static {
//        List<Byte> ba = PacketFormatter.encode(
//                "", // Server Address
//                (short) 0, // Port
//                (byte) 2 // Next State (1: status, 2: login, 3: transfer)
//        );
//        // Login Request
//        ba.addAll(Login.REQUEST);
//        loginPacketSuffix = ba;
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
            request.addAll(0, VarInt.encode(request.size() - Login.REQUEST.size())); // Rest of the packet
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
                        if (protocol >= 766) {
                            // In 1.20.5 they allowed having encryption and online status to be separate things
                            return
                                    // The final byte (bool) represents weather it's in online or offline mode (client auth?)
                                    packet[packet.length - 1] == 1 ?
                                            LoginAttempt.ONLINE :
                                            LoginAttempt.OFFLINE;
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
            String status = Status.ping(so);
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
