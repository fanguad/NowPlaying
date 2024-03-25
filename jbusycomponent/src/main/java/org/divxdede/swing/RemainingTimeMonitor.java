/*
 * Copyright (c) 2010 ANDRE Sébastien (divxdede).  All rights reserved.
 * RemainingTimeMonitor.java is a part of this JBusyComponent library
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
package org.divxdede.swing;

import org.divxdede.collection.CyclicBuffer;
import org.divxdede.commons.Disposable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.concurrent.TimeUnit;

/**
 * Tools class that compute remaining time of a long duration task.<br>
 * The task progression is represented by the common interface {@link BoundedRangeModel}.
 * <p>
 * <code>RemainingTimeMonitor</code> store few past samples of the advance progression's speed
 * and use it for compute the remaining time.
 * <p>
 * This monitor use at least the last <strong>10s</strong> to do estimation but it can use greater samples depending on how much frequently the {@link BoundedRangeModel} fire changes.
 * <p>
 * Exemple:
 * <pre>
 *          // Create a tracker
 *          RemainingTimeMonitor rtp = new RemainingTimeMonitor( myModel );
 *
 *          // Just simply call #getRemainingTime()
 *          long remainingTime = getRemainingTime();
 *          if( remainingTime != -1 ) {
 *              // you have a remaining time, you can re-invoke this method for update the remaining time
 *          }
 * </pre>
 *
 * @author André Sébastien (divxdede)
 * @since 1.2
 */
public class RemainingTimeMonitor implements Disposable {

    private BoundedRangeModel          model = null;

    private static final long          MINIMUM_SAMPLE_DELAY = 1000;
    private static final long          MINIMUM_INITIAL_SAMPLE_DELAY = 100;
    private static final int           SAMPLE_COUNT = 10;

    private       CyclicBuffer<Sample> samples = null;

    private       Sample               currentSample = null;
    private       Sample               lastSampleUsed = null;
    private       long                 lastRemainingTimeResult = -1;
    private       long                 whenLastRemainingTimeResult = 0L;

