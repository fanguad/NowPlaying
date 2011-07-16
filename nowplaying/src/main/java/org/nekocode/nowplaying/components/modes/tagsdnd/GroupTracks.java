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

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.lang.String.format;

/**
 * A dialog that adds files to a group.  Has a TrackTableComponent for loading and displaying tracks.
 */
public class GroupTracks extends JPanel {

    private static final Logger log = Logger.getLogger(GroupTracks.class);
    private static final String GROUP_NAME_DEFAULT = "New group name...";

    private Executor workerThread = Executors.newFixedThreadPool(1, new DaemonThreadFactory());

    private TagModel tagModel;
    private JComboBox groupNamePullDown;
    private GroupNameComboBoxModel groupNameModel;

    private Runnable setGroup = new SetGroup();
    private TrackTableComponent table;

    public GroupTracks(TrackTableComponent table, TagModel tagModel) {
        this.table = table;
        this.tagModel = tagModel;

        JLabel tagLabel = new JLabel("group name:");
        tagLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));

        ActionListener applyAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                workerThread.execute(setGroup);
//                log.debug("select group is: " + groupNamePullDown.getSelectedItem());
            }
        };

        groupNameModel = new GroupNameComboBoxModel();
        groupNameModel.addGroupName(GROUP_NAME_DEFAULT);
        groupNameModel.setSelectedItem(GROUP_NAME_DEFAULT);
        groupNamePullDown = new JComboBox(groupNameModel);
        groupNamePullDown.setEditable(true);

        JButton apply = new JButton("Add Tracks to Group");
        apply.addActionListener(applyAction);
        
        table.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (e instanceof TrackTableComponent.TrackTableChangeEvent) {
                    TrackTableComponent.TrackTableChangeEvent ttce = (TrackTableComponent.TrackTableChangeEvent) e;
                    switch (ttce.getType()) {
                        case ADD:
                            groupNameModel.addGroupNames(GroupTracks.this.tagModel.getGroups(ttce.getTrack()));
                            break;
                        case CLEAR:
                            groupNameModel.clear();
                            break;
                    }
                }
            }
        });

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        layout.setHorizontalGroup(layout.createSequentialGroup().
                addComponent(tagLabel).
                addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE).
                addComponent(groupNamePullDown).
                addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE).
                addComponent(apply));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER).
                addComponent(tagLabel).
                addComponent(groupNamePullDown).
                addComponent(apply));
    }

    private void setBusy(boolean locked) {
        table.setBusy(locked);
    }

    public void clear() {
        groupNameModel.clear();
        groupNameModel.setSelectedItem(GROUP_NAME_DEFAULT);
    }

    private class SetGroup implements Runnable {
        public void run() {
            log.debug("locking GUI");
            setBusy(true);
            List<Track> tracks = table.getTracks();
            String groupName = (String) groupNamePullDown.getSelectedItem();
            if (tracks.isEmpty() || groupName == null || groupName.length() == 0 || GROUP_NAME_DEFAULT.equals(groupName)) {
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

                log.debug(format("Adding tracks to group \"%s\":%n%s", groupName, sb));
            }
            // create group
            boolean success = tagModel.setGroup(groupName, tracks);

            // display errors
            if (!success) {
                JOptionPane.showMessageDialog(GroupTracks.this,
                        format("Error creating group \"%s\"", groupName), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }

            log.debug("unlocking GUI");
            setBusy(false);
        }
    }
    
    private static class GroupNameComboBoxModel extends AbstractListModel implements ComboBoxModel {

        private List<String> groupNames = new ArrayList<String>();
        private String selectedItem = null;

        @Override
        public void setSelectedItem(Object anItem) {
            if (!Objects.equals(selectedItem, anItem)) {
                selectedItem = (String) anItem;
                fireContentsChanged(this, -1, -1);
            }
        }

        @Override
        public String getSelectedItem() {
            return selectedItem;
        }

        @Override
        public int getSize() {
            return groupNames.size();
        }

        @Override
        public String getElementAt(int index) {
            return groupNames.get(index);
        }

        public void addGroupNames(Collection<String> newGroupNames) {
            // ensure there are no duplicates, and that it comes out sorted
            Set<String> allGroupNames = new TreeSet<String>();
            allGroupNames.addAll(groupNames);
            allGroupNames.addAll(newGroupNames);
            groupNames = new ArrayList<String>(allGroupNames);
            fireContentsChanged(this, -1, -1);
        }

        public void clear() {
            selectedItem = null;
            groupNames.clear();
            fireContentsChanged(this, -1, -1);
        }

        /**
         * Add a single group name to this model.
         *
         * @param groupName group name to add
         */
        public void addGroupName(String groupName) {
            addGroupNames(Collections.singleton(groupName));
        }
    }
}
