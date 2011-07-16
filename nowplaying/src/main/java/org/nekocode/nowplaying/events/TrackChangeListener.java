/*
 * Copyright (c) 2010, fanguad@nekocode.org
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