/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.events;

import org.nekocode.nowplaying.objects.Track;

import org.jetbrains.annotations.NotNull;

/**
 * Fired when the currently playing track has changed. This includes changes to
 * the information stored in the current track as well as notification when a
 * different track starts.
 *
 * @author dan.clark@nekocode.org
 */
public class TrackChangeEvent {

	public enum ChangeType {
		PLAY_STATE_CHANGE,
		CURRENT_SONG_CHANGE,
		FILE_CHANGE,
		/**
		 * Non-art metadata has changed
		 */
		METADATA_CHANGE,
		TAG_CHANGE,
		/**
		 * Art for the current track became available.  Track did not actually change
		 */
		ART_CHANGE,
	}

	@NotNull
	private final Track track;
	@NotNull
	private final ChangeType type;

	public TrackChangeEvent(@NotNull Track track, @NotNull ChangeType type) {
		this.track = track;
		this.type = type;
	}

	@NotNull
	public Track getTrack() {
		return track;
	}

	/**
	 * @return the type
	 */
	@NotNull
	public ChangeType getType() {
		return type;
	}
}