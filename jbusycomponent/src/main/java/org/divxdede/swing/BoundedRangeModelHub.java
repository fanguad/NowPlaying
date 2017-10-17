/*
 * Copyright (c) 2010 ANDRE Sébastien (divxdede).  All rights reserved.
 * BoundedRangeModelHub.java is a part of this JBusyComponent library
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

import org.divxdede.collection.ArrayIterator;
import org.divxdede.commons.Disposable;
import org.divxdede.commons.Sizable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A <code>BoundedRangeModelHub</code> can split a <code>BoundedRangeModel</code> (call <code>master model</code>) in sub-models.
 * <p>
 * Each sub-models represent a progression part of the master model.<br>
 * Each sub-models have a weight that describe how combine theses sub-models for computing the master's value.
 * <p>
 * Two sub-models with the same <code>weight</code> represent the same range progression inside the master model.<br>
 * A sub-model which has a double weight than another sub-model represent a double range progression inside the master model.<br>
 * This weighting must be a positive weight and just serve to compute a factor for each sub-models regarding theses weight.
 * <p>
 * Exemple:
 * <pre>
 *            BoundedRangeModelHub hub   = new BoundedRangeModelHub();
 *            
 *            BoundedRangeModel    taskA = hub.createFragment(40);  // will represent 20% of the master model
 *            BoundedRangeModel    taskB = hub.createFragment(160); // will represent 80% of the master model
 *            
 *            hub.setMasterBoundedRangeModel(monModelePrincipal);
 *            
 *            taskA.setMaximum(1000);
 *            for(int i = 0 ; i < 1000 ; i++ ) {
 *                taskA.setValue(i);
 *                // taskA job
 *            }
 *            
 *            // At this time, the master model is at 20% because taskA is completed
 *            
 *            taskB.setMaximum(10);
 *            for(int i = 0 ; i < 10 ; i++ ) {
 *            	   taskB.setValue(i);
 *                 // taskB job
 *            }
 *            
 *            // At this time, the master model is at 100% becase taskA and taskB are completed
 *            
 *            // dispose hub resources (listener and so one)
 *            hub.dispose();
 * </pre>
 * <p>
 * Some statics methods allow to split in a one call a model in sub-model:
 * <ul>
 *  <li>{@link #split(BoundedRangeModel, int)} an uniform split (each sub-models have the same weight)</li>
 *  <li>{@link #split(BoundedRangeModel, float...)} a non uniform split (each weight are specified by this method)</li>
 * </ul>
 * 
 * @author André Sébastien - INFASS Systèmes (http://www.infass.com)
 * @since 1.1
 */
public class BoundedRangeModelHub implements ChangeListener, Iterable<BoundedRangeModel>, Sizable, Disposable {

    private List<WeightBoundedRangeModel> subs = new ArrayList<WeightBoundedRangeModel>();
    private BoundedRangeModel master = null;
    private float totalWeight = 0f;
    private boolean changing = false;

    /** Create an empty <code>BoundedRangeModelHub</code> without master model.
     *  <p>
     *  The master model must be set with the {@link #setMasterBoundedRangeModel(javax.swing.BoundedRangeModel)} method.<br>
     *  Sub-models must be created or added with {@link #createFragment(float)} or {@link #addFragment(javax.swing.BoundedRangeModel, float)}  methods.
     *
     *  @see #setMasterBoundedRangeModel(BoundedRangeModel)
     *  @see #createFragment(float)
     */
    public BoundedRangeModelHub() {
        this(null);
    }

    /** Create an empty <code>BoundedRangeModelHub</code> with the specified master model.
     *  <p>
     *  Sub-models must be created or added with {@link #createFragment(float)} or {@link #addFragment(javax.swing.BoundedRangeModel, float)} methods.
     *
     *  @see #setMasterBoundedRangeModel(BoundedRangeModel)
     *  @see #createFragment(float)
     */
    public BoundedRangeModelHub(BoundedRangeModel master) {
        setMasterBoundedRangeModel(master);
    }

