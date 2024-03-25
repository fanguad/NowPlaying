/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.components.swing;

import javax.swing.*;
import java.awt.*;

/**
 * Like a JLabel, but uses antialiased fonts and adds a glow effect.
 *
 * @author dan.clark@nekocode.org
 */
public class NekoLabel extends JLabel
{
    public NekoLabel(String text) {
		super(text);
		this.setUI(new NekoLabelUI());
	}

	public NekoLabel(String text, int horizontalAlignment) {
		super(text, horizontalAlignment);
		this.setUI(new NekoLabelUI());
	}

	public NekoLabel(String text, Rotation rotation) {
		super(text);
		this.setUI(new NekoLabelUI(rotation));
	}

    @Override
    public void updateUI() {
        // UI has already been set in the constructor
    }

    /**
	 * Finds this component's top-level frame.
	 *
	 * @return
	 */
	public Window getTopLevelFrame() {
		// find the top level frame
		Container myParent = this.getParent();
		Window frame = null;
		while (myParent != null) {
			if (myParent instanceof Window) {
				frame = (Window) myParent;
				break;
			}
			myParent = myParent.getParent();
		}
		return frame;
	}
}
