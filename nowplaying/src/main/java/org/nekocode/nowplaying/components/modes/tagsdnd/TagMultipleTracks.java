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

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A dialog that tags files in bulk.  Has a TrackTableComponent for loading and displaying tracks.
 */
public class TagMultipleTracks extends JPanel {

    private static final Logger log = Logger.getLogger(TagMultipleTracks.class);

    private Executor workerThread = Executors.newFixedThreadPool(1, new DaemonThreadFactory());

    private TagModel tagModel;
    private JTextField tagInput;

    private Runnable applyTags = new ApplyTags();
    private TrackTableComponent table;

    public TagMultipleTracks(TrackTableComponent table, TagModel tagModel) {
        this.table = table;
        this.tagModel = tagModel;

        JLabel tagLabel = new JLabel("label to add:");
        tagLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));

        ActionListener applyAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                workerThread.execute(applyTags);
            }
        };
        tagInput = new JTextField();
        tagInput.addActionListener(applyAction);
        JButton apply = new JButton("Apply tag to tracks");
        apply.addActionListener(applyAction);
        
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        layout.setHorizontalGroup(layout.createSequentialGroup().
                addComponent(tagLabel).
                addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE).
                addComponent(tagInput).
                addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE).
                addComponent(apply));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER).
                addComponent(tagLabel).
                addComponent(tagInput).
                addComponent(apply));
    }

    public void clear() {
        tagInput.setText("");
    }

    private class ApplyTags implements Runnable {
        public void run() {
            log.debug("locking GUI");
            setBusy(true);
            tagInput.selectAll();
            List<Track> tracks = table.getTracks();
            String tag = tagInput.getText();
            if (tracks.isEmpty() || tag.length() == 0) {
                // quit early if we're not going to do anything
                log.debug("unlocking GUI - nothing to do");
                setBusy(false);
                return;
            }

            String metadata = null;
            int separator = tag.indexOf(": ");
            if (separator > 0) {
                // TODO hack to get metadata in there
                metadata = tag.substring(0, separator);
                tag = tag.substring(separator+2);
                log.debug(String.format("HACK for splitting metadata from tag: %s/%s", tag, metadata));
            }

            // remove any bad tracks
            LinkedList<Track> filteredTracks = new LinkedList<Track>(tracks);
            Iterator<Track> i = filteredTracks.iterator();
            while (i.hasNext()) {
                if (i.next().getPersistentId() == null) {
                    i.remove();
                }
            }

            if (!filteredTracks.isEmpty()) {
                try {
                    tagModel.addTagAndWait(filteredTracks, tag, metadata);
                } catch (ExecutionException e) {
                    log.error("error adding tags", e);
                } catch (InterruptedException e) {
                    log.error("error adding tags", e);
                }
            }

            log.debug("unlocking GUI");
            setBusy(false);
        }
    }

    private void setBusy(boolean locked) {
        table.setBusy(locked);
    }
}
