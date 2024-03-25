/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

/*
 * Filename:   NamedThreadFactory.java
 * Created On: Apr 9, 2008
 */
package org.nekocode.nowplaying.internals;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * Thread Factory that makes daemon threads only.
 *
 * @author dan.clark@nekocode.org
 */
public class NamedThreadFactory implements ThreadFactory {

    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePattern;
    private final boolean daemon;

    public NamedThreadFactory(String namePrefix, boolean daemon) {
        group = Thread.currentThread().getThreadGroup();
        this.daemon = daemon;
        this.namePattern = namePrefix + "-%s" + (daemon ? " (daemon)" : "");
    }

	@Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,
                              format(namePattern, threadNumber.getAndIncrement()));
        t.setDaemon(daemon);

        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }

}
