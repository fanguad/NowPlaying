/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.itunes.remote;

import org.nekocode.nowplaying.objects.Track;

import javax.swing.ImageIcon;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * A track that indicates nothing is currently playing.
 */
public class EmptyTrack implements Track {
    @Override
    public String getAlbum() {
        return "fanguad@nekocode.org";
    }

    @Override
    public String getArtist() {
        return "by fanguad";
    }

    @Override
    public Collection<ImageIcon> getArtwork(int size) {
        return Collections.emptySet();
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
        return "NowPlaying...";
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
        return getTrackId() + "";
    }
}
