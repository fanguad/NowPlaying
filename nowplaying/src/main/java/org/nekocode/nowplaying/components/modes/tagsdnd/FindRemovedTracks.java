/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.divxdede.swing.busy.BusyModel;
import org.divxdede.swing.busy.JBusyComponent;
import org.divxdede.swing.busy.ui.BasicBusyLayerUI;
import org.nekocode.nowplaying.MediaPlayer;
import org.nekocode.nowplaying.components.icons.SpinningDialBusyIcon;
import org.nekocode.nowplaying.internals.NamedThreadFactory;
import org.nekocode.nowplaying.objects.Track;
import org.nekocode.nowplaying.tags.TagModel;
import org.nekocode.nowplaying.tags.cloud.TagCloudEntry;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This panel searches for tracks that exist in the database, but not in the media player,
 * and provides utilities to delete them from the database.
 *
 * @author fanguad
 */
public class FindRemovedTracks extends JBusyComponent<JPanel> {
    private static final Logger log = LogManager.getLogger(FindRemovedTracks.class);
    private final SpinningDialBusyIcon busyIcon;

    private ExecutorService workerThread = Executors.newSingleThreadExecutor(new NamedThreadFactory("FindRemovedTracks", false));
    
    private MediaPlayer mediaPlayer;
    private TagModel tagModel;
    private DefaultTableModel tableModel;
    private JTable tagTable;
    private BusyModel busyModel;

    public FindRemovedTracks(MediaPlayer mediaPlayer, TagModel tagModel) {
        this.mediaPlayer = mediaPlayer;
        this.tagModel = tagModel;

        JPanel view = new JPanel(new BorderLayout());
        setView(view);
        BasicBusyLayerUI busyLayerUI = new BasicBusyLayerUI();
        busyIcon = new SpinningDialBusyIcon(64, 64);
        busyLayerUI.setBusyIcon(busyIcon);
//        BusyLayerUI busyLayerUI = new BasicBusyLayerUI(0, 0.85f, Color.WHITE);
        setBusyLayerUI(busyLayerUI);

        // use the BusyModel to control the busy state of this component
        busyModel = getBusyModel();
        busyModel.setCancellable(false);
        busyModel.setDeterminate(false);

        JButton search = new JButton("Find Removed Tracks");
        search.setToolTipText("These tracks are no longer present in the media player");
        JButton delete = new JButton("Purge Tracks from Database");
        delete.setToolTipText("Clean database by removing tracks and associated tags");
        JPanel topRow = new JPanel();
        topRow.add(search);
        topRow.add(delete);

        tableModel = new DefaultTableModel();
        tableModel.addColumn("Track Id");
        tableModel.addColumn("Tags");
        tagTable = new JTable(tableModel);
        JScrollPane scrollpane = new JScrollPane(tagTable);
        scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        view.add(topRow, BorderLayout.PAGE_START);
        view.add(scrollpane, BorderLayout.CENTER);

        final Runnable findUnusedTags = new FindRemovedTracksAction();
        search.addActionListener(e -> workerThread.execute(findUnusedTags));

        final Runnable deleteTracks = new PurgeTracksAction();
        delete.addActionListener(e -> workerThread.execute(deleteTracks));
    }

    public void shutdown() {
        workerThread.shutdown();
        busyIcon.shutdown();
    }

    private class FindRemovedTracksAction implements Runnable {

        @Override
        public void run() {
            busyModel.setBusy(true);

            // load all track ids from tagModel
            List<String> trackIdsFromDatabase = tagModel.getAllTrackIds();

            // MonkeyTunes doesn't do a "find all" search the same way as iTunes
            List<String> trackIdsFromPlayer;
            if (false) {
                // load all track ids from media player (only grab the ids, don't make objects)
                trackIdsFromPlayer = mediaPlayer.findTrackIds(null, null, null);
            } else {
                // this bit probably wouldn't work properly in iTunes, since persistentId and itemId aren't the same
                trackIdsFromPlayer = new ArrayList<>();
                for (String trackId : trackIdsFromDatabase) {
                    try {
                        Track track = mediaPlayer.getTrack(Integer.parseInt(trackId));
                        if (track != null) {
                            trackIdsFromPlayer.add(trackId);
                        }
                    } catch (NumberFormatException e) {
                        // the id wasn't an integer - this will remove any badly-formed (by MonkeyTunes standards, at least) ids
                    }
                }
            }

            Set<String> difference = new HashSet<>(trackIdsFromDatabase);
            difference.removeAll(trackIdsFromPlayer);

            if (log.isDebugEnabled()) {
                Collections.sort(trackIdsFromDatabase);
                Collections.sort(trackIdsFromPlayer);
                log.debug("tracks from database: " + trackIdsFromDatabase.size());
                log.debug("tracks from database: " + trackIdsFromDatabase);
                log.debug("tracks from player:   " + trackIdsFromPlayer.size());
                log.debug("tracks from player:   " + trackIdsFromPlayer);
                log.debug("difference:           " + difference.size());
                log.debug("difference:           " + difference);
            }

            Map<String, List<TagCloudEntry>> tags = tagModel.getTagsById(difference, false);

            Object[][] dataVector = new Object[tags.size()][];
            int i = 0;
            for (Map.Entry<String, List<TagCloudEntry>> e : tags.entrySet()) {
                dataVector[i++] = new Object[] {e.getKey(), toString(e.getValue())};
            }

            tableModel.setDataVector(dataVector, new Object[] {"Track Id", "Tags"});

            busyModel.setBusy(false);
        }
        
        /**
         * join() method for a collection of tags.
         *
         * @param tags collection of  tags
         * @return single string containing all the tags separated by a delimiter ('; ' in this case)
         */
        private String toString(Collection<TagCloudEntry> tags) {
            StringBuilder sb = new StringBuilder();

            for (TagCloudEntry tag : tags) {
                sb.append(tag.getTag());
                sb.append("; ");
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        }
    }

    private class PurgeTracksAction implements Runnable {

        @Override
        public void run() {
            busyModel.setBusy(true);

            List<String> idsToDelete = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                idsToDelete.add((String) tableModel.getValueAt(i, 0));
            }
            tagModel.deleteTracks(idsToDelete);

            busyModel.setBusy(false);
        }
    }
}