    /** Define the master model to compute from changes mades on sub-models.
     *  Any changes that applies from sub-models are forwarded to the master model and the hub re-compute it's value.
     *  <p>
     *  Each sub-models can be created or added by {@link #createFragment(float)} or {@link #addFragment(javax.swing.BoundedRangeModel, float)} methods.<br>
     *  Each sub-models have a weight that describe how combine theses sub-models for computing the master's value.
     *  <p>
     *  Two sub-models with the same <code>weight</code> represent the same range progression inside the master model.<br>
     *  A sub-model which has a double weight than another sub-model represent a double range progression inside the master model.<br>
     *  This weighting must be a positive weight and just serve to compute a factor for each sub-models regarding theses weight.
     *
     *  @param model New master model to bound to this hub. (can be <code>null</code>)
     *  @see #createFragment(float)
     */
    public synchronized void setMasterBoundedRangeModel(BoundedRangeModel model) {
        BoundedRangeModel oldMaster = this.master;
        if (oldMaster != null) {
            oldMaster.removeChangeListener(this);
        }

        this.master = model;
        if (this.master == null) {
            for (WeightBoundedRangeModel sub : subs) {
                sub.getModel().removeChangeListener(this);
            }
        } else {
            this.master.addChangeListener(this);
            if (oldMaster == null) {
                for (WeightBoundedRangeModel sub : subs) {
                    sub.getModel().addChangeListener(this);
                }
            }
        }
        if (oldMaster != this.master) {
            stateChanged(null);
        }
    }

    /** Retrieve the master model managed by this hub.<br>
     *  Any changes that applies from sub-models are forwarded to this model and this hub update it's value.
     *
     *  @return Master model managed by this hub (may be null)
     *  @see #createFragment(float)
     */
    public synchronized BoundedRangeModel getMasterBoundedRangeModel() {
        return this.master;
    }

    /** Create a sub-model with a specified <strong>weight</strong>.<br>
     *  Any changes that applies from this created sub-model are forwarded to the master model for update it's value.
     *  <p>
     *  The new sub-model has a weight that describe how much this fragment take part on the master model.<br>
     *  Two sub-models with the same <code>weight</code> represent the same range progression inside the master model.<br>
     *  A sub-model which has a double weight than another sub-model represent a double range progression inside the master model.<br>
     *  This weighting must be a positive weight and just serve to compute a factor for each sub-models regarding theses weight.
     *
     * @param weight Weight to bound to the newly created sub-model (fragment)
     * @return The newly created sub-model.
     * @throws IllegalArgumentException if <code>weight</code> is negative.
     */
    public synchronized BoundedRangeModel createFragment(float weight) {
        return addFragment(new DefaultBoundedRangeModel(), weight);
    }

    /** Add a {@link BoundedRangeModel} as a sub-model with a specified <strong>weight</strong>.<br>
     *  Any changes that applies from this added sub-model are forwarded to the master model for update it's value.
     *  <p>
     *  The added sub-model has a weight that describe how much this fragment take part on the master model.<br>
     *  Two sub-models with the same <code>weight</code> represent the same range progression inside the master model.<br>
     *  A sub-model which has a double weight than another sub-model represent a double range progression inside the master model.<br>
     *  This weighting has no particular constraint, it juste help to compute a factor for each sub-models regarding theses weight.
     *
     * @param fragment Sub-model to add to this hub
     * @param weight Weight to bound to the newly created sub-model (fragment)
     * @return Return the added sub-model.
     * @throws NullPointerException if fragment is <code>null</code>
     * @throws IllegalArgumentException if <code>weight</code> is negative.
     */
    public synchronized BoundedRangeModel addFragment(BoundedRangeModel fragment, float weight) {
        if (fragment == null) {
            throw new NullPointerException();
        }
        if( weight < 0 ) throw new IllegalArgumentException("weight must be positive");

        WeightBoundedRangeModel splitted = new WeightBoundedRangeModel(fragment, weight);
        if (master != null) {
            splitted.getModel().addChangeListener(this);
        }
        subs.add(splitted);

        this.totalWeight += weight;

        stateChanged(null);
        return splitted.getModel();
    }

    /** Remove a sub-model specified by it's index ordinal from this hub.<br>
     *  Indexes are defined by the creation/insertion order. You can use {@link #indexOf(javax.swing.BoundedRangeModel)} for retrieve an index's sub-model.
     *  <p>
     *
     * @param index Index of the sub-model to remove from this hub
     * @return The removed sub-model, <code>null</code> if no sub-model was removed.
     * @throws IndexOutOfBoundsException If index is out of bound.
     */
    public synchronized BoundedRangeModel removeFragment(int index) {
        WeightBoundedRangeModel splitted = this.subs.remove(index);
        if (splitted != null) {
            splitted.getModel().removeChangeListener(this);
            this.totalWeight = this.totalWeight - splitted.getWeight();
            stateChanged(null);

            return splitted.getModel();
        }
        return null;
    }

