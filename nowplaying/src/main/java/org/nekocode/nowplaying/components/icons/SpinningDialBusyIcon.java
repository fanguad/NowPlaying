/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.icons;

import furbelow.SpinningDial;
import org.divxdede.swing.busy.BusyIcon;

import javax.swing.BoundedRangeModel;

/**
 * furbelow's SpinningDial in a form that BusyComponent can use
 */
public class SpinningDialBusyIcon extends SpinningDial implements BusyIcon {
    private BoundedRangeModel model;

    public SpinningDialBusyIcon() {
    }

    public SpinningDialBusyIcon(int w, int h) {
        super(w, h);
    }

    public SpinningDialBusyIcon(int w, int h, int spokes) {
        super(w, h, spokes);
    }

    @Override
    public void setModel(BoundedRangeModel model) {
        this.model = model;
    }

    @Override
    public BoundedRangeModel getModel() {
        return model;
    }

    @Override
    public boolean isDeterminate() {
        return false;
    }
}
