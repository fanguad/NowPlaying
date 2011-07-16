/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.components.swing;

import org.apache.log4j.Logger;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;

@SuppressWarnings("serial")
public class NekoFrame extends JFrame {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(NekoFrame.class);
	private AnchorPosition anchor;

	public enum AnchorPosition {
		NORTHWEST, NORTHEAST, SOUTHEAST, SOUTHWEST
	}

	public NekoFrame(String title) {
		super(title);
		anchor = AnchorPosition.NORTHWEST;
	}

	/**
	 * Does pack, but keeps the lower right corner in the same place.
	 * Normally, the upper left corner is the one that stays put.
	 *
	 * @see Window#pack()
	 */
	@Override
	public void pack() {
		Dimension beforeSize = this.getSize();
		Point location = this.getLocation();
		super.pack();
		Dimension afterSize = this.getSize();

		log.debug("Adjusting position: anchor corner = " + anchor);
		// move the window so that anchor corner will remain in the same position
		switch (anchor) {
		// case NORTHWEST:
			// no movement necessary
		case NORTHEAST:
			// only x movement necessary - pick it up in fall-through
		case SOUTHEAST:
			// both x and y necessary - fall through for y
			location.x += beforeSize.width - afterSize.width;
			if (anchor == AnchorPosition.NORTHEAST) {
				break;
			}
		case SOUTHWEST:
			location.y += beforeSize.height - afterSize.height;
		}

		this.setLocation(location);
	}

	/**
     * Sets the anchored corner of the frame. This is the corner that will remain in the same
     * location when the size of the frame changes. The default (which matches the defaults of
     * window managers the author knows of) is NORTHWEST.
     *
     * @param anchor new anchor corner.  NORTHWEST will be used if anchor is null.
     */
	public void setAnchorPosition(AnchorPosition anchor) {
		this.anchor = anchor == null ? AnchorPosition.NORTHWEST : anchor;
	}
}
