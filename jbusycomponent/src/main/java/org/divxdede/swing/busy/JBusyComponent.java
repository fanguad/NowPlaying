/*
 * 
 * Copyright (c) 2007 ANDRE Sébastien (divxdede).  All rights reserved.
 * JBusyComponent.java is a part of this JBusyComponent library
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

import org.divxdede.swing.busy.ui.BasicBusyLayerUI;
import org.divxdede.swing.busy.ui.BusyLayerUI;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;

/**
 * Component decorator that enhance <strong>any swing components</strong> with
 * <strong>busy</strong> feature.
 * <p>
 * This decorator enhance a view (swing component) that provide a smart animation
 * when it's view is busy and restrict any acces to it.
 * The decorator take parts on the components hierarchy and must be added to the 
 * container instead of the original component that is now a simple view of the 
 * <code>JBusyComponent</code>
 * <p>
 * Your component still the same as before and keep all of theses features and
 * behaviour. The main difference is that now you can refer and use a 
 * <code>BusyModel</code> from the <code>JBusyComponent</code> decorator.<br>
 * This model allow you to control the <strong>busy property</strong> and some
 * other related informations.
 * <p>
 * Typically, a busy component is locked (can't be accessed anymore) and show
 * on an overlay a smart animation showing this busy state. Regarding the 
 * <code>BusyModel</code> configuration, you can have also a progress bar (if
 * the <code>BusyModel</code> is on a determinate state) and/or a cancel button 
 * (if the <code>BusyModel</code> is cancellable</code).
 * <p>
 * <code>JBusyComponent</code> is at the top of this API.
 * But in fact, it's just a wrapper of <code>JXLayer</code> and a <code>LayerUI</code> implementation.<br>
 * All business implementation are done by the <code>LayerUI</code> and you can use directly
 * a <code>JXLayer</code> instead of a <code>JBusyComponent</code>.
 * <p>
 * This is a little example:
 * <pre>
 *      // your component to enhance
 *      JTree comp = .....;
 * 
 *      // Create the JBusyComponent enhancer
 *      JBusyComponent<JTree> busyComp = new JXLayer<JTree>(comp);
 * 
 *      // Add our JBusyComponent to the container instead of our component
 *      myContainer.add( layer );
 * 
 *      // Use the BusyModel for control the busy state on our component
 *      BusyModel model = busyComp.getBusyModel();
 * 
 *      // Let's got to put our original component to a busy state
 *      model.setBusy(true); // an animation over our component is shown
 * </pre>
 * 
 * @see BusyModel
 * @see BusyLayerUI
 * @author André Sébastien (divxdede)
 */
public class JBusyComponent<C extends JComponent> extends JComponent implements Scrollable {

    /** Members
     */
    private JLayer<JComponent> layer = null;
    private BusyLayerUI         ui    = null;
    
    /** 
     * Create a <code>JBusyComponent</code> with no view
     * @see #setView
     */
    public JBusyComponent() {
        this(null);
    }
    
    /** 
     * Create a <code>JBusyComponent</code> with a specified view
     * @param view The view of this component
     */
    public JBusyComponent( final C view ) {
        this( view , new BasicBusyLayerUI() );
    }
    
    /** 
     * Create a <code>JBusyComponent</code> with a specified view and a BusyLayerUI
     * @param view The view of this component
     * @param ui The ui of this component
     */
    public JBusyComponent( final C view  , final BusyLayerUI ui) {
        
        /** Create the layer
         */
        this.layer = new JLayer<JComponent>(view);
        
        /** Configure it's fixed contents
         */
        super.setLayout( new BorderLayout() );
        super.add( this.layer );
        super.setOpaque(false);
        
        /** Install the UI
         */
        setBusyLayerUI(ui);

        /** Create a default model
         */
        setBusyModel( new DefaultBusyModel() );
    }
    
    /** 
     * Returns the View of this JBusyComponent
     * @return the underlying view of this JBusyComponent
     */
    public C getView() {
        return (C)this.layer.getView();
    }
    
    /**
     * Define the view of this JBusyComponent
     * @param view the new view of this component
     */
    public void setView(final C view) {
        this.layer.setView(view);
    }
    
    /** 
     * Returns the BusyLayerUI used by this component.
     * @return BusyLayerUI used by this component, this ui subclass LayerUI
     */
    public BusyLayerUI getBusyLayerUI() {
        return this.ui;
    }
    
    /** 
     * Define which BusyLayerUI this component must used for render the "busy" state
     * @param newUI New BusyLayerUI to use
     */
    public void setBusyLayerUI( BusyLayerUI newUI) {
        
        if( newUI == null )  newUI = new BasicBusyLayerUI();
        else {
            if( ! (newUI instanceof LayerUI) ) {
                throw new IllegalArgumentException("newUI must subclass LayerUI");
            }
        }

        /** Keep track to our "Busy Model"
         */
        BusyModel model = null;
        if( getBusyLayerUI() != null ) {
            model = getBusyLayerUI().getBusyModel();
            getBusyLayerUI().setBusyModel(null);
        }

        /** Change the UI
         */
        this.ui = newUI;
        this.layer.setUI( (LayerUI)getBusyLayerUI() );
        
        /** Update the BusyLayerUI with our "Busy Model"
         */
        if( getBusyLayerUI() != null ) {
            getBusyLayerUI().setBusyModel(model);
        }
    }
    
