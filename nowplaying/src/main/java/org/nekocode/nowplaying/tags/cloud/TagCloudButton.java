/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.tags.cloud;

import org.nekocode.nowplaying.components.swing.NekoButton;
import org.nekocode.nowplaying.components.swing.Rotation;
import org.nekocode.nowplaying.tags.TagModel;

import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * A button that, when pressed, opens up the tag cloud viewer. No actions need to be hooked up to it
 * at all for the basic functionality to work, although hooks may be added later to allow reacting
 * to events in the tag cloud.
 *
 * @author dan.clark@nekocode.org
 */
@Log4j2
public class TagCloudButton extends NekoButton {
	private final TagModel tagModel;
	private static final int MINIMUM_ENTRIES = 5;
	private static final int TAG_CLOUD_SIZE = 800;

	public TagCloudButton(TagModel tagModel, Rotation r) {
		super("tag cloud", r);
		this.setHorizontalAlignment(JLabel.CENTER);
		this.tagModel = tagModel;

		addActionListener(_ -> {
            log.debug("Preparing to display tag cloud...");
            new CloudLoader(TagCloudButton.this.tagModel).execute();
        }
        );
	}

    public void shutdown() {
        
    }

    /**
	 * Loads tags in the background and displays them when ready.
	 */
	private static class CloudLoader extends SwingWorker<ImageIcon, Object> {

		private final TagModel tagModel;

		public CloudLoader(TagModel tagModel) {
			this.tagModel = tagModel;
		}

		@Override
		protected ImageIcon doInBackground() {
            Collection<TagCloudEntry> allTags = tagModel.getAllTags(MINIMUM_ENTRIES);
			return KumoCloud.createCloud(allTags, TAG_CLOUD_SIZE);
		}

		@Override
		protected void done() {
			try {
				ImageIcon imageIcon = get();
				JFrame frame = new JFrame("tag cloud");
				frame.getContentPane().add(new JLabel(imageIcon));
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.setMinimumSize(new Dimension(TAG_CLOUD_SIZE,TAG_CLOUD_SIZE));
				frame.pack();
				frame.setLocationByPlatform(true);
				frame.setVisible(true);
			} catch (InterruptedException | ExecutionException e) {
				log.error(e);
			}
		}
	}
}
