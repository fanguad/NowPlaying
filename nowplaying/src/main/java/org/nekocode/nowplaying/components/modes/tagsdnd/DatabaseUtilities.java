/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import org.nekocode.nowplaying.MediaPlayer;
import org.nekocode.nowplaying.tags.TagModel;

import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import java.awt.Window;

/**
 * This panel provides utilities to search for and correct database issues.
 *
 * @author fanguad
 */
public class DatabaseUtilities extends JDialog {

    public DatabaseUtilities(Window owner, MediaPlayer mediaPlayer, TagModel tagModel) {
        super(owner, "Database Utilities");

        JTabbedPane tabbedPane = new JTabbedPane();

        this.setContentPane(tabbedPane);

        final FindUnusedTags findUnusedTags = new FindUnusedTags(tagModel);
//        findUnusedTags.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        final FindRemovedTracks findRemovedTracks = new FindRemovedTracks(mediaPlayer, tagModel);

        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addTab("Find Unused Tags", findUnusedTags);
        tabbedPane.addTab("Find Removed Tracks", findRemovedTracks);
    }
}
