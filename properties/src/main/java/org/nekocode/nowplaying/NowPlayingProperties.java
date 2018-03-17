/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Defines the properties that appear in the nowplaying.properties file
 *
 * @author dan.clark@nekocode.org
 */
public enum NowPlayingProperties {
    ALBUM_ART_SIZE,
    TRANSPARENCY,
    TAG_DATABASE,
    MEDIA_PLAYER,
    CORNER_RADIUS,
    MEDIA_PLAYER_GUID,
    REMOTE_MACHINE,
    WINDOW_ANCHOR,
    WINDOW_POSITION,
    ;

    public static final String PROPERTIES_FILE = "nowplaying.properties";
    private static final Logger log = LogManager.getLogger(NowPlayingProperties.class);
    private static Properties properties;

    @NotNull
    public static Properties loadProperties() {
        if (properties == null) {
            properties = new Properties();
            try {
                // first, try to load properties from a file in the current directory
                File propertiesFile = new File(NowPlayingProperties.PROPERTIES_FILE);
                InputStream in;
                if (propertiesFile.exists()) {
                    in = new FileInputStream(propertiesFile);
                } else {
                    // if that doesn't work - try to load it as a resource located at the root of the project
                    in = NowPlayingProperties.class.getResourceAsStream("/" + PROPERTIES_FILE);
                }
                if (in != null) {
                    properties.load(in);
                    in.close();
                } else {
                    log.warn("Unable to load " + PROPERTIES_FILE + " from either file system or resource path");
                }
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
