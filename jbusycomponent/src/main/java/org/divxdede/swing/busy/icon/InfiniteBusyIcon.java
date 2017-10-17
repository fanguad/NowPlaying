/*
 * Copyright (c) 2010 ANDRE Sébastien (divxdede).  All rights reserved.
 * InfiniteBusyIcon.java is a part of this JBusyComponent library
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

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.painter.BusyPainter;

import javax.swing.*;
import java.awt.*;

/**
 * An infinite icon rendering always an <code>undeterminate</code> state as long as the model is busy (whenever the model's state).
 * <p>
 * This implementation use a {@link BusyPainter} from the swingx library.<br>
 * But this icon can be used as a simple icon without constraint to be embedded into a {@link JXBusyLabel} in order to be animated.<br>
 * You can set the frame rate using the method {@link #setDelay(int)} that give the delay in milliseconds between 2 frames.
 *
 * @author André Sébastien (divxdede)
 * @since 1.1
 */
public class InfiniteBusyIcon extends AbstractBusyIcon {

    private BusyPainter painter   = null;
    private int         frame     = 0;
    private Dimension   dimension = null;
    private int         delay     = -1;

    // force ui-contribution like "JXBusyLabel.delay" property
    static {
       new JXBusyLabel();
    }

    /** Default constructor of an InfiniteIcon.<br>
     *  This icon is set with a dimension of 26x26 and use default BusyPainter
     */
    public InfiniteBusyIcon() {
        this(null,null);
    }

    /** Create an InfiniteBusyIcon using the default BusyPainter with the specified dimension.
     *  @param dim Dimension of this icon
     */
    public InfiniteBusyIcon(Dimension dim) {
        this(null,dim);
    }

    /** Create an InfiniteBusyIcon using the specified BusyPainter with the specified dimension.
     *  @param painter BusyPainter to use
     *  @param dim Dimension of this icon
     */
    public InfiniteBusyIcon(BusyPainter painter,Dimension dim) {
        if( painter == null ) {
            painter = new BusyPainter();
        }
        if( dim == null ) {
            dim = new Dimension( 26 , 26 );
        }
        this.dimension = dim;
        this.setBusyPainter( painter);
    }

    /** In an idle state, this icon paints only the background
     */
    @Override
    protected void paintIdle(Component c, Graphics g, int x, int y) {
        paintBackground(c,g,x,y);
    }

    /** This method paint nothing, determinate state is not supported
     *  by this implementation
     */
    @Override
    protected void paintDeterminate(Component c, Graphics g, int x, int y, float ratio) {
        paintIdle(c,g,x,y);
    }

    /** Paint this icon in an undeterminate state
     */
    @Override
    protected void paintUndeterminate(Component c, Graphics g, int x, int y, int frame) {
        paintBackground(c,g,x,y);

        if( getBusyPainter() != null ) getBusyPainter().setFrame( frame );
        paintInfiniteSpinner(c,g,x,y);
    }

    /** Paint a background.<br>
     *  By default this method do nothing, but it can be overriden by subclasses
     */
    protected void paintBackground(Component c, Graphics g, int x, int y) {
        /* do nothing */
    }

    /** Paint infinite spinner animation using the {@link BusyPainter}
     */
    protected void paintInfiniteSpinner(Component c, Graphics g, int x, int y) {
        BusyPainter painter = getBusyPainter();
        if( painter != null ) {
            Graphics2D g2d = (Graphics2D)g.create();
            try {
                g2d.translate(x,y);
                painter.paint( g2d , null , getIconWidth() , getIconHeight() );
            }
            finally {
                g2d.dispose();
            }
        }
    }

    /** This icon don't support a determinate state. It's why this method return always <code>false</code>.
     *  @return <code>false</code>
     */
    @Override
    public boolean isDeterminate() {
        return false; //
    }

    /** Define the delay (in milliseconds) between 2 points
     *  @param delay delay (in milliseconds) between 2 points
     */
    public void setDelay( int delay ) {
        BusyPainter painter = getBusyPainter();
        this.delay = delay;
        if( painter != null ) {
            setUndeterminateFrameRate( getDelay() , painter.getPoints() );
        }
        else {
            setUndeterminateFrameRate( 0 , 0 ); // stop timer
        }
    }

    /** Return the delay (in milliseconds) between 2 points
     * @return the delay (in milliseconds) between 2 points
     */
    public int getDelay() {
        if( this.delay == -1 ) return UIManager.getInt("JXBusyLabel.delay");
        return this.delay;
    }

    /** Define a new {@link BusyPainter} to use by this icon.<br>
     * @param painter New BusyPainter to use by this icon.
     */
    public void setBusyPainter(BusyPainter painter) {
        this.painter = painter;
        this.frame = 0;
        if( this.painter != null ) {
            setUndeterminateFrameRate( getDelay() , painter.getPoints() );
        }
        else {
            setUndeterminateFrameRate( 0 , 0 ); // stop the timer
        }
        repaint(true);
    }

    /** Retrieve the {@link BusyPainter} used by this icon.
     *  @return The BusyPainter used by this icon
     */
    public BusyPainter getBusyPainter() {
        return this.painter;
    }

    /**
     * Returns the icon's width.
     *
     * @return an int specifying the fixed width of the icon.
     */
    public int getIconWidth() {
        return (int)this.dimension.getWidth();
    }

    /**
     * Returns the icon's height.
     *
     * @return an int specifying the fixed height of the icon.
     */
    public int getIconHeight() {
        return (int)this.dimension.getHeight();
    }

    /** Since this icon render only undeterminate state, this method
     *  has no really interrest. we return arbitrary 0.01f (1%) offset
     */
    @Override
    protected float getSignificantRatioOffset() {
        return 0.01f;
    }
}