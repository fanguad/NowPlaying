/*
 * 
 * Copyright (c) 2007 ANDRE Sébastien (divxdede).  All rights reserved.
 * BasicBusyLayerUI.java is a part of this JBusyComponent library
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

import org.divxdede.swing.RemainingTimeMonitor;
import org.divxdede.swing.busy.BusyIcon;
import org.divxdede.swing.busy.BusyModel;
import org.divxdede.swing.busy.icon.InfiniteBusyIcon;
import org.divxdede.text.TimeFormat;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.BusyPainter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Default implementation of BusyLayerUI.
 * <p>
 * This UI provide a busy animation, progress bar and cancellation button
 * regarding the <code>BusyModel</code>.
 * <p>
 * You can enhance any swing components with busy fonctionnality like it:
 * <pre>
 *      // your component to enhance
 *      JComponent comp = .....;
 * 
 *      // Create the JXLayer decorator
 *      JXLayer<JComponent> layer = new JXLayer<JComponent>(comp);
 * 
 *      // Create the Busy Layer UI delegate
 *      BusyLayerUI ui = new BasicBusyLayerUI();
 * 
 *      // Attach the UI to the decorator
 *      layer.setUI( (LayerUI)ui );
 * 
 *      // Add the decorator to the container instead of our component
 *      myContainer.add( layer );
 * 
 *      // Use the BusyModel for control the busy state on our component
 *      // If multiple components share the same BusyModel, all of theses will be
 *      // triggered by the same model
 *      BusyModel model = ui.getBusyModel();
 *      model.setBusy(true); // an animation over our component is shown
 * </pre>
 * 
 * @author André Sébastien (divxdede)
 */
public class BasicBusyLayerUI extends AbstractBusyLayerUI {

    /** Components
     */
    JXPanel              jXGlassPane       = new JXPanel();
    JLabel               jLabel            = new JLabel();
    JProgressBar         jProgressBar      = new JProgressBar();
    JXHyperlink          jXHyperlinkCancel = new JXHyperlink();
    BusyIcon             busyIcon          = null;
    Observer             observer          = new AnimationObserver();


    /** Members managing popup trigger and remaining time
     */
    boolean              remainingTimeVisible  = false;
    RemainingTimeMonitor monitor               = null;
    long                 monitorStartTime      = -1L;
    final TimeFormat     timeFormat            = new TimeFormat( TimeUnit.SECONDS );
    int                  millisToDecideToPopup = 300;
    int                  millisToPopup         = 1200;
    
    /** Listener for cancelling action
     */
    ActionListener cancelListener    = null;
    
    /** Veil members (color and alpha) to render
     *  A null color or a 0 alpha mean no veil
     */
    float                  veilAlpha               = 0;     // 0 < alpha > 1
    Color                  veilColor               = null;
    long                   backgroundVeilStartTime = 0L;    // > 0 : busy transition start time. < 0 idle transition start time

    /** Shading members for rendering the veil with an animation
     *  A shadeDelayInterval <= 0 means no shading
     */
    int                    shadeDelayTotal    = 0;  // in milliseconds

    /** Internal members for manage shading & veil rendering
     */
    private int            alpha              = 0;
    private Timer          timer              = null;
    private Painter        painter            = null;
    private final AtomicBoolean  repainted          = new AtomicBoolean(false);

    /** Insets used
     */
    private static final Border NO_SPACE      = new EmptyBorder( new Insets(0,0,0,0) );
    private static final Border MARGIN        = new EmptyBorder( new Insets(0,10,0,0) );
    private static final int    REFRESH_DELAI = 32;

    /** Basic Implementation with default values
     */
    public BasicBusyLayerUI() {
        this( 400 , 0.85f  , Color.WHITE );
    }

    /** Basic Implementation with shading configuration's
     *  @param shadeDelay Shading delay in milliseconds for render <code>busy</code> state change, 0 means no shading
     *  @param veilAlpha Alpha ratio to use for the veil when the model is <code>busy</code>
     *  @param veilColor Color to use for render the veil
     */ 
    public BasicBusyLayerUI(final int shadeDelay , final float veilAlpha  , final Color veilColor) {
        
        this.cancelListener     = createCancelListener();
        this.timer              = createTimer();
                                  createGlassPane();
        
        this.shadeDelayTotal    = shadeDelay;
        this.veilAlpha          = veilAlpha;
        this.veilColor          = veilColor;

        this.setBusyIcon( new InfiniteBusyIcon() );
    }
    
