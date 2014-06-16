package org.eu.galaxie.vertx.mod.gwez.warp.file.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

public class GetFileServerHandler extends ChannelInboundHandlerAdapter {

    private String path = "";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        path += buf.toString(Charset.defaultCharset());
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {

        RandomAccessFile fileToSend;

        try {
            fileToSend = new RandomAccessFile(path, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        ctx.writeAndFlush(Unpooled.buffer(8).writeLong(fileToSend.length()));

        ChunkedNioFile nioFile = new ChunkedNioFile(fileToSend.getChannel());
        ctx.writeAndFlush(nioFile)
                .addListener(ChannelFutureListener.CLOSE)
                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }
}
