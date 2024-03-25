/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.components.swing;

/**
 * Indicates which direction a component should be rotated, relative to its normal rotation.
 */
public enum Rotation {
    NONE, CLOCKWISE, COUNTER_CLOCKWISE, FLIPPED;

    public boolean isVertical() {
        return this == CLOCKWISE || this == COUNTER_CLOCKWISE;
    }

    public Rotation getOpposite() {
        Rotation opposite;
        switch (this) {
        case CLOCKWISE:
            opposite = COUNTER_CLOCKWISE;
            break;
        case COUNTER_CLOCKWISE:
            opposite = CLOCKWISE;
            break;
        case NONE:
            opposite = FLIPPED;
            break;
        case FLIPPED:
        default:
            opposite = NONE;
        }
        return opposite;
    }
}
