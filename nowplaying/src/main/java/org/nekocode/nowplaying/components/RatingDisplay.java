/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.components;

import org.apache.log4j.Logger;
import org.nekocode.nowplaying.resources.images.Icons;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * JComponent that displays rating, and allows for updates to rating.
 *
 * @author fanguad@nekocode.org
 */
@SuppressWarnings("serial")
public class RatingDisplay extends JComponent
{
   private static final Logger log = Logger.getLogger(RatingDisplay.class);

   private static final Image heart_full;
   private static final Image heart_empty;
   private static final Dimension size;
   private static final int one_width;

   static {
      ImageIcon _heart_full = new ImageIcon(Icons.class.getResource("emblem-favorite-filled.png"));
      ImageIcon _heart_empty = new ImageIcon(Icons.class.getResource("emblem-favorite-unfilled.png"));
      heart_full = _heart_full.getImage();
      heart_empty = _heart_empty.getImage();
      one_width = _heart_full.getIconWidth();
      size = new Dimension(one_width * 10, _heart_full.getIconHeight());
   }

   private int value;
   private int xoffset; // offset from 0 horizontal # that the image is drawn

   private Set<ChangeListener> listeners = new HashSet<ChangeListener>();

   public RatingDisplay() {
      // by default, all hearts are empty
      value = 0;

      addMouseListener(new MouseAdapter() {

         @Override
         public void mouseClicked(MouseEvent e)
         {
        	 changeValue(getRatingFromLocation(e.getPoint()));
         }});

      Action setRating = new AbstractAction() {
    	  /* (non-Javadoc)
    	   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    	   */
    	  @Override
    	  public void actionPerformed(ActionEvent e) {
    		  int value = Integer.parseInt(e.getActionCommand());
    		  if (value == 0) {
    			  value = 100;
    		  } else {
    			  value *= 10;
    		  }
    		  changeValue(value);
    	  }};
      for (char i = '0'; i <= '9'; i++) {
          getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(i), "setRating");
      }
      getActionMap().put("setRating", setRating);

      Action changeRating = new AbstractAction() {
    	  /* (non-Javadoc)
    	   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    	   */
    	  @Override
    	  public void actionPerformed(ActionEvent e) {
    		  int value;
    		  if (e.getActionCommand().equals("+")) {
    			  value = RatingDisplay.this.getValue() + 10;
    		  } else {
    			  value = RatingDisplay.this.getValue() - 10;
    		  }
    		  value = Math.max(value, 0);
    		  value = Math.min(value, 100);
    		  changeValue(value);
    	  }};
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('-'), "changeRating");
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('+'), "changeRating");
      getActionMap().put("changeRating", changeRating);

   }

   /**
    * Gets the rating value based on the specified point inside the component
    * @param p
    * @return
    */
   public int getRatingFromLocation(Point p) {
      int hearts = (p.x - xoffset) / one_width + 1; // +1 since the first heart is 1, not 0
//      int hearts = (p.x) / one_width;
//      log.debug("user clicked on point " + p);
//      log.debug(String.format("rating = (%d - %d) / %d", p.x, xoffset, one_width));
//      log.debug("user clicked on rating value " + hearts);
      hearts = Math.max(0, hearts); // clip it to 0-10
      hearts = Math.min(10, hearts);
      log.debug("Setting rating to " + (hearts * 10));
      return hearts * 10;
   }

   protected void fireChangeEvent() {
      ChangeEvent e = new ChangeEvent(this);

      for (ChangeListener listener : listeners) {
         listener.stateChanged(e);
      }
   }

   public void addChangeListener(ChangeListener listener)
   {
      listeners.add(listener);
   }

   public int getValue()
   {
      return value;
   }

   public void setValue(int i)
   {
      value = i;
      repaint();
   }

   @Override
   public Dimension getPreferredSize() {
      return size;
   }

   /* (non-Javadoc)
    * @see javax.swing.JComponent#getMinimumSize()
    */
   @Override
   public Dimension getMinimumSize() {
	   return size;
   }

   @Override
   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      // if this component is bigger than the minimum size,
       // figure out where to start so that it's centered
      xoffset = 0;
      if (getSize().width > size.width) {
         xoffset = (getSize().width - size.width) / 2;
      }
//      log.debug("xoffset set to " + xoffset);

      int number_hearts = value / 10;

      int i;
      for (i = 0; i < number_hearts; i++) {
         g.drawImage(heart_full, xoffset + one_width * i, 0, null);
      }
      for (; i < 10; i++) {
         g.drawImage(heart_empty, xoffset + one_width * i, 0, null);
      }
   }

   /**
    * Sets the value, and fires an event to any listeners.
    *
    * @param value
    */
   private void changeValue(int value) {
	   setValue(value);
	   fireChangeEvent();
   }
}
