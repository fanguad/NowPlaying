/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nekocode.nowplaying.events.TrackChangeListener;
import org.nekocode.nowplaying.objects.Playlist;
import org.nekocode.nowplaying.objects.Track;

import java.util.List;

/**
 * Represents the base application.  Provides an interface into the common
 * functionality of all media players.
 *
 * @author dan.clark@nekocode.org
 */
public interface MediaPlayer {

	enum PlayerState {
		PLAYING, STOPPED
	}

	/**
	 * Gets the currently playing (or paused) track.
	 *
	 * @return current track, or null if player is not connected or stopped
	 */
	@Nullable
	Track getCurrentTrack();

	/**
	 * Register a listener for changes to the current playing track.  This
	 * includes changes to the information stored in the current track as well
	 * as notification when a different track starts.
     *
     * @param l track change listener
     */
	void addTrackChangeListener(@NotNull TrackChangeListener l);

	/**
	 * Remove an already registered change listener.  If the listener is not
	 * registered, does nothing.
     * 
     * @param l track change listener
	 */
	void removeTrackChangeListener(@NotNull TrackChangeListener l);

	void play();

	void pause();

	void next();

	void previous();

	/**
	 * This method should be called before this object is destroyed.
	 */
	void onShutdown();

	/**
	 * Assigns a new rating to the specified track.  newRating is on a scale
	 * from 0-100, but the MediaPlayer is free to scale this to its own internal
	 * representation.
	 *
	 * @param track		track to update
	 * @param newRating	new rating of track
	 */
	void updateTrackRating(@NotNull Track track, int newRating);

	/**
	 * Returns the position in the current track in seconds.
	 *
	 * @return position in current track.
	 */
	double getCurrentTrackPosition();

    Playlist getCurrentPlaylist();

	@Nullable
	Track getTrack(int trackId);

	/**
	 * Indicates whether a track is currently playing or not.
	 *
	 * @return current state of media player
	 */
	@NotNull
	PlayerState getPlayerState();

    /**
     * Finds tracks matching the specified input.
     *
     * @return list of tracks that match the search parameters
     */
	@NotNull
    List<Track> findTracks(@Nullable String title, @Nullable String artist, @Nullable String album);

    /**
     * Finds tracks matching the specified input.
     *
     * @return list of track persistent ids that match the search parameters
     */
	@NotNull
    List<String> findTrackIds(@Nullable String title, @Nullable String artist, @Nullable String album);
}
