package org.eu.galaxie.vertx.mod.gwez.warp.file.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import org.eu.galaxie.vertx.mod.gwez.warp.NamingThreadFactory;

import java.util.concurrent.ThreadFactory;

public class FileClient {
    private final String host;
    private final int port;
    private final String filename;

    public FileClient(final String host, final int port,
                      final String filename) {
        this.host = host;
        this.port = port;
        this.filename = filename;
    }

    public void start() throws Exception {
        // Configure the client.
        final ThreadFactory connectFactory = new NamingThreadFactory("connect");

        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(1, connectFactory, NioUdtProvider.BYTE_PROVIDER);

        try {
            final Bootstrap boot = new Bootstrap();

            boot.group(connectGroup)
                    .channelFactory(NioUdtProvider.BYTE_CONNECTOR)
                    .option(ChannelOption.SO_RCVBUF, 1024)
                    .handler(new ChannelInitializer<UdtChannel>() {
                        @Override
                        public void initChannel(final UdtChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(new GetFileClientHandler(filename));
                        }
                    });

            final ChannelFuture f = boot.connect(host, port).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            connectGroup.shutdownGracefully();
        }
    }

    private static void download(final String host, final Integer port, final String desiredPath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new FileClient(host, port, desiredPath).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        download("localhost", 8080, "/server/readable/path/file.sample");
    }
}