    @Override
    public void installUI(final JComponent c) {
        super.installUI(c);
        
        final JLayer layer = (JLayer)c;
        layer.setGlassPane( this.jXGlassPane );
    }

    @Override
    public void uninstallUI(final JComponent c) {
        // Issue 13 : Stop timer if the view is uninstalled
        if( this.timer.isRunning() ) this.timer.stop();
        super.uninstallUI(c);
        final JLayer layer = (JLayer)c;
        layer.setGlassPane( null );
    }

    /** Define the BusyIcon to use by this ui to render the busy animation.
     *  @param icon New BusyIcon to use by this ui
     *  @since 1.1
     */
    public void setBusyIcon(BusyIcon icon) {
        if( this.busyIcon != null && this.busyIcon instanceof Observable ) {
            ((Observable)this.busyIcon).deleteObserver( this.observer );
        }
        if( this.busyIcon != null ) {
            this.busyIcon.setModel(null);
        }

        this.busyIcon = icon;

        if( this.busyIcon != null && this.busyIcon instanceof Observable ) {
            ((Observable)this.busyIcon).addObserver( this.observer );
        }
        if( this.busyIcon != null ) {
            this.busyIcon.setModel( getBusyModel() );
        }
        this.jLabel.setIcon( this.busyIcon );
        updateUI();
    }

    /** Return the BusyIcon used by this ui for render the busy animation.
     *  @return BusyIcon used by this ui
     *  @since 1.1
     */
    public BusyIcon getBusyIcon() {
        return this.busyIcon;
    }

    /**
     * Specifies the amount of time to wait before deciding whether or
     * not to make busy the component when it's underlying model is.
     * <p>
     * This feature purpose is to prevent to show a progress bar for a very very short time.<br>
     * This {@link BusyLayerUI} wait few times (300ms by default) and decide to popup the progress bar or not.
     * <p>
     * With a determinate model, the decision was made by computing a predicted remaining time of the underlying task.<br>
     * If the remaining time is long enough (>= 1200ms by default), the progress bar will shown.<br>
     * With an undeterminate model, this layer consider that it must render the busy state after the decision time is reach.
     * <p>
     * When the model gone busy, the component is instantly locked (can't be accessed anymore)
     * even if the progress bar is not yet visible.
     * <p>
     * Setting a 0 value or any negative value disable this feature.
     * In this case, the progress bar will popup instantly when the model become busy.
     *
     * @param millisToDecideToPopup  an int specifying the time to wait, in milliseconds
     * @see #getMillisToDecideToPopup
     * @since 1.2
     */
    public void setMillisToDecideToPopup(int millisToDecideToPopup) {
        this.millisToDecideToPopup = millisToDecideToPopup;
    }

    /**
     * Returns the amount of time this object waits before deciding whether
     * or not to propage the busy state from the model to the component.
     * <p>
     * This feature purpose is to prevent to show a progress bar for a very very short time.<br>
     * This {@link BusyLayerUI} wait few times (300ms by default) and decide to popup the progress bar or not.
     * <p>
     * With a determinate model, the decision was made by computing a predicted remaining time of the underlying task.<br>
     * If the remaining time is long enough (>= 1200ms by default), the progress bar will shown.<br>
     * With an undeterminate model, this layer consider that it must render the busy state after the decision time is reach.
     * <p>
     * When the model gone busy, the component is instantly locked (can't be accessed anymore)
     * even if the progress bar is not yet visible.
     * <p>
     * Getting a 0 value or any negative value indicate a disabled feature.<br>
     * In this case, the progress bar will popup instantly when the model become busy.
     *
     * @see #setMillisToDecideToPopup
     * @since 1.2
     */
    public int getMillisToDecideToPopup() {
        return millisToDecideToPopup;
    }

    /**
     * Specifies the amount of remaining time required when this object take it's decision.
     * <p>
     * After {@link #getMillisToDecideToPopup()}, this layer compute the remaining time's job,<br>
     * If its long enough regarding this property, the progress bar will be shown.
     * <p>
     * This feature purpose is to prevent to show a progress bar for a very very short time.<br>
     * This {@link BusyLayerUI} wait few times (300ms by default) and decide to popup the progress bar or not.
     * <p>
     * With a determinate model, the decision was made by computing a predicted remaining time of the underlying task.<br>
     * If the remaining time is long enough (>= 1200ms by default), the progress bar will shown.<br>
     * With an undeterminate model, this layer consider that it must render the busy state after the decision time is reach.
     * <p>
     * When the model gone busy, the component is instantly locked (can't be accessed anymore)
     * even if the progress bar is not yet visible.
     * <p>
     * Setting a 0 value or any negative value disable this feature.
     * In this case, the progress bar will popup instantly when the model become busy.
     *
     * @param millisToPopup  an int specifying the time in milliseconds
     * @see #getMillisToPopup
     * @since 1.2
     */
    public void setMillisToPopup(int millisToPopup) {
        this.millisToPopup = millisToPopup;
    }

