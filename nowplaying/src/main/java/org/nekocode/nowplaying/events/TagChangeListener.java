/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.events;

import org.nekocode.nowplaying.objects.Track;

/**
 * Listens for changes to tags on tracks.
 *
 * @author fanguad@nekocode.org
 */
public interface TagChangeListener
{
    void tagRemoved(Track track, String tag);
    void tagAdded(Track track, String tag);

    /**
     * Called if the tags change, but only if {@link #tagAdded(org.nekocode.nowplaying.objects.Track, String)} or
     * {@link #tagRemoved(org.nekocode.nowplaying.objects.Track, String)} is not applicable.
     *
     * @param track track whose tags changed
     */
    void tagsChanged(Track track);
}
