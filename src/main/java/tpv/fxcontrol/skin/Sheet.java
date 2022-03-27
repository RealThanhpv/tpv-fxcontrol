package tpv.fxcontrol.skin;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Cell;
import javafx.scene.layout.Region;
import tpv.fxcontrol.FlowIndexedCell;

import java.util.ArrayList;
import java.util.BitSet;


public class Sheet<T extends FlowIndexedCell> extends Region {
    private static final double MAGIC_X = 0;
    /**
     * Indicates that this is a newly created cell and we need call processCSS for it.
     * <p>
     * See RT-23616 for more details.
     */
    static final String NEW_CELL = "newcell";
    private static final int ROW_SAMPLE_NUMBER = 10;
    private DoubleProperty horizontalGap = new SimpleDoubleProperty(this, "horizontal-gap", 0);
    /**
     * The breadth of the viewport portion of the VirtualFlow as computed during
     * the layout pass. In a vertical flow this would be the same as the clip
     * view width. In a horizontal flow this is the clip view height.
     * The access on this variable is package ONLY FOR TESTING.
     */
    private double viewportBreadth;
    /**
     * A list containing the cached version of the calculated size (height for
     * vertical, width for horizontal) for a (fictive or real) cell for
     * each element of the backing data.
     * This list is used to calculate the estimatedSize.
     * The list is not expected to be complete, but it is always up to date.
     * When the size of the items in the backing list changes, this list is
     * cleared.
     */
    ArrayList<double[]> itemSizeCache = new ArrayList<>();
    private final BitSet dirtyCells = new BitSet();

    final void setViewPortWidth(double value) {
        this.viewportBreadth = value;
    }


    final double getViewPortWidth() {
        return viewportBreadth;
    }

    /**
     * The length of the viewport portion of the VirtualFlow as computed
     * during the layout pass. In a vertical flow this would be the same as the
     * clip view height. In a horizontal flow this is the clip view width.
     * The access on this variable is package ONLY FOR TESTING.
     */
    private double viewportLength;

    void setViewPortHeight(double value) {
        this.viewportLength = value;

    }

    VirtualFlow<T> flow;

    Sheet(VirtualFlow<T> flow) {
        this.flow = flow;
    }

    /**
     * For test only
     */
    Sheet() {

    }

    Point2D computePosition(T cell) {
        //vertical layout
        int index = cell.getIndex();
        double layoutX = 0;
        double layoutY = 0;
        double maxHeight = 0;

        int start = getFirst().getIndex();
//        System.out.println("start index: "+ start);

        for (int i = start; i < index; i++) {
            Cell calCel = get(i);
            if (calCel == null) {
                calCel = getCellFromPile(i);
            }

            double prefWidth = 0;
            double prefHeight = 0;
            if (cell.getParent() != null) {
                prefWidth = calCel.prefWidth(-1);
                prefHeight = calCel.prefHeight(-1);
            } else {
                double[] size = getOrCreateCacheCellSize(i);
                prefWidth = size[0];
                prefHeight = size[1];
            }

            double checkLayoutX = layoutX + prefWidth;

            if (!isInRow(layoutX)) {
                layoutX = 0;
                layoutY = layoutY + maxHeight;
                maxHeight = prefHeight;
            } else {
                layoutX = checkLayoutX;
                if (maxHeight < prefHeight) {
                    maxHeight = prefHeight;
                }
            }
        }

        Point2D p = new Point2D(layoutX, layoutY);
        return p;
    }

    boolean isInRow(double x) {
        return x < (getViewPortWidth() - MAGIC_X);
    }

    T getFirstVisibleCellWithinViewport() {
        if (isEmpty() || getViewPortHeight() <= 0) {
            return null;
        }

        T cell;
        for (int i = 0; i < size(); i++) {
            cell = get(i);
            if (cell.isEmpty()) continue;

            final double cellStartY = computePosition(cell).getY();
            if (cellStartY >= 0) {
                return cell;
            }
        }

        return null;
    }


    double getViewPortHeight() {
        return viewportLength;
    }

    void clearChildren() {
        getChildren().clear();
    }

    void dumpAllToPile() {
        for (int i = 0, max = size(); i < max; i++) {
            T cell = removeFirst();
            cell.updateIndex(-1);
            addToPile(cell);
        }
    }

    void clearCompletely() {
        clear();
    }



