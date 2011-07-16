/*
 * Copyright (c) 2010, fanguad@nekocode.org
 */

/*
 * Filename:   TagCloudGroup.java
 * Created On: Sep 18, 2009
 */
package org.nekocode.nowplaying.tags.cloud;

import java.awt.Color;

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

	/* (non-Javadoc)
	 * @see org.nekocode.nowplaying.tags.cloud.TagCloudEntry#getColor()
	 */
	@Override
	public Color getColor() {
		return Color.blue;
	}

}
