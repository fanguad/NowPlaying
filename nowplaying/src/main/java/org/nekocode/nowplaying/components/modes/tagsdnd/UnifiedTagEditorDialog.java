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

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.LayoutStyle;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Window;

/**
 * A single dialog that contains all the different kind of tag editing functionality.
 *
 * @author fanguad
 */
public class UnifiedTagEditorDialog extends JDialog {

    private static final Logger log = LogManager.getLogger(UnifiedTagEditorDialog.class);
    private final TagMultipleTracks tagMultipleTracks;
    private final ChangeTags changeTags;
    private final TagDuplicateTracks tagDuplicateTracks;
    private final GroupTracks groupTracks;
    private final TrackTableComponent table;

    public UnifiedTagEditorDialog(Window owner, MediaPlayer mediaPlayer, TagModel tagModel) {
        super(owner, "Edit Tags for Multiple Tracks at Once");

        JPanel contentPane = new JPanel(new BorderLayout());
        this.setContentPane(contentPane);

        table = new TrackTableComponent(mediaPlayer, tagModel);

        tagMultipleTracks = new TagMultipleTracks(table, tagModel);
        tagMultipleTracks.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        changeTags = new ChangeTags(table, tagModel);
        tagDuplicateTracks = new TagDuplicateTracks(table, tagModel);
        groupTracks = new GroupTracks(table, tagModel);

        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.tabsOverlapBorder", true);

//        BasicTabbedPaneUI noInsetsUI = new BasicTabbedPaneUI() {
//            @Override
//            protected Insets getContentBorderInsets(int tabPlacement) {
//                return new Insets(0, 0, 0, 0);
//            }
//        };

        JTabbedPane tabbedPane = new JTabbedPane();
//        tabbedPane.setUI(noInsetsUI);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addTab("Add Tags", tagMultipleTracks);
        tabbedPane.addTab("Change Existing Tags", changeTags);
        tabbedPane.addTab("Mark Tracks as Duplicates", tagDuplicateTracks);
        tabbedPane.addTab("Add Tracks to Group", groupTracks);

        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> {
            table.clear();
            tagMultipleTracks.clear();
            changeTags.clear();
            tagDuplicateTracks.clear();
            groupTracks.clear();
        });


        JPanel topComponent = new JPanel();
        GroupLayout layout = new GroupLayout(topComponent);
        topComponent.setLayout(layout);

        layout.setHorizontalGroup(layout.createSequentialGroup().
                addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE).
                addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE).
                addComponent(clear));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).
                addComponent(tabbedPane).
                addComponent(clear));


        contentPane.add(topComponent, BorderLayout.PAGE_START);
        contentPane.add(table, BorderLayout.CENTER);
    }

    public void shutdown()
    {
        log.info("shutting down Unified Tag Editor Dialog");
        table.shutdown();
        tagMultipleTracks.shutdown();
        changeTags.shutdown();
        tagDuplicateTracks.shutdown();
        groupTracks.shutdown();
        log.info("finished shutting down Unified Tag Editor Dialog");
    }
}
