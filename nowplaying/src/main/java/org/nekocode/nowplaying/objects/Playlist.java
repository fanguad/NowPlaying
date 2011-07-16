/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.objects;

import java.util.List;

/**
 * Interface for playlist.
 *
 * @author dclark
 * @since Jun 15, 2010
 */
public interface Playlist {
    
    public List<Track> getTracks();

    public int count();
}
