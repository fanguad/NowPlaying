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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
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
            log.info("Loading all track ids in database");
            List<String> trackIdsFromDatabase = tagModel.getAllTrackIds();
            int databaseSize = trackIdsFromDatabase.size();
            log.info("Found {} tracks in database", databaseSize);

            Vector<Vector<String>> dataVector = new Vector<>();
            tableModel.setDataVector(dataVector, new Vector<>(Arrays.asList("Track Id", "Tags")));

            // MonkeyTunes doesn't do a "find all" search the same way as iTunes
            List<String> trackIdsFromPlayer;
            if (false) {
                // load all track ids from media player (only grab the ids, don't make objects)
                trackIdsFromPlayer = mediaPlayer.findTrackIds(null, null, null);
            } else {
                // this bit probably wouldn't work properly in iTunes, since persistentId and itemId aren't the same
                trackIdsFromPlayer = new ArrayList<>();
                for (int i = 0; i < databaseSize; i++) {
                    String trackId = trackIdsFromDatabase.get(i);
                    log.debug("Checking track {} ({} of {})", trackId, i, databaseSize);
                    try {
                        Track track = mediaPlayer.getTrack(Integer.parseInt(trackId));
                        if (track != null) {
                            trackIdsFromPlayer.add(trackId);
//                            log.debug("Track {} is present in media player", trackId);
                        }
                        else {
                            log.debug("Track {} is not present in media player", trackId);

                            List<TagCloudEntry> tags = tagModel.getTagsById(trackId, false);

                            int oldSize = dataVector.size();
                            dataVector.add(new Vector<>(Arrays.asList(trackId, toString(tags))));
                            tableModel.fireTableRowsInserted(oldSize, oldSize);
                        }
                    } catch (NumberFormatException e) {
                        // the id wasn't an integer - this will remove any badly-formed (by MonkeyTunes standards, at least) ids
                    }
                }
            }
            busyModel.setBusy(false);

            if (log.isDebugEnabled()) {
                Set<String> difference = new HashSet<>(trackIdsFromDatabase);
                difference.removeAll(trackIdsFromPlayer);

                Collections.sort(trackIdsFromDatabase);
                Collections.sort(trackIdsFromPlayer);
                log.debug("tracks from database: " + databaseSize);
                log.debug("tracks from database: " + trackIdsFromDatabase);
                log.debug("tracks from player:   " + trackIdsFromPlayer.size());
                log.debug("tracks from player:   " + trackIdsFromPlayer);
                log.debug("difference:           " + difference.size());
                log.debug("difference:           " + difference);
            }
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
