/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.components;

import javax.swing.event.ChangeEvent;

/**
 * Event that is fired when the user manually changes the rating via the GUI.
 *
 * @author dan.clark@nekocode.org
 */
public class RatingChangeEvent extends ChangeEvent
{
   private final int newRating;

   public RatingChangeEvent(Object source, int newRating)
   {
      super(source);
      this.newRating = newRating;
   }

   public int getNewRating() {
      return newRating;
   }
}
