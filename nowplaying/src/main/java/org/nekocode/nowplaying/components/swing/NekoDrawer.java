/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.swing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nekocode.nowplaying.resources.images.Icons;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JWindow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * A drawer that can be attached to the side of another window.
 *
 * @author dan.clark@nekocode.org
 */
@SuppressWarnings("serial")
public class NekoDrawer extends JWindow {
	private static final Logger log = LogManager.getLogger(NekoDrawer.class);
	
	// TODO: make images not hard-coded
	/** left side of tab - right side is same, but flipped **/
	private static final BufferedImage tab_left;
	private static final BufferedImage tab_right;
	/** one pixel wide center of tab */
	private static final BufferedImage tab_center;
	
	private static final int edge_width;
	
	static {
        System.setProperty("sun.java2d.noddraw", "true");

		BufferedImage temp_left;
		BufferedImage temp_center;
		int temp_width;
		try {
			temp_left = ImageIO.read(Icons.class.getResource("xp_edge.png"));
			temp_center = ImageIO.read(Icons.class.getResource("xp_center.png"));
			temp_width = temp_left.getWidth();
        } catch (IOException e) {
	        // TODO make it so we're not totally screwed
        	log.fatal(e);
        	BufferedImage image = new BufferedImage(1, 15, BufferedImage.TYPE_3BYTE_BGR);
        	Graphics g = image.getGraphics();
        	g.setColor(Color.LIGHT_GRAY);
        	g.fillRect(0, 0, 1, 15);
        	temp_left = temp_center = image;
        	temp_width = 1;
        }
        
    	tab_left = temp_left;
    	tab_center = temp_center;
    	edge_width = temp_width;
    	
    	// make tab_right
    	AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-edge_width, 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        tab_right = op.filter(tab_left, null);	
	}
	
	private Rotation rotation;
	private Component content;
	
	private Window owner;
	private boolean open;

	private ImageIcon closedTab;

	private NekoButton tab;

	/**
	 * Creates a new NekoDrawer attached the specified window.  The width of the
	 * tab can be specified.
	 * 
	 * TODO make a safeguard so that the width isn't wider than the parent.  Be careful of parents that don't know how wide they're supposed to be
	 * 
	 * @param owner
	 * @param content
	 * @param width
	 */
	public NekoDrawer(Window owner, Rotation rotation, Component content, int width) {
		super(owner);
		this.owner = owner;
		this.content = content;
		this.rotation = rotation;
		
		closedTab = createTab(width);
        this.setBackground(new Color(0,0,0,0));
        
		tab = new NekoButton("", rotation);
		tab.setIcon(closedTab);

		tab.addActionListener(e -> flip_drawer());
		open = false; // drawer not shown initially
		
		// initial position is closed
		setContentPane(new JPanel(new BorderLayout()));
		
		switch (rotation) {
		case NONE:
			getContentPane().add(tab, BorderLayout.NORTH);
			break;
		case COUNTER_CLOCKWISE:
			getContentPane().add(tab, BorderLayout.WEST);
			break;
		case FLIPPED:
			getContentPane().add(tab, BorderLayout.SOUTH);
			break;
		case CLOCKWISE:
			getContentPane().add(tab, BorderLayout.EAST);
			break;
		}
		
		addOwnerListeners();
	}

	/**
	 * Opens or closes the drawer.
	 */
	protected void flip_drawer() {
	    open = !open;
		if (open) {
			// drawer is now open
			getContentPane().add(content, BorderLayout.CENTER);
			tab.setIcon(createTab(content.getPreferredSize().width));
		} else {
			// drawer is now closed
			getContentPane().remove(content);
			tab.setIcon(closedTab);
		}
		setPosition();
    }
	
	private void addOwnerListeners() {
		ComponentAdapter componentAdapter = new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				setPosition();
//				toFront();
			}

			@Override
            public void componentHidden(ComponentEvent e) {
				setVisible(false);
            }

			@Override
            public void componentShown(ComponentEvent e) {
				setVisible(true);
            }

			@Override
			public void componentResized(ComponentEvent e) {
				// make it so the drawer doesn't exceed the height/width (as appropriate)
				Dimension parentSize = owner.getSize();
				switch (rotation) {
				case NONE:
				case FLIPPED:
					NekoDrawer.this.setMaximumSize(new Dimension(Integer.MAX_VALUE, parentSize.width));
					break;
				case CLOCKWISE:
				case COUNTER_CLOCKWISE:
					NekoDrawer.this.setMaximumSize(new Dimension(parentSize.height, Integer.MAX_VALUE));
					break;
				}
			}
		};
		// set the initial maximum size
		componentAdapter.componentResized(null);
		owner.addComponentListener(componentAdapter);
		
		owner.addWindowListener(new WindowAdapter() {
			@Override
            public void windowDeiconified(WindowEvent e) {
				setVisible(true);
            }

			@Override
            public void windowIconified(WindowEvent e) {
				setVisible(false);
            }

			@Override
            public void windowClosed(WindowEvent e) {
				dispose();
            }

			@Override
            public void windowGainedFocus(WindowEvent e) {
				setVisible(true);
				toFront();
            }

			@Override
            public void windowLostFocus(WindowEvent e) {
				setVisible(false);
            }
		});
		
		setFocusableWindowState(true);
		setPosition();
		
		this.content.addComponentListener(new ComponentAdapter() {
			@Override
            public void componentResized(ComponentEvent e) {
				setPosition();
            }});
	}

	private void setPosition() {
        // code to follow our parent around
		
		// first, find out how big we are
		pack();
		Dimension size = getPreferredSize();
		int x, y;
		Point parentLocation = owner.getLocation();
		Dimension parentSize = owner.getSize();
		
//		log.debug("setPosition: Rotation is " + rotation);
		
		// line us up with top/bottom/center of frame
		switch (this.rotation) {
		case COUNTER_CLOCKWISE:
			x = parentLocation.x - size.width;
			break;
		case CLOCKWISE:
			x = parentLocation.x;
			break;
		default: // page-oriented
			x = parentLocation.x + (parentSize.width - size.width) / 2;
		}
		
		switch (this.rotation) {
		case NONE:
			y = parentLocation.y - size.height;
			break;
		case FLIPPED:
			y = parentLocation.y;
			break;
		default: // line-oriented
			y = parentLocation.y + (parentSize.height - size.height) / 2;
		}
		
		setLocation(new Point(x, y));
//		log.debug(String.format("parent position is  (%d, %d)", parentLocation.x, parentLocation.y));
//		log.debug(String.format("setting position to (%d, %d)", x, y));
    }

	private ImageIcon createTab(int width) {
	    // figure out "actual" width (since the edge_width * 2 is minimum)
		width = Math.max(width, edge_width * 2);
//		log.debug(String.format("width: %d, edge_width: %d", width, edge_width));
		
		// get the final image
		BufferedImage image = new BufferedImage(width, tab_center.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics g = image.getGraphics();
		g.drawImage(tab_left, 0, 0, null);
//		log.debug(String.format("drawing left at (%d, %d)", 0, 0));
		g.drawImage(tab_right, width - edge_width, 0, null);
//		log.debug(String.format("drawing right at (%d, %d)", width - edge_width, 0));
		// draw left and right
		for (int i = edge_width; i < width - edge_width; i++ ) {
			g.drawImage(tab_center, i, 0, null);
//			log.debug(String.format("drawing center at (%d, %d)", i, 0));
		}
        return new ImageIcon(image);
    }
}
