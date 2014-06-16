package org.eu.galaxie.vertx.mod.gwez.warp.file.client;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class GetFileClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final String filename;

    private final ByteBuf request;

    private FileChannel destFile;

    private long fileSize = -1;

    private long bytesWritten = 0;

    public GetFileClientHandler(final String filename) {
        super(false);
        this.filename = filename;

        request = Unpooled.buffer(this.filename.length());
        request.writeBytes(filename.getBytes());
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(request);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Inactive");
        if (destFile != null) {
            System.out.println("Closing");
            destFile.close();
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (destFile == null) {
            fileSize = msg.readLong();
            String[] path = filename.split(File.separator);
            destFile = new RandomAccessFile("/tmp/" + path[path.length - 1], "rw").getChannel();
        }

        ByteBuffer buf = msg.nioBuffer();
        while (buf.hasRemaining()) {
            bytesWritten += destFile.write(buf);
        }

        msg.release();
        if (bytesWritten == fileSize) {
            ctx.close();
        }
    }
}
