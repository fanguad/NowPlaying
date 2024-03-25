/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.components.swing;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * The content pane of a NekoFrame. Items added to a NekoPanel will show up as
 * drawers on the specified side of the panel.
 * 
 * @author dan.clark@nekocode.org
 */
public class NekoPanel extends JPanel {
	public enum BorderPositions {
		PAGE_START(BorderLayout.PAGE_START, Rotation.NONE),
		PAGE_END(BorderLayout.PAGE_END, Rotation.FLIPPED),
		LINE_START(BorderLayout.LINE_START, Rotation.COUNTER_CLOCKWISE),
		LINE_END(BorderLayout.LINE_END, Rotation.CLOCKWISE);

		private final String position;
		@Getter
		private final Rotation rotation;
		private BorderPositions(String layoutPosition, Rotation rotation) {
			this.position = layoutPosition;
			this.rotation = rotation;
		}
		public String getLayoutPosition() {
			return position;
		}

		public BorderPositions getOpposite() {
			BorderPositions opposite = switch (this) {
                case LINE_END -> LINE_START;
                case LINE_START -> LINE_END;
                case PAGE_START -> PAGE_END;
                default -> PAGE_START;
            };
            return opposite;
		}
	}

	/**
	 * Default constructor.
	 */
	public NekoPanel() {
		super(new BorderLayout());
	}

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
	 * @param comp         The component to be added to the drawer.
	 * @param constraints  The position of the drawer where the component will be added.
	 */
	public void add(Component comp, BorderPositions constraints) {
		NekoDrawer bw = new NekoDrawer(getTopLevelFrame(this), constraints.rotation, comp, 64);
		bw.setVisible(true);
	}

	/**
    * Finds this panel's top-level frame (to which the drawers will be
    * attached).
    *
	 * @param parent the NekoPanel from which to start searching for the top-level frame
	 * @return the top-level JFrame that the drawers will be attached to
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
