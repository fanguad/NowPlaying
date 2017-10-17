/*
 * 
 * Copyright (c) 2007 ANDRE Sébastien (divxdede).  All rights reserved.
 * DefaultBusyModel.java is a part of this JBusyComponent library
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Default Implementation of interface <code>BusyModel</code>.
 * <p>
 * It add <code>AutoCompletion</code> feature for determinate model.
 * This feature allow to move the current value to the minimum range when the busy 
 * property is set to <code>true</code>.<br>
 * At the other side, when the  current value reach the maximum bounded range, it set
 * automatically the busy property to <code>false</code>.
 * 
 * @author André Sébastien (divxdede)
 */
public class DefaultBusyModel extends DefaultBoundedRangeModel implements BusyModel {

    private boolean         busyState           = false;
    private boolean         determinateState    = false;
    private boolean         autoCompletionState = false;
    private boolean         cancellableState    = false;
    private String          description         = null;
    
    /** 
     * Define if the model is on a "busy" state.
     * This method fire an {@link ActionEvent} of {@link #START_ACTION_ID} or  {@link #STOP_ACTION_ID} following by a {@link ChangeEvent}
     * @param value true to going in a busy state
     */
    public void setBusy(final boolean value) {
        // new Exception("PASS(" + value + ")").printStackTrace();
        final boolean oldValue = isBusy();
        this.busyState = value;
        
        if( oldValue != isBusy() ) {
            if( this.isBusy() && this.isDeterminate() && this.isAutoCompletionEnabled() ) {
                this.setValue( getMinimum() );
            }
            try {
                if( isBusy() ) this.fireActionPerformed( new ActionEvent(this,START_ACTION_ID,START_ACTION_COMMAND,System.currentTimeMillis(),0) );
                else           this.fireActionPerformed( new ActionEvent(this,STOP_ACTION_ID,STOP_ACTION_COMMAND,System.currentTimeMillis(),0) );
            }
            finally {
                this.fireStateChanged();
            }
        }
    }
    
    /**
     * Returns true if the model is currently on a <code>busy</code> state
     * @return tue if the model is currently busy
     */
    public boolean isBusy() {
        return this.busyState;
    }

    /** Manage auto completion
     */
    @Override
    public void setValue(final int n) {
        super.setValue(n);
        
        if( isDeterminate() && isAutoCompletionEnabled() && getValue() >= getMaximum() ) {
            setBusy(false);
        }
    }
    
    /** 
     * Define if the model is in a <code>determinate mode</code> or not
     * @param value true for change this model in a determinate mode
     */
    public void setDeterminate(final boolean value) {
        final boolean oldValue = isDeterminate();
        this.determinateState = value;
        
        if( oldValue != isDeterminate() )
            this.fireStateChanged();
    }
    
    /** 
     * Returns true if the model is in a <code>determinate mode</code>.
     * @return true if the model is in a determinate mode.
     */
    public boolean isDeterminate() {
        return this.determinateState;
    }
    
    /** 
     * Define if the range value must manage the completion automatically.
     * This property is significant only when this model is <code>determinate</code>.
     * When the <code>busy</code> property is set to true the range <code>value</code> is set to the <code>minimum</code>.
     * When the range <code>value</code> reach the <code>maximum</code>, the <code>busy</code> property is set to <code>false</code>.
     */
    public void setAutoCompletionEnabled(final boolean value) {
        final boolean oldValue = isAutoCompletionEnabled();
        
        this.autoCompletionState = value;
        
        if( oldValue != this.isAutoCompletionEnabled() )
            this.fireStateChanged();
    }
    
    /** 
     * Returns <code>true</code> if the range value must manage the completion automatically.
     * This property is significant only when this model is <code>determinate</code>.
     * When the <code>busy</code> property is set to true the range <code>value</code> is set to the <code>minimum</code>.
     * When the range <code>value</code> reach the <code>maximum</code>, the <code>busy</code> property is set to <code>false</code>.
     */
    public boolean isAutoCompletionEnabled() {
        return this.autoCompletionState;
    }

    /** 
     * Returns true if the model is <code>cancellable</code> the performing the job responsible on the <code>busy</code> state
     * @return true is the model is cancellable
     */
    public boolean isCancellable() {
        return this.cancellableState;
    }

    /**
     * Default implementation that simply stop the <code>busy</code> state
     */
    public void cancel() {
        if( ! isCancellable() ) throw new IllegalStateException("this model is not cancellable");
        try {
            this.fireActionPerformed( new ActionEvent(this, CANCEL_ACTION_ID , CANCEL_ACTION_COMMAND , System.currentTimeMillis() , 0 ) );
        }
        finally {
            setBusy(false);
        }
    }

    /** 
     * Define if this model is <code>cancellable</code>
     * @param value true for set this model cancellable.
     */
    public void setCancellable(final boolean value) {
        final boolean oldValue = isCancellable();
        
        this.cancellableState = value;
        
        if( oldValue != isCancellable() ) {
            this.fireStateChanged();
        }
    }

    /** Description to show by UI when the model is busy
     *  Return null for let the UI render the native description
     *  @return Description to show by UI when the model is busy
     */
    public String getDescription() {
        return this.description;
    }

    /** Define the description to show by UI when the model is busy
     *  @param s new description to show by UI, set null if you want to restore default value
     */
    public void setDescription(String s) {
        String old = s;
        this.description = s;
        if( old != this.description ) {
            if( old != null && this.description != null ) {
                if( old.equals( this.description ) ) return;
            }
            fireStateChanged();
        }
    }

    /**
     * Adds an <code>ActionListener</code> to the model.
     * @param l the <code>ActionListener</code> to be added
     * @since 1.2.2
     */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /**
     * Removes an <code>ActionListener</code> from the model.
     * If the listener is the currently set <code>Action</code>
     * for the button, then the <code>Action</code>
     * is set to <code>null</code>.
     *
     * @param l the listener to be removed
     * @since 1.2.2
     */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    /**
     * Returns an array of all the <code>ActionListener</code>s added
     * to this Model with addActionListener().
     *
     * @return all of the <code>ActionListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.2.2
     */
    public ActionListener[] getActionListeners() {
        return (ActionListener[])(listenerList.getListeners(ActionListener.class));
    }
    
    @Override
    protected void fireStateChanged() {
        if( ! SwingUtilities.isEventDispatchThread() ) {
            final Runnable doRun = new Runnable() {
                public void run() {
                    fireStateChanged();
                }
            };
            SwingUtilities.invokeLater(doRun);
            return;
        }
        super.fireStateChanged();
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the <code>event</code>
     * parameter.
     *
     * @param event  the <code>ActionEvent</code> object
     * @see EventListenerList
     * @since 1.2.2
     */
    protected void fireActionPerformed(final ActionEvent event) {
        // Ensure on EDT
        if( !SwingUtilities.isEventDispatchThread() ) {
            Runnable doRun = new Runnable() {
                public void run() {
                    fireActionPerformed(event);
                }
            };
            SwingUtilities.invokeLater(doRun);
            return;
        }
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(event);
            }
        }
    }
}
