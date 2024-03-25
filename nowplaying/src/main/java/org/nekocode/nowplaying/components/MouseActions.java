/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.components;

import org.nekocode.nowplaying.components.swing.NekoFrame;
import org.nekocode.nowplaying.components.swing.NekoFrame.AnchorPosition;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import static java.lang.Math.*;
import static java.lang.String.format;

public class MouseActions extends MouseInputAdapter {
	private Point offset;
    // location of last mouse event (for dragging), so we don't process the same event multiple times
    private Point lastPoint;

	private final NekoFrame frame;
	private final JPopupMenu menu;

	public MouseActions(NekoFrame frame) {
		this.frame = frame;
		this.menu = createPopupMenu();
	}

	private void showPopup(MouseEvent e) {
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	/**
     * Creates the popup menu. This only needs to be done once, since there is no dynamic data in the menu.
     *
     * @return popup menu
     */
    private JPopupMenu createPopupMenu() {
	    JPopupMenu m = new JPopupMenu();
		m.setLightWeightPopupEnabled(false);
		m.add(new AbstractAction("Hide") {
			public void actionPerformed(ActionEvent e) {
				frame.setState(JFrame.ICONIFIED);
			}
		});
		m.add(new AbstractAction("Close") {
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
			}
		});

		JMenu dockmenu = new JMenu("Dock to...");

		dockmenu.add(new AbstractAction("Dock to NW") {
			public void actionPerformed(ActionEvent e) {
				dock(AnchorPosition.NORTHWEST);
			}
		});
		dockmenu.add(new AbstractAction("Dock to NE") {
			public void actionPerformed(ActionEvent e) {
				dock(AnchorPosition.NORTHEAST);
			}
		});
		dockmenu.add(new AbstractAction("Dock to SE") {
			public void actionPerformed(ActionEvent e) {
				dock(AnchorPosition.SOUTHEAST);
			}
		});
		dockmenu.add(new AbstractAction("Dock to SW") {
			public void actionPerformed(ActionEvent e) {
				dock(AnchorPosition.SOUTHWEST);
			}
		});

		m.add(dockmenu);

		m.pack();
	    return m;
    }

	@Override
    public void mouseClicked(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 2) {
			// double click means hide the frame for 5 seconds
			frame.setVisible(false);
			Timer t = new Timer(0, null);
			t.stop();
			t.setInitialDelay(5000);
			t.setRepeats(false);
			t.addActionListener(e1 -> frame.setVisible(true));
			t.start();
		}
    }

	@Override
	public void mousePressed(MouseEvent e) {
		offset = new Point(
                e.getLocationOnScreen().x - frame.getLocationOnScreen().x,
                e.getLocationOnScreen().y - frame.getLocationOnScreen().y);

		if (e.isPopupTrigger()) {
			showPopup(e);
		} else {
			frame.setCursor(new Cursor(Cursor.MOVE_CURSOR));
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!SwingUtilities.isLeftMouseButton(e))
			return;
        if (e.getLocationOnScreen().equals(lastPoint)) {
            // if we've already received this mouse event, don't process it again
            return;
        }
        lastPoint = e.getLocationOnScreen();
        Point loc = new Point(
                e.getLocationOnScreen().x - offset.x,
                e.getLocationOnScreen().y - offset.y);

		frame.setLocation(loc.x, loc.y);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger()) {
			showPopup(e);
		} else {
			frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

    /**
     * Find the bounds of the screen device the window is current located in.
     *
     * @param x x coordinate of the corner to check for inclusion
     * @param y y coordinate of the corner to check for inclusion
     * @return bounds of device the corner is closest to
     */
	private Rectangle getScreenBounds(int x, int y) {
        Rectangle closestBounds = null;
        double closestDistance = Double.POSITIVE_INFINITY;

		for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			Rectangle bounds = device.getDefaultConfiguration().getBounds();

            // easy case - this device contains the point in question
            if (bounds.contains(x, y)) {
                return bounds;
            }

            // the point might be outside of any physical devices, so calculate which
            // one it is closest to
            double xDist = max(0, min(x-bounds.x+bounds.width, bounds.x - x));
            double yDist = max(0, min(y-bounds.y+bounds.height, bounds.y - y));
            double dist = sqrt(xDist * xDist + yDist * yDist);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestBounds = bounds;
            }
		}

		if (closestBounds == null)
			throw new HeadlessException(format("Unable to find point [%d, %d] within the bounds of any screen device", x, y));

        return closestBounds;
    }

	public PopupMenu getPopupMenu() {
		PopupMenu m = new PopupMenu();

		MenuItem hide = new MenuItem("Hide");
		hide.addActionListener(e -> frame.setState(JFrame.ICONIFIED));

		MenuItem close = new MenuItem("Close");
		close.addActionListener(e -> frame.dispose());

		Menu dockmenu = new Menu("Dock to...");

		MenuItem dockNW = new MenuItem("Dock to NW");
		dockNW.addActionListener(e -> dock(AnchorPosition.NORTHWEST));
		dockmenu.add(dockNW);
		MenuItem dockNE = new MenuItem("Dock to NE");
		dockNE.addActionListener(e -> dock(AnchorPosition.NORTHEAST));
		dockmenu.add(dockNE);
		MenuItem dockSE = new MenuItem("Dock to SE");
		dockSE.addActionListener(e -> dock(AnchorPosition.SOUTHEAST));
		dockmenu.add(dockSE);
		MenuItem dockSW = new MenuItem("Dock to SW");
		dockSW.addActionListener(e -> dock(AnchorPosition.SOUTHWEST));
		dockmenu.add(dockSW);

		m.add(dockmenu);
		m.add(hide);
		m.add(close);

	    return m;
	}

    private void dock(AnchorPosition anchorPosition)
    {
        frame.setAnchor(anchorPosition);
        Dimension size = frame.getSize();
        Rectangle bounds;
        switch (anchorPosition)
        {
            case NORTHWEST:
                bounds = getScreenBounds(frame.getX(), frame.getY());
                frame.setLocation(bounds.x, bounds.y);
                break;
            case NORTHEAST:
                bounds = getScreenBounds(frame.getX() + size.width, frame.getY());
                frame.setLocation(bounds.x + bounds.width - size.width, bounds.y);
                break;
            case SOUTHEAST:
                bounds = getScreenBounds(frame.getX() + size.width, frame.getY() + size.height);
                frame.setLocation(bounds.x + bounds.width - size.width, bounds.y + bounds.height - size.height);
                break;
            case SOUTHWEST:
                bounds = getScreenBounds(frame.getX(), frame.getY() + size.height);
                frame.setLocation(bounds.x, bounds.y + bounds.height - size.height);
                break;
        }
    }
}
