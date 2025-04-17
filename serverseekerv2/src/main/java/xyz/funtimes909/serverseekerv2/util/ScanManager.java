package xyz.funtimes909.serverseekerv2.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import xyz.funtimes909.serverseekerv2.Main;
import xyz.funtimes909.serverseekerv2.builders.Masscan;
import xyz.funtimes909.serverseekerv2.network.Connect;
import xyz.funtimes909.serverseekerv2.network.protocols.Handshake;
import xyz.funtimes909.serverseekerv2.network.protocols.QuickLogin;
import xyz.funtimes909.serverseekerv2_core.database.Database;
import xyz.funtimes909.serverseekerv2_core.records.Server;
import xyz.funtimes909.serverseekerv2_core.types.LoginAttempt;
import xyz.funtimes909.serverseekerv2_core.util.MotdUtils;
import xyz.funtimes909.serverseekerv2_core.util.ServerObjectBuilder;

import java.net.Socket;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ScanManager {
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static final Semaphore lock = new Semaphore(2500);
    public static int size;

    public static void scan() {
        List<Masscan> serverList = MasscanUtils.parse(Main.masscanOutput);
        if (serverList == null) return;
        size = serverList.size();

        ProgressBarStyle style = ProgressBarStyle.builder()
                .leftBracket("[")
                .rightBracket("]")
                .block('=')
                .rightSideFractionSymbol('>')
                .build();

        ProgressBarBuilder bar = new ProgressBarBuilder()
                .clearDisplayOnFinish()
                .setStyle(style)
                .showSpeed()
                .setTaskName("Scanning")
                .continuousUpdate()
                .setInitialMax(size);

        for (Masscan server : ProgressBar.wrap(serverList, bar)) {
            Runnable task = () -> {
                try {
                    String address = server.ip();
                    short port = server.ports().getFirst().port();

                    JsonObject parsedJson;
                    try (Socket so = Connect.connect(address, port)) {
                        String json = Handshake.ping(so);
                        parsedJson = JsonParser.parseString(json).getAsJsonObject();

                        // Build MOTD as soon as server responds to see if user has opted out of scanning
                        if (parsedJson.has("description")) {
                            StringBuilder motd = new StringBuilder();

                            if (parsedJson.get("description").isJsonObject()) {
                                MotdUtils.buildMOTD(parsedJson.get("description").getAsJsonObject(), 10, motd);
                            } else {
                                motd.append(parsedJson.get("description").getAsString());
                            }

                            // Opt-out code
                            if (motd.toString().contains("§b§d§f§d§b")) return;
                        }
                    }

                    // Servers close connection after handshake, we need to make a new socket
                    LoginAttempt loginAttempt;
                    try (Socket so = Connect.connect(address, port)) {
                        loginAttempt = QuickLogin.quickLogin(
                                so,
                                // Get the protocol version of the server from the handshake
                                parsedJson.get("version").getAsJsonObject().get("protocol").getAsInt()
                        );
                    } // Even if the login method failed, still log the rest of the info

                    // Build server using server ping and the loginAttempt
                    Server builtServer = ServerObjectBuilder.buildServerFromPing(
                            address,
                            port,
                            parsedJson,
                            loginAttempt
                    );

                    if (builtServer == null) return;

                    Connection conn = ConnectionPool.getConnection();
                    Database.updateServer(conn, builtServer);
                } catch (Exception ignored) {
                } finally {
                    lock.release();
                    size -= 1;
                }
            };
            lock.acquireUninterruptibly();
            executor.execute(task);
        }
    }
}