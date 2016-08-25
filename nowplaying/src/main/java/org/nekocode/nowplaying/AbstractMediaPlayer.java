/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.events.TrackChangeListener;

/**
 * Implements common functionality for MediaPlayers
 *
 * @author dan.clark@nekocode.org
 */
public abstract class AbstractMediaPlayer implements MediaPlayer {
	private static final Logger log = LogManager.getLogger(AbstractMediaPlayer.class);

	protected final Set<TrackChangeListener> trackChangeListeners;

	public AbstractMediaPlayer() {
		trackChangeListeners = new HashSet<>();
	}
	
	/**
     * Adds a listener to be notified of track changes. This includes both
     * changes to the which track is playing, but also when the track is
     * updated.
     * 
     * @param l
     */
	public void addTrackChangeListener(@NotNull TrackChangeListener l) {
		trackChangeListeners.add(l);
	}

	public void removeTrackChangeListener(@NotNull TrackChangeListener l) {
		trackChangeListeners.remove(l);
    }
	
	protected void fireTrackChanged(@NotNull TrackChangeEvent e) {
		for (TrackChangeListener listener : trackChangeListeners) {
			listener.trackChanged(e);
		}
	}
}
