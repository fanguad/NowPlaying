/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.swing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Like a JLabel, but uses antialiased fonts and adds a glow effect.
 *
 * @author dan.clark@nekocode.org
 */
@SuppressWarnings("serial")
public class NekoLabel extends JLabel
{
    @SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(NekoLabel.class);

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
