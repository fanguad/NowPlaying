/*
 * Copyright (c) 2010, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.objects;

import java.io.File;

/**
 * A track that represents a file on disk.  It can be either audio or video.
 *
 * @author fanguad@nekocode.org
 */
public interface FileTrack extends Track {
	/**
     * The full path to the file represented by this track.
     */
    File getLocation();
}
