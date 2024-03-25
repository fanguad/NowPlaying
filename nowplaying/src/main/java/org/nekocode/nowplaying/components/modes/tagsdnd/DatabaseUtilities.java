/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import lombok.extern.log4j.Log4j2;
import org.nekocode.nowplaying.MediaPlayer;
import org.nekocode.nowplaying.tags.TagModel;

import javax.swing.*;
import java.awt.*;

/**
 * This panel provides utilities to search for and correct database issues.
 *
 * @author fanguad
 */
@Log4j2
public class DatabaseUtilities extends JDialog {
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
