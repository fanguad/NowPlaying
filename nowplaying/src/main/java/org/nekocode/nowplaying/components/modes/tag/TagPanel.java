/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.modes.tag;

import org.jetbrains.annotations.NotNull;
import org.nekocode.nowplaying.components.NowPlayingControl;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType;
import org.nekocode.nowplaying.tags.TagModel;
import org.nekocode.nowplaying.tags.TagView;
import org.nekocode.nowplaying.tags.cloud.TagCloudEntry;

import java.awt.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * A Panel containing controls for manipulating tags.
 *
 * @author dan.clark@nekocode.org
 */
public class TagPanel extends NowPlayingControl {
	private final static EnumSet<ChangeType> shouldUpdate = EnumSet.of(
			ChangeType.CURRENT_SONG_CHANGE, ChangeType.TAG_CHANGE, ChangeType.FILE_CHANGE);

	private final TagView tagView;
	private final TagModel tagModel;

	public TagPanel(TagModel model, TagView tagView) {
		this.tagView = tagView;
		this.tagModel = model;

		setLayout(new BorderLayout());

		add(tagView, BorderLayout.CENTER);
	}

	/**
	 * This method will be called whenever the currently playing track changes
	 * (either to a different track, or the data of the same track changes).
	 */
	@Override
	public void updateTrack(@NotNull TrackChangeEvent trackChange) {
		if (shouldUpdate.contains(trackChange.getType())) {
			tagView.setLoadingTags();
            List<TagCloudEntry> tags = Collections.emptyList();
            try {
                tags = tagModel.getTags(trackChange.getTrack(), true);
            } finally {
                tagView.setTags(trackChange.getTrack(), tags);
            }
        }
    }

	@Override
	public void shutdown() {
		// nothing particular to do
	}

	@Override
	public String getModeName() {
		return "tags";
	}
}
