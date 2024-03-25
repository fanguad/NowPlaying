/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.events;

import org.jetbrains.annotations.NotNull;
import org.nekocode.nowplaying.objects.Track;

/**
 * Listens for changes to tags on tracks.
 *
 * @author dan.clark@nekocode.org
 */
public interface TagChangeListener
{
    void tagRemoved(@NotNull Track track, @NotNull String tag);
    void tagAdded(@NotNull Track track, @NotNull String tag);

    /**
     * Called if the tags change, but only if {@link #tagAdded(org.nekocode.nowplaying.objects.Track, String)} or
     * {@link #tagRemoved(org.nekocode.nowplaying.objects.Track, String)} is not applicable.
     *
     * @param track track whose tags changed
     */
    void tagsChanged(@NotNull Track track);
}
