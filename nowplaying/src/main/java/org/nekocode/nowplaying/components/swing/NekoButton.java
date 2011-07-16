/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.components.swing;

import javax.swing.AbstractButton;
import javax.swing.DefaultButtonModel;
import java.awt.Color;
import java.awt.Insets;

/**
 * A pretty button.
 */
public class NekoButton extends AbstractButton {

    public static final Color DEFAULT_FOREGROUND = Color.black;
    public static final Color DEFAULT_BACKGROUND = new Color(.4f, .4f, .4f, .5f);

    public NekoButton(String text) {
        this(text, Rotation.NONE);
    }

    public NekoButton(String text, Rotation rotation) {
        setUI(new NekoButtonUI(rotation));
        setForeground(DEFAULT_FOREGROUND);
        setBackground(DEFAULT_BACKGROUND);
        setModel(new DefaultButtonModel());
        setOpaque(false);
        
        init(text, null);

        setMargin(new Insets(
                0, NekoTextPainter.TEXT_INSET, NekoTextPainter.TEXT_INSET,
                NekoTextPainter.TEXT_INSET*2));
        setFocusPainted(false);
        setRolloverEnabled(false);
        setBorderPainted(false);
    }

    public void updateUI() {
        // already set in constructor
    }
}
