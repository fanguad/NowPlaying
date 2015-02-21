/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

/*
 * Filename:   TagCloud.java
 * Created On: Apr 7, 2008
 */

package org.nekocode.nowplaying.tags.cloud;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nekocode.nowplaying.components.swing.NekoLabel;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Displays a cloud of tags.
 *
 * @author dan.clark@nekocode.org
 */
public class TagCloud extends ScrollableFlowPanel {

	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(TagCloud.class);

	private Collection<TagCloudEntry> tags;

	private final Map<Float, Font> fontCache;

	private TagCloudEntryMouseListener tagListener;
	private Set<MouseListener> mouseListeners;

	private TagCloudEntryRenderer defaultRenderer = TagCloudEntry::getTag;
	private TagCloudEntryRenderer renderer = defaultRenderer;
    private Font baseFont;

    /**
	 * Creates a new TagCloud with no tags.
	 */
	public TagCloud() {
		fontCache = new HashMap<>();
		tagListener = new TagCloudEntryMouseListener();
		mouseListeners = new HashSet<>();

		((FlowLayout)getLayout()).setHgap(10);
		setOpaque(false);
        // use the default font for a neko label
        this.baseFont = new NekoLabel("").getFont();
	}

	/**
	 * Creates a new TagCloud with the given tags as elements.
	 *
	 * @param tags tag cloud entries
	 */
	public TagCloud(Collection<TagCloudEntry> tags) {
		this();
		setTagEntries(tags);
	}

	/**
	 * Creates a new TagCloud with no tags.
	 *
	 * @param renderer the renderer to use in displaying the tags
	 */
	public TagCloud(TagCloudEntryRenderer renderer) {
		this();
		this.renderer = renderer;
	}

	/**
	 * Creates a new TagCloud with the given tags as elements.
	 *
	 * @param renderer the renderer to use in displaying the tags
	 * @param tags tag cloud entries
	 */
	public TagCloud(TagCloudEntryRenderer renderer, Collection<TagCloudEntry> tags) {
		this(renderer);
		setTagEntries(tags);
	}

	/**
	 * Adds a mouse listener that will be used for all tag cloud entries.
	 *
	 * @param l mouse listener
	 */
	public void addTagCloudEntryMouseListener(MouseListener l) {
		mouseListeners.add(l);
	}

	/**
	 * Change the current tag entries of this cloud.
	 *
	 * @param tags new tag entries
	 */
	public void setTagEntries(Collection<TagCloudEntry> tags) {
		this.tags = tags;
		removeAll();
		for (TagCloudEntry tag : tags) {
			NekoLabel tagLabel = new NekoLabel(renderer.getText(tag));
			if (tag.getMetadata() != null) {
				tagLabel.setToolTipText(tag.getMetadata());
			}

			float fontSize = 8 + (1.0f * tag.getScale() / TagCloudEntry.NUM_LEVELS) * 10;
			Font newFont = fontCache.get(fontSize);
			if (newFont == null) {
				newFont = baseFont.deriveFont(fontSize);
                fontCache.put(fontSize, newFont);
			}
			tagLabel.setFont(newFont);

			tagLabel.setForeground(tag.getColor());

//			log.debug(String.format("%s (%d) = %.1f", tag.getTag(), tag.getScale(), fontSize));

			tagLabel.addMouseListener(tagListener);
			add(tagLabel);
		}
	}

	/**
	 * Sends mouse events from tag cloud entries to all registered listeners.
	 */
	private class TagCloudEntryMouseListener implements MouseListener {

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseClicked(MouseEvent e) {
			for (MouseListener l : mouseListeners) {
				l.mouseClicked(e);
			}
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseEntered(MouseEvent e) {
			for (MouseListener l : mouseListeners) {
				l.mouseEntered(e);
			}
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseExited(MouseEvent e) {
			for (MouseListener l : mouseListeners) {
				l.mouseExited(e);
			}
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		@Override
		public void mousePressed(MouseEvent e) {
			for (MouseListener l : mouseListeners) {
				l.mousePressed(e);
			}
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseReleased(MouseEvent e) {
			for (MouseListener l : mouseListeners) {
				l.mouseReleased(e);
			}
		}
	}

	/**
	 * Defines how a TagCloudEntry should be displayed.
	 */
	public interface TagCloudEntryRenderer {
		public String getText(TagCloudEntry entry);
	}

	/**
	 * @param renderer the renderer to set
	 */
	public void setRenderer(TagCloudEntryRenderer renderer) {
		this.renderer = renderer;
		// need to change all the existing entries
		setTagEntries(tags);
	}
}
