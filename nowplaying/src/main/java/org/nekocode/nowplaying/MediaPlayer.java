/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying;

import org.nekocode.nowplaying.events.TrackChangeListener;
import org.nekocode.nowplaying.objects.Playlist;
import org.nekocode.nowplaying.objects.Track;

import java.util.List;
import java.util.Set;

/**
 * Represents the base application.  Provides an interface into the common
 * functionality of all media players.
 *
 * @author fanguad@nekocode.org
 */
public interface MediaPlayer {

	/**
	 * Gets the currently playing (or paused) track.
	 *
	 * @return current track
	 */
	public Track getCurrentTrack();

	/**
	 * Register a listener for changes to the current playing track.  This
	 * includes changes to the information stored in the current track as well
	 * as notification when a different track starts.
     *
     * @param l track change listener
     */
	public void addTrackChangeListener(TrackChangeListener l);

	/**
	 * Remove an already registered change listener.  If the listener is not
	 * registered, does nothing.
     * 
     * @param l track change listener
	 */
	public void removeTrackChangeListener(TrackChangeListener l);

	public void play();

	public void pause();

	public void next();

	public void previous();

	/**
	 * This method should be called before this object is destroyed.
	 */
	public void onShutdown();

	/**
	 * Assigns a new rating to the specified track.  newRating is on a scale
	 * from 0-100, but the MediaPlayer is free to scale this to its own internal
	 * representation.
	 *
	 * @param track		track to update
	 * @param newRating	new rating of track
	 */
	public void updateTrackRating(Track track, int newRating);

	/**
	 * Returns the position in the current track in seconds.
	 *
	 * @return position in current track.
	 */
	double getCurrentTrackPosition();

    Playlist getCurrentPlaylist();

    static enum PlayerState {
		PLAYING, STOPPED
	}

	/**
	 * Indicates whether a track is currently playing or not.
	 *
	 * @return current state of media player
	 */
	PlayerState getPlayerState();

    /**
     * Finds tracks matching the specified input.
     *
     * @param title
     * @param artist
     * @param album
     * @return list of tracks that match the search parameters
     */
    List<Track> findTracks(String title, String artist, String album);

    /**
     * Finds tracks matching the specified input.
     *
     * @return list of track persistent ids that match the search parameters
     */
    List<String> findTrackIds(String title, String artist, String album);
}