    public void addCell(T cell) {
        getChildren().add(cell);
        cell.setVisible(true);
    }

    /**
     * The list of cells representing those cells which actually make up the
     * current view. The cells are ordered such that the first cell in this
     * list is the first in the view, and the last cell is the last in the
     * view. When pixel scrolling, the list is simply shifted and items drop
     * off the beginning or the end, depending on the order of scrolling.
     * <p>
     * This is package private ONLY FOR TESTING
     */
    final ArrayLinkedList<T> cells = new ArrayLinkedList<T>();

    /**
     * A structure containing cells that can be reused later. These are cells
     * that at one time were needed to populate the view, but now are no longer
     * needed. We keep them here until they are needed again.
     * <p>
     * This is package private ONLY FOR TESTING
     */
    final ArrayLinkedList<T> pile = new ArrayLinkedList<T>();


    T get(int cellIndex) {
        return cells.get(cellIndex);
    }

    int size() {
        return cells.size();
    }

    void clear() {
        cells.clear();
        pile.clear();
    }

    T getLast() {
        return cells.getLast();
    }

    T getFirst() {
        return cells.getFirst();
    }

    boolean contains(Parent p) {
        return cells.contains(p);
    }

    boolean isEmpty() {
        return cells.isEmpty();
    }

    boolean contains(Node owner) {
        return cells.contains(owner);
    }

    T remove(int i) {
        return cells.remove(i);
    }

    void addFirst(T cell) {
        cells.addFirst(cell);

    }

    void addLast(T cell) {
        cells.addLast(cell);
    }

    public T removeFirst() {
        return cells.removeFirst();
    }

    T getAndRemoveCellFromPile(int prefIndex) {
        T cell = null;

        for (int i = 0, max = pile.size(); i < max; i++) {
            T _cell = pile.get(i);
            assert _cell != null;
            if (_cell.getIndex() == prefIndex) {
                cell = _cell;
                pile.remove(i);
                break;
            }
        }
        if (cell == null && !pile.isEmpty()) {
            cell = pile.removeLast();
        }

        return cell;
    }

    /**
     * Puts the given cell onto the pile. This is called whenever a cell has
     * fallen off the flow's start.
     */
    void addToPile(T cell) {
        pile.addLast(cell);
    }

    /**
     * This method will remove all cells from the VirtualFlow and remove them,
     * adding them to the 'pile' (that is, a place from where cells can be used
     * at a later date). This method is protected to allow subclasses to clean up
     * appropriately.
     */
    void moveAllCellsToPile() {
        for (int i = 0, max = size(); i < max; i++) {
            addToPile(removeFirst());
        }
    }

    void cleanPile() {
        boolean wasFocusOwner = false;

        for (int i = 0, max = pile.size(); i < max; i++) {
            T cell = pile.get(i);
            wasFocusOwner = wasFocusOwner || doesCellContainFocus(cell);
            cell.setVisible(false);
        }

        // Fix for RT-35876: Rather than have the cells do weird things with
        // focus (in particular, have focus jump between cells), we return focus
        // to the VirtualFlow itself.
        if (wasFocusOwner) {
            requestFocus();
        }
    }

    /**
     * Gets a cell for the given index if the cell has been created and laid out.
     * "Visible" is a bit of a misnomer, the cell might not be visible in the
     * viewport (it may be clipped), but does distinguish between cells that
     * have been created and are in use vs. those that are in the pile or
     * not created.
     *
     * @param index the index
     * @return the visible cell
     */
    public T getVisibleCell(int index) {
        if (isEmpty()) {
            return null;
        }

        // check the last index
        T lastCell = getLast();
        int lastIndex = lastCell.getIndex();
        if (index == lastIndex) {
            return lastCell;
        }

        // check the first index
        T firstCell = getFirst();
        int firstIndex = firstCell.getIndex();
        if (index == firstIndex) {
            return firstCell;
        }

        // if index is > firstIndex and < lastIndex then we can get the index
        if (index > firstIndex && index < lastIndex) {
            T cell = get(index - firstIndex);
            if (cell.getIndex() == index) return cell;
        }

        // there is no visible cell for the specified index
        return null;
    }


    private T getCellFromPile(int index) {
        T cell = null;
        for (int i = 0; i < pile.size(); i++) {
            cell = pile.get(i);
            if (cell.getIndex() == index) {
                return cell;
            }
        }

        return cell;
    }

