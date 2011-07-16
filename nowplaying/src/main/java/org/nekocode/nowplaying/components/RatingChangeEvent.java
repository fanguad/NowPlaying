/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components;

import javax.swing.event.ChangeEvent;

/**
 * Event that is fired when the user manually changes the rating via the GUI.
 *
 * @author fanguad@nekocode.org
 */
@SuppressWarnings("serial")
public class RatingChangeEvent extends ChangeEvent
{
   private int newRating;

   public RatingChangeEvent(Object source, int newRating)
   {
      super(source);
      this.newRating = newRating;
   }

   public int getNewRating() {
      return newRating;
   }
}
