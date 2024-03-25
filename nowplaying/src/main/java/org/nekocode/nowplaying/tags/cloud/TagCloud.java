/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.tags.cloud;

import lombok.extern.log4j.Log4j2;
import org.nekocode.nowplaying.components.swing.NekoLabel;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;

/**
 * Displays a cloud of tags.
 *
 * @author dan.clark@nekocode.org
 */
@Log4j2
public class TagCloud extends ScrollableFlowPanel {
	private Collection<TagCloudEntry> tags;

	private final Map<Float, Font> fontCache;

	private final TagCloudEntryMouseListener tagListener;
	private final Set<MouseListener> mouseListeners;

	private final TagCloudEntryRenderer defaultRenderer = TagCloudEntry::getTag;
	private TagCloudEntryRenderer renderer = defaultRenderer;
    private final Font baseFont;

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
			float fontSize = 8 + (1.0f * tag.getScale() / TagCloudEntry.NUM_LEVELS) * 10;
			Font newFont = fontCache.get(fontSize);
			if (newFont == null) {
				newFont = baseFont.deriveFont(fontSize);
                fontCache.put(fontSize, newFont);
			}
			NekoLabel tagLabel = new NekoLabel(renderer.getText(tag));
			tagLabel.setForeground(tag.getColor());
			tagLabel.setFont(newFont);
            tagLabel.setPreferredSize(tagLabel.getPreferredSize());

			if (tag.getMetadata() != null) {
				tagLabel.setToolTipText(tag.getMetadata());
			}

//			log.debug(String.format("%s (%d) = %.1f", tag.getTag(), tag.getScale(), fontSize));

			tagLabel.addMouseListener(tagListener);
			add(tagLabel);
		}
	}

	/**
	 * Sends mouse events from tag cloud entries to all registered listeners.
	 */
	private class TagCloudEntryMouseListener implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			for (MouseListener l : mouseListeners) {
				l.mouseClicked(e);
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			for (MouseListener l : mouseListeners) {
				l.mouseEntered(e);
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {
			for (MouseListener l : mouseListeners) {
				l.mouseExited(e);
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			for (MouseListener l : mouseListeners) {
				l.mousePressed(e);
			}
		}

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
