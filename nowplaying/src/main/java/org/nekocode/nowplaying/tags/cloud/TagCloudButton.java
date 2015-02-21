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
import org.nekocode.nowplaying.tags.cloud.TagCloud.TagCloudEntryRenderer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A button that, when pressed, opens up the tag cloud viewer. No actions need to be hooked up to it
 * at all for the basic functionality to work, although hooks may be added later to allow reacting
 * to events in the tag cloud.
 *
 * @author fanguad@nekocode.org
 */
@SuppressWarnings("serial")
public class TagCloudButton extends NekoButton {
	@SuppressWarnings("unused")
    private static final Logger log = LogManager.getLogger(TagCloudButton.class);
	private TagModel tagModel;
	private static final int MINIMUM_ENTRIES = 5;

	public TagCloudButton(TagModel tagModel, Rotation r) {
		super("tag cloud", r);
		this.setHorizontalAlignment(JLabel.CENTER);
		this.tagModel = tagModel;

		addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("Preparing to display tag cloud...");
                new CloudLoader(TagCloudButton.this.tagModel).execute();
            }
        }
        );
	}

    public void shutdown() {
        
    }

    /**
	 * Loads tags in the background and displays them when ready.
	 */
	private static class CloudLoader extends SwingWorker<List<TagCloudEntry>, Object> {

		private TagModel tagModel;

		public CloudLoader(TagModel tagModel) {
			this.tagModel = tagModel;
		}

		/* (non-Javadoc)
		 * @see javax.swing.SwingWorker#doInBackground()
		 */
		@Override
		protected List<TagCloudEntry> doInBackground() throws Exception {
            Collection<TagCloudEntry> allTags = tagModel.getAllTags(MINIMUM_ENTRIES);
			List<TagCloudEntry> sorted = new ArrayList<>(allTags);
			Collections.sort(sorted, new Comparator<TagCloudEntry>() {
				public int compare(TagCloudEntry o1, TagCloudEntry o2) {
					return o1.getTag().compareTo(o2.getTag());
				}
			});
			return sorted;
		}

		/* (non-Javadoc)
		 * @see javax.swing.SwingWorker#done()
		 */
		@Override
		protected void done() {
			TagCloudEntryRenderer renderer = entry -> String.format("%s (%d)", entry.getTag(), entry.getCount());

			try {
				JFrame frame = new JFrame("tag cloud");
				frame.getContentPane().add(new TagCloud(renderer, get()));
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.setMinimumSize(new Dimension(550,550));
				frame.pack();
				frame.setLocationByPlatform(true);
				frame.setVisible(true);
			} catch (InterruptedException | ExecutionException e) {
				log.error(e);
			}
		}
	}
}
