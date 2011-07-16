/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.events;

/**
 * Class with which to register for notifications about changes to the currently
 * playing track.
 *
 * @author fanguad@nekocode.org
 */
public interface TrackChangeListener {
   public void trackChanged(TrackChangeEvent e);
}