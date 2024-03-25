/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.events;

import org.jetbrains.annotations.NotNull;

/**
 * Class with which to register for notifications about changes to the currently
 * playing track.
 *
 * @author dan.clark@nekocode.org
 */
public interface TrackChangeListener {
   void trackChanged(@NotNull TrackChangeEvent e);
}