/*
 * Copyright (c) 2007 ANDRE Sébastien (divxdede).  All rights reserved.
 * BusyModel.java is a part of this JBusyComponent library
 * ====================================================================
 * 
 * JBusyComponent library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or any later version.
 * 
 * This is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */
package org.divxdede.swing.busy;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * DataModel describe a <strong>busy</strong> state behaviour.
 * A busy state represent a disabled state (inacessible) for a while.
 * <p>
 * This state is commonly bound to a swing component that can't be used while it
 * is busy. Typically a pretty animation will be show.
 * <p>
 * When the model is gone to busy, it can be determinate that allow to track the
 * progress and time remaining like a <code>JProgressBar</code>.
 * In fact, a BusyModel is a BoundedRangeModel that allow it to be bounded to a
 * <code>JProgressBar</code>.
 * <p>
 * BusyModel can be cancellable to allow the controller of this model to cancel the 
 * underlying task. 
 * 
 * @author André Sébastien (divxdede)
 */
public interface BusyModel extends BoundedRangeModel {

    /** Start Action ID used in {@link ActionEvent} fired to {@link ActionListener}
     *  @since 1.2.2
     */
    public static final int    START_ACTION_ID      = 1;

    /** Start Action Command used in {@link ActionEvent} fired to {@link ActionListener}
     *  @since 1.2.2
     */
    public static final String START_ACTION_COMMAND = "Start";

    /** Cancel Action ID used in {@link ActionEvent} fired to {@link ActionListener}
     *  @since 1.2.2
     */
    public static final int    CANCEL_ACTION_ID      = 2;

    /** Cancel Action Command used in {@link ActionEvent} fired to {@link ActionListener}
     *  @since 1.2.2
     */
    public static final String CANCEL_ACTION_COMMAND = "Cancel";

    /** Stop Action ID used in {@link ActionEvent} fired to {@link ActionListener}
     *  @since 1.2.2
     */
    public static final int    STOP_ACTION_ID      = 3;

    /** Stop Action Command used in {@link ActionEvent} fired to {@link ActionListener}
     *  @since 1.2.2
     */
    public static final String STOP_ACTION_COMMAND = "Stop";

    /** 
     * Define if the model is on a "busy" state
     * @param value true to going in a busy state
     */
    public void setBusy(final boolean value);
    
    /**
     * Returns true if the model is currently on a <code>busy</code> state
     * @return tue if the model is currently busy
     */
    public boolean isBusy();
    
    /** 
     * Define if the model is in a <code>determinate mode</code> or not
     * @param value true for change this model in a determinate mode
     */
    public void setDeterminate(final boolean value);
    
    /** 
     * Returns true if the model is in a <code>determinate mode</code>.
     * @return true if the model is in a determinate mode.
     */
    public boolean isDeterminate();

    /** 
     * Returns true if the model is <code>cancellable</code> the performing the job responsible on the <code>busy</code> state.
     * @return true is the model is cancellable
     */
    public boolean isCancellable();
    
    /** 
     * Define if this model is <code>cancellable</code>
     * @param value true for set this model cancellable.
     */
    public void setCancellable(final boolean value);
    
    /** Invoke this method to cancel the current job responsible of the <code>busy</code> state.
     *  You need to override this method for implements you own cancellation process.
     *  Cancelling a task fire an {@link ActionEvent} to all registered {@link ActionListener} to this model.
     */
    public void cancel();

    /** Description to show by UI when the model is busy
     *  Return null for let the UI render the native description
     *  @return Description to show by UI when the model is busy
     */
    public String getDescription();

    /**
     * Adds an <code>ActionListener</code> to the model.
     * @param l the <code>ActionListener</code> to be added
     * @since 1.2.2
     */
    public void addActionListener(ActionListener listener);

    /**
     * Removes an <code>ActionListener</code> from the model.
     * @param l the listener to be removed
     * @since 1.2.2
     */
    public void removeActionListener(ActionListener listener);
}
