/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.events;

import org.nekocode.nowplaying.objects.Track;

/**
 * Fired when the currently playing track has changed. This includes changes to
 * the information stored in the current track as well as notification when a
 * different track starts.
 *
 * @author fanguad@nekocode.org
 */
public class TrackChangeEvent {

	public enum ChangeType {
		PLAY_STATE_CHANGE,
		CURRENT_SONG_CHANGE,
		FILE_CHANGE,
		TAG_CHANGE
	}

	private Track track;
	private ChangeType type;

	public TrackChangeEvent(Track track, ChangeType type) {
		this.track = track;
		this.type = type;
	}

	public Track getTrack() {
		return track;
	}

	/**
	 * @return the type
	 */
	public ChangeType getType() {
		return type;
	}
}