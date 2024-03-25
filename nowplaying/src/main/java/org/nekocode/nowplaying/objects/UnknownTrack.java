/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.objects;

import javax.swing.*;
import java.util.Collection;
import java.util.Date;

/**
 * This class represents a Track that we've been told about, but we can't seem to find information on it.
 */
public class UnknownTrack implements Track {
    @Override
    public String getAlbum() {
        return null;
    }

    @Override
    public String getArtist() {
        return null;
    }

    @Override
    public Collection<ImageIcon> getArtwork(int size) {
        return null;
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
        return 0;
    }

    @Override
    public String getTitle() {
        return null;
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
