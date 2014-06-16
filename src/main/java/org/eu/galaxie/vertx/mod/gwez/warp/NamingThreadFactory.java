package org.eu.galaxie.vertx.mod.gwez.warp;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamingThreadFactory implements ThreadFactory {

    private static final AtomicInteger counter = new AtomicInteger();

    private final String name;

    public NamingThreadFactory(final String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        return new Thread(runnable, name + '-' + counter.getAndIncrement());
    }
}