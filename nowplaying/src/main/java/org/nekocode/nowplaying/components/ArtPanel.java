/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.components;

import org.apache.log4j.Logger;
import org.nekocode.nowplaying.NowPlayingProperties;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints.Key;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.lang.Math.min;
import static java.lang.String.format;

/**
 * A panel that draws album art.
 *
 * @author fanguad@nekocode.org
 */
@SuppressWarnings("serial")
public class ArtPanel extends JPanel
{
	private static Logger log = Logger.getLogger(ArtPanel.class);

	/**
	 * An ID that gets incremented each time the art changes.  This can be used
	 * by outside components to see if they are out of date.
	 * Kind of a hack.
	 */
	private int artId = 0;
	private Image art;
	private Image background;
	private int fixedWidth;
	private int maxSize;
	private float transparency;

	private int artXOffset;

	private int cornerRadius;
    private static final Map<Key, Object> RENDERING_HINTS = Collections.singletonMap(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

    /**
	 * Creates an ArtPanel - a transparent, rounded corner JPanel that displays
	 * a given image.
	 *
	 * @param maxSize the maximum width and height of this panel
	 * @param fixedwidth if true, the width will always equal maxSize
	 * @param fixedheight if true, the height will always equal maxSize
	 * 					  TODO not yet implemented
	 */
	public ArtPanel(int maxSize, boolean fixedwidth, boolean fixedheight) {
        super();
        setOpaque(false);

        String cornerRadiusString = NowPlayingProperties.loadProperties().getProperty(NowPlayingProperties.CORNER_RADIUS.name());
        double cornerRadiusPercent = 0.05; // each side is 5% corner-90% flat-5% corner
        if (cornerRadiusString != null) {
            try {
                cornerRadiusPercent = Double.parseDouble(cornerRadiusString);
            } catch (NumberFormatException e) {
                // leave as default
            }
        }

        this.maxSize = maxSize;
        this.cornerRadius = (int) (maxSize * cornerRadiusPercent);

        if (fixedwidth)
            fixedWidth = maxSize;
        try {
            String transparencyProperty = NowPlayingProperties.loadProperties().getProperty(
                    NowPlayingProperties.TRANSPARENCY.name());
            if (transparencyProperty != null) {
                transparency = Float.parseFloat(transparencyProperty);
            } else {
                throw new NumberFormatException(); // "null" is an invalid number, too
            }
        } catch (NumberFormatException e) {
            transparency = 1;
            NowPlayingProperties.loadProperties().setProperty(
                    NowPlayingProperties.TRANSPARENCY.name(), "1");
        }
    }

	/**
	 * Sets the background image of this panel.
	 *
	 * @param art background image - must be fully loaded
	 */
	public void setArt(Image art) {
		log.debug("art changed");
		this.art = art;
        background = null;
		artId++;
	}

	/**
	 * Sets the background image of this panel.
	 *
	 * @param art background image
	 */
	public void setArt(ImageIcon art) {
		setArt(art.getImage());
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(Graphics g)
	{
        if (background == null) {
            background = mergeLayers();
		}
		g.drawImage(background, 0, 0, null);
	}

    /**
     * Creates the background image that will be used.  This image includes any
     * transparency, and should be cached.
     */
    private void calculateArtOffsets() {
        if (art != null) {
            artXOffset = 0;
            // if using fixed width, center in available space
            int imageWidth = art.getWidth(null);
            if (fixedWidth > 0) {
                artXOffset = (fixedWidth - imageWidth) / 2;
            }
            // if the image is less than the max width and foreground components
            // are larger than image, center image
            Dimension superSize = super.getPreferredSize();
            int actualWidth = min(maxSize, superSize.width); // won't be wider than maxSize
            if (actualWidth > imageWidth ) {
                artXOffset = (actualWidth - imageWidth) / 2;
            }
        }
    }

    /**
     * Masks the area represented by the provided graphics object.
     *
     * @param g2           graphics object into which to draw the mask
     * @param width        width of mask
     * @param height       height of mask
     * @param x            x offset of mask
     * @param y            y offset of mask
     * @param curveRadius  size of corner radius, in pixels
     * @param transparency transparency of mask
     */
    static void maskImage(Graphics2D g2, int width, int height, int x, int y, int curveRadius, float transparency) {
        int curveDiameter = curveRadius * 2;

        g2.addRenderingHints(RENDERING_HINTS);
        g2.setColor(new Color(1, 1, 1, transparency));
        // the center of the area, including the top & bottom sections
        g2.fillRect(x + curveRadius, y, width - 2 * curveRadius, height);
        g2.fillRect(x, y + curveRadius, curveRadius, height - 2 * curveRadius); // the right section
        g2.fillRect(x + width - curveRadius, y + curveRadius, curveRadius, height - 2 * curveRadius); // the left section
        // draw the corners
        g2.fillArc(x + width - curveDiameter, y, curveDiameter, curveDiameter, 0, 90);        // upper right
        g2.fillArc(x, y, curveDiameter, curveDiameter, 90, 90);                         // upper left
        g2.fillArc(x, y + height - curveDiameter, curveDiameter, curveDiameter, 180, 90);    // lower left
        g2.fillArc(x + width - curveDiameter, y + height - curveDiameter, curveDiameter, curveDiameter, 270, 90); // lower right
    }

    /* (non-Javadoc)
      * @see javax.swing.JComponent#getPreferredSize()
      */
	@Override
	public Dimension getPreferredSize() {
		Dimension size = super.getPreferredSize();
		int height = 0, width = 0;
		if (art == null) {
			width = maxSize;
            log.debug("getPreferredSize: no art");
		} else {
			height = art.getHeight(null);
			width = art.getWidth(null);
            log.debug(format("getPreferredSize: art size = [%d, %d]", width, height));
		}
		size.height = Math.max(height, size.height);
		size.width = Math.max(width, size.width);

		// even if it isn't fixed width, the minimum size is still constrained
		size.height = Math.min(maxSize, size.height);
		size.width = Math.min(maxSize, size.width);

		if (fixedWidth > 0)
			size.width = fixedWidth;
		return size;
	}

	/**
	 * Removes the art from the panel
	 */
	public void clearArt()
	{
		setArt((Image)null);
	}

	/**
	 * art = artwork (may be null)
	 * windowMask = rounded outside corners
	 * progress = wedge of progress
	 * progressMask = inner mask
	 *
	 * @return
	 */
	private BufferedImage mergeLayers() {
		Dimension size = this.getPreferredSize();

		// see: http://weblogs.java.net/blog/campbell/archive/2006/07/java_2d_tricker.html
		//   also: http://java.sun.com/docs/books/tutorial/2d/advanced/compositing.html

		BufferedImage background = new BufferedImage(size.width, size.height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = (Graphics2D)background.getGraphics();
		maskImage(g, size.width, size.height, 0, 0, cornerRadius, transparency); // for the default white background

		// draw the art
		g.setComposite(AlphaComposite.SrcIn);
        calculateArtOffsets();
		g.drawImage(art, artXOffset, 0, null);

		// make the corners transparent
		g.setComposite(AlphaComposite.DstIn);
        maskImage(g, size.width, size.height, 0, 0, cornerRadius, transparency); // for the default white background

		g.dispose();

		return background;
	}

	/**
	 * @return the cornerRadius
	 */
	public int getCornerRadius() {
		return cornerRadius;
	}

	/**
	 * @return the artId
	 */
	public int getArtId() {
		return artId;
	}
}
