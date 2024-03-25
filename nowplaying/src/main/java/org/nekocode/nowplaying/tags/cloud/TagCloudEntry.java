/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.tags.cloud;

import java.awt.*;

public class TagCloudEntry implements Comparable<TagCloudEntry> {
	public static final int NUM_LEVELS = 20;

	private String tag;
	private String metadata;
	private int count;
	private int scale;

	public String getTag() { return tag; }
	public void setTag(String tag) { this.tag = tag; }

	public int getCount() { return count; }
	public void setCount(int count) { this.count = count; }

	/**
	 * Constructor.
	 *
	 * @param tag name of tag
	 * @param metadata additional information about the tag
	 * @param count number of times this tag occurs
	 * @param maximumCount the maximum count of any tag in the system
	 */
	public TagCloudEntry(String tag, String metadata, int count, int maximumCount) {
		this.tag = tag;
		this.count = count;
		this.metadata = metadata;

		if (maximumCount <= 0)
			scale = 0;
		else
			setScale((getCount() * NUM_LEVELS) / maximumCount);
	}

	public TagCloudEntry(String tag, int count, int maximumCount) {
		this(tag, "", count, maximumCount);
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		if (scale < 0)
			scale = 0;
		if (scale >= NUM_LEVELS)
			scale = NUM_LEVELS-1;
		this.scale = scale;
	}
	@Override
	public int compareTo(TagCloudEntry o) {
		return tag.compareTo(o.tag);
	}
	/**
	 * @return the metadata
	 */
	public String getMetadata() {
		return metadata;
	}
	/**
	 * @param metadata the metadata to set
	 */
	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
	@Override
	public String toString() {
		return getTag();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TagCloudEntry && tag.equals(((TagCloudEntry) obj).tag);
	}
	@Override
	public int hashCode() {
		return tag.hashCode();
	}

	public Color getColor() {
		return Color.black;
	}
}