    /** 
     *  Define the BusyModel used by this component
     *  @param model New BusyModel to use by this component
     */
    public void setBusyModel(final BusyModel model) {
        final BusyLayerUI myUI = getBusyLayerUI();
        if( myUI == null ) throw new IllegalStateException("Can't set a BusyModel on a JBusyComponent without a BusyLayerUI");
        myUI.setBusyModel( model );
    }
    
    /** 
     *  Returns the BusyModel used by this component
     *  @return BusyModel used by this component
     */
    public BusyModel getBusyModel() {
        final BusyLayerUI myUI = getBusyLayerUI();
        if( myUI == null ) return null;
        
        return myUI.getBusyModel();
    }
    
    /**
     * Returns <code>true</code> if this component is on a busy state
     * @return <code>true</code> if this component is on a busy state
     */
    public boolean isBusy() {
        return getBusyModel() != null && getBusyModel().isBusy();
    }

    /**
     *  Define if this component is in a busy state or not
     *  @param value <code>true</code> value set this component in a busy state
     */
    public void setBusy(final boolean value) {
        final BusyModel model = getBusyModel();
        if( model != null ) model.setBusy(value);
    }

    /**
     * Returns the preferred size of the viewport for a view component.
     * For example, the preferred size of a <code>JList</code> component
     * is the size required to accommodate all of the cells in its list.
     * However, the value of <code>preferredScrollableViewportSize</code>
     * is the size required for <code>JList.getVisibleRowCount</code> rows.
     * A component without any properties that would affect the viewport
     * size should just return <code>getPreferredSize</code> here.
     *
     * @return the preferredSize of a <code>JViewport</code> whose view
     *    is this <code>Scrollable</code>
     * @see JViewport#getPreferredSize
     */
    public Dimension getPreferredScrollableViewportSize() {
        if( getView() instanceof Scrollable ) return ((Scrollable)getView()).getPreferredScrollableViewportSize();
        return getPreferredSize();
    }


    /**
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one new row
     * or column, depending on the value of orientation.  Ideally,
     * components should handle a partially exposed row or column by
     * returning the distance required to completely expose the item.
     * <p>
     * Scrolling containers, like JScrollPane, will use this method
     * each time the user requests a unit scroll.
     *
     * @param visibleRect The view area visible within the viewport
     * @param orientation Either SwingConstants.VERTICAL or SwingConstants.HORIZONTAL.
     * @param direction Less than zero to scroll up/left, greater than zero for down/right.
     * @return The "unit" increment for scrolling in the specified direction.
     *         This value should always be positive.
     * @see JScrollBar#setUnitIncrement
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if( getView() instanceof Scrollable ) return ((Scrollable)getView()).getScrollableUnitIncrement(visibleRect, orientation, direction);
        return 1;
    }


    /**
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one block
     * of rows or columns, depending on the value of orientation.
     * <p>
     * Scrolling containers, like JScrollPane, will use this method
     * each time the user requests a block scroll.
     *
     * @param visibleRect The view area visible within the viewport
     * @param orientation Either SwingConstants.VERTICAL or SwingConstants.HORIZONTAL.
     * @param direction Less than zero to scroll up/left, greater than zero for down/right.
     * @return The "block" increment for scrolling in the specified direction.
     *         This value should always be positive.
     * @see JScrollBar#setBlockIncrement
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if( getView() instanceof Scrollable ) return ((Scrollable)getView()).getScrollableBlockIncrement(visibleRect, orientation, direction);
        return 1;
    }


    /**
     * Return true if a viewport should always force the width of this
     * <code>Scrollable</code> to match the width of the viewport.
     * For example a normal
     * text view that supported line wrapping would return true here, since it
     * would be undesirable for wrapped lines to disappear beyond the right
     * edge of the viewport.  Note that returning true for a Scrollable
     * whose ancestor is a JScrollPane effectively disables horizontal
     * scrolling.
     * <p>
     * Scrolling containers, like JViewport, will use this method each
     * time they are validated.
     *
     * @return True if a viewport should force the Scrollables width to match its own.
     */
    public boolean getScrollableTracksViewportWidth() {
        if( getView() instanceof Scrollable ) return ((Scrollable)getView()).getScrollableTracksViewportWidth();
        return false;
    }

    /**
     * Return true if a viewport should always force the height of this
     * Scrollable to match the height of the viewport.  For example a
     * columnar text view that flowed text in left to right columns
     * could effectively disable vertical scrolling by returning
     * true here.
     * <p>
     * Scrolling containers, like JViewport, will use this method each
     * time they are validated.
     *
     * @return True if a viewport should force the Scrollables height to match its own.
     */
    public boolean getScrollableTracksViewportHeight() {
        if( getView() instanceof Scrollable ) return ((Scrollable)getView()).getScrollableTracksViewportHeight();
        return false;
    }
    
    @Override
    public Component add(final Component comp) {
        throw new UnsupportedOperationException("JBusyComponent.add() is not supported.");
    }    
    
    @Override
    public void remove(final Component comp) {
        throw new UnsupportedOperationException("JBusyComponent.remove(Component) is not supported.");
    }

    @Override
    public void removeAll() {
        throw new UnsupportedOperationException("JBusyComponent.removeAll() is not supported.");
    }    
}
