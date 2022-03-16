/*
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

package tpv.fxcontrol.skin;

import com.sun.javafx.scene.ParentHelper;
import com.sun.javafx.scene.control.Logging;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.skin.Utils;
import com.sun.javafx.scene.traversal.Algorithm;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.ParentTraversalEngine;
import com.sun.javafx.scene.traversal.TraversalContext;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleRole;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Cell;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.util.Duration;
import com.sun.javafx.logging.PlatformLogger;
import tpv.fxcontrol.FlowIndexedCell;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Implementation of a virtualized container using a cell based mechanism. This
 * is used by the skin implementations for UI controls such as
 * {@link javafx.scene.control.ListView}, {@link javafx.scene.control.TreeView},
 * {@link javafx.scene.control.TableView}, and {@link javafx.scene.control.TreeTableView}.
 *
 * @since 9
 */
public class VirtualFlow<T extends FlowIndexedCell> extends Region {


    /* *************************************************************************
     *                                                                         *
     * Static fields                                                           *
     *                                                                         *
     **************************************************************************/
    private static double MAGIC_X = 2;
    private static double MAGIC_Y = 2;


    /**
     * Scroll events may request to scroll about a number of "lines". We first
     * decide how big one "line" is - for fixed cell size it's clear,
     * for variable cell size we settle on a single number so that the scrolling
     * speed is consistent. Now if the line is so big that
     * MIN_SCROLLING_LINES_PER_PAGE of them don't fit into one page, we make
     * them smaller to prevent the scrolling step to be too big (perhaps
     * even more than one page).
     */
    private static final int MIN_SCROLLING_LINES_PER_PAGE = 8;

    /**
     * Indicates that this is a newly created cell and we need call processCSS for it.
     *
     * See RT-23616 for more details.
     */
    private static final String NEW_CELL = "newcell";

    private static final double GOLDEN_RATIO_MULTIPLIER = 0.618033987;

    /**
     * The default improvement for the estimation of the total size. A value
     * of x means that every time we need to estimate the size, we will add
     * x new cells that are not yet available into the calculations
     */
    private static final int DEFAULT_IMPROVEMENT = 2;



    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private boolean touchDetected = false;
    private boolean mouseDown = false;

    /**
     * The width of the VirtualFlow the last time it was laid out. We
     * use this information for several fast paths during the layout pass.
     */
    double lastWidth = -1;

    /**
     * The height of the VirtualFlow the last time it was laid out. We
     * use this information for several fast paths during the layout pass.
     */
    double lastHeight = -1;

    /**
     * The number of "virtual" cells in the flow the last time it was laid out.
     * For example, there may have been 1000 virtual cells, but only 20 actual
     * cells created and in use. In that case, lastCellCount would be 1000.
     */
    int lastCellCount = 0;



    /**
     * The position last time we laid out. If none of the lastXXX vars have
     * changed respective to their values in layoutChildren, then we can just punt
     * out of the method (I hope...)
     */
    double lastPosition;







    /**
     * A special cell used to accumulate bounds, such that we reduce object
     * churn. This cell must be recreated whenever the cell factory function
     * changes. This has package access ONLY for testing.
     */
    T accumCell;

    /**
     * This group is used for holding the 'accumCell'. 'accumCell' must
     * be added to the skin for it to be styled. Otherwise, it doesn't
     * report the correct width/height leading to issues when scrolling
     * the flow
     */
    Group accumCellParent;

    /**
     * The group which holds the cells.
     */
    final Sheet<T> sheet;



    /**
     * The scroll bar used for scrolling horizontally. This has package access
     * ONLY for testing.
     */
    private VirtualScrollBar hbar = new VirtualScrollBar(this);

    /**
     * The scroll bar used to scrolling vertically. This has package access
     * ONLY for testing.
     */
    private VirtualScrollBar vbar = new VirtualScrollBar(this);

    /**
     * Control in which the cell's sheet is placed and forms the viewport. The
     * viewportBreadth and viewportLength are simply the dimensions of the
     * clipView. This has package access ONLY for testing.
     */
    ClippedContainer clipView;

    /**
     * When both the horizontal and vertical scroll bars are visible,
     * we have to 'fill in' the bottom right corner where the two scroll bars
     * meet. This is handled by this corner region. This has package access
     * ONLY for testing.
     */
    StackPane corner;

    /**
     * The offset in pixels between the top of the virtualFlow and the content it
     * shows. When manipulating the position of the content (e.g. by scrolling),
     * the absoluteOffset must be changed so that it always returns the number of
     * pixels that, when applied to a translateY (for vertical) or translateX
     * (for horizontal) operation on each cell, the first cell aligns with the
     * node.
     * The following relation should always be true:
     * 0 <= absoluteOffset <= (estimatedSize - viewportLength)
     * Based on this relation, the position p is defined as
     * 0 <= absoluteOffset/(estimatedSize - viewportLength) <= 1
     * As a consequence, whenever p, estimatedSize, or viewportLength
     * changes, the absoluteOffset needs to change as well.
     * The method <code>adjustAbsoluteOffset()</code> can be used to calculate the
     * value of <code>absoluteOffset</code> based on the value of the other 3
     * variables.
     * Vice versa, if we change the <code>absoluteOffset</code>, we need to make
     * sure that the <code>position</code> is changed in a consistent way. This
     * can be done by calling <code>adjustPosition()</code>
     */
    double absoluteOffset = 0d;

    /**
     * An estimation of the total size (height for vertical, width for horizontal).
     * A value of -1 means that this value is unusable and should not be trusted.
     * This might happen before any calculations take place, or when a method
     * invocation is guaranteed to invalidate the current estimation.
     */
    double estimatedSize = -1d;

    /**
     * A list containing the cached version of the calculated size (height for
     * vertical, width for horizontal) for a (fictive or real) cell for
     * each element of the backing data.
     * This list is used to calculate the estimatedSize.
     * The list is not expected to be complete, but it is always up to date.
     * When the size of the items in the backing list changes, this list is
     * cleared.
     */
    private ArrayList<double[]> itemSizeCache = new ArrayList<>();


    // used for panning the virtual flow
    private double lastX;
    private double lastY;
    private boolean isPanning = false;

    private boolean fixedCellSizeEnabled = false;

    private boolean needsReconfigureCells = false; // when cell contents are the same
    private boolean needsRecreateCells = false; // when cell factory changed
    private boolean needsRebuildCells = false; // when cell contents have changed
    private boolean sizeChanged = false;
    private final BitSet dirtyCells = new BitSet();

    Timeline sbTouchTimeline;
    KeyFrame sbTouchKF1;
    KeyFrame sbTouchKF2;