    boolean doesCellContainFocus(Cell<?> c) {
        Scene scene = c.getScene();
        final Node focusOwner = scene == null ? null : scene.getFocusOwner();

        if (focusOwner != null) {
            if (c.equals(focusOwner)) {
                return true;
            }

            Parent p = focusOwner.getParent();
            while (p != null && !(p instanceof VirtualFlow)) {
                if (c.equals(p)) {
                    return true;
                }
                p = p.getParent();
            }
        }

        return false;
    }

    /**
     * Update the size of a specific cell.
     * If this cell was already in the cache, its old value is replaced by the
     * new size.
     *
     * @param cell
     */
    double[] updateCellCacheSize(T cell) {
        int cellIndex = cell.getIndex();

        if (cellIndex < itemSizeCache.size() && cellIndex > -1) {

            double newW = cell.getLayoutBounds().getWidth();
            double newH = cell.getLayoutBounds().getHeight();

            double[] size = itemSizeCache.get(cellIndex);
            if (size == null) {
                size = new double[]{newW, newH};
            } else {
                size[0] = newW;
                size[1] = newH;
            }
            itemSizeCache.set(cellIndex, size);

            return size;

        }

        return null;


    }


    /**
     * Get a cell which can be used in the layout. This function will reuse
     * cells from the pile where possible, and will create new cells when
     * necessary.
     *
     * @param prefIndex the preferred index
     * @return the available cell
     */

    T createCell() {
        T cell = flow.getCellFactory().call(flow);
        cell.getProperties().put(Sheet.NEW_CELL, null);
        return cell;
    }

    void resetSizeCache() {
        itemSizeCache.clear();
    }

    //TODO need to re-implement

    /**
     * Compute start cell index at a absolute position
     *
     * @return
     */
    int computeStartIndex(int itemCount, double absoluteOffset) {
        double totalY = 0;
        int index = -1;
        double layoutX = 0;
        double maxHeight = 0;

        for (int i = 0; i < itemCount; i++) {
            double[] nextSize = getOrCreateCacheCellSize(i);

            double checkLayoutX = layoutX + nextSize[0];

            if (!isInRow(checkLayoutX)) { //new row
                layoutX = 0;
                totalY = totalY + maxHeight;
                maxHeight = nextSize[1];
            } else {
                layoutX = checkLayoutX;
                if (maxHeight < nextSize[1]) {
                    maxHeight = nextSize[1];
                }
            }

            if (totalY >= absoluteOffset) {
                index = i;
                break;
            }


        }
        if (index == -1) {
            index = itemCount == 0 ? 0 : itemCount - 1;
        }
        return index;
    }

    void updateDirtyCells() {
        if (!dirtyCells.isEmpty()) {
            int index;
            final int cellsSize = size();
            while ((index = dirtyCells.nextSetBit(0)) != -1 && index < cellsSize) {
                T cell = get(index);
                // updateIndex(-1) works for TableView, but breaks ListView.
                // For now, the TableView just does not use the dirtyCells API
//                cell.updateIndex(-1);
                if (cell != null) {
                    cell.requestLayout();
                    updateCellCacheSize(cell);
                }
                dirtyCells.clear(index);
            }

        }
    }

    void setCellDirty(int index) {
        dirtyCells.set(index);
    }


    double[] getCellSize(int idx) {
        return getOrCreateCacheCellSize(idx, false);
    }

    /**
     * Get the size of the considered element.
     * If the requested element has a size that is not yet in the cache,
     * it will be computed and cached now.
     *
     * @return the size of the element; or 1 in case there are no cells yet
     */
    private double[] getCacheCellSize(int idx) {
        if (idx < 0) {
            return null;
        }
        // is the current cache long enough to contain idx?
        if (itemSizeCache.size() > idx) {
            // is there a non-null value stored in the cache?
            if (itemSizeCache.get(idx) != null) {
                return itemSizeCache.get(idx);
            }
        }
        return null;
    }

    double[] getOrCreateCacheCellSize(int idx) {
        return getOrCreateCacheCellSize(idx, true);
    }