    /** Retrieve all sub-models in this hub.<br>
     *  The result array order ensure that sub-models's index on the array are the same that sub-models's index on this hub.
     *
     *  @return Each sub-models managed by this hub for update the master model.
     */
    public synchronized BoundedRangeModel[] getFragments() {
        BoundedRangeModel[] result = new BoundedRangeModel[this.subs.size()];
        for (int i = 0; i < this.subs.size(); i++) {
            result[i] = this.subs.get(i).getModel();
        }
        return result;
    }

    /** Create an iterator over sub-models managed by this hub.<br>
     *  The iteration order ensure that sub-models's index on the array are the same that sub-models's index on this hub.
     *
     *  @return Iterator from all sub-models managed by this hub for update the master model.
     */
    public synchronized Iterator<BoundedRangeModel> iterator() {
        return new ArrayIterator<BoundedRangeModel>(getFragments());
    }

    /** Return the number of sub-models in this hub.
     *  @return The number of sub-models in this hub.
     */
    public synchronized int size() {
        return this.subs.size();
    }

    /** Return sub-model's index in this hub for the specified sub-model.<br>
     *  This index can be used with {@link #getWeight(int)}, {@link #setWeight(int, float)} and much more.
     *  <p>
     *  If the specified model is not a sub-model on this hub, this method return <code>-1</code
     *
     *  @param model Sub-model for which we want it's index
     *  @return Sub-model's index or <code>-1</code> if the specified model is not a sub-model on this hub.
     */
    public synchronized int indexOf(BoundedRangeModel model) {
        for (int i = 0; i < this.subs.size(); i++) {
            if (model == this.subs.get(i).getModel()) {
                return i;
            }
        }
        return -1;
    }

    /** Get a sub-model by it's index inside this hub.
     *
     * @param index Index of the requested sub-model
     * @return Requested sub-model
     * @throws IndexOutOfBoundsException if index is out of bound.
     */
    public synchronized BoundedRangeModel getFragment(int index) {
        return this.subs.get(index).getModel();
    }

    /** Get the current <strong>weight</strong> of a sub-model specified by it's index.
     *  <p>
     *  A sub-model has a weight that describe how much this fragment take part on the master model.<br>
     *  Two sub-models with the same <code>weight</code> represent the same range progression inside the master model.<br>
     *  A sub-model which has a double weight than another sub-model represent a double range progression inside the master model.<br>
     *  This weighting has no particular constraint, it juste help to compute a factor for each sub-models regarding theses weight.
     *
     *  @param index Index of the requested sub-model for which this method will return it's weight.
     *  @return Weight of the specified sub-model.
     *  @throws IndexOutOfBoundsException if index is out of bound.
     *  @see #indexOf(BoundedRangeModel)
     *  @see #setWeight(int, float)
     */
    public synchronized float getWeight(int index) {
        return this.subs.get(index).getWeight();
    }

    /** Define a new <strong>weight</strong> of sub-model specified by it's index.
     *  <p>
     *  A sub-model has a weight that describe how much this fragment take part on the master model.<br>
     *  Two sub-models with the same <code>weight</code> represent the same range progression inside the master model.<br>
     *  A sub-model which has a double weight than another sub-model represent a double range progression inside the master model.<br>
     *  This weighting must be a positive weight and just serve to compute a factor for each sub-models regarding theses weight.
     *
     *  @param index Index of the requested sub-model for which this method will change it's weight.
     *  @param newWeight New weight to bound to the specified sub-model.
     *  @throws IllegalArgumentException if <code>weight</code> is negative.
     *  @see #indexOf(BoundedRangeModel)
     *  @see #getWeight(int)
     */
    public synchronized void setWeight(int index, float newWeight) {
        if( newWeight < 0 ) throw new IllegalArgumentException("Weight must be positive");

        float oldWeight = getWeight(index);
        this.subs.get(index).setWeight(newWeight);
        this.totalWeight = this.totalWeight - oldWeight + newWeight;
        stateChanged(null);
    }

    /** Get the total weight of this hub.<br>
     *  This total is the <strong>sum</strong> of all sub-model's weight.
     *  <p>
     *  This total is used to determiner the range proportion of each sub-models regarding theses weights.
     *
     *  @return Total weight of this hub.
     */
    public synchronized float getTotalWeight() {
        return this.totalWeight;
    }

