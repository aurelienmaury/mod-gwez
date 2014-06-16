package org.eu.galaxie.vertx.mod.gwez.warp.message.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
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
public class MessageEchoClient {
    private static final Logger log = Logger.getLogger(MessageEchoClient.class.getName());

    private final String host;
    private final int port;
    private final int messageSize;

    public MessageEchoClient(final String host, final int port,
                         final int messageSize) {
        this.host = host;
        this.port = port;
        this.messageSize = messageSize;
    }

    public void run() throws Exception {
        // Configure the client.
        final ThreadFactory connectFactory = new NamingThreadFactory("connect");
        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(1,
                connectFactory, NioUdtProvider.MESSAGE_PROVIDER);
        try {
            final Bootstrap boot = new Bootstrap();
            boot.group(connectGroup)
                    .channelFactory(NioUdtProvider.MESSAGE_CONNECTOR)
                    .handler(new ChannelInitializer<UdtChannel>() {
                        @Override
                        public void initChannel(final UdtChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new LoggingHandler(LogLevel.INFO),
                                    new MessageEchoClientHandler(messageSize));
                        }
                    });
            // Start the client.
            final ChannelFuture f = boot.connect(host, port).sync();
            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            connectGroup.shutdownGracefully();
        }
    }

    public static void main(final String[] args) throws Exception {
        log.info("init");


        final String host = "localhost";

        final int port = 1234;
        final int messageSize = 64 * 1024;

        new MessageEchoClient(host, port, messageSize).run();

        log.info("done");
    }
}
