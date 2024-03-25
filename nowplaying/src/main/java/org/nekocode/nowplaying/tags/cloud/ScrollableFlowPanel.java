/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.tags.cloud;

import javax.swing.*;
import java.awt.*;

/**
 * http://forum.java.sun.com/thread.jspa?forumID=57&threadID=701797&start=2
 *
 * This allows the contents to wrap around on the inside of the JPanel
 */
public class ScrollableFlowPanel extends JPanel implements Scrollable {
	@Override
	public void setBounds( int x, int y, int width, int height ) {
		super.setBounds( x, y, getParent().getWidth(), height );
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension( getWidth(), getPreferredHeight() );
	}

	public Dimension getPreferredScrollableViewportSize() {
		return super.getPreferredSize();
	}

	public int getScrollableUnitIncrement( Rectangle visibleRect, int orientation, int direction ) {
		int hundredth = ( orientation ==  SwingConstants.VERTICAL
				? getParent().getHeight() : getParent().getWidth() ) / 100;
		return ( hundredth == 0 ? 1 : hundredth );
	}

	public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ) {
		return orientation == SwingConstants.VERTICAL ? getParent().getHeight() : getParent().getWidth();
	}

	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	private int getPreferredHeight() {
		int rv = 0;
		for ( int k = 0, count = getComponentCount(); k < count; k++ ) {
			Component comp = getComponent( k );
			Rectangle r = comp.getBounds();
			int height = r.y + r.height;
			if ( height > rv )
				rv = height;
		}
		rv += ( (FlowLayout) getLayout() ).getVgap();
		return rv;
	}
}