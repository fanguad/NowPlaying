/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger log = LogManager.getLogger(DatabaseUtilities.class);
    private final FindUnusedTags findUnusedTags;
    private final FindRemovedTracks findRemovedTracks;

    public DatabaseUtilities(Window owner, MediaPlayer mediaPlayer, TagModel tagModel) {
        super(owner, "Database Utilities");

        JTabbedPane tabbedPane = new JTabbedPane();

        this.setContentPane(tabbedPane);

        findUnusedTags = new FindUnusedTags(tagModel);
//        findUnusedTags.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        findRemovedTracks = new FindRemovedTracks(mediaPlayer, tagModel);

        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addTab("Find Unused Tags", findUnusedTags);
        tabbedPane.addTab("Find Removed Tracks", findRemovedTracks);
    }

    public void shutdown()
    {
        log.info("shutting down database utilities dialog");
        findUnusedTags.shutdown();
        findRemovedTracks.shutdown();
        log.info("finished shutting down database utilities dialog");
    }
}
