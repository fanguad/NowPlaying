/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.icons;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;

/**
 * A square Icon of arbitrary size that has nothing in it.  For use with JBusyComponent.
 */
 public class EmptyIcon implements Icon {
        private int size;

        public EmptyIcon(int size) {
            this.size = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }
    }
