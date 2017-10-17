/*
 *
 * Copyright (c) 2007 ANDRE Sébastien (divxdede).  All rights reserved.
 * BusySwingWorker.java is a part of this JBusyComponent library
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

import org.divxdede.swing.RemainingTimeMonitor;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A {@link SwingWorker} providing a {@link BusyModel} for disable components while this worker is running.
 * <p>
 * The {@link BusyModel}'s state reflet changes mades on this worker.
 * <p>
 * When this {@link SwingWorker} start, the {@link BusyModel} gone to busy.<br>
 * When this {@link SwingWorker}'s end, the {@link BusyModel} gone to idle.<br>
 * If this {@link SwingWorker} use it's {@link #setProgress(int)} method to indicate the progression of it's task, the {@link BusyModel} will be automatically {@code determinate}.<br>
 * In the counterpart, if the {@link BusyModel} is {@code cancellable}, cancel the {@link BusyModel} will cancel also the {@link SwingWorker}.
 * <p>
 * You can use the {@link #getProgressModel()} for updating the progression of this <code>worker</code> with your own value range (you must define {@link BoundedRangeModel#getMinimum()} and {@link BoundedRangeModel#getMaximum()}).<br>
 * The underlying {@link BoundedRangeModel} call the {@link #setProgress(int)} with the value automatically scale to the [0 ~ 100] range.
 *
 * @author André Sébastien (divxdede)
 * @since 1.2.2
 */
public abstract class BusySwingWorker<T,V> extends SwingWorker<T,V> {

    private final StateAndProgressListener listener = new StateAndProgressListener();
    private       BusyModel                model    = null;
    private final BoundedRangeModel        progress = new DefaultBoundedRangeModel();

    /**
     * Constructs this {@code BusySwingWorker} providing the specified {@link BusyModel} to use with.
     */
    public BusySwingWorker(BusyModel model) {
        super();
        
        // listen itself, because no better way to subclass and enhance the SwingWorker (all callbacks are final or private)
        this.addPropertyChangeListener(listener);

        // Set the model
        setBusyModel(model);
    }

    /**
     * Constructs this {@code BusySwingWorker}.
     */
    public BusySwingWorker() {
        this( createDefaultBusyModel() );
    }

    /** Return the @{link BusyModel} that allow to disable a component or a container while this {@link SwingWorker} is running.<br>
     *  You can set this BusyModel to a {@link JBusyComponent} that will define which component or container (view) to disable.
     *
     *  @return BusyModel handled by this {@link SwingWorker}
     */
    public final BusyModel getBusyModel() {
        return this.model;
    }

    /** Define the @{link BusyModel} that allow to disable a component or a container while this {@link SwingWorker} is running.<br>
     *  @param model to handle by this {@link SwingWorker}
     */
    public final void setBusyModel(BusyModel model) {
        BusyModel old = getBusyModel();
        if( old != null ) {
            old.removeActionListener( this.listener );
        }
        this.model = model;
        if( this.model != null ) {
            this.model.addActionListener( this.listener );
        }
        this.firePropertyChange("busyModel", old , getBusyModel() );
    }

    /** Private method creating a default {@link BusyModel} when no one was provided
     */
    private static BusyModel createDefaultBusyModel() {
        BusyModel myModel = new DefaultBusyModel();

        myModel.setDeterminate(false); // by default we don't know if the swing worker is determinate
                                       // if we receive a "progress" change we would consider that it is.
        myModel.setCancellable(true);
        myModel.setMinimum(0);
        myModel.setMaximum(100);

        return myModel;
    }

    /** Return a {@link BoundedRangeModel} that can be used to set the progression of this worker.<br>
     *  This tool facilite the way to set the progression without the restriction of a range [0 ~ 100].
     *  <p>
     *  You can define a user-range for the progression. Each changes made on the model will be set on the worker by calling {@link #setProgress(int)}
     *
     *  @return A usable {@link BoundedRangeModel} for set the progression
     */
    public BoundedRangeModel getProgressModel() {
        return progress;
    }

    /** Internal PropertyChangeListener listening "State" and "Progress" properties.
     *  It's this listener that propage SwingWorker state to the BusyModel
     */
    private class StateAndProgressListener implements PropertyChangeListener , ActionListener , ChangeListener {

        public void propertyChange(final PropertyChangeEvent evt) {
            if( !SwingUtilities.isEventDispatchThread() ) {
                Runnable doRun = new Runnable() {
                    public void run() {
                        propertyChange(evt);
                    }
                };
                SwingUtilities.invokeLater(doRun);
                return;
            }
            // On EDT
            if( evt.getPropertyName().equals("state") ) {
                StateValue newValue = (StateValue)evt.getNewValue();
                switch( newValue ) {
                    case DONE    : getBusyModel().setBusy(false);
                                   progress.removeChangeListener( this );
                                   break;
                    case PENDING : // Do nothing
                                   break;
                    case STARTED : getBusyModel().setBusy(true);
                                   getBusyModel().setValue( getBusyModel().getMinimum() );
                                   progress.addChangeListener( this );
                                   break;
                }
            }
            else if( evt.getPropertyName().equals("progress") ) {
                getBusyModel().setDeterminate(true); // the first time we receive a progress chunk, we can consider this worker as determinate

                Integer newValue = (Integer)evt.getNewValue();
                getBusyModel().setValue( scale( newValue , getBusyModel() ) ); // forward to the model
            }
        }

        /** Handle cancel action on the BusyModel
         *  @param e
         */
        public void actionPerformed(ActionEvent e) {
            if( e.getSource() == getBusyModel() && e.getID() == BusyModel.CANCEL_ACTION_ID ) {
                cancel(true);
            }
        }

        /** State change on the ProgressModel
         */
        public void stateChanged(ChangeEvent e) {
            BoundedRangeModel pModel = getProgressModel();
            if( e.getSource() == pModel ) {
                setProgress( Math.round( RemainingTimeMonitor.getRatio( getProgressModel() ) * 100f ) );
            }
        }

        /** Scale the progress value for the model range.
         */
        private int scale(float value , BusyModel model ) {
            // normal scale : 0 ~ 100
            float range = model.getMaximum() - model.getMinimum();

            // scale it
            float result = ( value * (range / 100) ) + model.getMinimum();

            // round it and return it
            return Math.round(result);
        }
    }
}