/*
 * Copyright (c) 2010, fanguad@nekocode.org
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
            org.apache.log4j.Logger.getLogger(LogMuter.class).warn(e);
        }
//      Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING);
//		Logger.getLogger("org.jaudiotagger.tag.id3").setLevel(Level.WARNING);
	}
}