    private double[] getOrCreateCacheCellSize(int idx, boolean create) {

        double[] cached = getCacheCellSize(idx);
        if (cached != null) {
            return cached;
        }

        if (!create) {
            return null;
        }

        T cell = getVisibleCell(idx);

        if (cell == null) { // we might get the accumcell here
            cell = getCellFromPile(idx);
        }

        if (cell == null) {
            cell = flow.getOrCreateAccumCell();
            setCellIndex(cell, idx);
        }
        // Make sure we have enough space in the cache to store this index
        while (idx >= itemSizeCache.size()) {
            itemSizeCache.add(itemSizeCache.size(), null);
        }

        double[] answer = updateCellCacheSize(cell);


        return answer;
    }


    /**
     * The VirtualFlow uses this method to set a cells index (rather than calling
     * {@link FlowIndexedCell#updateIndex(int)} directly), so it is a perfect place
     * for subclasses to override if this if of interest.
     *
     * @param cell  The cell whose index will be updated.
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

    void positionCell(T cell, double positionX, double positionY) {
        cell.setLayoutX(snapSizeX(positionX));
        cell.setLayoutY(snapSpaceY(positionY));

    }


    public T getLastVisibleCell() {
        if (isEmpty() || getViewPortHeight() <= 0) {
            return null;
        }

        T cell;
        for (int i = size() - 1; i >= 0; i--) {
            cell = get(i);
            if (!cell.isEmpty()) {
                return cell;
            }
        }

        return null;
    }

    void addLastCellToSheet(T cell) {
        addLast(cell);
        Point2D p = computePosition(cell);
        positionCell(cell, p.getX(), p.getY());
        updateCellCacheSize(cell);
    }

    /**
     * Gets the length of a specific cell
     */
    double getCellHeight(T cell) {
        return cell.getLayoutBounds().getHeight();
    }

    /**
     * Gets the breadth of a specific cell
     */
    double getCellWidth(Cell cell) {
        return cell.getLayoutBounds().getWidth();
    }


    public int getCacheSize() {
        return itemSizeCache.size();
    }


    public double getHorizontalGap() {
        return horizontalGap.get();
    }

    public DoubleProperty horizontalGapProperty() {
        return horizontalGap;
    }

    public void setHorizontalGap(double horizontalGap) {
        this.horizontalGap.set(horizontalGap);
    }



    /**
     * @param width
     * @param start
     * @param end
     * @param rowLimit -1 to exclude row limit condition
     * @param outCount the number of row from start to end
     * @return
     */
    private double computeTotalHeight(final double width, final int start, final int end, final int rowLimit, final int[] outCount) {

        int count = 0;
        if (end < 1) {
            return 0;
        }

        double totalWidth = 0;
        double totalHeight = 0;
        double maxHeight = 0;
        double[] size ;

        for (int i = start; i < end; i++) {
             size = getOrCreateCacheCellSize(i);
            double checkWidth = totalWidth + size[0] + getHorizontalGap();

            if (maxHeight < size[1]) {
                maxHeight = size[1];
            }

            if (checkWidth < width) {
                totalWidth = checkWidth;


            } else { //new row
                totalWidth = size[0];
                totalHeight += maxHeight;
                maxHeight = size[1];
                count++;
//                size = null;
            }

            System.out.printf("i: %s,max: %s, size[0]: %s, size[1]: %s, checkWidth: %s, width: %s, totalWidth: %s, maxHeight: %s, totalHeight %s, count: %s\n", i, end, size[0], size[1], checkWidth, width, totalWidth, maxHeight, totalHeight, count);
            if (rowLimit > 0 && count >= rowLimit) {
                break;
            }
        }
        if (count == 0) {
            count = 1;
            totalHeight = maxHeight;
        }

//        if(size == null){
//            totalHeight += maxHeight;
//        }

        outCount[0] = count;

        return totalHeight;
    }

    double sampleAverageRowHeight(double width, int rowLimit) {
        int start = 0;
        int end = getCacheSize();
        if (end < 1) {
            return 1;
        }
        int[] outCount = new int[1];
        double height = computeTotalHeight(width, start, end, rowLimit, outCount);
        if (height == 0) {
            return 1;
        }

        return height / outCount[0];
    }

    double estimateLength(final int improve, final int itemCount) {
        int added = 0;

        while ((itemSizeCache.size() < itemCount) && (added < improve)) {
            getOrCreateCacheCellSize(itemSizeCache.size());
            added++;
        }
        int start = 0;
        int end = itemSizeCache.size();
        double width = getViewPortWidth();
        int rowLimit = -1;
        int[] countOut = new int[1];

        double totalHeight = computeTotalHeight(width, start, end, rowLimit, countOut);

        return totalHeight;

    }


}
