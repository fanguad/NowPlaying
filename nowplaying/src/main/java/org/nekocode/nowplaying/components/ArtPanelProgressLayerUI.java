/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

/*
 * Filename:   ArtPanelProgressLayerUI.java
 * Created On: Oct 1, 2009
 */
package org.nekocode.nowplaying.components;

import java.awt.*;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.plaf.LayerUI;

import org.apache.log4j.Logger;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.events.TrackChangeListener;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.lang.Math.PI;

/**
 * A layer that draws a progress bar around the ArtPanel.
 *
 * @author dan.clark@nekocode.org
 */
public class ArtPanelProgressLayerUI extends LayerUI<JComponent> implements TrackChangeListener {
	private static Logger log = Logger.getLogger(ArtPanelProgressLayerUI.class);
    private static final Map<Key, Object> RENDERING_HINTS =
        Collections.singletonMap(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    private static final String DIRTY_SONG = "dirty song";
    private static final String DIRTY = "dirty";

	private final int borderSize;
	private final Color progressColor;

	/**
	 * The song has changed.
	 */
	private boolean dirtySong;

	private int lastArtId;

	/**
	 * Number of pixels of the border that were filled in as of the last update.
	 */
	private double lastFilledLength;
    /**
     * Number of pixels of the border that are currently filled in.
     */
	private double currentFilledLength;

	private BufferedImage corner;
    private BufferedImage partial;
	private final ArtPanel artPanel;

	// cached information about the current panel
	private int cornerDiameter;
	private int cornerRadius;
	private int[] sectionLengths;
	private int height;
	private int width;
	// total length (in pixels) of the layer's perimeter
	private int perimeter;
	// lengths (in pixels) of the parts of the perimeter
	private double cornerLength;
	private int side;
	private int bottom;
	private int topHalf;

	// clip regions, so we don't have to unnecessarily draw stuff
	private Rectangle clipAll;
    private Rectangle clipNE;
    private Rectangle clipSE;
    private Rectangle clipSW;
    private Rectangle clipNW;

    public ArtPanelProgressLayerUI(ArtPanel artPanel, int borderSize, Color progressColor) {
		this.borderSize = borderSize;
		this.progressColor = progressColor;
		this.artPanel = artPanel;
		// always initialize things the first time this is displayed
		dirtySong = true;
		sectionLengths = new int[9];
	}

    /**
     * {@inheritDoc}
     *
     * The JComponent passed in is actually the JLayer wrapping the component.
     */
	@Override
    public void paint(Graphics g, JComponent component) {
		super.paint(g, component);

		boolean dirtyPanel = lastArtId != artPanel.getArtId();
		lastArtId = artPanel.getArtId();

		// has the song changed, or time gone down?
		if (dirtySong || dirtyPanel) {
			log.debug(String.format("song: %s, panel: %s", dirtySong, dirtyPanel));

			initializeLayer(component);
		}

		drawTrackProgress(g);

		// song hasn't changed
		dirtySong = false;
	}

	private void initializeLayer(JComponent component) {
        if (cornerRadius != artPanel.getCornerRadius()) {
            cornerRadius = artPanel.getCornerRadius();
            cornerDiameter = cornerRadius * 2;

            corner = new BufferedImage(cornerRadius, cornerRadius, BufferedImage.TYPE_4BYTE_ABGR);
            partial = new BufferedImage(cornerRadius, cornerRadius, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = (Graphics2D) corner.getGraphics();
            g.setRenderingHints(RENDERING_HINTS);
            g.setColor(progressColor);
            g.fillArc(-cornerRadius, 0, cornerDiameter, cornerDiameter, 90, -90); // arc for NE corner
            g.setComposite(AlphaComposite.DstOut);
            g.fillArc(-cornerRadius+borderSize, borderSize,
                    cornerDiameter - borderSize*2, cornerDiameter - borderSize*2, 90, -90); // arc for NE corner
            g.dispose();
        }

        width = component.getWidth();
        height = component.getHeight();
        
		perimeter = 2*width + 2*height - 8* cornerRadius + (int)(2*PI* cornerRadius);

		topHalf = width / 2 - cornerRadius;
		bottom = width - 2* cornerRadius;
		side = height - 2* cornerRadius;
		cornerLength = PI / 2 * cornerRadius;

		sectionLengths[0] = topHalf;
		sectionLengths[1] = sectionLengths[0] + (int)cornerLength;
		sectionLengths[2] = sectionLengths[1] + side;
		sectionLengths[3] = sectionLengths[0] + side + (int)(2 * cornerLength);
		sectionLengths[4] = sectionLengths[3] + bottom;
		sectionLengths[5] = sectionLengths[0] + side + bottom + (int)(3 * cornerLength);
		sectionLengths[6] = sectionLengths[5] + side;
		sectionLengths[7] = sectionLengths[0] + side + side + bottom + (int)(4 * cornerLength);
		sectionLengths[8] = perimeter; // do I need this one?

		clipAll = new Rectangle(0, 0, width, height);

        clipNE = new Rectangle(width- cornerRadius, 0, cornerRadius, cornerRadius);
        clipSE = new Rectangle(width- cornerRadius, height- cornerRadius, cornerRadius, cornerRadius);
        clipSW = new Rectangle(0, height- cornerRadius, cornerRadius, cornerRadius);
        clipNW = new Rectangle(0, 0, cornerRadius, cornerRadius);
	}

    /**
     * Draws the current track progress into the progress bar.
     * Track progress (currentFilledLength, in pixels) is set prior to invoking this method.
     *
     * @param g2 graphics object to draw progress bar into
     */
    private void drawTrackProgress(Graphics g2) {
		double filledLength = currentFilledLength;

		Graphics2D g = (Graphics2D) g2;
        g.setRenderingHints(RENDERING_HINTS);
		g.setColor(progressColor);

        // find out which section we're currently in by looping
        int i = 0;
        // find out if we drew any corners (and will need compositing)
		for (; i < sectionLengths.length && sectionLengths[i] < filledLength; i++) {
            // ensure that each previous section is fully filled in (to prevent gaps)
			fillSection(g, i);
		}

        log.debug(String.format("Drawing Progress [%s]: %s", i, filledLength));
        if (i > 0) {
            // change filledLength from the total # of pixels filled into the number
            // of pixels filled of the current section
            filledLength -= sectionLengths[i-1];
        }
        double degrees = (int) (filledLength / cornerLength * -90);
        AffineTransform originalTransform = g.getTransform();

        switch (i) {
		case 8:
			g.fill(new Rectangle2D.Double(cornerRadius, 0, filledLength, borderSize));
			break;
		case 7:
            g.setTransform(createCornerTransform(270, cornerRadius, cornerRadius, cornerRadius, 0));
            g.drawImage(getPartialCorner(degrees), 0, 0, null);
            break;
		case 6:
			// starts at bottom, so fillRect parameters need to be switched around
			g.fill(new Rectangle2D.Double(0, cornerRadius + side - filledLength, borderSize, filledLength));
			break;
		case 5:
            g.setTransform(createCornerTransform(180, cornerRadius, height - cornerRadius, cornerRadius, height - cornerDiameter));
            g.drawImage(getPartialCorner(degrees), 0, 0, null);
			break;
		case 4:
			// starts at right, so fillRect parameters need to be switched around
			g.fill(new Rectangle2D.Double(cornerRadius + bottom - filledLength,
                    height - borderSize, filledLength, borderSize));
			break;
		case 3:
            g.setTransform(createCornerTransform(90, width - cornerRadius, height - cornerRadius, width - cornerRadius, height - cornerDiameter));
            g.drawImage(getPartialCorner(degrees), 0, 0, null);
			break;
		case 2:
			g.fill(new Rectangle2D.Double(width - borderSize, cornerRadius, borderSize, filledLength));
			break;
		case 1:
            g.drawImage(getPartialCorner(degrees), width - cornerRadius, 0, null);
			break;
		case 0:
			g.fill(new Rectangle2D.Double(width / 2, 0, filledLength, borderSize));
			break;
		}
        g.setTransform(originalTransform);
	}

    /**
     * Completely fill in a progress bar section.
     * Used when jumping to a new position.
     * Also used after a section has been completed to ensure that there are no gaps.
     *
     * @param g graphics object to draw into
     * @param section section to fill
     * @see #getSection(double)
     */
    private void fillSection(Graphics2D g, int section) {
        AffineTransform originalTransform = g.getTransform();
        switch (section) {
            case 7:
                g.setTransform(AffineTransform.getRotateInstance(Math.toRadians(270), cornerRadius, cornerRadius));
                g.drawImage(corner, cornerRadius, 0, null);
                break;
            case 6:
                g.fill(new Rectangle2D.Double(0, cornerRadius, borderSize, side));
                break;
            case 5:
                g.setTransform(AffineTransform.getRotateInstance(Math.toRadians(180), cornerRadius, height - cornerRadius));
                g.drawImage(corner, cornerRadius, height - cornerDiameter, null);
                break;
            case 4:
                g.fill(new Rectangle2D.Double(cornerRadius, height - borderSize, bottom, borderSize));
                break;
            case 3:
                g.setTransform(AffineTransform.getRotateInstance(Math.toRadians(90), width - cornerRadius, height - cornerRadius));
                g.drawImage(corner, width - cornerRadius, height - cornerDiameter, null);
                break;
            case 2:
                g.fill(new Rectangle2D.Double(width - borderSize, cornerRadius, borderSize, side));
                break;
            case 1:
                g.drawImage(corner, width - cornerRadius, 0, null);
                break;
            case 0:
                g.fill(new Rectangle2D.Double(width / 2, 0, topHalf, borderSize));
                break;
        }
        g.setTransform(originalTransform);
    }

    /**
     * Clips the corner image to the given number of degrees (<= 90).
     * @param degrees # of degrees to show (max = 90)
     * @return image to display
     */
    private Image getPartialCorner(double degrees) {
        Graphics2D g = (Graphics2D) partial.getGraphics();
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, cornerRadius, cornerRadius);
        g.setRenderingHints(RENDERING_HINTS);
        final int buffer = 5; // need to draw this arc bigger than the arc in corner so nothing gets clipped
        g.fill(new Arc2D.Double(-cornerRadius - buffer, -buffer,
                cornerDiameter + buffer * 2, cornerDiameter + buffer * 2, 90, degrees, Arc2D.PIE));
        g.setComposite(AlphaComposite.SrcIn);
        g.drawImage(corner, 0, 0, null);
        g.dispose();
        return partial;
    }

    /**
     * Create an affine transform that will position the corner in the correct location.
     *
     * @param rotateAngle angle of rotation
     * @param anchorX x anchor of rotation
     * @param anchorY y anchor of rotation
     * @param tx x translation
     * @param ty y translation
     * @return affine transform that will rotate then translate
     */
    private AffineTransform createCornerTransform(int rotateAngle, int anchorX, int anchorY, int tx, int ty) {
        AffineTransform rotate = AffineTransform.getRotateInstance(Math.toRadians(rotateAngle), anchorX, anchorY);
        AffineTransform translate = AffineTransform.getTranslateInstance(tx, ty);
        rotate.concatenate(translate);
        return rotate;
    }

    /* (non-Javadoc)
      * @see org.nekocode.nowplaying.events.TrackChangeListener#trackChanged(org.nekocode.nowplaying.events.TrackChangeEvent)
      */
	@Override
	public void trackChanged(TrackChangeEvent e) {
        dirtySong = true;
        firePropertyChange(DIRTY_SONG, false, true);
	}

    /**
	 * Set the layer's progress bar to the specified percent complete.
	 *
	 * @param percent percent complete
	 */
	public void setTrackProgress(double percent) {
		log.debug("track progress: " + percent);

		double filledLength = percent * perimeter;
		if (currentFilledLength > filledLength) {
			dirtySong = true;

            firePropertyChange(DIRTY_SONG, false, true);
		}
		if (currentFilledLength < filledLength) {
            int lastSide = getSection(currentFilledLength);
            int currentSide = getSection(filledLength);

            if (currentSide > (lastSide + 1)) {
                // this update was more than a single side, probably the track position jumped:
                // redraw the whole thing
                firePropertyChange(DIRTY_SONG, false, true);
            } else {
                // notify that we need a repaint (normal update)
                firePropertyChange(DIRTY, false, true);
            }
		}

        lastFilledLength = currentFilledLength;
		currentFilledLength = filledLength;
	}

    /**
     * Handles property changes on this LayerUI.  The property changes handled here are generated by
     * {@link #trackChanged(TrackChangeEvent)} or {@link #setTrackProgress(double)}.
     *
     * @param evt the PropertyChangeEvent generated by this {@code LayerUI}
     * @param l the {@code JLayer} this LayerUI is set to
     */
    @Override
    public void applyPropertyChange(PropertyChangeEvent evt, JLayer<? extends JComponent> l) {
        if (DIRTY.equals(evt.getPropertyName())) {
            l.repaint(getClip());
        } else if (DIRTY_SONG.equals(evt.getPropertyName())) {
            l.repaint();
        }
    }

    /**
     * Get the appropriate clip region for a track update in the current song.
     * The clip region depends upon the extent of the change.  At the minimum, it must
     * encompass from the first changed pixel to the last changed pixel.  Larger clip
     * areas will work fine, but repainting will be more expensive.
     *
     * @return clip region for current update
     */
	public Rectangle getClip() {
        int i = getSection(currentFilledLength);

        int sectionCurrentFill = (int) Math.ceil(currentFilledLength);
        int sectionLastFill = (int) Math.floor(lastFilledLength);
        if (i > 0) {
            // change filledLength from the total # of pixels filled into the number
            // of pixels filled of the current section
            sectionCurrentFill -= sectionLengths[i-1];
            sectionLastFill -= sectionLengths[i-1];
        }

        if (sectionLastFill < 0) {
            // TODO need to handle special case for when currentFilledLength and lastFilledLength aren't on the same section of the border
        }

        // TODO create small clip bounds for corners

        // +2 on the end for 1 pixel overage on each side
        int changeSize = sectionCurrentFill - sectionLastFill + 2;
        switch (i) {
		case 0:
            return new Rectangle(width / 2 + sectionLastFill - 1, 0, changeSize, borderSize);
		case 8:
            return new Rectangle(cornerRadius + sectionLastFill - 1, 0, changeSize, borderSize);
		case 2:
            return new Rectangle(width- borderSize, cornerRadius + sectionLastFill - 1, borderSize, changeSize);
		case 4:
			return new Rectangle(width - cornerRadius - sectionCurrentFill - 1, height- borderSize, changeSize, borderSize );
		case 6:
            return new Rectangle(0, height - cornerRadius - sectionCurrentFill - 1, borderSize, changeSize);
		case 7:
            return clipNW;
		case 5:
            return clipSW;
		case 3:
            return clipSE;
		case 1:
            return clipNE;
		default:
			// if we don't understand the input, redraw everything
			return clipAll;
		}
	}

    /**
     * Finds the section of the progress bar that the indicated length (in pixels) from the start is located in.
     * <ul>
     *     <li>0: upper bar, right half</li>
     *     <li>1: upper right corner</li>
     *     <li>2: right bar</li>
     *     <li>3: lower right corner</li>
     *     <li>4: lower bar</li>
     *     <li>5: lower left corner</li>
     *     <li>6: left bar</li>
     *     <li>7: upper left corner</li>
     *     <li>8: upper bar, left half</li>
     * </ul>
     *
     * @param fillLength length (in pixels) of progress bar location
     * @return section number of progress bar location
     */
    public int getSection(double fillLength) {
        int i = 0;
        for (; i < sectionLengths.length && sectionLengths[i] < fillLength; i++);
        return i;
    }
}
