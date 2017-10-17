/*
 *
 * Copyright (c) 2010 ANDRE Sébastien (divxdede).  All rights reserved.
 * BusyIcon.java is a part of this JBusyComponent library
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
package org.divxdede.swing.busy;

import org.divxdede.swing.busy.ui.BusyLayerUI;

import javax.swing.*;
import java.util.Observable;

/**
 * <code>BusyIcon</code> are simple icons with <code>Busy</code> renderable capabilities.
 * <p>
 * Such icons draw a animations related to the current state of the underlying {@link BoundedRangeModel}.<br>
 * A <code>BusyIcon</code> can be bound to a basic <code>BoundedRangeModel</code> or to a more featured {@link BusyModel}.
 * <p>
 * When a <code>BusyIcon</code> is bound to a {@link BusyModel}, this icon can render undeterminable state or idle state (not busy).<br>
 * Otherwise, if an icon is bound to a simple {@link BoundedRangeModel}, this icon will be always considered in a determinate state.
 * <p>
 * This interface don't provide any contract on how a <code>BusyIcon</code> should fire an event to the graphical interface when it
 * need to be repainted (when the <code>model</code> or <code>state</Code> change).<br>
 * <strong>But we strong recommand</strong> to support the {@link Observable} class for the best integration inside {@link BusyLayerUI}.
 *
 * @author André Sébastien (divxdede)
 * @since 1.1
 */
public interface BusyIcon extends Icon {

    /** Define the {@link BoundedRangeModel} used for render this icon progression.
     *  <p>
     *  If the BoundedRangeModel is a {@link BusyModel}, this icon will use
     *  attributes like {@link BusyModel#isBusy()} and {@link BusyModel#isDeterminate()} for render this icon.
     *
     *  @param model BoundedRangeModel to bound to this icon (can be null)
     */
    public void setModel( BoundedRangeModel model );

    /** Return the current {@link BoundedRangeModel} used for render this icon progression.
     *  <p>
     *  If the BoundedRangeModel is a {@link BusyModel}, this icon will use
     *  attributes like {@link BusyModel#isBusy()} and {@link BusyModel#isDeterminate()} for render this icon.
     *
     * @return The underlying BoundedRangeModel (may be null)
     */
    public BoundedRangeModel getModel();

    /**
     * Returns <code>true</code> if this icon currently render a determinate state<br>
     * Some implementation of BusyIcon don't support to render a determinate state whatever the current state of the underlying Model.<br>
     * In theses cases, this method should indicate that this icon render an undeterminate state even if the BusyModel is on an determinate state.
     * <p>
     * If this icon can render all states (undeterminate,determinate), this method should be in sync with the underlying model.<br>
     * In the other case, this asynchronous state can be used by {@link BusyLayerUI} to determine if they need to add some externals informations
     * (exemple a Progress Bar) in addition of the icon for track the progression advance of the BusyModel.
     *
     * @return true if this icon currently render a determinate state
     */
    public boolean isDeterminate();
}