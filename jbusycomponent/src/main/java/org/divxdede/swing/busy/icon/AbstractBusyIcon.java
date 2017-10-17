/*
 * Copyright (c) 2010 ANDRE Sébastien (divxdede).  All rights reserved.
 * AbstractBusyIcon.java is a part of this JBusyComponent library
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
package org.divxdede.swing.busy.icon;

import org.divxdede.swing.busy.BusyIcon;
import org.divxdede.swing.busy.BusyModel;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;

/**
 * An implementation of the {@link BusyIcon} interface to serve as a 
 * basis for implementing various kinds of Busy Icons.
 * <p>
 * At this level, this class give some features for make BusyIcon implementation easier:<br>
 * <ul>
 *   <li>Paint methods for various states of icons <code>(determinate,undeterminate,idle)</code></li>
 *   <li>Common implementation for storing either a simple {@link BoundedRangeModel} or a more featured {@link BusyModel}</li>
 *   <li>Buffer Image cache for optimize repaint's event from {@link BoundedRangeModel} changes : {@link #setUseCache(boolean)}, {@link #getSignificantRatioOffset()}</li>
 *   <li>Automatic frame rate mecanism for render undeterminate state animation : {@link #setUndeterminateFrameRate(int, int)}</li>
 *   <li>Provide a {@link #repaint(boolean)} for subclasses uses when they needs to send a repaint event</li>
 *   <li>Extends {@link Observable} providing a delegate mecanism for repaint's event</li>
 * </ul>
 * <p>
 * This implementation provide 3 methods to implements for render a busy icon:
 * <ul>
 *   <li>{@link #paintDeterminate(java.awt.Component, java.awt.Graphics, int, int, float)} for render a determinate state with the specified ratio of the current progression</li>
 *   <li>{@link #paintUndeterminate(java.awt.Component, java.awt.Graphics, int, int, int)} for render an undeterminate state with the specified frame</li>
 *   <li>{@link #paintIdle(java.awt.Component, java.awt.Graphics, int, int)} for render an idle state (not busy)</li>
 * </ul>
 * <p>
 * This icon accept generic {@link BoundedRangeModel} or a more specific {@link BusyModel}.<br>
 * When this icon is bound to a generic <code>BoundedRangeModel</code>, this icon is always considered in a <code>determinate</code> state.<br>
 * But if this icon is bound to a <code>BusyModel</code>, this model can control the busy state or the determinate/undeterminate state.
 * <p>
 * The buffer image cache is done for optimize painting process when this icon state are unchanged.<br>
 * Instead of call real paints methods each time, this abstract implementation use a previously rendered image of this icon.<br>
 * In the other side, subclasses must implements {@link #getSignificantRatioOffset()} in the way to help this implementation determine
 * if the current buffer image cache is up to date regarding the current state of this icon.<br>
 * This basis implementation help us to prevent from various change from the {@link BoundedRangeModel}.
 * Theses models can have data range that can be large, and any minor change will fire a repaint event even if the change
 * don't be significant in the ui representation. That's why, you should implements the {@link #getSignificantRatioOffset()} accordingly
 * to your ui.
 * <p>
 * When this busy icon is on an <code>undeterminate</code> state, an internal timer will fire repaint events periodically.<br>
 * The {@link #paintUndeterminate(java.awt.Component, java.awt.Graphics, int, int, int)} method will be use for rendering this icon.<br>
 * The provided frame number is incremented each paint event and is cyclic accordingly to the configuration done by {@link #setUndeterminateFrameRate(int, int)}.<br>
 * This method should be used by subclasses in order to configure the undeterminate frame rate animation.
 * <p>
 * A protected {@link #repaint(boolean)} method is provided for subclasses when they need to fire a repaint event.<br>
 * This method call a {@link Component#repaint()} on each components registered on this icon.<br>
 * Registered components are all components specified to the {@link #paintIcon(java.awt.Component, java.awt.Graphics, int, int)}.<br>
 * In the other side, you can specify an {@link Observer} to this icon. If it's the case, components are not registered and only observer
 * will receive repaint's event.
 * <p>
 * This class don't give any basic implementation for {@link #getIconWidth()} and {@link #getIconHeight()} and must be implemented by subclasses.
 *
 * @author André Sébastien (divxdede)
 * @since 1.1
 */
