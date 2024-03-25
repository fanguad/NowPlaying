/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import lombok.extern.log4j.Log4j2;
import org.nekocode.nowplaying.internals.NamedThreadFactory;
import org.nekocode.nowplaying.objects.Track;
import org.nekocode.nowplaying.tags.TagModel;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A dialog that tags files in bulk.  Has a TrackTableComponent for loading and displaying tracks.
 */
@Log4j2
public class TagMultipleTracks extends JPanel {
    private final ExecutorService workerThread = Executors.newSingleThreadExecutor(new NamedThreadFactory("TagMultipleTracks", false));

    private final TagModel tagModel;
    private final JTextField tagInput;

    private final Runnable applyTags = new ApplyTags();
    private final TrackTableComponent table;

    public TagMultipleTracks(TrackTableComponent table, TagModel tagModel) {
        this.table = table;
        this.tagModel = tagModel;

        JLabel tagLabel = new JLabel("label to add:");
        tagLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));

        ActionListener applyAction = e -> workerThread.execute(applyTags);
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

    public void shutdown() {
        workerThread.shutdown();
    }

    private class ApplyTags implements Runnable {
        public void run() {
            log.debug("locking GUI");
            setBusy(true);
            tagInput.selectAll();
            List<Track> tracks = table.getTracks();
            String tag = tagInput.getText();
            if (tracks.isEmpty() || tag.isEmpty()) {
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
            LinkedList<Track> filteredTracks = new LinkedList<>(tracks);
            filteredTracks.removeIf(track -> track.getPersistentId() == null);

            if (!filteredTracks.isEmpty()) {
                try {
                    tagModel.addTagAndWait(filteredTracks, tag, metadata);
                } catch (ExecutionException | InterruptedException e) {
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
