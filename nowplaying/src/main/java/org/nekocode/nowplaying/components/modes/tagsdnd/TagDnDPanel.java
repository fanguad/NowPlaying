/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import org.nekocode.nowplaying.MediaPlayer;
import org.nekocode.nowplaying.components.NowPlayingControl;
import org.nekocode.nowplaying.components.swing.NekoButton;
import org.nekocode.nowplaying.components.swing.Rotation;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.tags.TagModel;
import org.nekocode.nowplaying.tags.cloud.TagCloudButton2;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Provides controls that open up dialogs allowing the user to manipulate tags through Drag and Drop.
 */
public class TagDnDPanel extends NowPlayingControl {
    private TagCloudButton2 tagCloudButton;
    private UnifiedTagEditorDialog editTags;
    private DatabaseUtilities dbUtils;

    public TagDnDPanel(MediaPlayer mediaPlayer, TagModel tagModel) {
		// find the top level frame
		Container parent = this.getParent();
		Window frame = null;
		while (parent != null) {
			parent = parent.getParent();
			if (parent instanceof Window) {
				frame = (Window) parent;
				break;
			}
		}

        // clicking on this button opens up a dialog allowing tags to be added using drag and drop
        NekoButton editTagsButton = new NekoButton("edit tags");
        editTags = new UnifiedTagEditorDialog(frame, mediaPlayer, tagModel);
        setUpDialog(editTagsButton, editTags);

        NekoButton dbUtilsButton = new NekoButton("db utils");
        dbUtils = new DatabaseUtilities(frame, mediaPlayer, tagModel);
        setUpDialog(dbUtilsButton, dbUtils);

        tagCloudButton = new TagCloudButton2(tagModel, Rotation.NONE);

        // need to manually create rows (boo)
        JPanel buttonRow1 = new JPanel();
        buttonRow1.setOpaque(false);
        buttonRow1.add(editTagsButton);
        buttonRow1.add(dbUtilsButton);
        buttonRow1.add(tagCloudButton);

        JPanel buttonPane = new JPanel(new GridLayout(0, 1));
        buttonPane.setOpaque(false);
        buttonPane.add(buttonRow1);

        setLayout(new BorderLayout());
        add(buttonPane, BorderLayout.PAGE_END);
    }

    private void setUpDialog(NekoButton displayButton, final JDialog dialog) {
        // set up the button to open the dialog
        displayButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.setVisible(true);
                    }
                });
        displayButton.setHorizontalAlignment(SwingConstants.CENTER);

        // set up the dialog
        dialog.setModal(false);
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.pack();
        // set initial width to be 2x smallest available width
        Dimension d = dialog.getSize();
        d.width *= 2;
        dialog.setSize(d);
        dialog.setLocationByPlatform(true);
    }

    @Override
    public void updateTrack(TrackChangeEvent trackChange) {
        // this panel has no dynamically updated information
    }

    @Override
    public void shutdown() {
        tagCloudButton.shutdown();
        editTags.dispose();
        dbUtils.dispose();
    }
}
