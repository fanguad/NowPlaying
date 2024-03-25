/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.components.icons;

import javax.swing.*;
import java.awt.*;

/**
 * A square Icon of arbitrary size that has nothing in it.  For use with JBusyComponent.
 */
 public class EmptyIcon implements Icon {
        private final int size;

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
