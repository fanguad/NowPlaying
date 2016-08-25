/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
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