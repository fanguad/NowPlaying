/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Defines the properties that appear in the nowplaying.properties file
 *
 * @author fanguad@nekocode.org
 */
public enum NowPlayingProperties {
    ALBUM_ART_SIZE,
    TRANSPARENCY,
//    ART_DIRECTORY,
    TAG_DATABASE,
    MEDIA_PLAYER,
//    MEDIA_PLAYER_PATH,
    CORNER_RADIUS,
    MEDIA_PLAYER_GUID,
    REMOTE_MACHINE,
    ;

    public static final String PROPERTIES_FILE = "nowplaying.properties";
    private static Logger log = Logger.getLogger(NowPlayingProperties.class);
    private static Properties properties;

    public static Properties loadProperties() {
        if (properties == null) {
            properties = new Properties();
            try {
                FileInputStream in = new FileInputStream(NowPlayingProperties.PROPERTIES_FILE);
                properties.load(in);
                in.close();
            } catch (IOException e) {
                log.warn("Could not load " + PROPERTIES_FILE, e);
            }
        }
        return properties;
    }

    public static void storeProperties() {
        if (properties != null) {
            try {
                FileOutputStream out = new FileOutputStream(NowPlayingProperties.PROPERTIES_FILE);
                properties.store(out, null);
                out.close();
            } catch (IOException e) {
                log.warn("Could not store " + PROPERTIES_FILE, e);
            }
        }
    }
}
