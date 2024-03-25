/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.tags.cloud;

import lombok.extern.log4j.Log4j2;
import org.nekocode.nowplaying.components.swing.NekoButton;
import org.nekocode.nowplaying.components.swing.Rotation;
import org.nekocode.nowplaying.tags.TagModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * A button that, when pressed, opens up the tag cloud viewer. No actions need to be hooked up to it
 * at all for the basic functionality to work, although hooks may be added later to allow reacting
 * to events in the tag cloud.
 *
 * @author dan.clark@nekocode.org
 */
@Log4j2
@SuppressWarnings("serial")
public class TagCloudButton2 extends NekoButton {
	private final TagModel tagModel;
	private static final int MINIMUM_ENTRIES = 5;

	public TagCloudButton2(TagModel tagModel, Rotation r) {
		super("tag cloud", r);
		this.setHorizontalAlignment(JLabel.CENTER);
		this.tagModel = tagModel;

		addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("Preparing to display tag cloud...");
                loadWordle();
            }
        }
        );
	}

    public void loadWordle() {
        Collection<TagCloudEntry> allTags = tagModel.getAllTags(MINIMUM_ENTRIES);
        List<TagCloudEntry> sorted = new ArrayList<>(allTags);
        Collections.sort(sorted, new Comparator<TagCloudEntry>() {
            public int compare(TagCloudEntry o1, TagCloudEntry o2) {
                return o1.getTag().compareTo(o2.getTag());
            }
        });

        WordleAccess.openWordle(allTags);
    }

    public void shutdown() {
    }
}
