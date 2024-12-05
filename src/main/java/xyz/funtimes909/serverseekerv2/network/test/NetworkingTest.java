package xyz.funtimes909.serverseekerv2.network.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.CharsetUtil;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class NetworkingTest {
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    public static void main(String[] args) throws Exception {
        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
//                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addFirst(new EchoClientHandler());
//                            p.addLast(new LoggingHandler(LogLevel.INFO));
//                            p.addLast(new EchoClientHandler());
                        }
                    });

            // Start the client.
            ChannelFuture f4 = b.connect(HOST, 25564).sync();
            ChannelFuture f5 = b.connect(HOST, 25565).sync();
            ChannelFuture f6 = b.connect(HOST, 25566).sync();
            ChannelFuture f7 = b.connect(HOST, 25567).sync();
            ChannelFuture f8 = b.connect(HOST, 25568).sync();

            // Wait until the connection is closed.
            f4.channel().closeFuture().sync();
            f5.channel().closeFuture().sync();
            f6.channel().closeFuture().sync();
            f7.channel().closeFuture().sync();
            f8.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}
