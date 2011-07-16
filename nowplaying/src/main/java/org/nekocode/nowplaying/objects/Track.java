/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.objects;

import javax.swing.ImageIcon;
import java.util.Collection;
import java.util.Date;

/**
 * A playable track.  Note that this can be local or remote, audio or video.
 *
 * @author fanguad@nekocode.org
 */
public interface Track {

	public abstract String getAlbum();

	public abstract String getArtist();

	public abstract Collection<ImageIcon> getArtwork(int size);

	public abstract String getComment();

	public abstract boolean isCompilation();

	public abstract String getComposer();

	public abstract int getDiscCount();

	public abstract int getDiscNumber();

	public abstract boolean isEnabled();

	public abstract String getGenre();

	public abstract String getGrouping();

	public abstract int getPlayedCount();

	public abstract Date getPlayedDate();

	public abstract int getRating();

	public abstract String getTitle();

	public abstract int getTrackCount();

	public abstract int getTrackNumber();

	public abstract int getVolumeAdjustment();

	public abstract int getYear();

	public abstract int getBitRate();

	public abstract int getBpm();

	public abstract Date getDateAdded();

    /**
     * @return duration of track in seconds
     */
	public abstract double getDuration();

	public abstract Date getModificationDate();

	public abstract int getSampleRate();

	public abstract int getSize();

	public abstract String getTime();

	/**
	 * Gets the database-specific ID of this track.  This track id can vary between program executions (but not during the same one - probably).
	 * 
	 * @return
	 */
	public abstract int getTrackId();

    /**
     * Returns the database this track belongs to.
     *
     * @return
     */
    public long getDatabaseId();

	/**
	 * Returns the native data object for this track.
	 * 
	 * @return
	 */
	public abstract Object getOriginal();

    public String getPersistentId();

}