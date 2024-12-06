package xyz.funtimes909.serverseekerv2.networking;

import xyz.funtimes909.serverseekerv2.Main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Connect {
    public static Socket connect(String address, int port) {
        try {
            // Don't use try-with-resources, socket needs to be used later
            Socket socket = new Socket();
            socket.setSoTimeout(Main.connectionTimeout);
            socket.connect(new InetSocketAddress(address, port), Main.connectionTimeout);
            return socket;
        } catch (IOException e) {
            return null;
        }
    };
}
