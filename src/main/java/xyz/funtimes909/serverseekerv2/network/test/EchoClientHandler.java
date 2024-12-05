package xyz.funtimes909.serverseekerv2.network.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import xyz.funtimes909.serverseekerv2.network.protocols.Handshake;
import xyz.funtimes909.serverseekerv2.types.varlen.VarInt;
import xyz.funtimes909.serverseekerv2.types.varlen.VarString;

/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class EchoClientHandler extends ChannelInboundHandlerAdapter {

    private final ByteBuf firstMessage;

    /**
     * Creates a client-side handler.
     */
    public EchoClientHandler() {
//        firstMessage = Unpooled.buffer(Handshake.REQUEST.length);
//        for (byte b: Handshake.REQUEST) {
//            firstMessage.writeByte(b);
//        }
        firstMessage = Unpooled.copiedBuffer(Handshake.REQUEST);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Active");
        ctx.writeAndFlush(firstMessage);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("Read");
        try {
            ByteBuf in = (ByteBuf) msg;
            int size = VarInt.read(in);
            ByteBuf packet = in.readBytes(size);
            int packetType = VarInt.read(packet);
            String json = VarString.read(packet);
            System.out.println(json);
        } finally {
            ReferenceCountUtil.release(msg);
        }
//        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("Flushed");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("Exception");
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}