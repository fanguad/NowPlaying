/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import org.apache.log4j.Logger;
import org.nekocode.nowplaying.internals.DaemonThreadFactory;
import org.nekocode.nowplaying.objects.Track;
import org.nekocode.nowplaying.tags.TagModel;
import org.nekocode.nowplaying.tags.cloud.TagCloudEntry;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.lang.String.format;

/**
 * A dialog that changes tags on multiple tracks at once.  Allows deletes and changes.
 * Changes differ from a delete followed by an add in that only tracks that had the tag
 * to begin with are changed.
 */
public class ChangeTags extends JPanel {

    private static final Logger log = Logger.getLogger(ChangeTags.class);

    private Executor workerThread = Executors.newFixedThreadPool(1, new DaemonThreadFactory());

    private TagModel tagModel;
    private JComboBox tagListPullDown;
    private JTextField newTagName;

    private TagNameComboBoxModel tagListModel;
    private TrackTableComponent table;
    private Runnable deleteTag = new DeleteTag();
    private Runnable changeTag = new ChangeTag();

    public ChangeTags(TrackTableComponent table, TagModel tagModel) {
        this.table = table;
        this.tagModel = tagModel;

        JLabel tagLabel = new JLabel("original tag:");
        tagLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));

        ActionListener deleteAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                workerThread.execute(deleteTag);
            }
        };

        ActionListener changeAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                workerThread.execute(changeTag);
            }
        };

        tagListModel = new TagNameComboBoxModel();
        tagListPullDown = new JComboBox(tagListModel);

        newTagName = new JTextField();

        JButton delete = new JButton("Delete Tag");
        delete.addActionListener(deleteAction);

        JButton change = new JButton("Change Tag");
        change.addActionListener(changeAction);

        table.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (e instanceof TrackTableComponent.TrackTableChangeEvent) {
                    TrackTableComponent.TrackTableChangeEvent ttce = (TrackTableComponent.TrackTableChangeEvent) e;
                    switch (ttce.getType()) {
                        case ADD:
                            tagListModel.addTagNames(ChangeTags.this.tagModel.getTags(ttce.getTrack()));
                            break;
                        case CLEAR:
                            tagListModel.clear();
                            break;
                    }
                }
            }
        });

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        layout.setHorizontalGroup(layout.createSequentialGroup().
                addComponent(tagLabel).
                addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE).
                addComponent(tagListPullDown, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE).
                addComponent(newTagName, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE).
                addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE).
                addComponent(change).
                addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE).
                addComponent(delete));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER).
                addComponent(tagLabel).
                addComponent(tagListPullDown).
                addComponent(newTagName).
                addComponent(change).
                addComponent(delete));
    }

    private void setBusy(boolean locked) {
        table.setBusy(locked);
    }

    public void clear() {
        newTagName.setText("");
        tagListModel.clear();
    }

    private class DeleteTag implements Runnable {
        public void run() {
            log.debug("locking GUI");
            setBusy(true);
            List<Track> tracks = table.getTracks();
            TagCloudEntry selectedTag = (TagCloudEntry) tagListPullDown.getSelectedItem();
            if (tracks.isEmpty() || selectedTag == null) {
                // quit early if we're not going to do anything
                log.debug("unlocking GUI - nothing to do");
                setBusy(false);
                return;
            }

            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                for (Track track : tracks) {
                    sb.append(String.format("%s / %s / %s%n",
                            track.getTitle(),
                            track.getArtist(),
                            track.getAlbum()));
                }

                log.debug(format("Removing tag \"%s\" from tracks:%n%s", selectedTag, sb));
            }

            try {
                tagModel.removeTagsAndWait(tracks, selectedTag.getTag());
            } catch (ExecutionException e) {
                log.error(format("error removing tag %s", selectedTag.getTag()), e);
            } catch (InterruptedException e) {
                log.error(format("error removing tag %s", selectedTag.getTag()), e);
            }

            log.debug("unlocking GUI");
            setBusy(false);
        }
    }

    private class ChangeTag implements Runnable {
        public void run() {
            log.debug("locking GUI");
            newTagName.selectAll();
            setBusy(true);
            List<Track> tracks = table.getTracks();
            TagCloudEntry selectedTag = (TagCloudEntry) tagListPullDown.getSelectedItem();
            String newTag = newTagName.getText();
            if (tracks.isEmpty() || selectedTag == null) {
                // quit early if we're not going to do anything
                log.debug("unlocking GUI - nothing to do");
                setBusy(false);
                return;
            }

            // find which tracks have the tag at the moment
            Map<Track, List<TagCloudEntry>> tags = tagModel.getTags(tracks);

            List<Track> tracksWithTag = new ArrayList<>();
            OUTER_LOOP:
            for (Map.Entry<Track, List<TagCloudEntry>> entry : tags.entrySet()) {
                for (TagCloudEntry tag : entry.getValue()) {
                    if (tag.equals(selectedTag)) {
                        tracksWithTag.add(entry.getKey());
                        continue OUTER_LOOP;
                    }
                }
            }

            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                for (Track track : tracksWithTag) {
                    sb.append(String.format("%s / %s / %s%n",
                            track.getTitle(),
                            track.getArtist(),
                            track.getAlbum()));
                }

                log.debug(format("Changing tag \"%s\" to \"%s\" from tracks:%n%s", selectedTag, newTag, sb));
            }

			// TODO hack to get metadata in there
            String metadata = null;
            String tag = newTag;
            int separator = tag.indexOf(": ");
            if (separator > 0) {
                metadata = tag.substring(0, separator);
                tag = tag.substring(separator+2);
            }

            try {
                tagModel.addTagAndWait(tracksWithTag, tag, metadata);
                log.debug("new tag added: " + tag);
                tagModel.removeTagsAndWait(tracksWithTag, selectedTag.getTag());
                log.debug("old tag removed: " + selectedTag.getTag());
            } catch (ExecutionException e) {
                log.error(format("error changing tag from %s to %s", selectedTag.getTag(), tag), e);
            } catch (InterruptedException e) {
                log.error(format("error changing tag from %s to %s", selectedTag.getTag(), tag), e);
            }

            log.debug("unlocking GUI");
            setBusy(false);
        }
    }

    /**
     * Maintains a list of Tag names.
     */
    private static class TagNameComboBoxModel extends AbstractListModel implements ComboBoxModel {

        private List<TagCloudEntry> tagNames = new ArrayList<>();
        private TagCloudEntry selectedItem = null;

        @Override
        public void setSelectedItem(Object anItem) {
            if (!Objects.equals(selectedItem, anItem)) {
                selectedItem = (TagCloudEntry) anItem;
                fireContentsChanged(this, -1, -1);
            }
        }

        @Override
        public TagCloudEntry getSelectedItem() {
            return selectedItem;
        }

        @Override
        public int getSize() {
            return tagNames.size();
        }

        @Override
        public TagCloudEntry getElementAt(int index) {
            return tagNames.get(index);
        }

        public void clear() {
            selectedItem = null;
            tagNames.clear();
            fireContentsChanged(this, -1, -1);
        }

        public void addTagNames(List<TagCloudEntry> tags) {
            // ensure there are no duplicates, and that it comes out sorted
            Set<TagCloudEntry> allTags = new TreeSet<>();
            allTags.addAll(tagNames);
            allTags.addAll(tags);
            tagNames = new ArrayList<>(allTags);
            if (selectedItem == null && !tagNames.isEmpty()) {
                selectedItem = tagNames.get(0);
            }
            fireContentsChanged(this, -1, -1);
        }
    }
}
