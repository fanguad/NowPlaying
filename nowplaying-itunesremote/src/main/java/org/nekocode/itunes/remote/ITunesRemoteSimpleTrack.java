/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.itunes.remote;

import org.apache.log4j.Logger;
import org.nekocode.itunes.remote.connection.ITunesRemoteResponse;
import org.nekocode.nowplaying.objects.Track;

import javax.swing.ImageIcon;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static org.nekocode.itunes.remote.connection.ContentCode.*;

/**
 * 
 */
public class ITunesRemoteSimpleTrack implements Track {
    private static final Logger log = Logger.getLogger(ITunesRemoteSimpleTrack.class);
    
    private String title;
    private String artist;
    private String album;
    private String genre;
    /* track length in ms */
    private double trackLength;
    private Collection<ImageIcon> artwork;
    private long databaseId;
    private int trackId;
    private int rating;
    private String persistentId;
    private String grouping;

    public String unknownIfNull(String input) {
        return input == null ? "Unknown" : input;
    }

    public ITunesRemoteSimpleTrack(ITunesRemoteResponse response, long databaseId, String persistentId) {
        // 
        response = response.getBranch(DMCP_STATUS);
        title = unknownIfNull(response.getString(DACP_NOW_PLAYING_NAME));
        artist = unknownIfNull(response.getString(DACP_NOW_PLAYING_ARTIST));
        album = unknownIfNull(response.getString(DACP_NOW_PLAYING_ALBUM));
        genre = unknownIfNull(response.getString(DACP_NOW_PLAYING_GENRE));
        Number number = response.getNumber(DACP_TOTAL_TIME);
        trackLength = number == null ? 0 : number.intValue();
        trackLength /= 1000;
        artwork = Collections.emptySet();

        this.persistentId = persistentId;
        trackId = response.getInt(ITEM_ID);
        this.databaseId = databaseId;
    }

    @Override
    public String getAlbum() {
        return album;
    }

    @Override
    public String getArtist() {
        return artist;
    }

    @Override
    public Collection<ImageIcon> getArtwork(int size) {
        return artwork;
    }

    public void setArtwork(Collection<ImageIcon> artwork) {
        this.artwork = artwork;
    }

    @Override
    public String getComment() {
        return "";
    }

    @Override
    public boolean isCompilation() {
        return false;
    }

    @Override
    public String getComposer() {
        return "";
    }

    @Override
    public int getDiscCount() {
        return 0;
    }

    @Override
    public int getDiscNumber() {
        return 0;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getGenre() {
        return "";
    }

    @Override
    public String getGrouping() {
        return grouping;
    }

    public void setGrouping(String grouping) {
        this.grouping = grouping;
    }

    @Override
    public int getPlayedCount() {
        return 0;
    }

    @Override
    public Date getPlayedDate() {
        return null;
    }

    @Override
    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getTrackCount() {
        return 0;
    }

    @Override
    public int getTrackNumber() {
        return 0;
    }

    @Override
    public int getVolumeAdjustment() {
        return 0;
    }

    @Override
    public int getYear() {
        return 0;
    }

    @Override
    public int getBitRate() {
        return 0;
    }

    @Override
    public int getBpm() {
        return 0;
    }

    @Override
    public Date getDateAdded() {
        return null;
    }

    @Override
    public double getDuration() {
        return trackLength;
    }

    @Override
    public Date getModificationDate() {
        return null;
    }

    @Override
    public int getSampleRate() {
        return 0;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String getTime() {
        return null;
    }

    @Override
    public int getTrackId() {
        return trackId;
    }

    @Override
    public long getDatabaseId() {
        return databaseId;
    }

    @Override
    public Object getOriginal() {
        return null;
    }

    @Override
    public String getPersistentId() {
        return persistentId;
    }
}
