/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import org.jetbrains.annotations.NotNull;
import org.nekocode.nowplaying.events.TagChangeListener;
import org.nekocode.nowplaying.objects.Track;
import org.nekocode.nowplaying.tags.TagModel;
import org.nekocode.nowplaying.tags.cloud.TagCloudEntry;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * A table model that knows how to display useful information about Tracks.
 */
public class TrackTableModel extends AbstractTableModel implements TagChangeListener {
    static enum Columns {
        FoundInMediaPlayer, Title, Artist, Album, Track, Tags
    }
    private TagModel tagModel;
    private List<Track> tracks;
    private Map<Track, SortedSet<String>> tagCache;

    public TrackTableModel(TagModel tagModel) {
        this.tagModel = tagModel;
        tracks = new ArrayList<>();
        tagCache = new WeakHashMap<>();
    }

    public List<Track> getTracks() {
        return Collections.unmodifiableList(tracks);
    }

    public void addTrack(Track newTrack) {
        if (!tracks.contains(newTrack)) {
            int rowIndex = tracks.size();
            tracks.add(newTrack);
            fireTableRowsInserted(rowIndex, rowIndex);
        }
    }

    public void setValues(List<Track> newTracks) {
        tracks.clear();
        tagCache.clear();
        tracks.addAll(newTracks);
        fireTableDataChanged();
    }

    @Override
    public void fireTableDataChanged() {
        tagCache.clear();
        super.fireTableDataChanged();
    }

    public void clear() {
        tracks.clear();
        tagCache.clear();
        fireTableDataChanged();
    }

    /**
     * Delete the entries at the specified row indices.
     *
     * @param rows row indices
     */
    public void deleteRows(int[] rows) {
        List<Track> tracksToDelete = new ArrayList<>();
        for (int row : rows) {
            tracksToDelete.add(tracks.get(row));
            tagCache.remove(tracks.get(row));
        }
        tracks.removeAll(tracksToDelete);
        fireTableDataChanged();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            // column zero is special - it will only be an icon so don't want
            // to waste space with a gigantic name
            return "";
        }
        return Columns.values()[column].name();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    @Override
    public int getColumnCount() {
        return Columns.values().length;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount() {
        return tracks.size();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Track track = tracks.get(rowIndex);

        String value = "--INVALID--";
        switch (Columns.values()[columnIndex]) {
            case FoundInMediaPlayer:
                // we've already handled the case where it isn't found
                return track.getPersistentId() != null;
            case Album:
                value = track.getAlbum();
                break;
            case Artist:
                value = track.getArtist();
                break;
            case Title:
                value = track.getTitle();
                break;
            case Track:
                int number = track.getTrackNumber();
                int max = track.getTrackCount();
                if (number > 0){
                    if (max > 0) {
                        value = String.format("%d/%d", number, max);
                    } else {
                        value = String.format("%d", number);
                    }
                } else {
                    value = "";
                }

                break;
            case Tags:
                value = getTags(track);
                break;
        }
        if (value == null) {
            value = "";
        }

        return value;
    }

    /**
     * Loads the tags for a particular track, accessing the tagModel only if necessary.
     *
     * @param track track to get tags for
     * @return a string containing all the tags for the track
     */
    private String getTags(Track track) {
        if (track.getPersistentId() == null) {
            return "";
        } else if (!tagCache.containsKey(track)) {
            // load tags from the model if they're not already in the cache
            SortedSet<String> tags = toStrings(tagModel.getTags(track, false));
            tagCache.put(track, tags);
        }
        return toString(tagCache.get(track));
    }

    /**
     * Convert a collection of TagCloudEntries into a collection of Strings.
     *
     * @param tags tag cloud entries
     * @return "tag" from each tag cloud entry
     */
    private SortedSet<String> toStrings(Collection<TagCloudEntry> tags) {
        return tags.stream().
                map(TagCloudEntry::getTag).
                collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * join() method for a collection of tags.
     *
     * @param tags collection of  tags
     * @return single string containing all the tags separated by a delimiter ('; ' in this case)
     */
    private String toString(Collection<String> tags) {
        StringBuilder sb = new StringBuilder();

        for (String tag : tags) {
            sb.append(tag);
            sb.append("; ");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * If the track exists in this table, updates the appropriate cell
     * and notifies any listeners on this DataModel.
     *
     * @param track track whose tags changed
     * @param tag name of tag removed
     */
    @Override
    public void tagRemoved(@NotNull Track track, @NotNull String tag) {
        if (tagCache.containsKey(track)) {
            tagCache.get(track).remove(tag);
            int index = tracks.indexOf(track);
            fireTableCellUpdated(index, Columns.Tags.ordinal());
        }
    }

    /**
     * If the track exists in this table, updates the appropriate cell
     * and notifies any listeners on this DataModel.
     *
     * @param track track whose tags changed
     * @param tag name of tag added
     */
    @Override
    public void tagAdded(@NotNull Track track, @NotNull String tag) {
        if (tagCache.containsKey(track)) {
            tagCache.get(track).add(tag);
            int index = tracks.indexOf(track);
            fireTableCellUpdated(index, Columns.Tags.ordinal());
        }
    }

    /**
     * If the track exists in this table, clear the cache and cause the
     * model to retrieve the full set of tags from the tag model.
     *
     * @param track track whose tags changed
     */
    @Override
    public void tagsChanged(@NotNull Track track) {
        if (tagCache.containsKey(track)) {
            tagCache.remove(track);
            int index = tracks.indexOf(track);
            fireTableCellUpdated(index, Columns.Tags.ordinal());
        }
    }
}
