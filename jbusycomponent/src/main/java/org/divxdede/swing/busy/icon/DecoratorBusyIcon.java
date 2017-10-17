/*
 * Copyright (c) 2010 ANDRE Sébastien (divxdede).  All rights reserved.
 * DecoratorBusyIcon.java is a part of this JBusyComponent library
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

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Observable;
import java.util.Observer;

/**
 * An implementation of the {@link BusyIcon} interface to serve as a
 * basis for implementing Busy Icons based on another icon extended with busy capabilities.
 * <p>
 * The method {@link #setDecoratedIcon(javax.swing.Icon)} can be used for change or set the decorated icon.<br>
 * This icon can have a bigger size than the decorated by fixing an {@link Insets} by subclasses.<br>
 * When an <code>Insets</code> is set, the decorated icon are centered regarding the Insets.
 * <p>
 * The decorated icon can be an animated icon (like a .gif), in this case, each time a new frame is available,
 * a repaint event will be fired for update this icon.<br>
 * For be able to catch theses new frame events, the decorated icon must be an {@link ImageIcon} or extends {@link Observable} class.
 * <p>
 * This implementation don't implements {@link #paintDeterminate(java.awt.Component, java.awt.Graphics, int, int, float)} or
 * {@link #paintUndeterminate(java.awt.Component, java.awt.Graphics, int, int, int) } but offer a {@link #paintDecoratedIcon(java.awt.Component, java.awt.Graphics, int, int) }
 * that can be used by subclasses for drawing the decorated icon.
 * <p>
 * The {@link #paintIdle(java.awt.Component, java.awt.Graphics, int, int) } is implemented by painting only the decorated icon as is (respecting insets).<br>
 * You can override this method if you need to provide a more sophisticate render process for the idle state.
 *
 * @author André Sébastien (divxdede)
 * @since 1.1
 */
public abstract class DecoratorBusyIcon extends AbstractBusyIcon {

    private Icon           icon          = null;
    private BufferedImage  iconFrame     = null;
    private FrameObserver  frameObserver = new FrameObserver();
    private Insets         insets        = null;

    /** Default constructor
     *  @param icon Decorated icon to set
     */
    public DecoratorBusyIcon(Icon icon) {
        this(icon,null);
    }

    /** Constructor with specified insets
     *  @param icon Decorated icon to set
     *  @param insets Insets to use
     */
    public DecoratorBusyIcon( Icon icon , Insets insets ) {
       setDecoratedIcon(icon);
       setInsets(insets);
    }

    /** Retrieve the current decorated icon of this decorator.<br>
     *  A decorator busy icon extends a decorated icon with some busy-ui capabilities.
     *
     *  @return Current decorated icon (may be <code>null</code>)
     */
    public Icon getDecoratedIcon() {
        return this.icon;
    }

    /** Define the new decorated icon.
     *  <p>
     *  A decorator busy icon extends a decorated icon with some busy capabilities.<br>
     *  This method allow to change dynamically the base of this icon by setting a new decorated icon.
     *  <p>
     *  The decorated icon will be centered on this icon accordingly to the current {@link Insets}
     *
     * @param icon New decorated icon of this decorator (may be <code>null</code>)
     */
    public synchronized void setDecoratedIcon( Icon icon ) {
        if( this.icon == icon ) return;

        if( this.icon instanceof ImageIcon ) {
            ((ImageIcon)this.icon).setImageObserver(null);
        }
        if( this.icon instanceof Observable ) {
            ((Observable)this.icon).deleteObserver(this.frameObserver);
        }

        this.icon = icon;
        this.iconFrame = null;

        if( this.icon != null ) {
             GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
             this.iconFrame = gc.createCompatibleImage( this.icon.getIconWidth() , this.icon.getIconHeight() , Transparency.TRANSLUCENT );

             if( this.icon instanceof ImageIcon ) {
                ((ImageIcon)this.icon).setImageObserver( this.frameObserver );
             }
             if( this.icon instanceof Observable ) {
                 ((Observable)this.icon).addObserver( this.frameObserver );
             }
        }
        doIconFrameUpdate(null);
    }

