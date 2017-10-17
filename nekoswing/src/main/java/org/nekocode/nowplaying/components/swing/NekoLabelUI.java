/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.swing;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLabelUI;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Rendering for a NekoLabel.
 */
class NekoLabelUI extends BasicLabelUI {
    /**
     * 1 for dilation of text, 1 for blurring
     */
    private NekoTextPainter textPainter;

    private boolean rotated;

    public NekoLabelUI() {
        textPainter = new NekoTextPainter();
    }

    public NekoLabelUI(Rotation rotation) {
        textPainter = new NekoTextPainter(rotation);
        rotated = rotation == Rotation.CLOCKWISE || rotation == Rotation.COUNTER_CLOCKWISE;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension dim = super.getPreferredSize(c);
        dim.width += NekoTextPainter.TEXT_INSET * 2;
        dim.height += NekoTextPainter.TEXT_INSET * 2;
        if (rotated) {
            dim = new Dimension(dim.height, dim.width);
        }
        return dim;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform tr = g2.getTransform();
        switch (textPainter.getRotation()) {
            case CLOCKWISE:
                g2.rotate(Math.PI / 2);
                g2.translate(0, -c.getWidth());
                break;
            case COUNTER_CLOCKWISE:
                g2.rotate(-Math.PI / 2);
                g2.translate(-c.getHeight(), 0);
                break;
            case FLIPPED:
                g2.rotate(Math.PI);
                g2.translate(-c.getWidth(), -c.getHeight());
                break;
            case NONE:
            default:
                // no rotation/translation
        }

        JLabel label = (JLabel) c;
        Icon icon = (label.isEnabled()) ? label.getIcon() : label.getDisabledIcon();

        Color textColor;
        if (label.isEnabled()) {
            textColor = label.getForeground();
        } else {
            textColor = label.getForeground().brighter();
        }
        textPainter.paintText(g2, label, label.getText(), icon, textColor,
                label.getVerticalAlignment(), label.getHorizontalAlignment(),
                label.getVerticalTextPosition(), label.getHorizontalTextPosition(),
                label.getIconTextGap());

        g2.setTransform(tr);
    }

}
