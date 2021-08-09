package org.nekocode.nowplaying.remote.mediamonkey5;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nekocode.nowplaying.objects.Track;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class MM5Track implements Track {
    private static final Logger LOG = LogManager.getLogger(MM5Track.class);
    private final String title;
    private final String artist;
    private final String album;
    private final int rating;

    public static Track getInstance(Map<String, Object> trackProperties)
    {
        try {
            String title = (String) trackProperties.get("title");
            String artist = (String) trackProperties.get("artist");
            String album = (String) trackProperties.get("album");
//        String genre = trackProperties.get("genre");
//        String path = trackProperties.get("path");
//        String custom1 = trackProperties.get("custom1");
//        String custom2 = trackProperties.get("custom2");
//        int idsong = trackProperties.get("idsong"); // also id, persistentID
        int rating = (Integer) trackProperties.get("rating");
//        int songLength = trackProperties.get("songLength");
//        int percentPlayed = trackProperties.get("percentPlayed");
//        int playbackPos = trackProperties.get("playbackPos");
            return new MM5Track(title, artist, album, rating);
        } catch (Exception e) {
            LOG.error("Error parsing property map", e);
        return ErrorTrack.ERROR_TRACK;
        }
    }

    private MM5Track(String title, String artist, String album, int rating) {

        this.title = title;
        this.artist = artist;
        this.album = album;
        this.rating = rating;
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
        return Collections.emptyList();
    }

    @Override
    public String getComment() {
        return null;
    }

    @Override
    public boolean isCompilation() {
        return false;
    }

    @Override
    public String getComposer() {
        return null;
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
        return false;
    }

    @Override
    public String getGenre() {
        return null;
    }

    @Override
    public String getGrouping() {
        return null;
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
        return 0;
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
        return 0;
    }

    @Override
    public long getDatabaseId() {
        return 0;
    }

    @Override
    public Object getOriginal() {
        return null;
    }

    @Override
    public String getPersistentId() {
        return null;
    }
}