    /** Retrieve current Insets of this <code>DecoratorBusyIcon</code>.<br>
     *  The decorated icon is centered on this icon accordingly to this insets.
     *
     *  @return Insets of this icon
     */
    protected Insets getInsets() {
        return this.insets;
    }

    /** Change current Insets of this <code>DecoratorBusyIcon</code>.
     *  <p>
     * @param insets
     */
    protected void setInsets(Insets insets) {
        this.insets = insets;
        repaint(true);
    }

    /** By default, a decorator icon paint an <code>idle</code> state by painting the decorated icon without anything else.<br>
     *  If you want a more sophisticated idle icon, you should override this method.
     */
    @Override
    protected void paintIdle(Component c, Graphics g, int x, int y) {
        paintDecoratedIcon(c, g, x, y);
    }

    /** Paint the decorated icon using <code>Insets</code> for place it inside this <code>DecoratorBusyIcon</code>
     *  <p>
     *  This method should be used by subclasses when they implements:
     *  <ul>
     *   <li>{@link #paintDeterminate(java.awt.Component, java.awt.Graphics, int, int, float)}</li>
     *   <mo>{@link #paintUndeterminate(java.awt.Component, java.awt.Graphics, int, int, int)}</li>
     *  </ul>
     */
    protected synchronized void paintDecoratedIcon(Component c , Graphics g , int x , int y ) {
        if( this.iconFrame != null ) {
            Insets margins = getInsets();
            if( margins != null ) {
                x = margins.left;
                y = margins.top;
            }
            g.drawImage( this.iconFrame , x  , y , null );
        }
    }

    /** Return the width of this <code>DecoratorBusyIcon</code>.<br>
     *  Width = <code>decorated icon width + insets.left + insets.right</code>
     * @return Width of this icon
     */
    public int getIconWidth() {
        Icon   icon    = getDecoratedIcon();
        Insets margins = getInsets();

        int  result = icon == null ? 0 : icon.getIconWidth();
        if( margins != null ) {
            result += margins.left + margins.right;
        }
        return result;
    }

    /** Return the height of this <code>DecoratorBusyIcon</code>.<br>
     *  Width = <code>decorated icon height + insets.top + insets.bottom</code>
     *  @return Width of this icon
     */
    public int getIconHeight() {
        Icon   icon    = getDecoratedIcon();
        Insets margins = getInsets();

        int  result = icon == null ? 0 : icon.getIconHeight();
        if( margins != null ) {
            result += margins.top + margins.bottom;
        }
        return result;
    }

    /** A new frame is available from the decorated icon.<br>
     *  We must update our reference-frame painted by the {@link #paintDecoratedIcon(java.awt.Component, java.awt.Graphics, int, int) }
     *
     * @param image Image containing the new frame. If this image is <code>null</code>, we take directly the icon for refresh our buffered image
     */
    private synchronized void doIconFrameUpdate(Image image) {
        if( iconFrame != null ) {
            Graphics2D g = (Graphics2D)iconFrame.createGraphics();
            try {
                g.setComposite(AlphaComposite.Clear);
                g.fillRect( 0 , 0 , this.icon.getIconWidth() , this.icon.getIconHeight() );
                g.setPaintMode();

                if( image != null ) {
                    g.drawImage( image , 0 , 0 , null );
                }
                else {
                    this.icon.paintIcon( null , g , 0 , 0 );
                }
            }
            finally {
                g.dispose();
            }
        }
        repaint(true);
    }
    
    /** Listener responsible to fire repaint event each time
     *  a new frame should be painted from the decorated icon
     */
    private class FrameObserver implements ImageObserver , Observer {

        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
            if( (infoflags & (FRAMEBITS|ALLBITS) ) != 0) {
                doIconFrameUpdate(img);
            }
            return (infoflags & (ALLBITS|ABORT)) == 0;
        }

        public void update(Observable o, Object arg) {
            doIconFrameUpdate(null);
        }
    }
}