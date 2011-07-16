/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.events.TrackChangeListener;

/**
 * Implements common functionality for MediaPlayers
 *
 * @author fanguad@nekocode.org
 */
public abstract class AbstractMediaPlayer implements MediaPlayer {
	private static final Logger log = Logger.getLogger(AbstractMediaPlayer.class);

	protected Set<TrackChangeListener> trackChangeListeners;

	public AbstractMediaPlayer() {
		trackChangeListeners = new HashSet<TrackChangeListener>();
	}
	
	/**
     * Adds a listener to be notified of track changes. This includes both
     * changes to the which track is playing, but also when the track is
     * updated.
     * 
     * @param l
     */
	public void addTrackChangeListener(TrackChangeListener l) {
		trackChangeListeners.add(l);
	}

	public void removeTrackChangeListener(TrackChangeListener l) {
		trackChangeListeners.remove(l);
    }
	
	protected void fireTrackChanged(TrackChangeEvent e) {
		for (TrackChangeListener listener : trackChangeListeners) {
			listener.trackChanged(e);
		}
	}
}