public abstract class AbstractBusyIcon extends Observable implements BusyIcon {

    /** main members
     */
    private       BoundedRangeModel              model                  = null;
    private final ModelListener                  modelListener          = new ModelListener();
    private final List<WeakReference<Component>> components             = new LinkedList<WeakReference<Component>>();

    /** Buffer image cache members
     */
    private       BufferedImage                  cache                  = null;
    private       float                          lastRatio              = -1f;
    private       int                            lastStateFlag          = 0;
    private       boolean                        discarded              = false;
    private       boolean                        useCache               = true;

    /** Undeterminate timer members
     */
    private       int                            undeterminateFrameRate = 0;
    private       Timer                          undeterminateTimer     = null;
    private final ActionListener                 undeterminateListener  = new TimerListener();
    private       int                            frameCount             = 0;
    private       int                            frame                  = 0;

    /** Default constructor
     */
    public AbstractBusyIcon() {
        this.undeterminateTimer = new Timer(0,this.undeterminateListener);
    }

    /** Paint this icon in a <code>determinate</code> state at the given ratio.
     *
     *  @param c Component using this icon
     *  @param g Graphics to paint on
     *  @param x Upper left corner (horizontal value)
     *  @param y Upper left corner (vertical value)
     *  @param ratio Current advance of the {@link BoundedRangeModel}
     */
    protected abstract void paintDeterminate(Component c , Graphics g , int x , int y , float ratio);

    /** Paint this icon in an <code>undeterminate</code> state.<br>
     *  Regarding configuration set with {@link #setUndeterminateFrameRate(int, int)},
     *  this method will be invoked periodically when this icon need to render an undeterminate state.<br>
     *  The given frame number is a cyclic counter in range [0 ~ frameCount - 1].
     *
     *  @param c Component using this icon
     *  @param g Graphics to paint on
     *  @param x Upper left corner (horizontal value)
     *  @param y Upper left corner (vertical value)
     *  @param frame Current undeterminate frame number
     */
    protected abstract void paintUndeterminate(Component c , Graphics g , int x , int y , int frame);


    /** Paint this icon in an <code>idle</code> state.<br>
     *  An idle state mean that this icon is not currently busy and this icon should not paint any related information from the <code>Data Model</code>
     *
     *  @param c Component using this icon
     *  @param g Graphics to paint on
     *  @param x Upper left corner (horizontal value)
     *  @param y Upper left corner (vertical value)
     */
    protected abstract void paintIdle(Component c , Graphics g , int x , int y );

    /** Return the minimum advance to reach by the {@link BoundedRangeModel}
     *  between two paint's requests in order to discard the current buffer image (cache) and update this icon with the current state.
     *  <p>
     *  The {@link BoundedRangeModel} can have a large length between it's minimum and maximum values.
     *  When this length is large, this icon may receive a lot of repaint's events.<br>
     *  All of theses event's should not be forwarded if theses event's don't perform a significant difference into this icon.
     *  <p>
     *  By exemple, a progress bar of <code>50</code> pixels should serve a repaint event if some pixels changes.
     *  The significant ratio offset should be <code>1/50 = 0.02f</code><br>
     *  In this case, this icon will be updated each time at least a pixel change inside the progress bar.
     *  <p>
     *  If you render a progress bar with a radial representation, typically you can return a significant ratio offset of <code>1/360</code>.<br>
     *  In this case, this icon will be updated each time at least a degree change inside the radial representation.
     *  <p>
     *  This information are used only in conjunction of the buffer image provided with {@link #useCache()} and {@link #setUseCache(boolean) } methods.<br>
     *  If the image cache is disabled, all repaint events notified by the {@link BoundedRangeModel} will be forwarded.
     *
     *  @return Minimum ratio offset that must be reach by the BoundedRangeModel between to paint processes. Can be set to 0 for serve all paint's requests
     *  @see #useCache()
     *  @see #setUseCache(boolean)
     *  @see #getRatio()
     */
    protected abstract float getSignificantRatioOffset();

