/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Properties;

/**
 * Defines the properties that appear in the nowplaying.properties file
 *
 * TODO make me JSON with specific configuration for each remote model
 *
 * @author dan.clark@nekocode.org
 */
@Log4j2
public enum NowPlayingProperties {
    ALBUM_ART_SIZE,
    TRANSPARENCY,
    TAG_DATABASE,
    MEDIA_PLAYER,
    CORNER_RADIUS,
    MEDIA_PLAYER_GUID,
    REMOTE_MACHINE,
    REMOTE_PORT,
    WINDOW_ANCHOR,
    WINDOW_POSITION,
    ;

    public static final String PROPERTIES_FILE = "nowplaying.properties";
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
