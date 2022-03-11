package tpv.fxcontrol.skin;/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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



import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.skin.CellSkinBase;
import javafx.scene.layout.Region;
import tpv.fxcontrol.FlowCell;
import tpv.fxcontrol.FlowView;
import tpv.fxcontrol.behavior.FlowCellBehavior;


/**
 * Default skin implementation for the {@link ListCell} control.
 *
 * @see ListCell
 * @since 9
 */
public class FlowCellSkin<T> extends CellSkinBase<FlowCell<T>> {

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final FlowCellBehavior<FlowCell<T>> behavior;

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ListCellSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public FlowCellSkin(FlowCell<T> control) {
        super(control);

        // install default input map for the ListCell control
        behavior = new FlowCellBehavior(control);
//        control.setInputMap(behavior.getInputMap());
    }

    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double pref = super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
        FlowView<T> listView = getSkinnable().getListView();
        return listView == null ? 0 :
                listView.getOrientation() == Orientation.VERTICAL ? pref : Math.max(pref, getCellSize());
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double fixedCellSize = getFixedCellSize();
        if (fixedCellSize > 0) {
            return fixedCellSize;
        }

        // Added the comparison between the default cell size and the requested
        // cell size to prevent the issue identified in RT-19873.
        final double cellSize = getCellSize();
        final double prefHeight = cellSize == DEFAULT_CELL_SIZE ? super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset) : cellSize;
        return prefHeight;
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double fixedCellSize = getFixedCellSize();
        if (fixedCellSize > 0) {
            return fixedCellSize;
        }

        return super.computeMinHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double fixedCellSize = getFixedCellSize();
        if (fixedCellSize > 0) {
            return fixedCellSize;
        }
        return super.computeMaxHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    private double getFixedCellSize() {
        FlowView<?> listView = getSkinnable().getListView();
        return listView != null ? listView.getFixedCellSize() : Region.USE_COMPUTED_SIZE;
    }
    static final double DEFAULT_CELL_SIZE = 24.0;
}