    /** Define the {@link BoundedRangeModel} used for render this icon progression.
     *  <p>
     *  If the BoundedRangeModel is a {@link BusyModel}, this icon will use
     *  attributes like {@link BusyModel#isBusy()} and {@link BusyModel#isDeterminate()} for render this icon.
     *
     *  @param model BoundedRangeModel to bound to this icon (can be null)
     */
    public synchronized void setModel(BoundedRangeModel model) {
        if( getModel() != null ) {
            getModel().removeChangeListener(this.modelListener);
        }
        this.model = model;
        if( getModel() != null ) {
            getModel().addChangeListener(this.modelListener);
        }
    }

    /** Return the current {@link BoundedRangeModel} used for render this icon progression.
     *  <p>
     *  If the BoundedRangeModel is a {@link BusyModel}, this icon will use
     *  attributes like {@link BusyModel#isBusy()} and {@link BusyModel#isDeterminate()} for render this icon.
     *
     * @return The underlying BoundedRangeModel (may be null)
     */
    public synchronized BoundedRangeModel getModel() {
        return this.model;
    }

    /** Indicate if this icon is rendering a determinate or undeterminate progression.
     *  <p>
     *  This attribute can be data-driven by a {@link BusyModel} set into {@link #setModel(javax.swing.BoundedRangeModel)}.<br>
     *  Any other instances of {@link BoundedRangeModel} will be considered as determinate.
     *
     *  @return <code>true</code> if this icon render a determinate progression, <code>false</code> otherwise
     */
    public boolean isDeterminate() {
        BusyModel m = getBusyModel();
        if( m != null ) return m.isDeterminate();
        return getModel() != null;
    }

    /** Indicate if this icon is rendering a busy state.
     *  <p>
     *  This attribute can be data-driven by a {@link BusyModel} set into {@link #setModel(javax.swing.BoundedRangeModel)}.<br>
     *  Any other instances of {@link BoundedRangeModel} will be considered as busy.
     *
     *  @return <code>true</code> if this icon render a busy state, <code>false</code> otherwise
     */
    public boolean isBusy() {
        BusyModel m = getBusyModel();
        if( m != null ) return m.isBusy();
        return getModel() != null;
    }

    /** If this icon supports animation for undeterminate model using a {@link BusyModel},
     *  This method should be call in order to configure the frame rate of this animation
     * @param delay
     */
    protected void setUndeterminateFrameRate(int delay, int frameCount) {
        this.undeterminateFrameRate = delay;
        this.frameCount = frameCount;
        refreshUndeterminateTimer();
    }

