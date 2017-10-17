/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.swing;

import com.jhlabs.image.ConvolveFilter;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Kernel;

/**
 * Rendering for pretty text components.
 */
class NekoTextPainter extends ComponentUI {
    /**
     * 1 for dilation of text, 1 for blurring
     */
    static final int TEXT_INSET = 2;

    private static final DilateBlurFilter GROW_AND_BLUR_FILTER = new DilateBlurFilter();
    private static final Color BACKGROUND_COLOR = new Color(.9f, .9f, .9f, 0);
    private static final Color GLOW_COLOR = new Color(.9f, .9f, .9f, .8f);

    private final Rotation rotation;

    private BufferedImage tempBuffer1;
    private Dimension cachedSize;
    private String cachedString;

    // these rectangles get used a lot, so keep them as fields
    // (this pattern is copied from BasicLabelUI/BasicButtonUI)
    private final Rectangle paintIconR = new Rectangle();
    private final Rectangle paintTextR = new Rectangle();
    private final Rectangle paintViewR = new Rectangle();

    public NekoTextPainter() {
        rotation = Rotation.NONE;
    }

    public NekoTextPainter(Rotation rotation) {
        this.rotation = rotation;
    }

    /**
     * Paint clippedText at textX, textY with the given foreground color.
     *
     *
     * @param c          component which is being painted
     * @param g          graphics object to paint into
     * @param s          string to paint
     * @param foreground foreground color of text
     * @param textX      text offset x
     * @param textY      text offset x
     * @see #paint(java.awt.Graphics, javax.swing.JComponent)
     */
    protected void paintText(JComponent c, Graphics g, String s, Color foreground, int textX, int textY) {
        if (c.getSize().equals(cachedSize) && s.equals(cachedString)) {
            // nothing has changed, so no need to recalculate everything
            g.drawImage(tempBuffer1, 0, 0, null);
            return;
        }

        int yOffSet;
        if (c instanceof NekoLabel)
        {
            yOffSet = -2;
        }
        else // NekoButton
        {
            yOffSet = 0;
        }

        cachedSize = c.getSize();
        cachedString = s;

        BufferedImage tempBuffer2;
        if (rotation.isVertical()) {
            // switch the height and width
            tempBuffer1 = new BufferedImage(c.getHeight(), c.getWidth(), BufferedImage.TYPE_INT_ARGB);
            tempBuffer2 = new BufferedImage(c.getHeight(), c.getWidth(), BufferedImage.TYPE_INT_ARGB);
        } else {
            tempBuffer1 = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
            tempBuffer2 = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
        }

        // clear the background
        //    make sure the background is the same color (but not opacity)
        //    as the glow color because the partially opaque portion will
        //    pull some color from the background color when rendering
        // NOTE: this is the same general effect as premultiplying the data,
        //       but the results are slightly different, and I like this better
        Graphics2D tempGraphics1_1 = tempBuffer1.createGraphics();
        tempGraphics1_1.setBackground(BACKGROUND_COLOR);
        tempGraphics1_1.clearRect(0, 0, tempBuffer1.getWidth(), tempBuffer1.getHeight());
        tempGraphics1_1.dispose();

        // no clue why I have to do this with a separate graphics object
        Graphics2D tempGraphics1_2 = tempBuffer1.createGraphics();
        tempGraphics1_2.setFont(c.getFont());
        tempGraphics1_2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        tempGraphics1_2.setColor(GLOW_COLOR);
        tempGraphics1_2.drawString(s, textX + TEXT_INSET, textY + yOffSet);

        GROW_AND_BLUR_FILTER.filter(tempBuffer1, tempBuffer2);

        // add the blurred image to the original to achieve the glow
        Composite originalComposite = tempGraphics1_2.getComposite();
        tempGraphics1_2.setComposite(AlphaComposite.SrcOut);
        tempGraphics1_2.drawImage(tempBuffer2, 0, 0, null);
        tempGraphics1_2.setComposite(originalComposite);

        tempGraphics1_2.setColor(foreground);
        tempGraphics1_2.drawString(s, textX + TEXT_INSET, textY + yOffSet);
        tempGraphics1_2.dispose();

        g.drawImage(tempBuffer1, 0, 0, null);
    }

    /**
     * Copied from BasicLabelUI/BasicButtonUI since {@link #layout(javax.swing.JLabel, java.awt.FontMetrics, int, int)}
     * is private.
     */
    public void paintText(Graphics g, JComponent c,
                          String text, Icon icon,
                          Color textColor,
                          int verticalAlignment, int horizontalAlignment,
                          int verticalTextPosition, int horizontalTextPosition,
                          int iconTextGap) {
        if ((icon == null) && (text == null)) {
            return;
        }

        int width, height;
        if (rotation.isVertical()) {
            // swap width and height
            width = c.getHeight();
            height = c.getWidth();
            // swap vertical/horizontal text position and alignment
            int temp = verticalAlignment;
            verticalAlignment = horizontalAlignment;
            horizontalAlignment = temp;
            temp = verticalTextPosition;
            verticalTextPosition = horizontalTextPosition;
            horizontalTextPosition = temp;
        } else {
            width = c.getWidth();
            height = c.getHeight();
        }

        FontMetrics fm = SwingUtilities2.getFontMetrics(c, g);
        Insets insets = c.getInsets(null);
        paintViewR.x = insets.left;
        paintViewR.y = insets.top;
        paintViewR.width = width - (insets.left + insets.right);
        paintViewR.height = height - (insets.top + insets.bottom);
        paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
        paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;
        String clippedText = SwingUtilities.layoutCompoundLabel(
                c,
                fm,
                text,
                icon,
                verticalAlignment,
                horizontalAlignment,
                verticalTextPosition,
                horizontalTextPosition,
                paintViewR,
                paintIconR,
                paintTextR,
                iconTextGap);

        if (icon != null) {
            icon.paintIcon(c, g, paintIconR.x, paintIconR.y);
        }

        if (clippedText != null && !clippedText.equals("")) {
            View v = (View) c.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                v.paint(g, paintTextR);
            } else {
                int textX = paintTextR.x;
                int textY = paintTextR.y + fm.getAscent();

                paintText(c, g, clippedText, textColor, textX, textY);
            }
        }
    }


    public Rotation getRotation() {
        return rotation;
    }

    public static class DilateBlurFilter extends ConvolveFilter {
        protected static final float[] DILATE_BLUR_KERNEL = {
                1/14f,  3/14f,  4/14f,  3/14f, 1/14f,
                3/14f,  7/14f, 10/14f,  7/14f, 3/14f,
                4/14f, 10/14f, 14/14f, 10/14f, 4/14f,
                3/14f,  7/14f, 10/14f,  7/14f, 3/14f,
                1/14f,  3/14f,  4/14f,  3/14f, 1/14f
        };

        public DilateBlurFilter() {
            super(new Kernel(5, 5, DILATE_BLUR_KERNEL));
        }

        public String toString() {
            return "Dilation+Blur";
        }
    }
}