    /** Free all resources of this hub.<br>
     *  This method is the same than a call to {@link #setMasterBoundedRangeModel(javax.swing.BoundedRangeModel)} with <code>null</code>.<br>
     *  That mean the the master model is removed by this method.
     *  <p>
     *  Sub-models are always present on this hub and can be reused after a new master will be set.<br>
     *  In fact, all registered-listeners from this hub are removed waiting for a new master-model.
     */
    public void dispose() {
        setMasterBoundedRangeModel(null);
    }

    /** Internal method call when a change apply from a sub-model (or the master model).<br>
     *  This method must be public because it's a part from the {@link ChangeListener} interface,<br>
     *  But not should be called directly.
     */
    public synchronized void stateChanged(ChangeEvent e) {
        if (changing) {
            return;
        }
        changing = true;
        try {
            if (getMasterBoundedRangeModel() == null) {
                return;
            }

            int extent = 0;
            for (WeightBoundedRangeModel sub : subs) {
                extent += sub.getExtentPartFor(getTotalWeight(), getMasterBoundedRangeModel());
            }
            getMasterBoundedRangeModel().setValue(extent);
        } finally {
            changing = false;
        }
    }

    /** Split the specified {@link BoundedRangeModel} on sub-models which all will have the same weight (<code>1.0f</code>).<br>
     *  The specified model will become the master model of the resulted {@link BoundedRangeModelHub}.<br>
     *
     * @param toSplit BoundedRangeModel to split.
     * @param length Number of sub-models to create. Each sub-models will have the same weight.
     * @return Hub resulting of this split operation.
     */
    public static BoundedRangeModelHub split(BoundedRangeModel toSplit, int length) {
        float[] weights = new float[length];
        for (int i = 0; i < length; i++) {
            weights[i] = 1.0f;
        }
        return split(toSplit, weights);
    }

    /** Split the specified {@link BoundedRangeModel} on multiple sub-models.
     *  <p>
     *  This method take an array of weight to distribute on sub-models.<br>
     *  The split operation will result in a sub-model's count equally to the length of the weight's array.<br>
     *
     * @param toSplit BoundedRangeModel to split.
     * @param weights Weight's array giving the number of sub-models to create and theses weights to use.
     * @return Hub resulting of this split operation.
     */
    public static BoundedRangeModelHub split(BoundedRangeModel toSplit, Number... weights) {
        float[] fWeight = new float[weights.length];
        for (int i = 0; i < weights.length; i++) {
            fWeight[i] = weights[i].floatValue();
        }
        return split(toSplit, fWeight);
    }

    /** Split the specified {@link BoundedRangeModel} on multiple sub-models.
     *  <p>
     *  This method take an array of weight to distribute on sub-models.<br>
     *  The split operation will result in a sub-model's count equally to the length of the weight's array.<br>
     *
     * @param toSplit BoundedRangeModel to split.
     * @param weights Weight's array giving the number of sub-models to create and theses weights to use.
     * @return Hub resulting of this split operation.
     */
    public static BoundedRangeModelHub split(BoundedRangeModel toSplit, float... weights) {
        if (weights == null) {
            return null;
        }

        BoundedRangeModelHub hub = new BoundedRangeModelHub(toSplit);
        for (int i = 0; i < weights.length; i++) {
            hub.createFragment(weights[i]);
        }
        return hub;
    }

    /** Private implementation, for store a weight to a sub-model
     */
    private static class WeightBoundedRangeModel {

        private BoundedRangeModel model = null;
        private float weight = 0f;

        private WeightBoundedRangeModel(BoundedRangeModel model, float weight) {
            this.model = model;
            this.weight = weight;
        }

        /** Récupere la valeur au sein du BoundedRangeModel spécifié correspondant à ce SplittedBoundedRangeModel
         */
        public int getExtentPartFor(float totalWeight, BoundedRangeModel other) {

            // min <= value <= value+extent <= max
            int length = getModel().getMaximum() - getModel().getMinimum();
            int position = (getModel().getValue() + getModel().getExtent()) - getModel().getMinimum();
            float ratio = (float) position / (float) length;

            int otherLength = other.getMaximum() - other.getMinimum() - other.getExtent();
            float otherRatio = ratio * (this.weight / totalWeight);
            return (int) (otherLength * otherRatio);
        }

        private float getWeight() {
            return this.weight;
        }

        private void setWeight(float newWeight) {
            this.weight = newWeight;
        }

        public BoundedRangeModel getModel() {
            return this.model;
        }
    }
}