    /** Paint this icon.
     *  <p>
     *  This method can use an image buffer for render quickly this icon if no significant change is available since the last paint process.<br>
     *  You can enable or disable this behaviour with the {@link #setUseCache(boolean)} method.
     *  <p>
     *  When using a buffer image cache, this implementation must determine if the current image is up to date and can be used
     *  instead of perform a full painting process. This determination are done using the {@link #getSignificantRatioOffset()} information.
     *  <p>
     *
     * @param c Component using this icon
     * @param g Graphics to paint on
     * @param x Upper left corner (horizontal value)
     * @param y Upper left corner (vertical value)
     */
    public final void paintIcon(Component c, Graphics g, int x, int y) {
       if( this.countObservers() == 0 )
            register(c);
       
       boolean isBusy      = isBusy();
       boolean determinate = isBusy && isDeterminate();
       float   ratio       = determinate ? getRatio() : 0f;
       int     nFrame      = frame < 0 ? 0 : frame;

       if( !isCacheUpToDate(isBusy,determinate,ratio) ) {
            if( useCache() ) {
                BufferedImage offscreenImage = this.getCache();
                Graphics2D    offscreen      = offscreenImage.createGraphics();

                offscreen.setComposite(AlphaComposite.Clear);
                offscreen.fillRect( 0 , 0 , getIconWidth() , getIconHeight() );
                offscreen.setPaintMode();

                if( isBusy ) {
                    if( determinate ) paintDeterminate(c,offscreen,0,0,ratio);
                    else              paintUndeterminate(c,offscreen,0,0,nFrame);
                }
                else {
                    paintIdle(c,offscreen,0,0);
                }
                lastRatio     = ratio;
                lastStateFlag = getStateFlag(isBusy, determinate);
                discarded     = false;
            }
            else {
                // direct paint
                if( isBusy ) {
                    if( determinate ) paintDeterminate(c,g,x,y,ratio);
                    else              paintUndeterminate(c, g, x, y, nFrame);
                }
                else {
                    paintIdle(c, g , x , y);
                }
                lastRatio = ratio;
                lastStateFlag = getStateFlag(isBusy, determinate);
                discarded = false;
                return;
            }
        }
        BufferedImage offscreenImage = this.getCache();
        g.drawImage( offscreenImage, x , y , c);
    }

    /** Indicate if this icon is able to use a buffer image for painting code when the context allow it.
     *  <p>
     *
     * @return <code>true</code> if this icon use a buffer image for optimize render.
     */
    public synchronized boolean useCache() {
        return useCache;
    }

    /** Define if this icon should use or note a buffer image for painting process for optimization.
     * @param enable <code>true</code> for enable buffer image, <code>false</code> otherwise
     */
    public void setUseCache(boolean enable) {
        boolean oldValue = useCache();
        this.useCache = enable;

        if( oldValue != useCache() )
            repaint(true);
    }

    /** Request a paint update on any components that are previously painted this icon.
     *  <p>
     *  This method check the buffer-image up to date and if it's the case, the repaint will be ignored unless the <code>force</code> attribute is at <code>true</code>.
     *
     *  @param force <code>true</code> for serve a repaint in all cases.
     */
    protected synchronized void repaint(boolean force) {
        if( !force ) {
            if( isCacheUpToDate( isBusy() , isDeterminate()  , getRatio() ) )
                return; // repaint request ignored
        }
        else {
            this.discarded = true;
        }

        Iterator<WeakReference<Component>> i = components.iterator();
        while( i.hasNext() ) {
            WeakReference<Component> ref = i.next();
            Component comp = ref.get();
            if( comp == null ) {
                i.remove();
            }
            else {
                if( comp.isShowing() ) {
                    comp.repaint();
                }
                else {
                    // we wan remove it, when this component will become visible again,
                    // it will perform a paint on this icon and we register it a new time.
                    i.remove();
                }
            }
        }
        
        /** Also notify all external observers
         */
        this.setChanged();
        this.notifyObservers();
    }

    /** Return the underlying BusyModel if the model is instanceof of BusyModel.<br>
     *  return <code>null</code> otherwhise
     */
    private BusyModel getBusyModel() {
        BoundedRangeModel m = getModel();
        if( m instanceof BusyModel ) return (BusyModel)m;
        return null;
    }

    /** Indicate if the current buffer image is up to date and can be use for a quick-render of this icon.
     *  @return <code>true</code> if the curent buffer image is up to date
     *  @see #useCache()
     *  @see #getSignificantRatioOffset()
     */
    private boolean isCacheUpToDate( boolean isBusy , boolean determinate , float ratio) {
        int state = getStateFlag(isBusy, determinate);
        if( state != this.lastStateFlag ) return false;

        if( determinate ) {
            return useCache() && !discarded && ( this.lastRatio >= 0f && getRatioOffset(ratio) < getSignificantRatioOffset() );
        }
        else {
            return useCache() && !discarded;
        }
    }

