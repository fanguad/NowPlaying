/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.objects;

import java.util.List;

/**
 * Interface for playlist.
 *
 * @author dan.clark@nekocode.org
 * @since Jun 15, 2010
 */
public interface Playlist {
    
    List<Track> getTracks();

    int count();
}
