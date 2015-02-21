/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.LogManager;

/**
 * Make all the logging in libraries used by NowPlaying... be quiet.
 *
 * @author fanguad@nekocode.org
 */
public final class LogMuter {
	public static void muteLogging() {
		// make jaudiotagger library shut up
//		LogFormatter.getLogger().setLevel(Level.OFF);
        try {
            LogManager.getLogManager().readConfiguration(new ByteArrayInputStream("org.jaudiotagger.level = OFF".getBytes()));
        } catch (IOException e) {
            org.apache.logging.log4j.LogManager.getLogger(LogMuter.class).warn(e);
        }
//      Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING);
//		Logger.getLogger("org.jaudiotagger.tag.id3").setLevel(Level.WARNING);
	}
}
