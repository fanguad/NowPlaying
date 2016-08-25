/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.tags.cloud;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nekocode.nowplaying.components.swing.NekoButton;
import org.nekocode.nowplaying.components.swing.Rotation;
import org.nekocode.nowplaying.tags.TagModel;

import javax.swing.JLabel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A button that, when pressed, opens up the tag cloud viewer. No actions need to be hooked up to it
 * at all for the basic functionality to work, although hooks may be added later to allow reacting
 * to events in the tag cloud.
 *
 * @author dan.clark@nekocode.org
 */
@SuppressWarnings("serial")
public class TagCloudButton2 extends NekoButton {
	@SuppressWarnings("unused")
    private static final Logger log = LogManager.getLogger(TagCloudButton2.class);
	private TagModel tagModel;
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
