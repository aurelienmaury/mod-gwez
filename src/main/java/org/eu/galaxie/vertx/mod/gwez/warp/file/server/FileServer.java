package org.eu.galaxie.vertx.mod.gwez.warp.file.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.eu.galaxie.vertx.mod.gwez.warp.NamingThreadFactory;

public class FileServer {

    private int port;

    public FileServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        final NioEventLoopGroup acceptGroup = createNioEventLoopGroup("accept");
        final NioEventLoopGroup connectGroup = createNioEventLoopGroup("connect");

        // Configure the server.
        try {
            final ServerBootstrap boot = new ServerBootstrap();

            boot.group(acceptGroup, connectGroup)
                    .channelFactory(NioUdtProvider.BYTE_ACCEPTOR)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<UdtChannel>() {
                        @Override
                        public void initChannel(final UdtChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new ChunkedWriteHandler(),
                                    new GetFileServerHandler()
                            );
                        }
                    });
            // Start the server.
            final ChannelFuture f = boot.bind(port).sync();
            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            acceptGroup.shutdownGracefully();
            connectGroup.shutdownGracefully();
        }
    }

    private NioEventLoopGroup createNioEventLoopGroup(final String name) {
        return new NioEventLoopGroup(1, new NamingThreadFactory(name), NioUdtProvider.BYTE_PROVIDER);
    }
}
