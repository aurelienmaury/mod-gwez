package org.eu.galaxie.vertx.mod.gwez.warp.message.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.udt.UdtMessage;
import io.netty.channel.udt.nio.NioUdtProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by amaury on 04/10/13.
 */
public class MessageEchoClientHandler extends SimpleChannelInboundHandler<UdtMessage> {
    private static final Logger log = Logger.getLogger(MessageEchoClientHandler.class.getName());

    private final UdtMessage message;

    public MessageEchoClientHandler(final int messageSize) {
        super(false);
        final ByteBuf byteBuf = Unpooled.buffer(messageSize);
        for (int i = 0; i < byteBuf.capacity(); i++) {
            byteBuf.writeByte((byte) i);
        }
        message = new UdtMessage(byteBuf);
    }


    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        log.info("ECHO active " + NioUdtProvider.socketUDT(ctx.channel()).toStringOptions());
        ctx.writeAndFlush(message);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
                                final Throwable cause) {
        log.log(Level.WARNING, "close the connection when an exception is raised", cause);
        ctx.close();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, UdtMessage msg) throws Exception {
        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