    /**
     * Returns the amount of remaining time required when this object take it's decision.
     * <p>
     * After {@link #getMillisToDecideToPopup()}, this layer compute the remaining time's job,<br>
     * If its long enough regarding this property, the progress bar will be shown.
     * <p>
     * This feature purpose is to prevent to show a progress bar for a very very short time.<br>
     * This {@link BusyLayerUI} wait few times (300ms by default) and decide to popup the progress bar or not.
     * <p>
     * With a determinate model, the decision was made by computing a predicted remaining time of the underlying task.<br>
     * If the remaining time is long enough (>= 1200ms by default), the progress bar will shown.<br>
     * With an undeterminate model, this layer consider that it must render the busy state after the decision time is reach.
     * <p>
     * When the model gone busy, the component is instantly locked (can't be accessed anymore)
     * even if the progress bar is not yet visible.
     * <p>
     * Getting a 0 value or any negative value indicate a disabled feature.<br>
     * In this case, the progress bar will popup instantly when the model become busy.
     * 
     * @see #setMillisToPopup
     * @since 1.2
     */
    public int getMillisToPopup() {
        return millisToPopup;
    }

    /** Define if this {@link BusyLayerUI} should show the remaining time
     *  of the job underlying the busy state.
     *  <p>
     *  This feature works only with determinate {@link BusyModel}
     *
     *  @param value set to <code>true</code> to show the remaining time when a determinate model is busy
     *  @see #isRemainingTimeVisible()
     *  @since 1.2
     */
    public void setRemainingTimeVisible(boolean value) {
        this.remainingTimeVisible = value;
    }

    /** Indicate if this {@link BusyLayerUI} should show the remaining time
     *  of the job underlying the busy state
     *  <p>
     *  This feature works only with determinate {@link BusyModel}
     *
     *  @return <code>true</code> of the remaining time should by shown
     *  @since 1.2
     */
    public boolean isRemainingTimeVisible() {
        return this.remainingTimeVisible;
    }

    /** 
     * Returns the busy painter to use for render the busy animation
     * @return BusyPainter used for render the friendly busy animation
     * @deprecated v1.1: Use instead {@link #getBusyIcon()}. This method can return <code>null</code> if the BusyIcon is not an {@link InfiniteBusyIcon}.
     */
    public BusyPainter getBusyPainter() {
        BusyIcon myIcon = getBusyIcon();
        if( myIcon instanceof InfiniteBusyIcon ) {
            return ((InfiniteBusyIcon)myIcon).getBusyPainter();
        }
        return null;
    }
    
    /** 
     * Define the busy painter to use for render the friendly busy animation
     * @param busyPainter New busy painter to use for render the friendly busy animation
     * @deprecated v1.1: Use instead a setBusyIcon( new InfiniteBusyIcon( busyPainter , null ) );
     */
    public void setBusyPainter(final BusyPainter busyPainter) {
        this.setBusyPainter(busyPainter,null);
    }
    
    /** 
     * Define the busy painter to use for render the friendly busy animation
     * @param busyPainter New busy painter to use for render the friendly busy animation
     * @param preferredSize Preferred Size to use for this painter animation
     * @deprecated v1.1: Use instead a setBusyIcon( new InfiniteBusyIcon( busyPainter , preferredSize ) );
     */
    public void setBusyPainter(final BusyPainter busyPainter , final Dimension preferredSize ) {
        setBusyIcon( new InfiniteBusyIcon(busyPainter,preferredSize) );
    }
    
    @Override
    protected void paintLayer(final Graphics2D g2, final JLayer<? extends JComponent> l) {
        super.paintLayer(g2, l);
        final Painter painter = getBackGroundPainter();
        if( painter != null ) {
            painter.paint(g2, null , l.getWidth(), l.getHeight() );
        }
    }
    