    private boolean needBreadthBar;
    private boolean needLengthBar;
    private boolean tempVisibility = false;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new VirtualFlow instance.
     */
    public VirtualFlow() {
        getStyleClass().add("virtual-flow");
        setId("virtual-flow");

        // initContent
        // --- sheet
        sheet = new Sheet<>();
        sheet.getStyleClass().add("sheet");
//


        // --- clipView
        clipView = new ClippedContainer(this);
        clipView.setNode(sheet);
        getChildren().add(clipView);

        // --- accumCellParent
        accumCellParent = new Group();
        accumCellParent.setVisible(false);
        getChildren().add(accumCellParent);


        /*
        ** don't allow the ScrollBar to handle the ScrollEvent,
        ** In a VirtualFlow a vertical scroll should scroll on the vertical only,
        ** whereas in a horizontal ScrollBar it can scroll horizontally.
        */
        // block the event from being passed down to children
        final EventDispatcher blockEventDispatcher = (event, tail) -> event;
        // block ScrollEvent from being passed down to scrollbar's skin
        final EventDispatcher oldHsbEventDispatcher = hbar.getEventDispatcher();
        hbar.setEventDispatcher((event, tail) -> {
            if (event.getEventType() == ScrollEvent.SCROLL &&
                    !((ScrollEvent)event).isDirect()) {
                tail = tail.prepend(blockEventDispatcher);
                tail = tail.prepend(oldHsbEventDispatcher);
                return tail.dispatchEvent(event);
            }
            return oldHsbEventDispatcher.dispatchEvent(event, tail);
        });
        // block ScrollEvent from being passed down to scrollbar's skin
        final EventDispatcher oldVsbEventDispatcher = vbar.getEventDispatcher();
        vbar.setEventDispatcher((event, tail) -> {
            if (event.getEventType() == ScrollEvent.SCROLL &&
                    !((ScrollEvent)event).isDirect()) {
                tail = tail.prepend(blockEventDispatcher);
                tail = tail.prepend(oldVsbEventDispatcher);
                return tail.dispatchEvent(event);
            }
            return oldVsbEventDispatcher.dispatchEvent(event, tail);
        });
        /*
        ** listen for ScrollEvents over the whole of the VirtualFlow
        ** area, the above dispatcher having removed the ScrollBars
        ** scroll event handling.
        */
        setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                if (Properties.IS_TOUCH_SUPPORTED) {
                    if (touchDetected == false &&  mouseDown == false ) {
                        startSBReleasedAnimation();
                    }
                }
                /*
                ** calculate the delta in the direction of the flow.
                */
                double virtualDelta = 0.0;
                if (isVertical()) {
                    switch(event.getTextDeltaYUnits()) {
                        case PAGES:
                            virtualDelta = event.getTextDeltaY() * lastHeight;
                            break;
                        case LINES:
                            double lineSize;
                            if (fixedCellSizeEnabled) {
                                lineSize = getFixedCellSize();
                            } else {
                                // For the scrolling to be reasonably consistent
                                // we set the lineSize to the average size
                                // of all currently loaded lines.
                                T lastCell = sheet.getLast();
                                lineSize =
                                        (getCellPosition(lastCell).getY()
                                            + getCellHeight(lastCell)
                                            - getCellPosition(sheet.getFirst()).getY())
                                        / sheet.size();
                            }

                            if (lastHeight / lineSize < MIN_SCROLLING_LINES_PER_PAGE) {
                                lineSize = lastHeight / MIN_SCROLLING_LINES_PER_PAGE;
                            }

                            virtualDelta = event.getTextDeltaY() * lineSize;
                            break;
                        case NONE:
                            virtualDelta = event.getDeltaY();
                    }
                } else { // horizontal
                    switch(event.getTextDeltaXUnits()) {
                        case CHARACTERS:
                            // can we get character size here?
                            // for now, fall through to pixel values
                        case NONE:
                            double dx = event.getDeltaX();
                            double dy = event.getDeltaY();

                            virtualDelta = (Math.abs(dx) > Math.abs(dy) ? dx : dy);
                    }
                }

                if (virtualDelta != 0.0) {
                    /*
                    ** only consume it if we use it
                    */
                    double result = scrollPixels(-virtualDelta);
                    if (result != 0.0) {
                        event.consume();
                    }
                }

                ScrollBar nonVirtualBar = isVertical() ? hbar : vbar;
                if (needBreadthBar) {
                    double nonVirtualDelta = isVertical() ? event.getDeltaX() : event.getDeltaY();
                    if (nonVirtualDelta != 0.0) {
                        double newValue = nonVirtualBar.getValue() - nonVirtualDelta;
                        if (newValue < nonVirtualBar.getMin()) {
                            nonVirtualBar.setValue(nonVirtualBar.getMin());
                        } else if (newValue > nonVirtualBar.getMax()) {
                            nonVirtualBar.setValue(nonVirtualBar.getMax());
                        } else {
                            nonVirtualBar.setValue(newValue);
                        }
                        event.consume();
                    }
                }
            }
        });


        addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                mouseDown = true;
                if (Properties.IS_TOUCH_SUPPORTED) {
                    scrollBarOn();
                }
                if (isFocusTraversable()) {
                    // We check here to see if the current focus owner is within
                    // this VirtualFlow, and if so we back-off from requesting
                    // focus back to the VirtualFlow itself. This is particularly
                    // relevant given the bug identified in RT-32869. In this
                    // particular case TextInputControl was clearing selection
                    // when the focus on the TextField changed, meaning that the
                    // right-click context menu was not showing the correct
                    // options as there was no selection in the TextField.
                    boolean doFocusRequest = true;
                    Node focusOwner = getScene().getFocusOwner();
                    if (focusOwner != null) {
                        Parent parent = focusOwner.getParent();
                        while (parent != null) {
                            if (parent.equals(VirtualFlow.this)) {
                                doFocusRequest = false;
                                break;
                            }
                            parent = parent.getParent();
                        }
                    }

                    if (doFocusRequest) {
                        requestFocus();
                    }
                }

                lastX = e.getX();
                lastY = e.getY();

                // determine whether the user has push down on the virtual flow,
                // or whether it is the scrollbar. This is done to prevent
                // mouse events being 'doubled up' when dragging the scrollbar
                // thumb - it has the side-effect of also starting the panning
                // code, leading to flicker
                isPanning = ! (vbar.getBoundsInParent().contains(e.getX(), e.getY())
                        || hbar.getBoundsInParent().contains(e.getX(), e.getY()));
            }
        });
        addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            mouseDown = false;
            if (Properties.IS_TOUCH_SUPPORTED) {
                startSBReleasedAnimation();
            }
        });
        addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (Properties.IS_TOUCH_SUPPORTED) {
                scrollBarOn();
            }
            if (! isPanning || ! isPannable()) return;

            // With panning enabled, we support panning in both vertical
            // and horizontal directions, regardless of the fact that
            // VirtualFlow is virtual in only one direction.
            double xDelta = lastX - e.getX();
            double yDelta = lastY - e.getY();

            // figure out the distance that the mouse moved in the virtual
            // direction, and then perform the movement along that axis
            // virtualDelta will contain the amount we actually did move
            double virtualDelta = isVertical() ? yDelta : xDelta;
            double actual = scrollPixels(virtualDelta);
            if (actual != 0) {
                // update last* here, as we know we've just adjusted the
                // scrollbar. This means we don't get the situation where a
                // user presses-and-drags a long way past the min or max
                // values, only to change directions and see the scrollbar
                // start moving immediately.
                if (isVertical()) lastY = e.getY();
                else lastX = e.getX();
            }

            // similarly, we do the same in the non-virtual direction
            double nonVirtualDelta = isVertical() ? xDelta : yDelta;
            ScrollBar nonVirtualBar = isVertical() ? hbar : vbar;
            if (nonVirtualBar.isVisible()) {
                double newValue = nonVirtualBar.getValue() + nonVirtualDelta;
                if (newValue < nonVirtualBar.getMin()) {
                    nonVirtualBar.setValue(nonVirtualBar.getMin());
                } else if (newValue > nonVirtualBar.getMax()) {
                    nonVirtualBar.setValue(nonVirtualBar.getMax());
                } else {
                    nonVirtualBar.setValue(newValue);

                    // same as the last* comment above
                    if (isVertical()) lastX = e.getX();
                    else lastY = e.getY();
                }
            }
        });

        /*
         * We place the scrollbars _above_ the rectangle, such that the drag
         * operations often used in conjunction with scrollbars aren't
         * misinterpreted as drag operations on the rectangle as well (which
         * would be the case if the scrollbars were underneath it as the
         * rectangle itself doesn't block the mouse.
         */
        // --- vbar
        vbar.setOrientation(Orientation.VERTICAL);
        vbar.addEventHandler(MouseEvent.ANY, event -> {
            event.consume();
        });
        getChildren().add(vbar);

        // --- hbar
        hbar.setOrientation(Orientation.HORIZONTAL);
        hbar.addEventHandler(MouseEvent.ANY, event -> {
            event.consume();
        });
        getChildren().add(hbar);

        // --- corner
        corner = new StackPane();
        corner.getStyleClass().setAll("corner");
        getChildren().add(corner);

        // initBinds
        // clipView binds
        InvalidationListener listenerX = valueModel -> {
            updateHbar();
        };
        verticalProperty().addListener(listenerX);
        hbar.valueProperty().addListener(listenerX);
        hbar.visibleProperty().addListener(listenerX);
        visibleProperty().addListener(listenerX);
        sceneProperty().addListener(listenerX);



        ChangeListener<Number> listenerY = (ov, t, t1) -> {
            clipView.setClipY(isVertical() ? 0 : vbar.getValue());
        };
        vbar.valueProperty().addListener(listenerY);

        super.heightProperty().addListener((observable, oldHeight, newHeight) -> {
            // Fix for RT-8480, where the VirtualFlow does not show its content
            // after changing size to 0 and back.
            if (oldHeight.doubleValue() == 0 && newHeight.doubleValue() > 0) {
                recreateCells();
            }
        });


        /*
        ** there are certain animations that need to know if the touch is
        ** happening.....
        */
        setOnTouchPressed(e -> {
            touchDetected = true;
            scrollBarOn();
        });

        setOnTouchReleased(e -> {
            touchDetected = false;
            startSBReleasedAnimation();
        });

        ParentHelper.setTraversalEngine(this, new ParentTraversalEngine(this, new Algorithm() {

            Node selectNextAfterIndex(int index, TraversalContext context) {
                T nextCell;
                while ((nextCell = sheet.getVisibleCell(++index)) != null) {
                    if (nextCell.isFocusTraversable()) {
                        return nextCell;
                    }
                    Node n = context.selectFirstInParent(nextCell);
                    if (n != null) {
                        return n;
                    }
                }
                return null;
            }

            Node selectPreviousBeforeIndex(int index, TraversalContext context) {
                T prevCell;
                while ((prevCell = sheet.getVisibleCell(--index)) != null) {
                    Node prev = context.selectLastInParent(prevCell);
                    if (prev != null) {
                        return prev;
                    }
                    if (prevCell.isFocusTraversable()) {
                        return prevCell;
                    }
                }
                return null;
            }

            @Override
            public Node select(Node owner, Direction dir, TraversalContext context) {
                T cell;
                if (sheet.isEmpty()) return null;
                if (sheet.contains(owner)) {
                    cell = (T) owner;
                } else {
                    cell = findOwnerCell(owner);
                    Node next = context.selectInSubtree(cell, owner, dir);
                    if (next != null) {
                        return next;
                    }
                    if (dir == Direction.NEXT) dir = Direction.NEXT_IN_LINE;
                }
                int cellIndex = cell.getIndex();
                switch(dir) {
                    case PREVIOUS:
                        return selectPreviousBeforeIndex(cellIndex, context);
                    case NEXT:
                        Node n = context.selectFirstInParent(cell);
                        if (n != null) {
                            return n;
                        }
                        // Intentional fall-through
                    case NEXT_IN_LINE:
                        return selectNextAfterIndex(cellIndex, context);
                }
                return null;
            }

            private T findOwnerCell(Node owner) {
                Parent p = owner.getParent();
                while (!sheet.contains(p)) {
                    p = p.getParent();
                }
                return (T)p;
            }

            @Override
            public Node selectFirst(TraversalContext context) {
                T firstCell = sheet.getFirst();
                if (firstCell == null) return null;
                if (firstCell.isFocusTraversable()) return firstCell;
                Node n = context.selectFirstInParent(firstCell);
                if (n != null) {
                    return n;
                }
                return selectNextAfterIndex(firstCell.getIndex(), context);
            }

            @Override
            public Node selectLast(TraversalContext context) {
                T lastCell = sheet.getLast();
                if (lastCell == null) return null;
                Node p = context.selectLastInParent(lastCell);
                if (p != null) {
                    return p;
                }
                if (lastCell.isFocusTraversable()) return lastCell;
                return selectPreviousBeforeIndex(lastCell.getIndex(), context);
            }
        }));
    }



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * There are two main complicating factors in the implementation of the
     * VirtualFlow, which are made even more complicated due to the performance
     * sensitive nature of this code. The first factor is the actual
     * virtualization mechanism, wired together with the PositionMapper.
     * The second complicating factor is the desire to do minimal layout
     * and minimal updates to CSS.
     *
     * Since the layout mechanism runs at most once per pulse, we want to hook
     * into this mechanism for minimal recomputation. Whenever a layout pass
     * is run we record the width/height that the virtual flow was last laid
     * out to. In subsequent passes, if the width/height has not changed then
     * we know we only have to rebuild the cells. If the width or height has
     * changed, then we can make appropriate decisions based on whether the
     * width / height has been reduced or expanded.
     *
     * In various places, if requestLayout is called it is generally just
     * used to indicate that some form of layout needs to happen (either the
     * entire thing has to be reconstructed, or just the cells need to be
     * reconstructed, generally).
     *
     * The accumCell is a special cell which is used in some computations
     * when an actual cell for that item isn't currently available. However,
     * the accumCell must be cleared whenever the cellFactory function is
     * changed because we need to use the cells that come from the new factory.
     *
     * In addition to storing the lastWidth and lastHeight, we also store the
     * number of cells that existed last time we performed a layout. In this
     * way if the number of cells change, we can request a layout and when it
     * occurs we can tell that the number of cells has changed and react
     * accordingly.
     *
     * Because the VirtualFlow can be laid out horizontally or vertically a
     * naming problem is present when trying to conceptualize and implement
     * the flow. In particular, the words "width" and "height" are not
     * precise when describing the unit of measure along the "virtualized"
     * axis and the "orthogonal" axis. For example, the height of a cell when
     * the flow is vertical is the magnitude along the "virtualized axis",
     * and the width is along the axis orthogonal to it.
     *
     * Since "height" and "width" are not reliable terms, we use the words
     * "length" and "breadth" to describe the magnitude of a cell along
     * the virtualized axis and orthogonal axis. For example, in a vertical
     * flow, the height=length and the width=breadth. In a horizontal axis,
     * the height=breadth and the width=length.
     *
     * These terms are somewhat arbitrary, but chosen so that when reading
     * most of the below code you can think in just one dimension, with
     * helper functions converting width/height in to length/breadth, while
     * also being different from width/height so as not to get confused with
     * the actual width/height of a cell.
     */

    // --- vertical
    /**
     * Indicates the primary direction of virtualization. If true, then the
     * primary direction of virtualization is vertical, meaning that cells will
     * stack vertically on top of each other. If false, then they will stack
     * horizontally next to each other.
     */
    private BooleanProperty vertical;
    public final void setVertical(boolean value) {
        verticalProperty().set(value);
    }

    public final boolean isVertical() {
        return vertical == null ? true : vertical.get();
    }

    public final BooleanProperty verticalProperty() {
        if (vertical == null) {
            vertical = new BooleanPropertyBase(true) {
                @Override protected void invalidated() {
                   relayoutAll();
                }

                @Override
                public Object getBean() {
                    return VirtualFlow.this;
                }

                @Override
                public String getName() {
                    return "vertical";
                }
            };
        }
        return vertical;
    }

    private void relayoutAll() {
        sheet.clearChildren();
        sheet.clear();
        lastWidth = lastHeight = -1;
        setMaxPrefBreadth(-1);
        sheet.setWidth(0);
        sheet.setHeight(0);
        lastPosition = 0;
        hbar.setValue(0);
        vbar.setValue(0);
        setPosition(0.0f);
        setNeedsLayout(true);
        requestLayout();
    }

    // --- pannable
    /**
     * Indicates whether the VirtualFlow viewport is capable of being panned
     * by the user (either via the mouse or touch events).
     */
    private BooleanProperty pannable = new SimpleBooleanProperty(this, "pannable", true);
    public final boolean isPannable() { return pannable.get(); }
    public final void setPannable(boolean value) { pannable.set(value); }
    public final BooleanProperty pannableProperty() { return pannable; }

    // --- cell count
    /**
     * Indicates the number of cells that should be in the flow. The user of
     * the VirtualFlow must set this appropriately. When the cell count changes
     * the VirtualFlow responds by updating the visuals. If the items backing
     * the cells change, but the count has not changed, you must call the
     * reconfigureCells() function to update the visuals.
     */
    private IntegerProperty itemCount = new SimpleIntegerProperty(this, "cellCount", 0) {
        private int oldCount = 0;

        @Override protected void invalidated() {
            int cellCount = get();
            resetSizeEstimates();
            recalculateEstimatedSize();

            boolean countChanged = oldCount != cellCount;
            oldCount = cellCount;

            // ensure that the virtual scrollbar adjusts in size based on the current
            // cell count.
            if (countChanged) {
                VirtualScrollBar lengthBar = isVertical() ? vbar : hbar;
                lengthBar.setMax(cellCount);
            }

            // I decided *not* to reset maxPrefBreadth here for the following
            // situation. Suppose I have 30 cells and then I add 10 more. Just
            // because I added 10 more doesn't mean the max pref should be
            // reset. Suppose the first 3 cells were extra long, and I was
            // scrolled down such that they weren't visible. If I were to reset
            // maxPrefBreadth when subsequent cells were added or removed, then the
            // scroll bars would erroneously reset as well. So I do not reset
            // the maxPrefBreadth here.

            // Fix for RT-12512, RT-14301 and RT-14864.
            // Without this, the VirtualFlow length-wise scrollbar would not change
            // as expected. This would leave items unable to be shown, as they
            // would exist outside of the visible area, even when the scrollbar
            // was at its maximum position.
            // FIXME this should be only executed on the pulse, so this will likely
            // lead to performance degradation until it is handled properly.
            if (countChanged) {
                layoutChildren();

                Parent parent = getParent();
                if (parent != null) parent.requestLayout();

                synchronizeAbsoluteOffsetWithPosition();
            }
            // TODO suppose I had 100 cells and I added 100 more. Further
            // suppose I was scrolled to the bottom when that happened. I
            // actually want to update the position of the mapper such that
            // the view remains "stable".
        }
    };

    /**
     * All items
     * @return
     */
    public final int getItemsCount() { return itemCount.get(); }
    public final void setItemsCount(int value) {
        itemCount.set(value);
    }
    public final IntegerProperty itemCountProperty() { return itemCount; }


    // --- position
    /**
     * The position of the VirtualFlow within its list of cells. This is a value
     * between 0 and 1. This is usually modified by the scroll bar
     */
    private DoubleProperty position = new SimpleDoubleProperty(this, "position") {
        @Override public void setValue(Number v) {
            super.setValue(com.sun.javafx.util.Utils.clamp(0, get(), 1));
        }

        @Override protected void invalidated() {
            super.invalidated();
            requestLayout();
        }
    };
    public final double getPosition() { return position.get(); }
    //Value will be clamped between [0, 1]
    public final void setPosition(double value) {
        position.set(value);
        // When the position is changed explicitly, we need to make sure
        // the absolute offset is changed accordingly.
        synchronizeAbsoluteOffsetWithPosition();
    }
    public final DoubleProperty positionProperty() { return position; }

    // --- fixed cell size
    /**
     * For optimisation purposes, some use cases can trade dynamic cell length
     * for speed - if fixedCellSize is greater than zero we'll use that rather
     * than determine it by querying the cell itself.
     */
    private DoubleProperty fixedCellSize = new SimpleDoubleProperty(this, "fixedCellSize") {
        @Override protected void invalidated() {
            fixedCellSizeEnabled = get() > 0;
            layoutChildren();
        }
    };
    public final void setFixedCellSize(final double value) { fixedCellSize.set(value); }
    public final double getFixedCellSize() { return fixedCellSize.get(); }
    public final DoubleProperty fixedCellSizeProperty() { return fixedCellSize; }


    // --- Cell Factory
    private ObjectProperty<Callback<VirtualFlow<T>, T>> cellFactory;

    /**
     * Sets a new cell factory to use in the VirtualFlow. This forces all old
     * cells to be thrown away, and new cells to be created with
     * the new cell factory.
     * @param value the new cell factory
     */
    public final void setCellFactory(Callback<VirtualFlow<T>, T> value) {
        cellFactoryProperty().set(value);
    }

    /**
     * Returns the current cell factory.
     * @return the current cell factory
     */
    public final Callback<VirtualFlow<T>, T> getCellFactory() {
        return cellFactory == null ? null : cellFactory.get();
    }

    /**
     * <p>Setting a custom cell factory has the effect of deferring all cell
     * creation, allowing for total customization of the cell. Internally, the
     * VirtualFlow is responsible for reusing cells - all that is necessary
     * is for the custom cell factory to return from this function a cell
     * which might be usable for representing any item in the VirtualFlow.
     *
     * <p>Refer to the {@link Cell} class documentation for more detail.
     * @return  the cell factory property
     */
    public final ObjectProperty<Callback<VirtualFlow<T>, T>> cellFactoryProperty() {
        if (cellFactory == null) {
            cellFactory = new SimpleObjectProperty<Callback<VirtualFlow<T>, T>>(this, "cellFactory") {
                @Override protected void invalidated() {
                    if (get() != null) {
                        setNeedsLayout(true);
                        recreateCells();
                        if (getParent() != null) getParent().requestLayout();
                    }
                    if (accumCellParent != null) {
                        accumCellParent.getChildren().clear();
                    }
                    accumCell = null;
                }
            };
        }
        return cellFactory;
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Overridden to implement somewhat more efficient support for layout. The
     * VirtualFlow can generally be considered as being unmanaged, in that
     * whenever the position changes, or other such things change, we need
     * to perform a layout but there is no reason to notify the parent. However
     * when things change which may impact the preferred size (such as
     * vertical, createCell, and configCell) then we need to notify the
     * parent.
     */
    @Override public void requestLayout() {
// Note: This block is commented as it was relaying on a bad assumption on how
//       layout request was handled in parent class that is now fixed.
//
//        // isNeedsLayout() is commented out due to RT-21417. This does not
//        // appear to impact performance (indeed, it may help), and resolves the
//        // issue identified in RT-21417.
//        setNeedsLayout(true);

        // The fix is to prograte this layout request to its parent class.
        // A better fix will be required if performance is negatively affected
        // by this fix.
        super.requestLayout();
    }

    /**
     * Keep the position constant and adjust the absoluteOffset to
     * match the (new) position.
     */
    void synchronizeAbsoluteOffsetWithPosition() {
        absoluteOffset  = (estimatedSize - sheet.getHeight()) * getPosition();
    }

    /**
     * Keep the absoluteOffset constant and adjust the position to match
     * the (new) absoluteOffset.
     */
    void synchronizePositionWithAbsoluteOffset() {
        if (sheet.getHeight() >= estimatedSize) {
            setPosition(0d);
        } else {
            setPosition(absoluteOffset / (estimatedSize - sheet.getHeight()));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void layoutChildren() {
        recalculateEstimatedSize();
        // if the last modification to the position was done via scrollPixels,
        // the absoluteOffset and position are already in sync.
        // However, the position can be modified via different ways (e.g. by
        // moving the scrollbar thumb), so we need to recalculate absoluteOffset
        // There is an exception to this: if cells are added/removed, we want
        // to keep the absoluteOffset constant, hence we need to adjust the position.

        if (lastCellCount != getItemsCount()) {
            synchronizePositionWithAbsoluteOffset();
        } else {
            synchronizeAbsoluteOffsetWithPosition();
        }
        if (needsRecreateCells) {
            lastWidth = -1;
            lastHeight = -1;
            releaseCell(accumCell);
            sheet.clearCompletely();
            releaseAllPrivateCells();
        } else if (needsRebuildCells) {
            lastWidth = -1;
            lastHeight = -1;
            releaseCell(accumCell);
            sheet.dumpAllToPile();
            releaseAllPrivateCells();
        } else if (needsReconfigureCells) {
            setMaxPrefBreadth(-1);
            lastWidth = -1;
            lastHeight = -1;
        }
        updateDirtyCells();


//        final boolean hasSizeChange = sizeChanged;
        boolean recreatedOrRebuilt = needsRebuildCells || needsRecreateCells || sizeChanged;

        needsRecreateCells = false;
        needsReconfigureCells = false;
        needsRebuildCells = false;
//        sizeChanged = false;


        final double width = getWidth();
        final double height = getHeight();
        final boolean isVertical = isVertical();
        final double position = getPosition();
        // if the width and/or height is 0, then there is no point doing
        // any of this work. In particular, this can happen during startup
        if (width <= 0 || height <= 0) {
            sheet.addAllToPile();
            lastWidth = width;
            lastHeight = height;
            hbar.setVisible(false);
            vbar.setVisible(false);
            corner.setVisible(false);
            return;
        }

        // we check if any of the cells in the cells list need layout. This is a
        // sign that they are perhaps animating their sizes. Without this check,
        // we may not perform a layout here, meaning that the cell will likely
        // 'jump' (in height normally) when the user drags the virtual thumb as
        // that is the first time the layout would occur otherwise.
        boolean cellNeedsLayout = false;
        boolean thumbNeedsLayout = false;

        if (Properties.IS_TOUCH_SUPPORTED) {
            if ((tempVisibility == true && (hbar.isVisible() == false || vbar.isVisible() == false)) ||
                (tempVisibility == false && (hbar.isVisible() == true || vbar.isVisible() == true))) {
                thumbNeedsLayout = true;
            }
        }

        for (int i = 0; i < sheet.size(); i++) {
            Cell<?> cell = sheet.get(i);
            cellNeedsLayout = cell.isNeedsLayout();
            if (cellNeedsLayout) break;
        }

        final int cellCount = getItemsCount();
        final T firstCell = getFirstVisibleCell();

        // If no cells need layout, we check other criteria to see if this
        // layout call is even necessary. If it is found that no layout is
        // needed, we just punt.
        if (! cellNeedsLayout && !thumbNeedsLayout) {


            if (width == lastWidth &&
                height == lastHeight &&
                cellCount == lastCellCount &&
                position == lastPosition
                )
            {
                // TODO this happens to work around the problem tested by
                // testCellLayout_LayoutWithoutChangingThingsUsesCellsInSameOrderAsBefore
                // but isn't a proper solution. Really what we need to do is, when
                // laying out cells, we need to make sure that if a cell is pressed
                // AND we are doing a full rebuild then we need to make sure we
                // use that cell in the same physical location as before so that
                // it gets the mouse release event.
                return;
            }
        }

        /*
         * This function may get called under a variety of circumstances.
         * It will determine what has changed from the last time it was laid
         * out, and will then take one of several execution paths based on
         * what has changed so as to perform minimal layout work and also to
         * give the expected behavior. One or more of the following may have
         * happened:
         *
         *  1) width/height has changed
         *      - If the width and/or height has been reduced (but neither of
         *        them has been expanded), then we simply have to reposition and
         *        resize the scroll bars
         *      - If the width (in the vertical case) has expanded, then we
         *        need to resize the existing cells and reposition and resize
         *        the scroll bars
         *      - If the height (in the vertical case) has expanded, then we
         *        need to resize and reposition the scroll bars and add
         *        any trailing cells
         *
         *  2) cell count has changed
         *      - If the number of cells is bigger, or it is smaller but not
         *        so small as to move the position then we can just update the
         *        cells in place without performing layout and update the
         *        scroll bars.
         *      - If the number of cells has been reduced and it affects the
         *        position, then move the position and rebuild all the cells
         *        and update the scroll bars
         *
         *  3) size of the cell has changed
         *      - If the size changed in the virtual direction (ie: height
         *        in the case of vertical) then layout the cells, adding
         *        trailing cells as necessary and updating the scroll bars
         *      - If the size changed in the non virtual direction (ie: width
         *        in the case of vertical) then simply adjust the widths of
         *        the cells as appropriate and adjust the scroll bars
         *
         *  4) vertical changed, cells is empty, maxPrefBreadth == -1, etc
         *      - Full rebuild.
         *
         * Each of the conditions really resolves to several of a handful of
         * possible outcomes:
         *  a) reposition & rebuild scroll bars
         *  b) resize cells in non-virtual direction
         *  c) add trailing cells
         *  d) update cells
         *  e) resize cells in the virtual direction
         *  f) all of the above
         *
         * So this function first determines what outcomes need to occur, and
         * then will execute all the ones that really need to happen. Every code
         * path ends up touching the "reposition & rebuild scroll bars" outcome,
         * so that one will be executed every time.
         */
        boolean rebuild = cellNeedsLayout  ||
                sheet.isEmpty()            ||
                getMaxPrefBreadth() == -1  ||
                position != lastPosition   ||
                cellCount != lastCellCount ||
                lastHeight != height ||
                lastWidth != width||
                (isVertical && height < lastHeight) || (! isVertical && width < lastWidth);

        if (!rebuild) {
            // Check if maxPrefBreadth didn't change
            double maxPrefBreadth = getMaxPrefBreadth();
            boolean foundMax = false;
            for (int i = 0; i < sheet.size(); ++i) {
                double breadth = getCellWidth(sheet.get(i));
                if (maxPrefBreadth == breadth) {
                    foundMax = true;
                } else if (breadth > maxPrefBreadth) {
                    rebuild = true;
                    break;
                }
            }
            if (!foundMax) { // All values were lower
                rebuild = true;
            }
        }

        if (!rebuild) {
            if ((isVertical() && height > lastHeight) || (!isVertical() && width > lastWidth)) {
                // resized in the virtual direction
                addTrailingCells();
            }
        }
        initViewport();

        // Get the index of the "current" cell
        int currentIndex = computeCurrentIndex();
        if (lastCellCount != cellCount) {
            // The cell count has changed. We want to keep the viewport
            // stable if possible. If position was 0 or 1, we want to keep
            // the position in the same place. If the new cell count is >=
            // the currentIndex, then we will adjust the position to be 1.
            // Otherwise, our goal is to leave the index of the cell at the
            // top consistent, with the same translation etc.
            if (position != 0 && position != 1 && (currentIndex >= cellCount)) {
                setPosition(1.0f);
            }

            // Update the current index
            currentIndex = computeCurrentIndex();
        }
        if (rebuild) {
            setMaxPrefBreadth(-1);
            sheet.addAllToPile();
            addLeadingCells(currentIndex);
            addTrailingCells();
        }
        computeBarVisibility();

        recreatedOrRebuilt = recreatedOrRebuilt || rebuild;
        updateScrollBarsAndCells(recreatedOrRebuilt);

        lastWidth = getWidth();
        lastHeight = getHeight();
        lastCellCount = getItemsCount();
        lastPosition = getPosition();
        recalculateEstimatedSize();
        sheet.cleanPile();
    }

    private void updateDirtyCells() {
        if (!dirtyCells.isEmpty()) {
            int index;
            final int cellsSize = sheet.size();
            while ((index = dirtyCells.nextSetBit(0)) != -1 && index < cellsSize) {
                T cell = sheet.get(index);
                // updateIndex(-1) works for TableView, but breaks ListView.
                // For now, the TableView just does not use the dirtyCells API
//                cell.updateIndex(-1);
                if (cell != null) {
                    cell.requestLayout();
                    updateCellCacheSize(cell);
                }
                dirtyCells.clear(index);
            }

            setMaxPrefBreadth(-1);
            lastWidth = -1;
            lastHeight = -1;
        }
    }

    /** {@inheritDoc} */
    @Override protected void setWidth(double value) {
        if (value != lastWidth) {
            super.setWidth(value);
            setNeedsLayout(true);
            requestLayout();
        }
    }

    /** {@inheritDoc} */
    @Override protected void setHeight(double value) {
        if (value != lastHeight) {
            super.setHeight(value);
            setNeedsLayout(true);
            requestLayout();
        }
    }


    /**
     * Get a cell which can be used in the layout. This function will reuse
     * cells from the pile where possible, and will create new cells when
     * necessary.
     * @param prefIndex the preferred index
     * @return the available cell
     */
    protected T getAvailableOrCreateCell(int prefIndex) {
        T cell  = sheet.getAndRemoveCellFromPile(prefIndex);
        if(cell == null){
            cell =  createNewCellAndAddToSheet();
        }
        setCellIndex(cell, prefIndex);
        return cell;
    }

    private T createNewCellAndAddToSheet(){
        T cell = getCellFactory().call(this);
        cell.getProperties().put(NEW_CELL, null);

        if (cell.getParent() == null) {
            sheet.getChildren().add(cell);
        }

        return cell;
    }




    /**
     * Locates and returns the last non-empty IndexedCell that is currently
     * partially or completely visible. This function may return null if there
     * are no cells, or if the viewport length is 0.
     * @return the last visible cell
     */
    public T getLastVisibleCell() {
        if (sheet.isEmpty() || sheet.getHeight() <= 0) return null;

        T cell;
        for (int i = sheet.size() - 1; i >= 0; i--) {
            cell = sheet.get(i);
            if (! cell.isEmpty()) {
                return cell;
            }
        }

        return null;
    }

    /**
     * Locates and returns the first non-empty IndexedCell that is partially or
     * completely visible. This really only ever returns null if there are no
     * cells or the viewport length is 0.
     * @return the first visible cell
     */
    public T getFirstVisibleCell() {
        if (sheet.isEmpty() || sheet.getHeight() <= 0) {
            return null;
        }
        T cell = sheet.getFirst();
        return cell.isEmpty() ? null : cell;
    }

    /**
     * Adjust the position of cells so that the specified cell
     * will be positioned at the start of the viewport. The given cell must
     * already be "live".
     * @param firstCell the first cell
     */
    public void scrollToTop(T firstCell) {
        if (firstCell != null) {
            scrollPixels(getCellPosition(firstCell).getY());
        }
    }

    /**
     * Adjust the position of cells so that the specified cell
     * will be positioned at the end of the viewport. The given cell must
     * already be "live".
     * @param lastCell the last cell
     */
    public void scrollToBottom(T lastCell) {
        if (lastCell != null) {
            scrollPixels(getCellPosition(lastCell).getY() + getCellHeight(lastCell) - sheet.getHeight());
        }
    }

    /**
     * Adjusts the cells such that the selected cell will be fully visible in
     * the viewport (but only just).
     * @param cell the cell
     */
    public void scrollTo(T cell) {
        if (cell != null) {
            final double start = getCellPosition(cell).getY();
            final double length = getCellHeight(cell);
            final double end = start + length;
            final double viewportLength = sheet.getHeight();

            if (start < 0) {
                scrollPixels(start);
            } else if (end > viewportLength) {
                scrollPixels(end - viewportLength);
            }
        }
    }

    /**
     * Adjusts the cells such that the cell in the given index will be fully visible in
     * the viewport.
     * @param index the index
     */
    public void scrollTo(int index) {
        T cell = sheet.getVisibleCell(index);
        if (cell != null) {
            scrollTo(cell);
        } else {
            // see JDK-8197536
            if (scrollOneCell(index, true)) {
                return;
            } else if (scrollOneCell(index, false)) {
                return;
            }

            adjustPositionToIndex(index);
            sheet.addAllToPile();
            requestLayout();
        }
    }

    // will return true if scroll is successful
    private boolean scrollOneCell(int targetIndex, boolean downOrRight) {
        // if going down, cell diff is -1, because it will get the target cell index and check if previous
        // cell is visible to base the position
        int indexDiff = downOrRight ? -1 : 1;

        T targetCell = sheet.getVisibleCell(targetIndex + indexDiff);
        if (targetCell != null) {
            T cell = getAvailableOrCreateCell(targetIndex);
            setMaxPrefBreadth(Math.max(getMaxPrefBreadth(), getCellWidth(cell)));
            cell.setVisible(true);
            if (downOrRight) {
                sheet.addLast(cell);
                scrollPixels(getCellHeight(cell));
            } else {
                // up or left
                sheet.addFirst(cell);
                scrollPixels(-getCellHeight(cell));
            }
            return true;
        }

        return false;
    }

    /**
     * Adjusts the cells such that the cell in the given index will be fully visible in
     * the viewport, and positioned at the very top of the viewport.
     * @param index the index
     */
    public void scrollToTop(int index) {
        boolean posSet = false;

        if (index > getItemsCount() - 1) {
            setPosition(1);
            posSet = true;
        } else if (index < 0) {
            setPosition(0);
            posSet = true;
        }

        if (! posSet) {
            adjustPositionToIndex(index);
        }

        requestLayout();
    }

//    //TODO We assume all the cell have the same length.  We will need to support
//    // cells of different lengths.
//    public void scrollToOffset(int offset) {
//        scrollPixels(offset * getCellLength(0));
//    }

    /**
     * Given a delta value representing a number of pixels, this method attempts
     * to move the VirtualFlow in the given direction (positive is down/right,
     * negative is up/left) the given number of pixels. It returns the number of
     * pixels actually moved.
     * @param delta the delta value
     * @return the number of pixels actually moved
     */
    private boolean scrollAtEightExtremity(final double delta){
        final boolean isVertical = isVertical();
        if (((isVertical && (tempVisibility ? !needLengthBar : !vbar.isVisible())) ||
                (! isVertical && (tempVisibility ? !needLengthBar : !hbar.isVisible())))) return true;

        double pos = getPosition();
        if (pos == 0.0f && delta < 0) return true;
        if (pos == 1.0f && delta > 0) return true;

        if (pos == getPosition()) {
            // The pos hasn't changed, there's nothing to do. This is likely
            // to occur when we hit either extremity
            return true;
        }

        return false;
    }

    private void layoutCells(){
        for (int i = 0; i < sheet.size(); i++) {
            T cell = sheet.get(i);
            assert cell != null;
            Point2D p = getCellPosition(cell);
            positionCell(cell,p.getX(), p.getY());
            updateCellCacheSize(cell);
        }
    }
    private void shiftCellsVertical(double layoutY){
        for (int i = 0; i < sheet.size(); i++) {
            T cell = sheet.get(i);
            assert cell != null;
            Point2D p = getCellPosition(cell);
            double actualLayoutY = p.getY();
            if (Math.abs(actualLayoutY - layoutY) > 0.001) {
                // we need to shift the cell to layoutY
                positionCell(cell, p.getX(), p.getY() - layoutY);
                updateCellCacheSize(cell);
            }

            layoutY += getCellHeight(cell);
        }
    }
    public double scrollPixels(final double delta) {
        // Short cut this method for cases where nothing should be done
        if (delta == 0) {return 0;}

       if(scrollAtEightExtremity(delta)){
           return  0;
       }

        recalculateEstimatedSize();

        double adjusted = adjustPositionByPixelAmount(delta);

        // Now move stuff around. Translating by pixels fundamentally means
        // moving the cells by the delta. However, after having
        // done that, we need to go through the cells and see which cells,
        // after adding in the translation factor, now fall off the viewport.
        // Also, we need to add cells as appropriate to the end (or beginning,
        // depending on the direction of travel).
        //
        // One simplifying assumption (that had better be true!) is that we
        // will only make it this far in the function if the virtual scroll
        // bar is visible. Otherwise, we never will pixel scroll. So as we go,
        // if we find that the maxPrefBreadth exceeds the viewportBreadth,
        // then we will be sure to show the breadthBar and update it
        // accordingly.
//        if (cells.size() > 0) {
//
//            layoutCells();
//
//            // Fix for RT-32908
//            T firstCell = cells.getFirst();
//            double layoutY = firstCell == null ? 0 : getCellPosition(firstCell).getY();
//            shiftCellsVertical(layoutY);
//            // end of fix for RT-32908
//            cull();
//            addLeadingCellsIfNecessary();
//            // Starting at the tail of the list, loop adding cells until
//            // all the space on the table is filled up. We want to make
//            // sure that we DO NOT add empty trailing cells (since we are
//            // in the full virtual case and so there are no trailing empty
//            // cells).
//            if (! addTrailingCells(false)) {
//                // Reached the end, but not enough cells to fill up to
//                // the end. So, remove the trailing empty space, and translate
//                // the cells down
//
//                final T lastCell = getLastVisibleCell();
//                final double lastCellSize = getCellLength(lastCell);
//                final double cellEnd = getCellPosition(lastCell).getY() + lastCellSize;
//                final double viewportLength = getViewportLength();
//
//                if (cellEnd < viewportLength) {
//                    // Reposition the nodes
//                    double emptySize = viewportLength - cellEnd;
//                    for (int i = 0; i < cells.size(); i++) {
//                        T cell = cells.get(i);
//                        Point2D p = getCellPosition(cell);
//                        positionCell(cell, p.getX(), p.getY() );
//                    }
//                    setPosition(1.0f);
//                    // fill the leading empty space
//                    firstCell = cells.getFirst();
//                    int firstIndex = firstCell.getIndex();
//                    double prevIndexSize = getCellLength(firstIndex - 1);
//                    addLeadingCells(firstIndex - 1, getCellPosition(firstCell).getY() - prevIndexSize);
//                }
//            }
//        }

        // Now throw away any cells that don't fit
//        cull();

        // Finally, update the scroll bars
//        updateScrollBarsAndCells(false);
//        lastPosition = getPosition();

        // notify
        return adjusted;
    }

    private void addLeadingCellsIfNecessary() {
        T firstCell = sheet.getFirst();

        // Add any necessary leading cells
        if (firstCell != null) {
            int firstIndex = firstCell.getIndex();
            double prevIndexSize = getCellHeight(firstIndex - 1);
            Point2D p = getCellPosition(firstCell);
            addLeadingCells(firstIndex - 1);
        } else {
            int currentIndex = computeCurrentIndex();

            // The distance from the top of the viewport to the top of the
            // cell for the current index.
//            double offset = -computeViewportOffset(getPosition());

            // Add all the leading and trailing cells (the call to add leading
            // cells will add the current cell as well -- that is, the one that
            // represents the current position on the mapper).
            addLeadingCells(currentIndex);
        }
    }


    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        double w = isVertical() ? getPrefBreadth(height) : getPrefLength();
        return w + vbar.prefWidth(-1);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width) {
        double h = isVertical() ? getPrefLength() : getPrefBreadth(width);
        return h + hbar.prefHeight(-1);
    }

    /**
     * Return a cell for the given index. This may be called for any cell,
     * including beyond the range defined by cellCount, in which case an
     * empty cell will be returned. The returned value should not be stored for
     * any reason.
     * @param index the index
     * @return the cell
     */
    public T getCell(int index){
        T cell = sheet.getAvailableCell(index);
        if(cell != null){
            return cell;
        }
        // We need to use the accumCell and return that
        return createOrUseAccumCell(index);

    }
    private T createOrUseAccumCell(int index){
        if (accumCell == null) {
            Callback<VirtualFlow<T>,T> cellFactory = getCellFactory();
            if (cellFactory != null) {
                accumCell = cellFactory.call(this);
                accumCell.getProperties().put(NEW_CELL, null);
                accumCellParent.getChildren().setAll(accumCell);

                // Note the screen reader will attempt to find all
                // the items inside the view to calculate the item count.
                // Having items under different parents (sheet and accumCellParent)
                // leads the screen reader to compute wrong values.
                // The regular scheme to provide items to the screen reader
                // uses getPrivateCell(), which places the item in the sheet.
                // The accumCell, and its children, should be ignored by the
                // screen reader.
                accumCell.setAccessibleRole(AccessibleRole.NODE);
                accumCell.getChildrenUnmodifiable().addListener((Observable c) -> {
                    for (Node n : accumCell.getChildrenUnmodifiable()) {
                        n.setAccessibleRole(AccessibleRole.NODE);
                    }
                });
            }
        }
        setCellIndex(accumCell, index);
        return accumCell;
    }

    /**
     * The VirtualFlow uses this method to set a cells index (rather than calling
     * {@link FlowIndexedCell#updateIndex(int)} directly), so it is a perfect place
     * for subclasses to override if this if of interest.
     *
     * @param cell The cell whose index will be updated.
     * @param index The new index for the cell.
     */
    protected void setCellIndex(T cell, int index) {
        assert cell != null;

        cell.updateIndex(index);

        // make sure the cell is sized correctly. This is important for both
        // general layout of cells in a VirtualFlow, but also in cases such as
        // RT-34333, where the sizes were being reported incorrectly to the
        // ComboBox popup.
        if ((cell.isNeedsLayout() && cell.getScene() != null) || cell.getProperties().containsKey(NEW_CELL)) {
            cell.applyCss();
            cell.getProperties().remove(NEW_CELL);
        }
    }





    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns the scroll bar used for scrolling horizontally. A developer who needs to be notified when a scroll is
     * happening could attach a listener to the {@link ScrollBar#valueProperty()}.
     *
     * @return the scroll bar used for scrolling horizontally
     * @since 12
     */
    public final ScrollBar getHbar() {
        return hbar;
    }

    /**
     * Returns the scroll bar used for scrolling vertically. A developer who needs to be notified when a scroll is
     * happening could attach a listener to the {@link ScrollBar#valueProperty()}. The {@link ScrollBar#getWidth()} is
     * also useful when adding a component over the {@code TableView} in order to clip it so that it doesn't overlap the
     * {@code ScrollBar}.
     *
     * @return the scroll bar used for scrolling vertically
     * @since 12
     */
    public final ScrollBar getVbar() {
        return vbar;
    }

    /**
     * The maximum preferred size in the non-virtual direction. For example,
     * if vertical, then this is the max pref width of all cells encountered.
     * <p>
     * In general, this is the largest preferred size in the non-virtual
     * direction that we have ever encountered. We don't reduce this size
     * unless instructed to do so, so as to reduce the amount of scroll bar
     * jitter. The access on this variable is package ONLY FOR TESTING.
     */
    private double maxPrefBreadth;
    private final void setMaxPrefBreadth(double value) {
        this.maxPrefBreadth = value;
    }
    final double getMaxPrefBreadth() {
        return maxPrefBreadth;
    }

    /**
     * Compute and return the length of the cell for the given index. This is
     * called both internally when adjusting by pixels, and also at times
     * by PositionMapper (see the getItemSize callback). When called by
     * PositionMapper, it is possible that it will be called for some index
     * which is not associated with any cell, so we have to do a bit of work
     * to use a cell as a helper for computing cell size in some cases.
     */
    double getCellHeight(int index) {
        if (fixedCellSizeEnabled) {
            return getFixedCellSize();
        }

        T cell = getCell(index);
        double length = getCellHeight(cell);

        releaseCell(cell);
        return length;
    }

    /**
     */
    double getCellWidth(int index) {
        T cell = getCell(index);
        double b = getCellWidth(cell);
        releaseCell(cell);
        return b;
    }

    /**
     * Gets the length of a specific cell
     */
    double getCellHeight(T cell) {
        if (cell == null)
        {return 0;}
        if (fixedCellSizeEnabled) {return getFixedCellSize();};
        return cell.getLayoutBounds().getHeight();
    }

    /**
     * Gets the breadth of a specific cell
     */
    double getCellWidth(Cell cell) {
        return cell.prefWidth(-1);
    }

    private Point2D getCellPosition(T cell) {
        //vertical layout
        double viewPortWidth = sheet.getWidth();
        int index = cell.getIndex();
        double layoutX = 0;
        double layoutY = 0;
        double maxCellHeight = 0;

        int start = sheet.getFirst().getIndex();
        for (int i = start; i < index; i++) {
            Cell calCel = sheet.get(i);
            if(calCel == null){
                continue;
            }
            double prefWidth = Math.round(calCel.prefWidth(-1) + 0.5);
            double prefHeight = calCel.prefHeight(-1);

            if(maxCellHeight < prefHeight){
                maxCellHeight = prefHeight;
            }

            if((layoutX + prefWidth) < viewPortWidth) {
                //in the same row
                layoutX = layoutX + prefWidth;
            }
            else { //new row
                layoutX = 0;
                layoutY = layoutY + maxCellHeight;
                maxCellHeight = 0;
            }
        }

        Point2D p =  new Point2D(layoutX, layoutY);
        return p;
    }



    private void positionCell(T cell, double positionX,  double positionY) {
        cell.setLayoutX(snapSpaceX(positionX));
        cell.setLayoutY(snapSpaceY(positionY));

    }


    /**
     * Resizes the given cell. If {@link #isVertical()} is set to {@code true}, the cell width will be the maximum
     * between the viewport width and the sum of all the cells' {@code prefWidth}. The cell height will be computed by
     * the cell itself unless {@code fixedCellSizeEnabled} is set to {@code true}, then {@link #getFixedCellSize()} is
     * used. If {@link #isVertical()} is set to {@code false}, the width and height calculations are reversed.
     *
     * @param cell the cell to resize
     * @since 12
     */
    @Deprecated
    protected void resizeCell(T cell) {

        if (cell == null) return;

        if (isVertical()) {
            double width = Math.max(getMaxPrefBreadth(), sheet.getWidth());

            cell.resize(width, fixedCellSizeEnabled ? getFixedCellSize() : Utils.boundedSize(cell.prefHeight(width), cell.minHeight(width), cell.maxHeight(width)));
        } else {
            double height = Math.max(getMaxPrefBreadth(), sheet.getHeight());
            cell.resize(fixedCellSizeEnabled ? getFixedCellSize() : Utils.boundedSize(cell.prefWidth(height), cell.minWidth(height), cell.maxWidth(height)), height);
        }


    }

    /**
     * Returns the list of cells displayed in the current viewport.
     * <p>
     * The cells are ordered such that the first cell in this list is the first in the view, and the last cell is the
     * last in the view. When pixel scrolling, the list is simply shifted and items drop off the beginning or the end,
     * depending on the order of scrolling.
     *
     * @return the cells displayed in the current viewport
     * @since 12
     */

    /**
     * Returns the last visible cell whose bounds are entirely within the viewport. When manually inserting rows, one
     * may need to know which cell indices are visible in the viewport.
     *
     * @return last visible cell whose bounds are entirely within the viewport
     * @since 12
     */
    public T getLastVisibleCellWithinViewport() {
        if (sheet.isEmpty() || sheet.getHeight() <= 0) return null;

        T cell;
        final double max = sheet.getHeight();
        for (int i = sheet.size() - 1; i >= 0; i--) {
            cell = sheet.get(i);
            if (cell.isEmpty()) continue;

            final double cellStart = getCellPosition(cell).getY();
            final double cellEnd = cellStart + getCellHeight(cell);

            // we use the magic +2 to allow for a little bit of fuzziness,
            // this is to help in situations such as RT-34407
            if (cellEnd <= (max + 2)) {
                return cell;
            }
        }

        return null;
    }

    /**
     * Returns the first visible cell whose bounds are entirely within the viewport. When manually inserting rows, one
     * may need to know which cell indices are visible in the viewport.
     *
     * @return first visible cell whose bounds are entirely within the viewport
     * @since 12
     */
    public T getFirstVisibleCellWithinViewport() {
        if (sheet.isEmpty() || sheet.getHeight() <= 0) return null;

        T cell;
        for (int i = 0; i < sheet.size(); i++) {
            cell = sheet.get(i);
            if (cell.isEmpty()) continue;

            final double cellStartY = getCellPosition(cell).getY();
            if (cellStartY >= 0) {
                return cell;
            }
        }

        return null;
    }

    /**
     * Adds all the cells prior to and including the given currentIndex, until
     * no more can be added without falling off the flow. The startOffset
     * indicates the distance from the leading edge (top) of the viewport to
     * the leading edge (top) of the currentIndex.
     */
    void addLeadingCells(int currentIndex) {
        // The offset will keep track of the distance from the top of the
        // viewport to the top of the current index. We will increment it
        // as we lay out leading cells.
//        double offset = startOffset;
        // The index is the absolute index of the cell being laid out
        int index = currentIndex;

        // Offset should really be the bottom of the current index
        boolean first = true; // first time in, we just fudge the offset and let
        // it be the top of the current index then redefine
        // it as the bottom of the current index thereafter
        // while we have not yet laid out so many cells that they would fall
        // off the flow, we will continue to create and add cells. The
        // offset is our indication of whether we can lay out additional
        // cells. If the offset is ever < 0, except in the case of the very
        // first cell, then we must quit.
        T cell = null;

        // special case for the position == 1.0, skip adding last invisible cell
        if (index == getItemsCount() ) {
            index--;
            first = false;
        }
        while (index >= 0 && ( first)) {

            cell = getAvailableOrCreateCell(index);
            setCellIndex(cell, index);
            sheet.addFirst(cell);

            // A little gross but better than alternatives because it reduces
            // the number of times we have to update a cell or compute its
            // size. The first time into this loop "offset" is actually the
            // top of the current index. On all subsequent visits, it is the
            // bottom of the current index.
            if (first) {
                first = false;
            }
            // Position the cell, and update the maxPrefBreadth variable as we go.
            Point2D p = getCellPosition(cell);
            positionCell(cell, p.getX(), p.getY());
            updateCellCacheSize(cell);
            setMaxPrefBreadth(Math.max(getMaxPrefBreadth(), getCellWidth(cell)));
            cell.setVisible(true);
            --index;
        }

        // There are times when after laying out the cells we discover that
        // the top of the first cell which represents index 0 is below the top
        // of the viewport. In these cases, we have to adjust the cells up
        // and reset the mapper position. This might happen when items got
        // removed at the top or when the viewport size increased.
        if (sheet.size() > 0) {
            cell = sheet.getFirst();
            int firstIndex = cell.getIndex();
            double firstCellPos = getCellPosition(cell).getY();
            if (firstIndex == 0 && firstCellPos > 0) {
                setPosition(0.0f);

                for (int i = 0; i < sheet.size(); i++) {
                    cell = sheet.get(i);
                    Point2D p = getCellPosition(cell);
                    positionCell(cell, p.getX(), p.getY());
                    updateCellCacheSize(cell);

                }
            }
        } else {
            // reset scrollbar to top, so if the flow sees cells again it starts at the top
            vbar.setValue(0);
            hbar.setValue(0);
        }
    }

    /**
     * Adds all the trailing cells that come <em>after</em> the last index in
     * the cells ObservableList.
     */
    //TODO re-implement
    boolean addTrailingCells() {

        if (sheet.isEmpty()) {
            return false;
        }

        final double viewPortHeight = sheet.getHeight();
        T lastCell = sheet.getLast();

        Point2D pos = getCellPosition(lastCell);
        double offsetX = pos.getX() + getCellWidth(lastCell);
        double offsetY = pos.getY() + getCellHeight(lastCell);

        int nextIndex = lastCell.getIndex() + 1;
        final int itemCount = getItemsCount();
        boolean isEmptyCell = nextIndex <= itemCount;

        if ((offsetY < 0 )|| offsetY  > viewPortHeight) {
            return false;
        }


        final double maxCellCount = viewPortHeight;//cell size = 1


        while (offsetY < viewPortHeight) {
            if (nextIndex >= itemCount) {
                notifyIndexExceedsItemCount();
                return false;
            }

            if(nextIndex > maxCellCount) {
                notifyIndexExceedsMaximum();
                return false;
            }

            T cell = getAvailableOrCreateCell(nextIndex);
            addLastCellToSheet(cell);
            double cellBreadth = getCellWidth(cell);

            offsetX += cellBreadth;
            if(!isInRow(offsetX)){
                offsetY += getCellHeight(cell);
                offsetX = 0;
            }

            cell.setVisible(true);
            ++nextIndex;
        }

        return isEmptyCell;
    }

    private void notifyIndexExceedsItemCount() {
        final PlatformLogger logger = Logging.getControlsLogger();
        if (logger.isLoggable(PlatformLogger.Level.INFO)) {
            logger.info("index exceeds itemCount." );
        }
    }

    private boolean isInRow(double x){
        return x < (sheet.getWidth() - MAGIC_X);
    }

    private void addLastCellToSheet(T cell) {
        sheet.addLast(cell);
        Point2D p = getCellPosition(cell);
        positionCell(cell, p.getX(), p.getY());
        updateCellCacheSize(cell);
    }

    private void notifyIndexExceedsMaximum() {

            final PlatformLogger logger = Logging.getControlsLogger();
            if (logger.isLoggable(PlatformLogger.Level.INFO)) {
                logger.info("index exceeds maxCellCount. Check size calculations " );
            }

    }

    /**
     * Informs the {@code VirtualFlow} that a layout pass should be done, and the cell contents have not changed. For
     * example, this might be called from a {@code TableView} or {@code ListView} when a layout is needed and no cells
     * have been added or removed.
     *
     * @since 12
     */
    public void reconfigureCells() {
        needsReconfigureCells = true;
        requestLayout();
    }

    /**
     * Informs the {@code VirtualFlow} that a layout pass should be done, and that the cell factory has changed. All
     * cells in the viewport are recreated with the new cell factory.
     *
     * @since 12
     */
    protected void recreateCells() {
        needsRecreateCells = true;
        requestLayout();
    }

    /**
     * Informs the {@code VirtualFlow} that a layout pass should be done, and cell contents have changed. All cells are
     * removed and then added properly in the viewport.
     *
     * @since 12
     */
    public void rebuildCells() {
        needsRebuildCells = true;
        requestLayout();
    }



    void setCellDirty(int index) {
        dirtyCells.set(index);
        requestLayout();
    }

    private void startSBReleasedAnimation() {
        if (sbTouchTimeline == null) {
            /*
            ** timeline to leave the scrollbars visible for a short
            ** while after a scroll/drag
            */
            sbTouchTimeline = new Timeline();
            sbTouchKF1 = new KeyFrame(Duration.millis(0), event -> {
                tempVisibility = true;
                requestLayout();
            });

            sbTouchKF2 = new KeyFrame(Duration.millis(1000), event -> {
                if (touchDetected == false && mouseDown == false) {
                    tempVisibility = false;
                    requestLayout();
                }
            });
            sbTouchTimeline.getKeyFrames().addAll(sbTouchKF1, sbTouchKF2);
        }
        sbTouchTimeline.playFromStart();
    }

    private void scrollBarOn() {
        tempVisibility = true;
        requestLayout();
    }

    void updateHbar() {
        if (! isVisible() || getScene() == null){
            return;
        }
        // Bring the clipView.clipX back to 0 if control is vertical or
        // the hbar isn't visible (fix for RT-11666)
        if (isVertical()) {
            if (needBreadthBar) {
                clipView.setClipX(hbar.getValue());
            } else {
                // all cells are now less than the width of the flow,
                // so we should shift the hbar/clip such that
                // everything is visible in the viewport.
                clipView.setClipX(0);
                hbar.setValue(0);
            }
        }
    }

    /**
     * @return true if bar visibility changed
     */
    private boolean computeBarVisibility() {
        if (sheet.isEmpty()) {
            // In case no cells are set yet, we assume no bars are needed
            needLengthBar = false;
            needBreadthBar = false;
            return true;
        }

        final boolean isVertical = isVertical();
        boolean barVisibilityChanged = false;

        VirtualScrollBar breadthBar = isVertical ? hbar : vbar;
        VirtualScrollBar lengthBar = isVertical ? vbar : hbar;

        final double viewportBreadth = sheet.getWidth();

        final int cellsSize = sheet.size();
        final int cellCount = getItemsCount();
        for (int i = 0; i < 2; i++) {
            Point2D pos = getCellPosition(sheet.getLast());
            final boolean lengthBarVisible = getPosition() > 0
                    || cellCount > cellsSize
                    || (cellCount == cellsSize && (pos.getY() + getCellHeight(sheet.getLast())) > sheet.getHeight())
                    || (cellCount == cellsSize - 1 && barVisibilityChanged && needBreadthBar);

            if (lengthBarVisible ^ needLengthBar) {
                needLengthBar = lengthBarVisible;
                barVisibilityChanged = true;
            }

            // second conditional removed for RT-36669.
            final boolean breadthBarVisible = (maxPrefBreadth > viewportBreadth);// || (needLengthBar && maxPrefBreadth > (viewportBreadth - lengthBarBreadth));
            if (breadthBarVisible ^ needBreadthBar) {
                needBreadthBar = breadthBarVisible;
                barVisibilityChanged = true;
            }
        }

        // Start by optimistically deciding whether the length bar and
        // breadth bar are needed and adjust the viewport dimensions
        // accordingly. If during layout we find that one or the other of the
        // bars actually is needed, then we will perform a cleanup pass

        if (!Properties.IS_TOUCH_SUPPORTED) {
            updateViewportDimensions();
            breadthBar.setVisible(needBreadthBar);
            lengthBar.setVisible(needLengthBar);
        } else {
            breadthBar.setVisible(needBreadthBar && tempVisibility);
            lengthBar.setVisible(needLengthBar && tempVisibility);
        }
        return barVisibilityChanged;
    }

    private void updateViewportDimensions() {
        final boolean isVertical = isVertical();
        final double breadthBarLength = isVertical ? snapSizeY(hbar.prefHeight(-1)) : snapSizeX(vbar.prefWidth(-1));
        final double lengthBarBreadth = isVertical ? snapSizeX(vbar.prefWidth(-1)) : snapSizeY(hbar.prefHeight(-1));

        if (!Properties.IS_TOUCH_SUPPORTED) {
            sheet.setWidth((isVertical ? getWidth() : getHeight()) - (needLengthBar ? lengthBarBreadth : 0));
            sheet.setHeight((isVertical ? getHeight() : getWidth()) - (needBreadthBar ? breadthBarLength : 0));
        } else {
            sheet.setWidth((isVertical ? getWidth() : getHeight()));
            sheet.setHeight((isVertical ? getHeight() : getWidth()));
        }
        synchronizeAbsoluteOffsetWithPosition();
    }



    private void initViewport() {
        // Initialize the viewportLength and viewportBreadth to match the
        // width/height of the flow
        final boolean isVertical = isVertical();

        updateViewportDimensions();

        VirtualScrollBar breadthBar = isVertical ? hbar : vbar;
        VirtualScrollBar lengthBar = isVertical ? vbar : hbar;

        // If there has been a switch between the virtualized bar, then we
        // will want to do some stuff TODO.
        breadthBar.setVirtual(false);
        lengthBar.setVirtual(true);
    }

    private void updateScrollBarsAndCells(boolean recreate) {
        // Assign the hbar and vbar to the breadthBar and lengthBar so as
        // to make some subsequent calculations easier.
        final boolean isVertical = isVertical();
        VirtualScrollBar breadthBar = isVertical ? hbar : vbar;
        VirtualScrollBar lengthBar = isVertical ? vbar : hbar;

        // We may have adjusted the viewport length and breadth after the
        // layout due to scroll bars becoming visible. So we need to perform
        // a follow up pass and resize and shift all the cells to fit the
        // viewport. Note that the prospective viewport size is always >= the
        // final viewport size, so we don't have to worry about adding
        // cells during this cleanup phase.
//        fitCells();

        // Update cell positions.
        // When rebuilding the cells, we add the cells and along the way compute
        // the maxPrefBreadth. Based on the computed value, we may add
        // the breadth scrollbar which changes viewport length, so we need
        // to re-position the cells.
        if (!sheet.isEmpty()) {

            final int currIndex = computeCurrentIndex() - sheet.getFirst().getIndex();
            final int size = sheet.size();

            for (int i = currIndex - 1; i >= 0 && i < size; i--) {
                final T cell = sheet.get(i);
                Point2D pos = getCellPosition(cell);
                positionCell(cell, pos.getX(), pos.getY());
                updateCellCacheSize(cell);
            }

            // position trailing cells
            for (int i = currIndex; i >= 0 && i < size; i++) {
                final T cell = sheet.get(i);
                Point2D pos = getCellPosition(cell);
                positionCell(cell, pos.getX(), pos.getY());
                updateCellCacheSize(cell);

            }
        }

        // Toggle visibility on the corner
        corner.setVisible(breadthBar.isVisible() && lengthBar.isVisible());

        double sumCellLength = 0;
        double flowLength = (isVertical ? getHeight() : getWidth()) -
                (breadthBar.isVisible() ? breadthBar.prefHeight(-1) : 0);

        final double viewportBreadth = sheet.getWidth();
        final double viewportLength = sheet.getHeight();

        // Now position and update the scroll bars
        if (breadthBar.isVisible()) {
            /*
            ** Positioning the ScrollBar
            */
            if (!Properties.IS_TOUCH_SUPPORTED) {
                if (isVertical) {
                    hbar.resizeRelocate(0, viewportLength,
                            viewportBreadth, hbar.prefHeight(viewportBreadth));
                } else {
                    vbar.resizeRelocate(viewportLength, 0,
                            vbar.prefWidth(viewportBreadth), viewportBreadth);
                }
            }
            else {
                if (isVertical) {
                    double prefHeight = hbar.prefHeight(viewportBreadth);
                    hbar.resizeRelocate(0, viewportLength - prefHeight,
                            viewportBreadth, prefHeight);
                } else {
                    double prefWidth = vbar.prefWidth(viewportBreadth);
                    vbar.resizeRelocate(viewportLength - prefWidth, 0,
                            prefWidth, viewportBreadth);
                }
            }

            if (getMaxPrefBreadth() != -1) {
                double newMax = Math.max(1, getMaxPrefBreadth() - viewportBreadth);
                if (newMax != breadthBar.getMax()) {
                    breadthBar.setMax(newMax);

                    double breadthBarValue = breadthBar.getValue();
                    boolean maxed = breadthBarValue != 0 && newMax == breadthBarValue;
                    if (maxed || breadthBarValue > newMax) {
                        breadthBar.setValue(newMax);
                    }

                    breadthBar.setVisibleAmount((viewportBreadth / getMaxPrefBreadth()) * newMax);
                }
            }
        }

        // determine how many cells there are on screen so that the scrollbar
        // thumb can be appropriately sized
        if (recreate && (lengthBar.isVisible() || Properties.IS_TOUCH_SUPPORTED)) {
            final int cellCount = getItemsCount();
            int numCellsVisibleOnScreen = 0;
            for (int i = 0, max = sheet.size(); i < max; i++) {
                T cell = sheet.get(i);
                if (cell != null && !cell.isEmpty()) {
                    sumCellLength += (isVertical ? cell.getHeight() : cell.getWidth());
                    if (sumCellLength > flowLength) {
                        break;
                    }

                    numCellsVisibleOnScreen++;
                }
            }

            lengthBar.setMax(1);
            if (numCellsVisibleOnScreen == 0 && cellCount == 1) {
                // special case to help resolve RT-17701 and the case where we have
                // only a single row and it is bigger than the viewport
                lengthBar.setVisibleAmount(flowLength / sumCellLength);
            } else {
                lengthBar.setVisibleAmount(viewportLength / estimatedSize);
            }
        }

        if (lengthBar.isVisible()) {
            // Fix for RT-11873. If this isn't here, we can have a situation where
            // the scrollbar scrolls endlessly. This is possible when the cell
            // count grows as the user hits the maximal position on the scrollbar
            // (i.e. the list size dynamically grows as the user needs more).
            //
            // This code was commented out to resolve RT-14477 after testing
            // whether RT-11873 can be recreated. It could not, and therefore
            // for now this code will remained uncommented until it is deleted
            // following further testing.
//            if (lengthBar.getValue() == 1.0 && lastCellCount != cellCount) {
//                lengthBar.setValue(0.99);
//            }

            /*
            ** Positioning the ScrollBar
            */
            if (!Properties.IS_TOUCH_SUPPORTED) {
                if (isVertical) {
                    vbar.resizeRelocate(viewportBreadth, 0, vbar.prefWidth(viewportLength), viewportLength);
                } else {
                    hbar.resizeRelocate(0, viewportBreadth, viewportLength, hbar.prefHeight(-1));
                }
            }
            else {
                if (isVertical) {
                    double prefWidth = vbar.prefWidth(viewportLength);
                    vbar.resizeRelocate(viewportBreadth - prefWidth, 0, prefWidth, viewportLength);
                } else {
                    double prefHeight = hbar.prefHeight(-1);
                    hbar.resizeRelocate(0, viewportBreadth - prefHeight, viewportLength, prefHeight);
                }
            }
        }

        if (corner.isVisible()) {
            if (!Properties.IS_TOUCH_SUPPORTED) {
                corner.resize(vbar.getWidth(), hbar.getHeight());
                corner.relocate(hbar.getLayoutX() + hbar.getWidth(), vbar.getLayoutY() + vbar.getHeight());
            }
            else {
                corner.resize(vbar.getWidth(), hbar.getHeight());
                corner.relocate(hbar.getLayoutX() + (hbar.getWidth()-vbar.getWidth()), vbar.getLayoutY() + (vbar.getHeight()-hbar.getHeight()));
                hbar.resize(hbar.getWidth()-vbar.getWidth(), hbar.getHeight());
                vbar.resize(vbar.getWidth(), vbar.getHeight()-hbar.getHeight());
            }
        }

        clipView.resize(snapSizeX(isVertical ? viewportBreadth : viewportLength),
                        snapSizeY(isVertical ? viewportLength : viewportBreadth));

        // If the viewportLength becomes large enough that all cells fit
        // within the viewport, then we want to update the value to match.
        if (getPosition() != lengthBar.getValue()) {
            lengthBar.setValue(getPosition());
        }
    }



    private void cull() {
        final double viewportLength = sheet.getHeight();
        for (int i = sheet.size() - 1; i >= 0; i--) {
            T cell = sheet.get(i);
            double cellSize = getCellHeight(cell);
            Point2D cellStart = getCellPosition(cell);
            double cellEnd = cellStart.getY() + cellSize;
            if (cellStart.getY() >= viewportLength || cellEnd < 0) {
                sheet.addToPile(sheet.remove(i));
            }
        }
    }

    /**
     * After using the accum cell, it needs to be released!
     */
    private void releaseCell(T cell) {
        if (accumCell != null && cell == accumCell) {
            accumCell.setVisible(false);
            accumCell.updateIndex(-1);
        }
    }


    /**
     * Creates and returns a new cell for the given index.
     * <p>
     * If the requested index is not already an existing visible cell, it will create a cell for the given index and
     * insert it into the {@code VirtualFlow} container. If the index exists, simply returns the visible cell. From that
     * point on, it will be unmanaged, and is up to the caller of this method to manage it.
     * <p>
     * This is useful if a row that should not be visible must be accessed (a row that always stick to the top for
     * example). It can then be easily created, correctly initialized and inserted in the {@code VirtualFlow}
     * container.
     *
     * @param index the cell index
     * @return a cell for the given index inserted in the VirtualFlow container
     * @since 12
     */
    public T getPrivateCell(int index)  {
        T cell = null;

        // If there are cells, then we will attempt to get an existing cell
        if (! sheet.isEmpty()) {
            // First check the cells that have already been created and are
            // in use. If this call returns a value, then we can use it
            cell = sheet.getVisibleCell(index);
            if (cell != null) {
                // Force the underlying text inside the cell to be updated
                // so that when the screen reader runs, it will match the
                // text in the cell (force updateDisplayedText())
                cell.layout();
                return cell;
            }
        }

        // check the existing sheet children
        if (cell == null) {
            for (int i = 0; i < sheet.getChildren().size(); i++) {
                T _cell = (T) sheet.getChildren().get(i);
                if (_cell.getIndex() == index) {
                    return _cell;
                }
            }
        }

        Callback<VirtualFlow<T>, T> cellFactory = getCellFactory();
        if (cellFactory != null) {
            cell = cellFactory.call(this);
        }

        if (cell != null) {
            setCellIndex(cell, index);
//            resizeCell(cell);
            cell.setVisible(false);
            sheet.getChildren().add(cell);
            privateCells.add(cell);
        }

        return cell;
    }

    private final List<T> privateCells = new ArrayList<>();

    private void releaseAllPrivateCells() {
        sheet.getChildren().removeAll(privateCells);
        privateCells.clear();
    }






    private double getPrefBreadth(double oppDimension) {
        double max = getMaxCellWidth(10);

        // This primarily exists for the case where we do not want the breadth
        // to grow to ensure a golden ratio between width and height (for example,
        // when a ListView is used in a ComboBox - the width should not grow
        // just because items are being added to the ListView)
        if (oppDimension > -1) {
            double prefLength = getPrefLength();
            max = Math.max(max, prefLength * GOLDEN_RATIO_MULTIPLIER);
        }

        return max;
    }

    private double getPrefLength() {
        double sum = 0.0;
        int rows = Math.min(10, getItemsCount());
        for (int i = 0; i < rows; i++) {
            sum += getCellHeight(i);
        }
        return sum;
    }

    double getMaxCellWidth(int rowsToCount) {
        double max = 0.0;

        // we always measure at least one row
        int rows = Math.max(1, rowsToCount == -1 ? getItemsCount() : rowsToCount);
        for (int i = 0; i < rows; i++) {
            max = Math.max(max, getCellWidth(i));
        }
        return max;
    }

    // Old PositionMapper
    /**
     * Given a position value between 0 and 1, compute and return the viewport
     * offset from the "current" cell associated with that position value.
     * That is, if the return value of this function where used as a translation
     * factor for a sheet that contained all the items, then the current
     * item would end up positioned correctly.
     * We calculate the total size until the absoluteOffset is reached.
     * For this calculation, we use the cached sizes for each item, or an
     * educated guess in case we don't have a cached size yet. While we could
     * fill the cache with the size here, we do not do it as it will affect
     * performance.
     */
    //TODO need to re-implement
    private double computeViewportOffset(double position) {
        double p = com.sun.javafx.util.Utils.clamp(0, position, 1);
        double bound = 0d;
        double estSize = estimatedSize / getItemsCount();

        for (int i = 0; i < getItemsCount(); i++) {
            double[] size = getCellSize(i);
            if (size == null){
                size = new double[2];
                size[1] = estSize;
            }
            if (bound + size[1] > absoluteOffset) {
                return absoluteOffset - bound;
            }
            bound += size[1];
        }
        return 0d;
    }
    //TODO need to re-implement
    private void adjustPositionToIndex(int index) {
        int cellCount = getItemsCount();
        if (cellCount <= 0) {
            setPosition(0.0f);
        } else {
            double targetOffset = 0;
            double estSize = estimatedSize/cellCount;
            for (int i = 0; i < index; i++) {
                double[] cz = getCellSize(i);
                if (cz[1] < 0) cz[1] = estSize;
                targetOffset = targetOffset+ cz[1];
            }
            this.absoluteOffset = targetOffset;
            synchronizePositionWithAbsoluteOffset();
        }

    }
    /**
     * Adjust the position based on a delta of pixels. If negative, then the
     * position will be adjusted negatively. If positive, then the position will
     * be adjusted positively. If the pixel amount is too great for the range of
     * the position, then it will be clamped such that position is always
     * strictly between 0 and 1
     * @return the actual number of pixels that have been applied
     */
    private double adjustPositionByPixelAmount(double numPixels) {
        if (numPixels == 0) return 0;
        // When we're at the top already, we can't move back further, unless we
        // want to allow for gravity-alike effects.
        if ((absoluteOffset <= 0) && (numPixels < 0)) return 0;

        // start with applying the requested modification
        double origAbsoluteOffset = this.absoluteOffset;
        this.absoluteOffset = Math.max(0.d, this.absoluteOffset + numPixels);
        double newPosition = Math.min(1.0d, absoluteOffset / (estimatedSize - sheet.getHeight()));
        // estimatedSize changes may result in opposite effect on position
        // in that case, modify current position 1% in the requested direction
        if ((numPixels > 0) && (newPosition < getPosition())) {
            newPosition = getPosition()*1.01;
        }
        if ((numPixels < 0) && (newPosition > getPosition())) {
            newPosition = getPosition()*.99;
        }

        // once at 95% of the total estimated size, we want a correct size, not
        // an estimated size anymore.
        if (newPosition > .95) {
            int cci = computeCurrentIndex();
            while (cci < getItemsCount()) {
                getOrCreateCellSize(cci); cci++;
            }
            recalculateEstimatedSize();
        }


        setPosition(newPosition);
        return absoluteOffset - origAbsoluteOffset;

    }
    //TODO need to re-implement
    private int computeCurrentIndex() {
        double total = 0;
        int currentCellCount = getItemsCount();
        double estSize = estimatedSize / currentCellCount;
        int index = -1;

        for (int i = 0; i < currentCellCount; i++) {
            double[] nextSize = getCellSize(i);
            if (nextSize == null) {
                nextSize = new double[]{0d, estSize};
            }
            total = total + nextSize[1];
            if (total > absoluteOffset) {
                index =  i;
                break;
            }
        }
        if(index == -1)
        index =  currentCellCount == 0 ? 0 : currentCellCount - 1;
        return index;
    }

    /**
     * Given an item index, this function will compute and return the viewport
     * offset from the beginning of the specified item. Notice that because each
     * item has the same percentage of the position dedicated to it, and since
     * we are measuring from the start of each item, this is a very simple
     * calculation.
     */
    private double computeOffsetForCell(int itemIndex) {
        double cellCount = getItemsCount();
        double p = com.sun.javafx.util.Utils.clamp(0, itemIndex, cellCount) / cellCount;
        return -(sheet.getHeight() * p);
    }

    double[] getCellSize(int idx) {
        return getOrCreateCellSize(idx, false);
    }

    /**
     * Get the size of the considered element.
     * If the requested element has a size that is not yet in the cache,
     * it will be computed and cached now.
     * @return the size of the element; or 1 in case there are no cells yet
     */
    double[] getOrCreateCellSize(int idx) {
        return getOrCreateCellSize (idx, true);
    }

    private double[] getOrCreateCellSize (int idx, boolean create) {
        // is the current cache long enough to contain idx?
        if (itemSizeCache.size() > idx) {
            // is there a non-null value stored in the cache?
            if (itemSizeCache.get(idx) != null) {
                return itemSizeCache.get(idx);
            }
        }
        if (!create) return null;
        boolean doRelease = false;

        // Do we have a visible cell for this index?
        T cell = sheet.getVisibleCell(idx);
        if (cell == null) { // we might get the accumcell here
            cell = getCell(idx);
            doRelease = true;
        }
        // Make sure we have enough space in the cache to store this index
        while (idx >= itemSizeCache.size()) {
            itemSizeCache.add(itemSizeCache.size(), null);
        }

        // if we have a valid cell, we can populate the cache
        double[] answer = new double[2];

        answer[0] = cell.getLayoutBounds().getWidth();
        answer[1] = cell.getLayoutBounds().getHeight();

        itemSizeCache.set(idx, answer);

        if (doRelease) { // we need to release the accumcell
            releaseCell(cell);
        }
        return answer;
    }

    /**
     * Update the size of a specific cell.
     * If this cell was already in the cache, its old value is replaced by the
     * new size.
     * @param cell
     */
    void updateCellCacheSize(T cell) {
        int cellIndex = cell.getIndex();
        if (itemSizeCache.size() > cellIndex) {

            double newW = cell.getLayoutBounds().getWidth();
            double newH = cell.getLayoutBounds().getHeight();

            double[] size = itemSizeCache.get(cellIndex);
            if(size == null){
                size = new double[]{newW, newH};
            }
            else {
                size[0] = newW;
                size[1] = newH;
            }
            itemSizeCache.set(cellIndex, size);

        }
    }

    /**
     * Recalculate the estimated size for this list based on what we have in the
     * cache.
     */
    private void recalculateEstimatedSize() {
        recalculateAndImproveEstimatedSize(DEFAULT_IMPROVEMENT);
    }


    private void recalculateAndImproveEstimatedSize(int improve) {
        int itemCount = getItemsCount();
        int added = 0;
        while ((itemCount > itemSizeCache.size()) && (added < improve)) {
            getOrCreateCellSize(itemSizeCache.size());
            added++;
        }
        int cacheCount = itemSizeCache.size();
        double totalX = 0d;
        double totalY = 0d;
//        int count = 0;
        int i = 0;
        for (; (i < itemCount && i < cacheCount); i++) {
            double[] size = itemSizeCache.get(i);
            if (size != null) {
                totalX = totalX + size[0];
//                count++;
                if(!isInRow(totalX)) {
                    totalY = totalY + size[1];
                    totalX = 0;
                }
            }
        }

        this.estimatedSize = i == 0 ? 1d: totalY * itemCount / i;

    }

    private void resetSizeEstimates() {
        itemSizeCache.clear();
        this.estimatedSize = 1d;
    }






    /* *************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/

    /**
     * A simple extension to Region that ensures that anything wanting to flow
     * outside of the bounds of the Region is clipped.
     */
    static class ClippedContainer extends Region {

        /**
         * The Node which is embedded within this {@code ClipView}.
         */
        private Node node;
        public Node getNode() { return this.node; }
        public void setNode(Node n) {
            this.node = n;

            getChildren().clear();
            getChildren().add(node);
        }

        public void setClipX(double clipX) {
            setLayoutX(-clipX);
            clipRect.setLayoutX(clipX);
        }

        public void setClipY(double clipY) {
            setLayoutY(-clipY);
            clipRect.setLayoutY(clipY);
        }

        private final Rectangle clipRect;

        public ClippedContainer(final VirtualFlow<?> flow) {
            if (flow == null) {
                throw new IllegalArgumentException("VirtualFlow can not be null");
            }

            getStyleClass().add("clipped-container");

            // clipping
            clipRect = new Rectangle();
            clipRect.setSmooth(false);
            setClip(clipRect);
            // --- clipping

            super.widthProperty().addListener(valueModel -> {
                clipRect.setWidth(getWidth());
            });
            super.heightProperty().addListener(valueModel -> {
                clipRect.setHeight(getHeight());
            });
        }
    }


}
