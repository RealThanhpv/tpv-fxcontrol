/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package tpv.fxcontrol.behavior;

import com.sun.javafx.scene.control.behavior.CellBehaviorBase;
import javafx.scene.control.*;
import tpv.fxcontrol.FlowCell;
import tpv.fxcontrol.FlowIndexedCell;
import tpv.fxcontrol.FlowView;

public class FlowCellBehavior<T> extends CellBehaviorBase<FlowCell<T>> {



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public FlowCellBehavior(FlowCell<T> control) {
        super(control);
    }


    @Override
    protected int getIndex() {
        return getNode() instanceof FlowIndexedCell ? getNode().getIndex() : -1;
    }




    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    @Override protected MultipleSelectionModel<T> getSelectionModel() {
        return getCellContainer().getSelectionModel();
    }

    @Override protected FocusModel<T> getFocusModel() {
        return getCellContainer().getFocusModel();
    }

    @Override protected FlowView<T> getCellContainer() {
        return getNode().getListView();
    }

    @Override protected void edit(FlowCell<T> cell) {
        int index = cell == null ? -1 : cell.getIndex();
        getCellContainer().edit(index);
    }
}