    @Override
    protected void updateUIImpl() {
        final BusyModel myModel = getBusyModel();
        final BusyIcon  myIcon  = getBusyIcon();
        final boolean   isBusy  = isComponentBusy();

        /** Ensure the timer is running when the model is busy (Issue 13 : add getLayer() != null)
         */
        if( myModel != null && myModel.isBusy() && !this.timer.isRunning() && getLayer() != null ) {
            this.timer.start();
        }
        repainted.set(true);
        
        /** Visible states
         */
        this.jXGlassPane.setVisible( isBusy );
        this.jLabel.setVisible( isBusy );
        this.jProgressBar.setVisible( isBusy && myModel.isDeterminate() && !myIcon.isDeterminate() );

        { final boolean hyperlinkVisible = isBusy && myModel.isCancellable();
          if( hyperlinkVisible && !this.jXHyperlinkCancel.isVisible() ) 
              this.jXHyperlinkCancel.setClicked(false);
        
          this.jXHyperlinkCancel.setVisible( hyperlinkVisible );
        }

        /** Background shading animation (check if start needed)
         */
        this.manageBackgroundVeil(isBusy);
        
        /** If cancellable, update it's border regarding the progress bar visible state
         */
        if( isBusy ) {
            if( myModel.isCancellable() )
                this.jXHyperlinkCancel.setBorder( this.jProgressBar.isVisible() ? MARGIN : NO_SPACE );

            /** Update the % 
             */
            String description = myModel.getDescription();
            String remaining   = getRemainingTimeString();

            if( isRemainingTimeVisible() ) {
                if( remaining == null ) remaining = " ";
                if( description == null ) {
                    this.jLabel.setText( remaining );
                    this.jProgressBar.setString( null );
                    this.jProgressBar.setStringPainted(true);
                }
                else {
                    this.jLabel.setText( description );
                    this.jProgressBar.setString( remaining );
                    this.jProgressBar.setStringPainted(true);
                }
            }
            else {
                if( description == null ) {
                    this.jLabel.setText( getPercentProgressionString() );
                    this.jProgressBar.setStringPainted(false);
                    this.jProgressBar.setString(null);
                }
                else {
                    this.jLabel.setText( description );
                    this.jProgressBar.setString( getPercentProgressionString() );
                    this.jProgressBar.setStringPainted(true);
                }
            }
        }
        super.updateUIImpl();
    }

    @Override
    public void setBusyModel(final BusyModel model) {
        super.setBusyModel(model);
        
        if( getBusyModel() != null )  {
            this.jProgressBar.setModel( model );
            if( this.busyIcon != null ) this.busyIcon.setModel( getBusyModel() );
        }
    }

    /** Create our glasspane
     */
    private JComponent createGlassPane() {
        
        final GridBagLayout layout = new GridBagLayout();
        this.jXGlassPane.setLayout( layout );
        this.jXGlassPane.setOpaque(false);

        final Insets emptyInsets = new Insets(0,0,0,0);
        final GridBagConstraints gbcLabel = new GridBagConstraints(1,1,2,1,0,0, GridBagConstraints.CENTER , GridBagConstraints.NONE , emptyInsets , 0 , 0 );
        final GridBagConstraints gbcBar   = new GridBagConstraints(1,2,1,1,0,0, GridBagConstraints.CENTER , GridBagConstraints.NONE , emptyInsets , 0 , 0 );
        final GridBagConstraints gbcLink  = new GridBagConstraints(2,2,1,1,0,0, GridBagConstraints.CENTER , GridBagConstraints.NONE , emptyInsets , 0 , 0 );
        
        this.jXGlassPane.add( this.jLabel , gbcLabel );
        this.jXGlassPane.add( this.jProgressBar , gbcBar );
        this.jXGlassPane.add( this.jXHyperlinkCancel , gbcLink );

        this.jLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        this.jLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        this.jLabel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        this.jXHyperlinkCancel.setText( UIManager.getString("OptionPane.cancelButtonText") );
        this.jXHyperlinkCancel.addActionListener( this.cancelListener );

        updateUI();
        return this.jXGlassPane;
    }
    
    /** Return the percent progression string
     */
    private String getPercentProgressionString() {
        final BusyModel myModel = getBusyModel();
        final boolean   isBusy  = isModelBusy();
        if( ! isBusy )                 return null;
        if( !myModel.isDeterminate() ) return null;
        
        final int   range   = myModel.getMaximum() - myModel.getMinimum();
        final int   value   = myModel.getValue();
        final float percent = ( 100f / range ) * ( value - myModel.getMinimum() );
        
        return  Integer.toString( (int)percent) + " %";
    }

