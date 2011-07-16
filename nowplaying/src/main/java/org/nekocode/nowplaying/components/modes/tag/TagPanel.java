/*
 * Copyright (c) 2010, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.components.modes.tag;

import java.awt.BorderLayout;
import java.util.EnumSet;

import org.apache.log4j.Logger;
import org.nekocode.nowplaying.NowPlayingView;
import org.nekocode.nowplaying.components.NowPlayingControl;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType;
import org.nekocode.nowplaying.tags.TagModel;
import org.nekocode.nowplaying.tags.TagView;

/**
 * A Panel containing controls for manipulating tags.
 *
 * @author dan.clark@nekocode.org
 */
public class TagPanel extends NowPlayingControl {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(NowPlayingView.class);
	private final static EnumSet<ChangeType> shouldUpdate = EnumSet.of(
			ChangeType.CURRENT_SONG_CHANGE, ChangeType.TAG_CHANGE);

	private TagView tagView;
	private TagModel tagModel;

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
	public void updateTrack(TrackChangeEvent trackChange) {
		if (shouldUpdate.contains(trackChange.getType())) {
			tagView.setLoadingTags();
			tagView.setTags(trackChange.getTrack(),
					tagModel.getTags(trackChange.getTrack()));
		}
	}

	/* (non-Javadoc)
	 * @see org.nekocode.nowplaying.components.NowPlayingControl#shutdown()
	 */
	@Override
	public void shutdown() {
		// nothing particular to do
	}
}
