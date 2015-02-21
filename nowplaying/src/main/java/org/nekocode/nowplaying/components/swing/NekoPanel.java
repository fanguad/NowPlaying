/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.swing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;

/**
 * The content pane of a NekoFrame. Items added to a NekoPanel will show up as
 * drawers on the specified side of the panel.
 * 
 * @author fanguad@nekocode.org
 */
@SuppressWarnings("serial")
public class NekoPanel extends JPanel {
	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(NekoPanel.class);

	public enum BorderPositions {
		PAGE_START(BorderLayout.PAGE_START, Rotation.NONE),
		PAGE_END(BorderLayout.PAGE_END, Rotation.FLIPPED),
		LINE_START(BorderLayout.LINE_START, Rotation.COUNTER_CLOCKWISE),
		LINE_END(BorderLayout.LINE_END, Rotation.CLOCKWISE);

		private String position;
		private Rotation rotation;
		private BorderPositions(String layoutPosition, Rotation rotation) {
			this.position = layoutPosition;
			this.rotation = rotation;
		}
		public String getLayoutPosition() {
			return position;
		}
		public Rotation getRotation() {
			return rotation;
		}
		public BorderPositions getOpposite() {
			BorderPositions opposite;
			switch (this) {
			case LINE_END:
				opposite = LINE_START;
				break;
			case LINE_START:
				opposite = LINE_END;
				break;
			case PAGE_START:
				opposite = PAGE_END;
				break;
			case PAGE_END:
			default:
				opposite = PAGE_START;
			}
			return opposite;
		}
	}

	/**
	 * Default constructor.
	 */
	public NekoPanel() {
		super(new BorderLayout());
	}

	/* (non-Javadoc)
	 * @see java.awt.Container#add(java.awt.Component, java.lang.Object)
	 */
	@Override
	public void add(Component comp, Object constraints) {
		if (constraints instanceof BorderPositions) {
			add(comp, (BorderPositions)constraints);
		} else {
			super.add(comp, constraints);
		}
	}

	/**
     * Adds the specified component to a drawer in the specified position.
     * 
	 * @param comp
	 * @param constraints
	 */
	public void add(Component comp, BorderPositions constraints) {
		NekoDrawer bw = new NekoDrawer(getTopLevelFrame(this), constraints.rotation, comp, 64);
		bw.setVisible(true);
	}

	/**
    * Finds this panel's top-level frame (to which the drawers will be
    * attached).
    * 
    * @param parent
    * @return
    */
	private static JFrame getTopLevelFrame(NekoPanel parent) {
        // find the top level frame
		Container myParent = parent;
		JFrame frame = null;
		while (myParent != null) {
			myParent = myParent.getParent();
			if (myParent instanceof JFrame) {
				frame = (JFrame) myParent;
				break;
			}
		}
        return frame;
    }
}
