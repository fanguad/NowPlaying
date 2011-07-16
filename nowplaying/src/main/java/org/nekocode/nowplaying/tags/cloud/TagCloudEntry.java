/*
 * Copyright (c) 2010, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.tags.cloud;

import java.awt.Color;

import org.apache.log4j.Logger;

public class TagCloudEntry implements Comparable<TagCloudEntry> {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(TagCloudEntry.class);

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
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
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
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getTag();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TagCloudEntry)
			return tag.equals(((TagCloudEntry)obj).tag);
		return false;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return tag.hashCode();
	}

	public Color getColor() {
		return Color.black;
	}
}
