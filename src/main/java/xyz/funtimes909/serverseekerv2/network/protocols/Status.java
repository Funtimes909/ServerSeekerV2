package xyz.funtimes909.serverseekerv2.network.protocols;

import com.google.common.primitives.Bytes;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import xyz.funtimes909.serverseekerv2.Main;
import xyz.funtimes909.serverseekerv2.network.Connect;
import xyz.funtimes909.serverseekerv2.network.PacketUtils;
import xyz.funtimes909.serverseekerv2.types.PacketTypes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;


public class Status extends ChannelInboundHandlerAdapter {
    public static final ByteBuf REQUEST;
    static {
        ByteBuf req = Unpooled.buffer();
        // Handshake Request
        ByteBuf handshakeBuff = Unpooled.buffer();
        PacketTypes.VarInt.write(handshakeBuff, 0); // Packet Type
        PacketTypes.VarInt.write(handshakeBuff, 0); // Minecraft Protocol Version
        PacketTypes.String.write(handshakeBuff, ":3"); // Server Address
        handshakeBuff.writeShort(0); // Port
        handshakeBuff.writeByte(1); // Next State (1: status, 2: login, 3: transfer)

        // Prefix with size
        PacketTypes.VarInt.write(req, handshakeBuff.writerIndex());
        req.writeBytes(handshakeBuff);

        // Status Request
        req.writeByte(1); // Size
        req.writeByte(0); // ID

        REQUEST = req.asReadOnly();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(REQUEST.copy());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        try {
            ByteBuf in = PacketUtils.read((ByteBuf) msg);
            int protocol = PacketTypes.VarInt.read(in);
            String json = PacketTypes.String.read(in);
            System.out.println(json);
        } finally {
            ctx.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        Main.logger.error("Netty status exception", e);
        // Close the connection when an exception is raised.
        ctx.close();
    }






    public static void main(String[] args) throws Exception {
        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
//                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new Status());
//                            p.addLast(new LoggingHandler(LogLevel.INFO));
                        }
                    });

            final String HOST = "127.0.0.1";

            // Start the client.
//            ChannelFuture f4 = b.connect(HOST, 25564).sync();
//            ChannelFuture f5 = b.connect(HOST, 25565).sync();
//            ChannelFuture f6 = b.connect(HOST, 25566).sync();
//            ChannelFuture f7 = b.connect(HOST, 25567).sync();
//            ChannelFuture f8 = b.connect(HOST, 25568).sync();

            // Wait until the connection is closed.
//            f4.channel().closeFuture().sync();
//            f5.channel().closeFuture().sync();
//            f6.channel().closeFuture().sync();
//            f7.channel().closeFuture().sync();
//            f8.channel().closeFuture().sync();

            // A slightly different way of doing it (which is slower...)
            b.connect(HOST, 25564).channel().closeFuture().sync();
            b.connect(HOST, 25565).channel().closeFuture().sync();
            b.connect(HOST, 25566).channel().closeFuture().sync();
            b.connect(HOST, 25567).channel().closeFuture().sync();
            b.connect(HOST, 25568).channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}