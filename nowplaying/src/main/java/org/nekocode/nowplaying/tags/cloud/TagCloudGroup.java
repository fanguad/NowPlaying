/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.tags.cloud;

import java.awt.*;

/**
 * A tag cloud entry that represents a group of tracks instead of a tag.
 *
 * @author dan.clark@nekocode.org
 */
public class TagCloudGroup extends TagCloudEntry {

	/**
	 * Creates a TagCloudGroup.
	 *
	 * @param groupName name of group
	 */
	public TagCloudGroup(String groupName) {
		super(groupName, 0, -1);
	}

	@Override
	public Color getColor() {
		return Color.blue;
	}

}
