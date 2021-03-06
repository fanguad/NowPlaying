/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.objects;

import java.io.File;

/**
 * A track that represents a file on disk.  It can be either audio or video.
 *
 * @author dan.clark@nekocode.org
 */
public interface FileTrack extends Track {
	/**
     * The full path to the file represented by this track.
     */
    File getLocation();
}
