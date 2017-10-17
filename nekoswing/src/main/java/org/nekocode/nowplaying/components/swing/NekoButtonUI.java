/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.swing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Button UI.
 */
public class NekoButtonUI extends BasicButtonUI {

    private static final Logger log = LogManager.getLogger(NekoButtonUI.class);
    private static final int ARC_SIZE = 20;

    private NekoTextPainter textPainter;
    private boolean rotated;

    public NekoButtonUI() {
        textPainter = new NekoTextPainter();
    }

    public NekoButtonUI(Rotation rotation) {
        textPainter = new NekoTextPainter(rotation);
        rotated = rotation == Rotation.CLOCKWISE || rotation == Rotation.COUNTER_CLOCKWISE;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension dim = super.getPreferredSize(c);
        int width = dim.width + NekoTextPainter.TEXT_INSET * 2;
        int height = dim.height + NekoTextPainter.TEXT_INSET * 2;
        if (rotated) {
            dim = new Dimension(height, width);
        } else {
            dim = new Dimension(width, height);
        }
        return dim;
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        ButtonModel model = b.getModel();
        FontMetrics fm = SwingUtilities2.getFontMetrics(b, g);

        Color textColor;
        /* Draw the Text */
        if (model.isEnabled()) {
            textColor = b.getForeground();
        } else {
            textColor = b.getForeground().brighter();
        }
        textPainter.paintText(b, g, text, textColor,
                textRect.x + getTextShiftOffset(),
                textRect.y + fm.getAscent() + getTextShiftOffset());
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g;

        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        if (model.isArmed() && model.isPressed()) {
            paintButtonPressedBackground(g2, b);
        } else {
            paintBackground(g2, c);
        }

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

        AbstractButton button = (AbstractButton) c;
        Icon icon = (button.isEnabled()) ? button.getIcon() : button.getDisabledIcon();

        Color textColor;
        if (button.isEnabled()) {
            textColor = button.getForeground();
        } else {
            textColor = button.getForeground().brighter();
        }
        textPainter.paintText(g2, button, button.getText(), icon, textColor,
                button.getVerticalAlignment(), button.getHorizontalAlignment(),
                button.getVerticalTextPosition(), button.getHorizontalTextPosition(),
                button.getIconTextGap());

        g2.setTransform(tr);
    }

    /**
     * Draws the button background.
     *
     * @param g graphics object
     * @param c component
     */
    private void paintBackground(Graphics2D g, JComponent c) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(c.getBackground());
        g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), ARC_SIZE, ARC_SIZE);
    }

    /**
     * Draws the button background when the button is being pressed.
     *
     * @param g graphics object
     * @param c component
     */
    private void paintButtonPressedBackground(Graphics2D g, JComponent c) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(c.getBackground().brighter());
        g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), ARC_SIZE, ARC_SIZE);
    }
}
