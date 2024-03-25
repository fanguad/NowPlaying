/*
 * 
 * Copyright (c) 2007 ANDRE Sébastien (divxdede).  All rights reserved.
 * FutureBusyModel.java is a part of this JBusyComponent library
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

import org.divxdede.commons.Disposable;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A BusyModel implementation allowing to reflet the execution of a Future task.
 * While the job task underlying the Future is running, this model will be set to a <code>busy</code> state.
 * <p>
 * Use <code>setFuture</code> for defining the <code>Future</code> to reflet.<br>
 * Since {@link SwingWorker} is also a {@link Future}, you can bound a SwingWorker to this model.<br>
 * When you bound a {@link SwingWorker} to this model, this model will be determinate and use the {@link SwingWorker#getProgress()}
 * <p>
 * When you don't need anymore to use this model, you must invoke {@link #dispose()} in order to free all threading resources.<br>
 * 
 * @author André Sébastien (divxdede)
 */
public class FutureBusyModel extends DefaultBusyModel implements Disposable {
    
    /** Members
     */
    private ExecutorService service             = null;
    private int             ticket              = 0;
    private Future          trackedFuture       = null;
    private Future          trackerFuture       = null;

    /** Listener of {@link SwingWorker} that listen "progress" property
     */
    private final PropertyChangeListener listener     = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            synchronized(FutureBusyModel.this) {
                if( evt.getSource() == trackedFuture && ( evt.getSource() instanceof SwingWorker ) && evt.getPropertyName().equals("progress") ) {
                    SwingWorker worker = (SwingWorker)evt.getSource();
                    setValue( worker.getProgress() );
                }
            }
        }
    };

    /** Default constructor
     */
    public FutureBusyModel() {
        this.setAutoCompletionEnabled(false);
    }
    
    /** 
     * Reflet a new <code>Future</code> to reflet.
     * This model will be set as <code>undeterminate</code> but <code>cancellable</code> model.
     * @param future New Future to reflet.
     */
    public synchronized void setFuture(final Future future) {
        setFuture(future,true);
    }

    /** You can't define the busy state manually on a {@link FutureBusyModel}
     *  Use instead the {@link #setFuture(Future)} for define which task to track.
     */
    @Override
    public final void setBusy(final boolean value) {
        // do nothing
    }
    
    /** Change a busy state and return a ticket identifier of this attempt
     */
    private synchronized int setBusyImpl(final boolean value) {
        super.setBusy(value);
        return (++this.ticket);
    }
    
    /** Change a busy state only if the ticket parameter is always the last given ticket
     */
    private synchronized boolean compareAndSetBusy( final boolean value , final int ticketValue ) {
        if( ticketValue == this.ticket ) {
            setBusyImpl(value);
            return true;
        }
        return false;
    }
    
    /** 
     * Reflet a new <code>Future</code> to reflet.
     * This model will be set as <code>undeterminate</code> and <code>cancellable</code> if specified.
     * @param future New Future to reflet.
     * @param cancellable true for let this future cancellable by the JBusyComponent
     */
    public synchronized void setFuture(final Future future , final boolean cancellable ) {
        /** unregister current future
         */
        unregister();

        /** define cancellable property
         */
        this.setCancellable(cancellable);

        /** register the new future
         */
        register(future);
    }

    /** Register the specified {@link Future} in this FutureBusyModel
     */
    private void register(Future future) {
        this.trackedFuture = future;
        if( this.trackedFuture instanceof SwingWorker ) {
            setDeterminate(true);
            setMinimum(0);
            setMaximum(100);
            
            SwingWorker worker = (SwingWorker)this.trackedFuture;
            worker.addPropertyChangeListener(this.listener);
        }
        else {
            setDeterminate(false);
        }

        if( this.trackedFuture != null ) {
            /** Tracker job to execute on our dedicated thread
             */
            final Runnable tracker = new Runnable() {
                public void run() {
                    int myTicket = 0;
                    try {
                        final Future myFuture = FutureBusyModel.this.trackedFuture;
                        while( ! myFuture.isDone() ) {
                            myTicket = setBusyImpl(true);
                            try {
                                myFuture.get();
                            }
                            catch(final Exception e) {
                               if( myFuture != FutureBusyModel.this.trackedFuture ) {
                                   /** probably the model must reflet now a different Future
                                    *  We must stop to reflet this one
                                    */
                                   break;
                               }
                            }
                        }
                    }
                    finally {
                        compareAndSetBusy( false, myTicket );
                    }
                }
            };

            if( this.service == null ) this.service = Executors.newSingleThreadExecutor();
            this.trackerFuture = this.service.submit(tracker);
        }
        else {
            setBusyImpl(false);
        }
    }

    /** Unregister the current future from this FutureBusyModel
     */
    private synchronized void unregister() {
        if( this.trackedFuture != null ) {
            if( this.trackedFuture instanceof SwingWorker ) {
                SwingWorker worker = (SwingWorker)this.trackedFuture;
                worker.removePropertyChangeListener(this.listener);
            }
        }
        this.setDeterminate(false);
        this.trackedFuture = null;

        if( this.trackerFuture != null ) {
            this.trackerFuture.cancel(true);
            this.trackerFuture = null;
        }
    }

    /** Cancel the current <code>future</code> under process
     */
    @Override
    public synchronized void cancel() {
        final Future toCancel = this.trackedFuture;
        if( toCancel != null ) {
            toCancel.cancel(true);
        }
    }

    /** Dispose the model by freeing threading resources.
     *  @since 1.2.2
     */
    public synchronized void dispose() {
        setFuture(null,false);
        if( this.service != null ) {
            this.service.shutdownNow();
            this.service = null;
        }
    }
}