    /** Create/Update or return the buffer image used as a cache
     */
    private synchronized BufferedImage getCache() {
        if( !useCache() )
            return null;

        if( this.cache == null || this.cache.getWidth() != getIconWidth() || this.cache.getHeight() != getIconHeight() ) {
             GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
             this.cache = gc.createCompatibleImage( getIconWidth() , getIconHeight() , Transparency.TRANSLUCENT );
        }
        return this.cache;
    }

    /** Return the current advance of the {@link BoundedRangeModel}.
     *  This advance is given as a ratio [0 ~ 1] where 0 = 0% and 1 == 100%
     *
     *  @return Curent advance of the {@link BoundedRangeModel}.
     *  @see #getSignificantRatioOffset()
     */
    private float getRatio() {
        BoundedRangeModel brm   = getModel();
        BusyModel         busy  = getBusyModel();

        if( busy != null ) {
            if( ! busy.isBusy() ) return 0f;
            if( ! busy.isDeterminate() ) return 0f;
        }
        if( brm != null ) {
            int   length    = brm.getMaximum() - brm.getMinimum();
            int   value     = brm.getValue() + brm.getExtent();
            return (float)value / (float)length;
        }
        else return 0f;
    }

    /** Return the <code>ratio offset</code> between the last call of the {@link #getRatio()} and now.
     *  <p>
     *  This method is used for determine if the buffer-image is up to date or not.
     *
      * @return ratio offset between the last call of the {@link #getRatio()} and now
     */
    private float getRatioOffset(float ratio) {
         return Math.abs( ratio - this.lastRatio );
    }

    /** Return an int that store the given state of a rendered model
     *    0 : not busy
     *    1 : undeterminate busy
     *    3 ! determinate busy
     */
    private int getStateFlag(boolean isBusy, boolean isDeterminate) {
        return (isBusy ? 1 : 0) + (isDeterminate ? 2 : 0 );
    }

    /** Register the component for be able to fire repaint event
     */
    private synchronized void register(Component c) {
        Iterator<WeakReference<Component>> i = components.iterator();
        while( i.hasNext() ) {
            WeakReference<Component> ref = i.next();
            Component comp = ref.get();
            if( comp == null ) i.remove();
            if( comp == c ) return;
        }
        components.add( new WeakReference<Component>(c) );
    }

    /** Unable to start/stop timer for paint undeterminate state
     *  @return <code>true</code> if the timer was changed
     */
    private boolean refreshUndeterminateTimer() {
        boolean timerEnabled = isBusy() && !isDeterminate() && this.undeterminateFrameRate > 0;
        if( timerEnabled ) {
            this.undeterminateTimer.setDelay( this.undeterminateFrameRate );
            if( ! this.undeterminateTimer.isRunning() ) {
                this.frame = -1;
                this.undeterminateTimer.start();
                return true;
            }
        }
        else {
            if( this.undeterminateTimer.isRunning() ) {
                this.undeterminateTimer.stop();
                return true;
            }
        }
        return false;
    }

    /** Private implementation of ChangeLister performing a repaint
     */
    private class ModelListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            BoundedRangeModel model = getModel();

            /** Refresh configuration timer with the new model state
             *  Maybe the timer should be stopped, started, reconfigured...
             */
            boolean force = refreshUndeterminateTimer();

            if( model != null ) {
                if( model.getValue() == model.getMaximum() && lastRatio < 1f ) {
                    force = true;
                }
            }
            repaint(force);
        }
    }

    private class TimerListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            frame++;
            if( frame >= frameCount ) frame = 0;
            repaint(true);
        }
    }
}