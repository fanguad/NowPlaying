/*
 * 
 * Copyright (c) 2007 ANDRE Sébastien (divxdede).  All rights reserved.
 * AbstractBusyLayerUI.java is a part of this JBusyComponent library
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
package org.divxdede.swing.busy.ui;

import org.divxdede.swing.busy.BusyModel;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract implementation of <code>BusyLayerUI</code>.
 * <p>
 * This implementation subclass <code>LockableUI</code> for protecting the view
 * across any access during the <code>busy</code> state.
 * <p>
 * <code>setBusyModel</code> and <code>getBusyModel</code> are provided with
 * default implementation that use the final <code>updateUI</code> method.
 * <p>
 * You must override <code>updateUIImpl</code> method for complete the layer update
 * when needed. This method is called for any changes on the model or ui.
 * 
 * @author André Sébastien (divxdede)
 */
public abstract class AbstractBusyLayerUI extends LockableUI implements BusyLayerUI {
    
    /** Busy model
     */
    private BusyModel model = null;
        
    /** Model listener
     */
    private ChangeListener modelListener = null;

    /** Refer to the last state known of the bounded model
     */
    private final AtomicBoolean lastBusyState = new AtomicBoolean(false);
    
    /** Default constructor
     */
    public AbstractBusyLayerUI() {
        this.modelListener = createModelListener();
    }
    
    /** 
     *  Define the BusyModel used by this ui
     *  @param model New BusyModel to use by this ui
     */
    public void setBusyModel( final BusyModel model ) {
        
        final BusyModel oldValue = this.getBusyModel();
        if( getBusyModel() != null ) {
            getBusyModel().removeChangeListener( this.modelListener );
        }
        
        this.model = model;
        
        if( getBusyModel() != null ) {
            this.lastBusyState.set( this.model.isBusy() );
            getBusyModel().addChangeListener( this.modelListener );
            updateUI();
        }
    }
    
    /** 
     *  Returns the BusyModel used by this ui
     *  @return BusyModel used by this ui
     */    
    public BusyModel getBusyModel() {
        return this.model;
    }
    
    /** Internal "update" of this UI.
     *  This method should update this layer ui from the BusyModel properties.
     */
    protected final void updateUI() {
       if( SwingUtilities.isEventDispatchThread() ) updateUIImpl();
       else {
           final Runnable doRun = new Runnable() {
               public void run() {
                   updateUIImpl();
               }
           };
           SwingUtilities.invokeLater(doRun);
       }
    }

    @Override
    public void updateUI(final JLayer<? extends JComponent> l) {
        this.updateUI();
        super.updateUI(l);
    }    
    
    /** Overridable method for customize updateUI()
     */
    protected void updateUIImpl() {
        setLocked( shouldLock() );
        setDirty(true);
    }

    /** Indicate if the {@link BusyModel} is busy or not
     *  @return <code>true</code> if the model is busy
     *  @since 1.2
     */
    protected boolean isModelBusy() {
         return ( getBusyModel() == null ? false : getBusyModel().isBusy() );
    }

    /** Indicate if this layer should be placed in a locked state.
     *  This default implementation lock the layer when the model is <code>busy</code>.
     */
    protected boolean shouldLock() {
        return isModelBusy();
    }
    
    /** Returns the ModelListener usable by this UI
     */
    private ChangeListener createModelListener() {
        return new ChangeListener() {
           public void stateChanged(final ChangeEvent e) {
              boolean newValue = getBusyModel().isBusy();

              // perform an updateUI only if the model change it's busy state
              if( lastBusyState.get() != newValue ) {
                  lastBusyState.set(newValue);
                  updateUI();
              }
           }
        };
    }    
}
