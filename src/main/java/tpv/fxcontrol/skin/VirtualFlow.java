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
import com.sun.javafx.scene.traversal.Algorithm;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.ParentTraversalEngine;
import com.sun.javafx.scene.traversal.TraversalContext;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.util.Duration;
import com.sun.javafx.logging.PlatformLogger;
import tpv.fxcontrol.FlowIndexedCell;


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

    private static final int ROW_SAMPLE_NUMBER = 10;

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
     * The scroll bar used to scrolling vertically. This has package access
     * ONLY for testing.
     */
    private VirtualScrollBar vbar;

    /**
     * Control in which the cell's sheet is placed and forms the viewport. The
     * viewportBreadth and viewportLength are simply the dimensions of the
     * clipView. This has package access ONLY for testing.
     */
    ClippedContainer clipView;



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




    // used for panning the virtual flow
    private double lastY;
    private boolean isPanning = false;

    private boolean fixedCellSizeEnabled = false;
    private boolean needsReconfigureCells = false; // when cell contents are the same
    private boolean needsRecreateCells = false; // when cell factory changed
    private boolean needsRebuildCells = false; // when cell contents have changed
    private boolean sizeChanged = false;



    Timeline sbTouchTimeline;
    KeyFrame sbTouchKF1;
    KeyFrame sbTouchKF2;

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

        sheet = new Sheet<>(this);
        sheet.getStyleClass().add("sheet");

        vbar = new VirtualScrollBar(this);


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
                                        (sheet.computePosition(lastCell).getY()
                                            + sheet.getCellHeight(lastCell)
                                            - sheet.computePosition(sheet.getFirst()).getY())
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

                if (virtualDelta != 0.0) {
                    /*
                    ** only consume it if we use it
                    */
                    double result = scrollPixels(-virtualDelta);
                    if (result != 0.0) {
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

                lastY = e.getY();

                // determine whether the user has push down on the virtual flow,
                // or whether it is the scrollbar. This is done to prevent
                // mouse events being 'doubled up' when dragging the scrollbar
                // thumb - it has the side-effect of also starting the panning
                // code, leading to flicker
                isPanning = ! (vbar.getBoundsInParent().contains(e.getX(), e.getY())
                        );
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
            double yDelta = lastY - e.getY();

            // figure out the distance that the mouse moved in the virtual
            // direction, and then perform the movement along that axis
            // virtualDelta will contain the amount we actually did move
            double virtualDelta =  yDelta;
            double actual = scrollPixels(virtualDelta);
            if (actual != 0) {
                // update last* here, as we know we've just adjusted the
                // scrollbar. This means we don't get the situation where a
                // user presses-and-drags a long way past the min or max
                // values, only to change directions and see the scrollbar
                // start moving immediately.
                 lastY = e.getY();

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

    void changeWidth(double oldWidth, double newWidth){
        double averageRowHeight =  sampleAverageRowHeight(newWidth, ROW_SAMPLE_NUMBER);
    }

    double sampleAverageRowHeight(double newWidth, int rowSampleNumber) {
        int max =  sheet.getCacheSize();

        int count = 0;
        if(max < 1 || rowSampleNumber < 1){
            return 1d;
        }

        double totalWidth = 0;
        double totalHeight = 0;
        double maxHeight = 0;


        for (int i = 0; i < max; i++) {
            double[] size = sheet.getOrCreateCacheCellSize(i);
            double checkWidth = totalWidth + size[0];


            if(maxHeight < size[1]){
                maxHeight = size[1];
            }

            if(checkWidth < newWidth){
                totalWidth += size[0];

            }
            else { //new row
                totalWidth = 0;
                totalHeight += maxHeight;
                maxHeight = size[1];
                count++;
            }

            System.out.printf("max: %s, size[0]: %s, size[1]: %s, checkWidth: %s, maxHeight: %s, totalHeight %s, count: %s\n",max, size[0], size[1], checkWidth,maxHeight,totalHeight, count);
            if(count >= rowSampleNumber){
                break;
            }

        }

        if(count == 0){ //only one unfinished row
            count = 1;
            totalHeight = maxHeight;
        }

        return totalHeight/count;
    }

    void changeHeight(double oldHeight,  double newHeight){

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
        sheet.clearCompletely();
        invalidateSizes();
        sheet.setViewPortWidth(0);
        sheet.setViewPortHeight(0);
        resetScrollBars();
        setNeedsLayout(true);
        requestLayout();
    }

    private void resetScrollBars(){
        lastPosition = 0;
        vbar.setValue(0);
        setScrollBarPosition(0.0f);
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
            sheet.resetSizeCache();
            estimatedSize = 1d;
            recalculateEstimatedSize();

            boolean countChanged = oldCount != cellCount;
            oldCount = cellCount;

            // ensure that the virtual scrollbar adjusts in size based on the current
            // cell count.
            if (countChanged) {
                vbar.setMax(cellCount);
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
    public final void setScrollBarPosition(double value) {
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
                        if (getParent() != null) {
                            getParent().requestLayout();
                        }
                    }
                    clearAccumCellAndParent();
                }
            };
        }
        return cellFactory;
    }

    /**
     * This must be call when cell factory is changed.
     */
    private void clearAccumCellAndParent() {
        if (accumCellParent != null) {
            accumCellParent.getChildren().clear();
        }
        accumCell = null;
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/


    /**
     * Keep the position constant and adjust the absoluteOffset to
     * match the (new) position.
     */
    void synchronizeAbsoluteOffsetWithPosition() {
        absoluteOffset  = (estimatedSize - sheet.getViewPortHeight()) * getPosition();
    }

    /**
     * Keep the absoluteOffset constant and adjust the position to match
     * the (new) absoluteOffset.
     */
    void synchronizePositionWithAbsoluteOffset() {
        if (sheet.getViewPortHeight() >= estimatedSize) {
            setScrollBarPosition(0d);
        } else {
            setScrollBarPosition(absoluteOffset / (estimatedSize - sheet.getViewPortHeight()));
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
            invalidateSizes();
            sheet.clearCompletely();
        } else if (needsRebuildCells) {
            invalidateSizes();
            sheet.dumpAllToPile();
            sheet.clearChildren();
        } else if (needsReconfigureCells) {
           invalidateSizes();
        }
        sheet.updateDirtyCells();
        invalidateSizes();


        boolean recreatedOrRebuilt = needsRebuildCells || needsRecreateCells || sizeChanged;

        needsRecreateCells = false;
        needsReconfigureCells = false;
        needsRebuildCells = false;


        final double width = getWidth();
        final double height = getHeight();
        final boolean isVertical = isVertical();
        final double position = getPosition();
        // if the width and/or height is 0, then there is no point doing
        // any of this work. In particular, this can happen during startup
        if (width <= 0 || height <= 0) {
            clearViewPort();
            return;
        }

        boolean cellNeedsLayout = cellNeedsLayout();



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
                lastHeight != height ||
                lastWidth != width||
                (isVertical && height < lastHeight) || (! isVertical && width < lastWidth);


        addTrailingCells();


        initViewport();

        // Get the index of the "current" cell
        int currentIndex = computeStartIndex(getItemsCount(), absoluteOffset);

        if (rebuild) {
           rebuild(currentIndex);
        }

        layoutCells();
        updateScrollBars(recreatedOrRebuilt || rebuild);
        reportSizesAndPosition();
        sheet.cleanPile();


    }

    private void clearViewPort() {
        sheet.moveAllCellsToPile();
        lastWidth = getWidth();
        lastHeight = getHeight();
        vbar.setVisible(false);
    }

    private boolean isSizeExpanded() {
        return (isVertical() && getHeight() > lastHeight) || (!isVertical() && getWidth() > lastWidth);
    }

    private void reportSizesAndPosition(){

        lastWidth = getWidth();
        lastHeight = getHeight();
        lastPosition = getPosition();
    }

    private void rebuild(int currentIndex) {
        setMaxPrefBreadth(-1);
        sheet.moveAllCellsToPile();
        addLeadingCells(currentIndex);
        addTrailingCells();
    }

    private boolean cellNeedsLayout() {
        boolean cellNeedsLayout = false;
        for (int i = 0; i < sheet.size(); i++) {
            Cell<?> cell = sheet.get(i);
            cellNeedsLayout = cell.isNeedsLayout();
            if (cellNeedsLayout) break;
        }

        return cellNeedsLayout;
    }


    private void invalidateSizes(){
        setMaxPrefBreadth(-1);
        lastWidth = -1;
        lastHeight = -1;
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
     * Locates and returns the last non-empty IndexedCell that is currently
     * partially or completely visible. This function may return null if there
     * are no cells, or if the viewport length is 0.
     * @return the last visible cell
     */


    /**
     * Locates and returns the first non-empty IndexedCell that is partially or
     * completely visible. This really only ever returns null if there are no
     * cells or the viewport length is 0.
     * @return the first visible cell
     */
    public T getFirstVisibleCell() {
        if (sheet.isEmpty() || sheet.getViewPortHeight() <= 0) {
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
            scrollPixels(sheet.computePosition(firstCell).getY());
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
            scrollPixels(sheet.computePosition(lastCell).getY() + sheet.getCellHeight(lastCell) - sheet.getViewPortHeight());
        }
    }

    /**
     * Adjusts the cells such that the selected cell will be fully visible in
     * the viewport (but only just).
     * @param cell the cell
     */
    public void scrollTo(T cell) {
        if (cell != null) {
            final double start = sheet.computePosition(cell).getY();
            final double length = sheet.getCellHeight(cell);
            final double end = start + length;
            final double viewportLength = sheet.getViewPortHeight();

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
            sheet.moveAllCellsToPile();
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
            T cell = sheet.getAndRemoveCellFromPile(targetIndex);
            if(cell == null){
                cell = sheet.createCell();
                sheet.addCell(cell);
            }
            sheet.setCellIndex(cell, targetIndex);

            cell.setVisible(true);
            if (downOrRight) {
                sheet.addLast(cell);
                scrollPixels(sheet.getCellHeight(cell));
            } else {
                // up or left
                sheet.addFirst(cell);
                scrollPixels(-sheet.getCellHeight(cell));
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
            setScrollBarPosition(1);
            posSet = true;
        } else if (index < 0) {
            setScrollBarPosition(0);
            posSet = true;
        }

        if (!posSet) {
            adjustPositionToIndex(index);
        }

        requestLayout();
    }



    /**
     * Given a delta value representing a number of pixels, this method attempts
     * to move the VirtualFlow in the given direction (positive is down/right,
     * negative is up/left) the given number of pixels. It returns the number of
     * pixels actually moved.
     * @param delta the delta value
     * @return the number of pixels actually moved
     */
    private boolean scrollAtEightExtremity(final double delta){
        if ((( (tempVisibility ? !needLengthBar : !vbar.isVisible())) )) {
            return true;
        }

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

        if (!sheet.isEmpty()) {
            final int currIndex = computeStartIndex(getItemsCount(), absoluteOffset) - sheet.getFirst().getIndex();
            final int size = sheet.size();

            for (int i = currIndex - 1; i >= 0 && i < size; i--) {
                final T cell = sheet.get(i);
                Point2D pos = sheet.computePosition(cell);
                sheet.positionCell(cell, pos.getX(), pos.getY());
                sheet.updateCellCacheSize(cell);
            }

            // position trailing cells
            for (int i = currIndex; i >= 0 && i < size; i++) {
                final T cell = sheet.get(i);
                Point2D pos = sheet.computePosition(cell);
                sheet.positionCell(cell, pos.getX(), pos.getY());
                sheet.updateCellCacheSize(cell);

            }
        }
    }

    public double scrollPixels(final double delta) {
        // Short cut this method for cases where nothing should be done
        if (delta == 0) {return 0;}

       if(scrollAtEightExtremity(delta)){
           return  0;
       }

        recalculateEstimatedSize();

        return adjustPositionByPixelAmount(delta);

    }



    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        double w =  getPrefBreadth(height);
        return w + vbar.prefWidth(-1);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width) {
        double h =  sheet.getViewPortHeight() ;
        return h ;
    }

    /**
     * Return a cell for the given index. This may be called for any cell,
     * including beyond the range defined by cellCount, in which case an
     * empty cell will be returned. The returned value should not be stored for
     * any reason.
     * @param index the index
     * @return the cell
     */

     T getOrCreateAccumCell(){
        if (accumCell == null) {
                accumCell = sheet.createCell();
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

        return accumCell;
    }







    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/
//
//    /**
//     * Returns the scroll bar used for scrolling horizontally. A developer who needs to be notified when a scroll is
//     * happening could attach a listener to the {@link ScrollBar#valueProperty()}.
//     *
//     * @return the scroll bar used for scrolling horizontally
//     * @since 12
//     */
//    public final ScrollBar getHbar() {
//        return hbar;
//    }

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
     * Adds all the cells prior to and including the given currentIndex, until
     * no more can be added without falling off the flow. The startOffset
     * indicates the distance from the leading edge (top) of the viewport to
     * the leading edge (top) of the currentIndex.
     */
    void addLeadingCells(int currentIndex) {

        int index = currentIndex;

        boolean first = true; // first time in, we just fudge the offset and let

        T cell = null;

        if (index == getItemsCount() ) {
            index--;
            first = false;
        }
        while (index >= 0 && ( first)) {

            cell = sheet.getAndRemoveCellFromPile(index);

            if(cell == null){
                cell = sheet.createCell();
                sheet.addCell(cell);
            }

            sheet.setCellIndex(cell, index);
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
            Point2D p = sheet.computePosition(cell);
            sheet.positionCell(cell, p.getX(), p.getY());
            sheet.updateCellCacheSize(cell);
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
            double firstCellPos = sheet.computePosition(cell).getY();
            if (firstIndex == 0 && firstCellPos > 0) {
                setScrollBarPosition(0.0f);

                for (int i = 0; i < sheet.size(); i++) {
                    cell = sheet.get(i);
                    Point2D p = sheet.computePosition(cell);
                    sheet.positionCell(cell, p.getX(), p.getY());
                    sheet.updateCellCacheSize(cell);

                }
            }
        } else {
            // reset scrollbar to top, so if the flow sees cells again it starts at the top
            vbar.setValue(0);
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

        final double viewPortHeight = sheet.getViewPortHeight();
        T lastCell = sheet.getLast();

        Point2D pos = sheet.computePosition(lastCell);

        double layoutX = pos.getX() + sheet.getCellWidth(lastCell);
        double layoutY = pos.getY() + sheet.getCellHeight(lastCell);
        double maxHeight = 0;

        int nextIndex = lastCell.getIndex() + 1;
        final int itemCount = getItemsCount();
        boolean isEmptyCell = nextIndex <= itemCount;

        if ((layoutY < 0 )|| layoutY  > viewPortHeight) {
            return false;
        }


        final double maxCellCount = viewPortHeight;//cell size = 1


        while (layoutY < viewPortHeight) {
            if (nextIndex > maxCellCount) {
                notifyIndexExceedsMaximum();
                return false;
            }


            T cell = sheet.getAndRemoveCellFromPile(nextIndex);
            if(cell ==  null){
                cell  = sheet.createCell();
                sheet.addCell(cell);
            }

            sheet.setCellIndex(cell, nextIndex);
            sheet.addLastCellToSheet(cell);

            double[] size = sheet.getOrCreateCacheCellSize(nextIndex);

            double checkLayoutX = layoutX + size[0];

            if(!sheet.isInRow(checkLayoutX)){ //new row
                layoutX  = 0;
                layoutY = layoutY + maxHeight;
                maxHeight = size[1];
            }
            else {
                layoutX = checkLayoutX;
                if(maxHeight < size[1]){
                    maxHeight = size[1];
                }
            }

            cell.setVisible(true);
            ++nextIndex;
        }

        return isEmptyCell;
    }





    private void notifyIndexExceedsMaximum() {

            final PlatformLogger logger = Logging.getControlsLogger();
            if (logger.isLoggable(PlatformLogger.Level.INFO)) {
                logger.info("index exceeds maxCellCount of %s. Check size calculations.", getItemsCount() );
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


    /**
     * @return true if bar visibility changed
     */
    private boolean computeBarVisibility() {
        if (sheet.isEmpty()) {
            // In case no cells are set yet, we assume no bars are needed
            needLengthBar = false;
            return true;
        }

        boolean barVisibilityChanged = false;

        VirtualScrollBar lengthBar =  vbar;

        final int cellsSize = sheet.size();
        final int itemCount = getItemsCount();
        T lastCell = sheet.getLast();
        for (int i = 0; i < 2; i++) {

            Point2D lastPos = sheet.computePosition(lastCell);
            final boolean lengthBarVisible =
                    getPosition() > 0
                    || itemCount > cellsSize
                    || (itemCount == cellsSize && (lastPos.getY() + sheet.getCellHeight(lastCell)) > sheet.getViewPortHeight());

            if (lengthBarVisible ^ needLengthBar) {
                needLengthBar = lengthBarVisible;
                barVisibilityChanged = true;
            }


        }

        // Start by optimistically deciding whether the length bar and
        // breadth bar are needed and adjust the viewport dimensions
        // accordingly. If during layout we find that one or the other of the
        // bars actually is needed, then we will perform a cleanup pass

        if (!Properties.IS_TOUCH_SUPPORTED) {
            updateViewportDimensions();
            lengthBar.setVisible(needLengthBar);
        } else {
            lengthBar.setVisible(needLengthBar && tempVisibility);
        }
        return barVisibilityChanged;
    }

    private void updateViewportDimensions() {
        final double lengthBarBreadth =  snapSizeX(vbar.prefWidth(-1)) ;
        sheet.setViewPortWidth(getWidth() - (needLengthBar ? lengthBarBreadth : 0));
        sheet.setViewPortHeight(getHeight()) ;
        synchronizeAbsoluteOffsetWithPosition();
    }



    private void initViewport() {

        updateViewportDimensions();

        vbar.setVirtual(true);
    }

    private double  computeSumCellHeight(){

        double totalY = 0;
        double layoutX = 0;
        double maxHeight  = 0;
        int size = sheet.size();

        for (int i = 0; i < size; i++) {
            double[] nextSize = sheet.getOrCreateCacheCellSize(i);

            double checkLayoutX = layoutX + nextSize[0];

            if(!sheet.isInRow(checkLayoutX)){ //new row
                layoutX  = 0;
                totalY = totalY + maxHeight;
                maxHeight = nextSize[1];
            }
            else {
                layoutX = checkLayoutX;
                if(maxHeight < nextSize[1]){
                    maxHeight = nextSize[1];
                }
            }

        }

        return totalY;

    }

    private void updateScrollBars(boolean recreate) {
        computeBarVisibility();

        VirtualScrollBar lengthBar =  vbar ;

        double sumCellLength = 0;
        double flowLength =  getHeight();

        final double viewportBreadth = sheet.getViewPortWidth();
        final double viewportLength = sheet.getViewPortHeight();

        if (recreate && (lengthBar.isVisible() || Properties.IS_TOUCH_SUPPORTED)) {
            final int cellCount = getItemsCount();
            int numCellsVisibleOnScreen = 0;
            for (int i = 0, max = sheet.size(); i < max; i++) {
                T cell = sheet.get(i);
                if (cell != null && !cell.isEmpty()) {
                    sumCellLength +=  cell.getHeight();
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

                    vbar.resizeRelocate(viewportBreadth, 0, vbar.prefWidth(viewportLength), viewportLength);

            }
            else {
                    double prefWidth = vbar.prefWidth(viewportLength);
                    vbar.resizeRelocate(viewportBreadth - prefWidth, 0, prefWidth, viewportLength);

            }
        }



        clipView.resize(snapSizeX( viewportBreadth ),
                snapSizeY(viewportLength));

        // If the viewportLength becomes large enough that all cells fit
        // within the viewport, then we want to update the value to match.
        if (getPosition() != lengthBar.getValue()) {
            lengthBar.setValue(getPosition());
        }
    }



    private double getPrefBreadth(double oppDimension) {
        double max = 0;

        // This primarily exists for the case where we do not want the breadth
        // to grow to ensure a golden ratio between width and height (for example,
        // when a ListView is used in a ComboBox - the width should not grow
        // just because items are being added to the ListView)
        if (oppDimension > -1) {
            double prefLength = sheet.getViewPortHeight();
            max =  prefLength * GOLDEN_RATIO_MULTIPLIER;
        }

        return max;
    }





    //TODO need to re-implement
    private void adjustPositionToIndex(int index) {
        int cellCount = getItemsCount();
        if (cellCount <= 0) {
            setScrollBarPosition(0.0f);
        } else {
            double targetOffset = 0;
            double estSize = estimatedSize/cellCount;
            for (int i = 0; i < index; i++) {
                double[] cz = sheet.getCellSize(i);
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
        if (numPixels == 0) {
            return 0;
        }
        // When we're at the top already, we can't move back further, unless we
        // want to allow for gravity-alike effects.
        if ((absoluteOffset <= 0) && (numPixels < 0)) {
            return 0;
        }

        // start with applying the requested modification
        double origAbsoluteOffset = this.absoluteOffset;
        this.absoluteOffset = Math.max(0.d, this.absoluteOffset + numPixels);
        double newPosition = Math.min(1.0d, absoluteOffset / (estimatedSize - sheet.getViewPortHeight()));
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
            int cci = computeStartIndex(getItemsCount(), absoluteOffset);
            while (cci < getItemsCount()) {
                sheet.getOrCreateCacheCellSize(cci);
                cci++;
            }
            recalculateEstimatedSize();
        }

        setScrollBarPosition(newPosition);
        return absoluteOffset - origAbsoluteOffset;

    }
    //TODO need to re-implement

    /**
     * Compute start cell index at a absolute position
     * @return
     */
    private int computeStartIndex(int itemCount, double absoluteOffset) {
        double totalY = 0;
        int index = -1;
        double layoutX = 0;
        double maxHeight  = 0;

        for (int i = 0; i < itemCount; i++) {
            double[] nextSize = sheet.getOrCreateCacheCellSize(i);

            double checkLayoutX = layoutX + nextSize[0];

            if(!sheet.isInRow(checkLayoutX)){ //new row
                layoutX  = 0;
                totalY = totalY + maxHeight;
                maxHeight = nextSize[1];
            }
            else {
                layoutX = checkLayoutX;
                if(maxHeight < nextSize[1]){
                    maxHeight = nextSize[1];
                }
            }

            if (totalY >= absoluteOffset) {
                index =  i;
                break;
            }



        }
        if(index == -1) {
            index = itemCount == 0 ? 0 : itemCount - 1;
        }
//        System.out.printf("current index: %s, offset %s\n",index, absoluteOffset);
        return index;
    }





    /**
     * Recalculate the estimated size for this list based on what we have in the
     * cache.
     */
    private void recalculateEstimatedSize() {
        estimatedSize = sheet.recalculateAndImproveEstimatedSize(DEFAULT_IMPROVEMENT, getItemsCount());
    }

    T getFirstVisibleCellWithinViewport() {
        return sheet.getFirstVisibleCellWithinViewport();
    }

    void setCellDirty(int i) {
        sheet.setCellDirty(i);
    }

    public T getVisibleCell(int index) {
        return sheet.getVisibleCell(index);
    }

    public T getLastVisibleCell() {
       return sheet.getLastVisibleCell();
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
        private final Rectangle clipRect;

        public void setNode(Node n) {

            getChildren().clear();
            getChildren().add(n);
        }

        public void setClipX(double clipX) {
            setLayoutX(-clipX);
            clipRect.setLayoutX(clipX);
        }

        public void setClipY(double clipY) {
            setLayoutY(-clipY);
            clipRect.setLayoutY(clipY);
        }



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
