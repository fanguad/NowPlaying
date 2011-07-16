/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import org.apache.log4j.Logger;
import org.nekocode.nowplaying.internals.DaemonThreadFactory;
import org.nekocode.nowplaying.objects.Track;
import org.nekocode.nowplaying.tags.TagModel;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A dialog that marks tracks as duplicates of each other.
 * Has a TrackTableComponent for loading and displaying tracks.
 */
public class TagDuplicateTracks extends JPanel {

    private static final Logger log = Logger.getLogger(TagDuplicateTracks.class);

    private Executor workerThread = Executors.newFixedThreadPool(1, new DaemonThreadFactory());

    private TagModel tagModel;

    private Runnable setDuplicates = new SetDuplicates();
    private TrackTableComponent table;

    public TagDuplicateTracks(TrackTableComponent table, TagModel tagModel) {
        this.table = table;
        this.tagModel = tagModel;

        JLabel tagLabel = new JLabel("Set the following tracks as duplicates of each other");

        ActionListener applyAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                workerThread.execute(setDuplicates);
            }
        };
        JButton apply = new JButton("Apply");
        apply.addActionListener(applyAction);
        
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        layout.setHorizontalGroup(layout.createSequentialGroup().
                addComponent(tagLabel).
                addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).
                addComponent(apply));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER).
                addComponent(tagLabel).
                addComponent(apply));
    }

    public void clear() {
        // nothing special needs to be done
    }

    private class SetDuplicates implements Runnable {
        public void run() {
            log.debug("locking GUI");
            setBusy(true);

            List<Track> tracks = table.getTracks();

            if (tracks.isEmpty()) {
                // quit early if we're not going to do anything
                log.debug("unlocking GUI - nothing to do");
                setBusy(false);
                return;
            }

            // remove any bad tracks
            LinkedList<Track> filteredTracks = new LinkedList<Track>(tracks);
            Iterator<Track> i = filteredTracks.iterator();
            while (i.hasNext()) {
                if (i.next().getPersistentId() == null) {
                    i.remove();
                }
            }

            boolean success = false;
            if (!filteredTracks.isEmpty()) {
                success = tagModel.setDuplicates(tracks);
            }
            // show results (failure only)
            if (!success) {
                JOptionPane.showMessageDialog(TagDuplicateTracks.this,
                        "Error setting track duplicates", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }

            log.debug("unlocking GUI");
            setBusy(false);
        }
    }

    private void setBusy(boolean locked) {
        table.setBusy(locked);
    }
}
