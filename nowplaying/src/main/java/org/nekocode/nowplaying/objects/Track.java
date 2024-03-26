/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.objects;

import javax.swing.*;
import java.util.Collection;
import java.util.Date;

/**
 * A playable track.  Note that this can be local or remote, audio or video.
 *
 * @author dan.clark@nekocode.org
 */
public interface Track {

	String getAlbum();

	String getArtist();

	Collection<String> getArtworkDescriptions();
	Collection<ImageIcon> getArtwork(int size);

	String getComment();

	boolean isCompilation();

	String getComposer();

	int getDiscCount();

	int getDiscNumber();

	boolean isEnabled();

	String getGenre();

	String getGrouping();

	int getPlayedCount();

	Date getPlayedDate();

	int getRating();

	String getTitle();

	int getTrackCount();

	int getTrackNumber();

	int getVolumeAdjustment();

	int getYear();

	int getBitRate();

	int getBpm();

	Date getDateAdded();

    /**
     * @return duration of track in seconds
     */
	double getDuration();

	Date getModificationDate();

	int getSampleRate();

	int getSize();

	String getTime();

	/**
	 * Gets the database-specific ID of this track.  This track id can vary between program executions (but not during the same one - probably).
	 * 
	 * @return
	 */
	int getTrackId();

    /**
     * Returns the database this track belongs to.
     *
     * @return
     */
	long getDatabaseId();

	/**
	 * Returns the native data object for this track.
	 * 
	 * @return
	 */
	Object getOriginal();

    String getPersistentId();
}