    /** Return the remaining time string
     */
    private String getRemainingTimeString() {
        if( ! isRemainingTimeVisible() ) return null;
        final BusyModel myModel = getBusyModel();
        if( !myModel.isDeterminate() ) return null;
        
        if( this.monitor != null ) {
            long timeRemaining = monitor.getRemainingTime( TimeUnit.SECONDS );
            if( timeRemaining > 0 ) {
                if( timeRemaining == Long.MAX_VALUE ) return " Remaining time: \u221E";
                return " Remaining time: " + timeFormat.format( timeRemaining );
            }
        }
        return null;
    }
    
    /** Create the Listener managing the cancel action when click on the hyperlink
     */
    private ActionListener createCancelListener() {
        return new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                try {
                    getBusyModel().cancel();
                }
                catch(final Exception e2) {
                    e2.printStackTrace();
                }
            }
        };
    }

    /** Indicate if the component should be busy.
     *  <p>
     * If you use {@link #getMillisToDecideToPopup()} and {@link #getMillisToPopup()}, the component
     * will not be busy instantly when the model is. This layer will take time to predict the remaining time and
     * decide if the component would be busy or not.
     *
     * @return <code>true</code> if the component would be busy
     */
    protected boolean isComponentBusy() {
        final boolean isModelBusy                    = isModelBusy();
        final boolean isDeterminate                  = isModelBusy && getBusyModel().isDeterminate();
        final boolean useDelayedBusyState            = getMillisToDecideToPopup() > 0;
        final boolean useDeterminateDelayedBusyState = useDelayedBusyState && getMillisToPopup() > 0;
        final boolean monitorRequired                = isModelBusy && isDeterminate && (isRemainingTimeVisible() || useDeterminateDelayedBusyState );

        if( isModelBusy ) {
            if( monitorStartTime <= 0 ) {
                monitorStartTime = System.currentTimeMillis();
            }
            if( monitor == null && monitorRequired ) {
                monitor = new RemainingTimeMonitor( getBusyModel() );
            }
            if( jXGlassPane.isVisible() ) {
                // if the component is already busy, we will let it busy until the end
                return true;
            }
            if( useDelayedBusyState ) {
                long currentTime = System.currentTimeMillis();
                if( currentTime - monitorStartTime >= getMillisToDecideToPopup() ) {
                    /** we must take a decision
                     */
                    if( isDeterminate && useDeterminateDelayedBusyState ) {
                        long remainingTime = monitor.getRemainingTime();
                        return remainingTime < 0 || remainingTime > getMillisToPopup();
                    }
                    return true; // Issue 12 : An undeterminate model can use delayed busy state if getMillisToDecideToPopup > 0
                }
                else {
                    return false; // delayed
                }
            }
        }
        else {
            if( monitor != null ) {
                monitor.dispose();
                monitor = null;
            }
            monitorStartTime = -1L;
        }
        return isModelBusy;
    }

    /** Indicate if this layer should be placed in a locked state.
     *  This default implementation return <code>true</code> if the model is "busy"
     *  OR the background animation is not ended.
     *  <p>
     *  Whenever the component is not yet busy because we wait some times to decide if
     *  progression would popup, the component will be locked instantly (when model is busy) anyway to prevent any access
     *  during the job.
     */
    @Override
    protected boolean shouldLock() {
        return isModelBusy() || isBackgroundPainterDirty( isComponentBusy() );
    }
    
    /** Get the painter ready for render over the specified component.
     *  If the internal painter is not compatible with the specified component, 
     *  then another one painter will be created
     */
    private synchronized Painter getBackGroundPainter() {
        return this.painter;
    }
    
    /** Overridable method that neeed to create a painter with a specified alpha level.
     *  <code>BasicBusyLayerUI</code> invoke this method each time requested
     *  for paint the shadowing animation. 
     *  @param alpha The alpha value (0 ~ 255) requested for the painter
     *  @return painter the new painter with the correct alpha value
     */
    protected Painter createBackgroundPainter( final int alpha ) {
        return new MattePainter( 
                new Color( this.veilColor.getRed() ,
                           this.veilColor.getGreen() ,
                           this.veilColor.getBlue() ,
                           alpha ) 
               );
    }
    
    /** Manage the background shading by starting the dedicated timer if needed.
     *  This method can only start the timer, never it stop it.
     *  <p>
     *  For start a timer, the background painter must be dirty (shading not completed)
     *  and the timer must not already running.
     *  <p>
     *  If no shading is requested (shadeDelayTotal <= 0 ) then the background is
     *  updated directly by this method without using any timer)
     */
    private synchronized void manageBackgroundVeil(boolean isBusy) {
        if( ! this.isBackgroundPainterDirty(isBusy) ) return;
        
        if( this.shadeDelayTotal <= 0 ) {
            /** Do it directly without using a timer (because no animation is needed)
             */
            this.updateBackgroundPainter(isBusy);
        }
    }

    /** Create the Timer responsible to animate this layerUI
     */
    private synchronized Timer createTimer() {
        final ActionListener actionListener = new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                synchronized( BasicBusyLayerUI.this ) {
                    boolean isBusy = isComponentBusy();
                    
                    if( updateBackgroundPainter( isBusy ) ) {
                        updateUI();
                    }
                    else {
                        if( !repainted.get() ) {
                            updateUI();
                            repainted.set(false); // the timer is on the EDT, the updateUI is really done
                        }
                    }
                    if( ! isModelBusy() && ! isBackgroundPainterDirty( isBusy ) ) {
                        ((Timer)e.getSource()).stop();
                    }
                }
            }
        };
        return new Timer( REFRESH_DELAI , actionListener );
    }
    
    /** Indicate if the background painter is dirty.
     *  This method consider the painter as dirty along the shading is not completed.
     *  If no veil is request by this UI, this method return <code>false</code>
     *  <p>
     *  The shading is considered as completed when
     *    - the painter is opaque and busy  (opaque is relative from the veilAlpha)
     *    - the painter is translucent and not busy 
     *  If it's the case, a new painter must be created and this layer UI should be repainted with it
     */
    private synchronized boolean isBackgroundPainterDirty(boolean isBusy) {
        if( this.veilColor == null || this.veilAlpha == 0f ) return false;
        
        if( isBusy  && this.alpha < 255 ) return true;
        if( !isBusy && this.alpha > 0 )   return true;
        return false;
    }

    /** Update the painter for paint the next step of the shading.
     *  <p>
     *  This method request an updateUI() if a new painter is created.
     *  The method update the alpha of the white veil depending <code>shadeDelayTotal</code> delay
     *  and <code>shadeDelayInterval</code> delay
     *
     *  @return <code>true</code> when this method request an {@link #updateUI()} call for refresh ui state
     */
    private synchronized boolean updateBackgroundPainter(boolean isBusy) {
        final Painter oldPainter = this.painter;
        
        if( isBusy && ( this.alpha < 255 || this.painter == null ) ) {
            if( backgroundVeilStartTime <= 0 ) backgroundVeilStartTime = System.currentTimeMillis();
            long   delay     = System.currentTimeMillis() - backgroundVeilStartTime;
            double veilRatio = (double)delay / (double)this.shadeDelayTotal;
            
            if( this.shadeDelayTotal <= 0 ) {
                this.alpha = 255;
                backgroundVeilStartTime = 0L;
            }
            else {
                this.alpha = (int)(255 * veilRatio);
                if( this.alpha >= 255 ) {
                    this.alpha = 255;
                    backgroundVeilStartTime = 0L;
                }
            }
            this.painter = createBackgroundPainter( (int)(this.alpha * this.veilAlpha) );
        }
        else if( !isBusy && ( this.alpha > 0 || this.painter != null ) ) {
            if( backgroundVeilStartTime >= 0 ) backgroundVeilStartTime = -System.currentTimeMillis();
            long   delay     = System.currentTimeMillis() + backgroundVeilStartTime;
            double veilRatio = (double)delay / (double)this.shadeDelayTotal;

            if( this.shadeDelayTotal <= 0 ) {
                this.alpha = 0;
                backgroundVeilStartTime = 0L;
            }
            else {
                this.alpha = 255 - (int)(255 * veilRatio);
            }
            
            if( this.alpha > 0 )
                this.painter = createBackgroundPainter( (int)(this.alpha * this.veilAlpha) );
            else {
                this.alpha = 0;
                this.painter = null;
                backgroundVeilStartTime = 0L;
            }
        }
        else {
            backgroundVeilStartTime = 0L;
        }
        return oldPainter != this.painter;
    }

    /** Observer implementation that allow the layer UI to listen any update from
     *  the busy icon and perform an updateUI
     */
    private class AnimationObserver implements Observer {

        public void update(Observable o, Object arg) {
            /** Observable from BusyIcon perform an update only when it is significant
             *  So let it to refresh our layer.
             */
            BasicBusyLayerUI.this.updateUI();
        }
    }
}