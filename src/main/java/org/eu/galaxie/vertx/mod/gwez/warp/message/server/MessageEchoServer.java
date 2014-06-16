package org.eu.galaxie.vertx.mod.gwez.warp.message.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.eu.galaxie.vertx.mod.gwez.warp.NamingThreadFactory;

import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

/**
 * Created by amaury on 04/10/13.
 */
public class MessageEchoServer {
    private static final Logger log = Logger.getLogger(MessageEchoServer.class.getName());

    private final int port;

    public MessageEchoServer(final int port) {
        this.port = port;
    }

    public void run() throws Exception {
        final ThreadFactory acceptFactory = new NamingThreadFactory("accept");
        final ThreadFactory connectFactory = new NamingThreadFactory("connect");
        final NioEventLoopGroup acceptGroup = new NioEventLoopGroup(1,
                acceptFactory, NioUdtProvider.MESSAGE_PROVIDER);
        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(1,
                connectFactory, NioUdtProvider.MESSAGE_PROVIDER);
        // Configure the server.
        try {
            final ServerBootstrap boot = new ServerBootstrap();
            boot.group(acceptGroup, connectGroup)
                    .channelFactory(NioUdtProvider.MESSAGE_ACCEPTOR)
                    .option(ChannelOption.RCVBUF_ALLOCATOR.SO_BACKLOG, 10)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<UdtChannel>() {
                        @Override
                        public void initChannel(final UdtChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new LoggingHandler(LogLevel.INFO),
                                    new MessageEchoServerHandler());
                        }
                    });
            // Start the server.
            final ChannelFuture future = boot.bind(port).sync();
            // Wait until the server socket is closed.
            future.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            acceptGroup.shutdownGracefully();
            connectGroup.shutdownGracefully();
        }
    }

    public static void main(final String[] args) throws Exception {
        log.info("init");

        final int port = 1234;

        new MessageEchoServer(port).run();

        log.info("done");
    }
}