    private ChangeListener listener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            tick();
        }
    };

    /** Create a <code>RemainingTimeMonitor</code> for the specified {@link BoundedRangeModel}.<br>
     *  This instance will use at least samples for a total of <strong>30s</strong>.
     *
     * @param model BoundedRangeModel for which compute the remaining time
     */
    public RemainingTimeMonitor(BoundedRangeModel model) {
        this.model = model;
        this.samples = new CyclicBuffer<Sample>(SAMPLE_COUNT);
        this.model.addChangeListener( this.listener );
    }

    /** Return the monitored model by this <code>RemainingTimeMonitor</code>.<br>
     * @return Monitored model
     */
    public BoundedRangeModel getModel() {
        return this.model;
    }

    /** Internal method that manages sample snapshot
     */
    private synchronized void tick() {
        if( currentSample == null ) {
            currentSample = new Sample( getCurrentRatio() );
            return;
        }
        long currentTime = System.currentTimeMillis();
        long delay       = currentTime - currentSample.getStartTime();
        if( ( samples.size() < 5 && delay >= MINIMUM_INITIAL_SAMPLE_DELAY ) || ( delay >= MINIMUM_SAMPLE_DELAY ) ) {
            float ratio = getCurrentRatio();

            /** Close the current bulk
             */
            currentSample.end( ratio );
            samples.add(currentSample);

            /** Start a new one
             */
            currentSample = new Sample( ratio );
        }
        disposeIfCompleted();
    }

    /** Free resources.<br>
     *  After this method call, this tool don't monitor anymore the underlying {@link BoundedRangeModel}
     */
    public synchronized void dispose() {
        if( this.listener != null ) {
            getModel().removeChangeListener( this.listener );
            this.listener = null;
        }
        this.samples.clear();
        this.currentSample = null;
        this.lastSampleUsed = null;
        this.lastRemainingTimeResult = 0; // it's ended
    }

    /** Indicate if {@link #getRemainingTime()} can give a result based on a new estimation.
     *  If this method returns <code>false</code>, it means the {@link #getRemainingTime()} will give a result based on the last estimation.
     *
     *  @return <code>true</code> if {@link #getRemainingTime()} will give a result based on a new estimation. <code>false</code> otherwise.
     *  @since 1.2.2
     */
    public synchronized boolean hasNewerEstimation() {
        return lastSampleUsed != samples.getLast();
    }

    /** Compute the remaining time of the task underlying the {@link BoundedRangeModel}.<br>
     *  This tool monitor and analysys the task advance speed and compute a predicted remaining time.<br>
     *  If it has'nt sufficient informations in order to compute the remaining time and will return <code>-1</code>
     *
     *  @param unit Specificy the time unit to use for return the remaining time (ex: TimeUnit.SECONDS)
     *  @return Remaining time in milliseconds of the task underlying the {@link BoundedRangeModel}
     */
    public long getRemainingTime(TimeUnit unit) {
        return unit.convert( getRemainingTime() , TimeUnit.MILLISECONDS );
    }

    /** Compute the remaining time of the task underlying the {@link BoundedRangeModel}.<br>
     *  This tool monitor and analysys the task advance speed and compute a predicted remaining time.<br>
     *  If it has'nt sufficient informations in order to compute the remaining time it will return <code>-1</code>
     *  In the counterpart, if the monitoring sample can't compute a finite task duration, it will return Long.MAX_VALUE
     *
     *  @return Remaining time in milliseconds of the task underlying the {@link BoundedRangeModel}
     */
    public synchronized long getRemainingTime() {
        if( !hasNewerEstimation() ) {
            if( lastRemainingTimeResult == -1 || lastRemainingTimeResult == Long.MAX_VALUE ) return lastRemainingTimeResult;
            return Math.max( 0L , lastRemainingTimeResult - (System.currentTimeMillis() - whenLastRemainingTimeResult ) );
        }

        if( samples.isEmpty() ) {
            lastRemainingTimeResult = -1;
            whenLastRemainingTimeResult = System.currentTimeMillis();
            lastSampleUsed = null;
            return -1L;
        }

        if( disposeIfCompleted() ) {
            return 0L;
        }
        
        float currentRatio = getRatio( getModel() );
        float advance      = 0f;
        long  time         = 0L;

        for(int i = 0 ; i < samples.size() ; i++ ) {
            lastSampleUsed = samples.get(i);

            advance += lastSampleUsed.getAdvance();
            time    += lastSampleUsed.getDuration();
        }

        float remainingRatio = 1.0f - currentRatio;

        if( advance < 0.0001f )  {
            this.lastRemainingTimeResult = Long.MAX_VALUE;
        }
        else {
            this.lastRemainingTimeResult = (long)( (1f / advance) * (float)time * remainingRatio );
        }
        this.whenLastRemainingTimeResult = System.currentTimeMillis();

        return this.lastRemainingTimeResult;
    }

    /** Return the current advance ratio of the specified {@link BoundedRangeModel}.
     *  This advance is given as a ratio [0 ~ 1] where 0 = 0% and 1 == 100%
     *
     *  @param model BoundedRangeModel for which we want to determine the current advance ratio
     *  @return Curent advance of the specidied{@link BoundedRangeModel}.
     *  @see #getSignificantRatioOffset()
     */
    public static float getRatio(BoundedRangeModel brm) {
        if( brm != null ) {
            int   length    = brm.getMaximum() - brm.getMinimum();
            int   value     = brm.getValue() + brm.getExtent();
            return (float)value / (float)length;
        }
        else return 0f;
    }

    /** Return the current advance as a ratio [0 ~ 1]
     */
    private float getCurrentRatio() {
        return getRatio( getModel() );
    }

    /** Dispose this monitor if the BoundedRangeModel is complete
     *  @return true if this monitor was disposed
     */
    private boolean disposeIfCompleted() {
        if( getModel().getValue() + getModel().getExtent() >= getModel().getMaximum() ) {
            dispose();
            return true;
        }
        return false;
    }

    /** Store the advance progression of the underlying task for a given duration.
     *  Some most recents samples are holded for estimate the remaining time by extrapolation of the total amount of advance by the duration it took.
     */
    private static class Sample {

        private long  duration;
        private float advance;

        private final long  startTime;
        private final float startRatio;
        private long endTime;

        public Sample(float ratio) {
            startTime  = System.currentTimeMillis();
            startRatio = ratio;
        }

        public void end(float ratio) {
            endTime = System.currentTimeMillis();
            duration = endTime - startTime;
            advance = ratio - startRatio;
        }

        public long getDuration() {
            return this.duration;
        }

        public float getAdvance() {
            return this.advance;
        }

        public long getStartTime() {
            return this.startTime;
        }
    }
}