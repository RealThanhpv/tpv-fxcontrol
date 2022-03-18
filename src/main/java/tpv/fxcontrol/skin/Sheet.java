package tpv.fxcontrol.skin;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Cell;
import tpv.fxcontrol.FlowIndexedCell;

import java.util.List;
import java.util.stream.Collectors;

public class Sheet<T extends FlowIndexedCell> extends Group {
    private static final double MAGIC_X = 2;
    private Group testParent = new Group();
    private Group layoutGroup = new Group();
    //To spare rows
    private ArrayLinkedList<VirtualRow<T>> rowPiles = new ArrayLinkedList();
    private ArrayLinkedList<T> cellsPiles = new ArrayLinkedList<>();
    /**
     * The breadth of the viewport portion of the VirtualFlow as computed during
     * the layout pass. In a vertical flow this would be the same as the clip
     * view width. In a horizontal flow this is the clip view height.
     * The access on this variable is package ONLY FOR TESTING.
     */
    private double viewportBreadth;
    public final void setWidth(double value) {
        this.viewportBreadth = value;
    }

    final double getWidth() {
        return viewportBreadth;
    }
    /**
     * The length of the viewport portion of the VirtualFlow as computed
     * during the layout pass. In a vertical flow this would be the same as the
     * clip view height. In a horizontal flow this is the clip view width.
     * The access on this variable is package ONLY FOR TESTING.
     */
    private double viewportLength;
    void setHeight(double value) {
        this.viewportLength = value;
    }


    private VirtualRow<T> getOrCreateRow(){
        if(rowPiles.isEmpty()){
            return new VirtualRow<>(this);
        }
        else {
            VirtualRow<T> row =  rowPiles.getFirst();
            return row;
        }
    }



    private boolean removeLastRow(){
        if(getChildren().isEmpty() ){
            return false;
        }

        VirtualRow<T> row = (VirtualRow<T>) getChildren().remove(getChildren().size() -1);

        dumpRow(row);
        return true;

    }

    //TODO
    private void dumpRow(VirtualRow<T> row) {
        row.getChildren().stream().map(n-> (T) n).forEach(c->{
            cellsPiles.addLast(c);
        });
        rowPiles.addLast(row);
    }


    Point2D getCellPosition(T cell) {
        //vertical layout
        VirtualRow<T> matchedRow = null;
        List<VirtualRow<T>> rows = getRows();
        for (int i = 0; i < rows.size(); i++) {
            VirtualRow<T> row =  rows.get(i);
            T rCell = row.getLastCell();
            if(rCell.getIndex() >= cell.getIndex()){
                matchedRow = row;
                break;
            }
        }

        double layoutX = - 1;
        if(matchedRow != null){
           layoutX =  matchedRow.getCellPosition(cell);
        }
        else {
            return new Point2D(0,0);
        }

        return new Point2D(layoutX, matchedRow.getLayoutY() );

    }

    private List<VirtualRow<T>> getRows() {
        return layoutGroup.getChildren().stream().map(n->(VirtualRow<T>)n).collect(Collectors.toList());
    }

    boolean isInRow(double x){
        return x < (getWidth() - MAGIC_X);
    }

    T getFirstVisibleCellWithinViewport() {
        if (isEmpty() || getHeight() <= 0) {return null;}

        T cell;
        for (int i = 0; i < size(); i++) {
            cell = get(i);
            if (cell.isEmpty()) continue;

            final double cellStartY = getCellPosition(cell).getY();
            if (cellStartY >= 0) {
                return cell;
            }
        }

        return null;
    }

    T getLastVisibleCellWithinViewport() {
        if (isEmpty() || getHeight() <= 0) return null;

        T cell;
        final double max = getHeight();
        for (int i = size() - 1; i >= 0; i--) {
            cell = get(i);
            if (cell.isEmpty()) continue;

            final double cellStart = getCellPosition(cell).getY();
            final double cellEnd = cellStart + cell.getLayoutBounds().getHeight();

            // we use the magic +2 to allow for a little bit of fuzziness,
            // this is to help in situations such as RT-34407
            if (cellEnd <= (max + 2)) {
                return cell;
            }
        }

        return null;
    }



    double getHeight() {
        return viewportLength;
    }
     void clearChildren() {
        layoutGroup.getChildren().clear();
    }

    void dumpAllToPile() {
        for (int i = 0, max = size(); i < max; i++) {
            T cell  = removeFirst();
            cell.updateIndex(-1);
            addToPile(cell);
        }
    }

    void clearCompletely() {
        clear();
    }


    private double computeRowLayoutHeight(VirtualRow<T> row){
        if(row.getScene() != null){
            return row.getLayoutBounds().getHeight();
        }

        testParent.getChildren().setAll(row);
        double h = row.getLayoutBounds().getHeight();
        testParent.getChildren().clear();

        return h;

    }

    private boolean addTrailingRow(VirtualRow<T> last) {
        double layoutY = 0;
        for (Node child : layoutGroup.getChildren()) {
            layoutY = layoutY + child.getLayoutBounds().getHeight();
        }
        last.setLayoutX(layoutY);
        double h = computeRowLayoutHeight(last);
        if(layoutY + h > getHeight()){
            return false;
        }
        last.updateIndex(layoutGroup.getChildren().size());
        layoutGroup.getChildren().add(last);

        return true;
    }

    public Sheet(){
        testParent.setVisible(false);
        getChildren().addAll(testParent, layoutGroup);
    }

    private VirtualRow<T> getLastRow(){
        if(layoutGroup.getChildren().isEmpty()){
            return null;
        }
       return (VirtualRow<T>) layoutGroup.getChildren().get(layoutGroup.getChildren().size() -1);
    }

    private VirtualRow<T> getFirstRow() {
        if (layoutGroup.getChildren().isEmpty()) {
            return null;
        }
        return (VirtualRow<T>) layoutGroup.getChildren().get(0);
    }






    private void cull() {
        final double viewportLength = getHeight();
        for (int i = size() - 1; i >= 0; i--) {
            T cell = get(i);
            double cellSize = cell.getLayoutBounds().getHeight();
            Point2D cellStart = getCellPosition(cell);
            double cellEnd = cellStart.getY() + cellSize;
            if (cellStart.getY() >= viewportLength || cellEnd < 0) {
                addToPile(remove(i));
            }
        }
    }


     T get(int i) {
        return cellsPiles.get(i);
    }

     int size() {
        return cellsPiles.size();
    }

     void clear() {
        cellsPiles.clear();
    }

     T getLast() {
        return cellsPiles.getLast();
    }

    T getFirst() {
        VirtualRow<T> firstRow = getFirstRow();
        if(firstRow == null){
            return null;
        }

       return firstRow.getFirstCell();
    }

     boolean contains(Parent p) {
         return cellsPiles.contains(p);
    }

    boolean isEmpty() {
        return cellsPiles.isEmpty();
    }

    boolean contains(Node owner) {
        return cellsPiles.contains(owner);
    }

    T remove(int i) {
        List<VirtualRow<T>> rows = getRows();
        VirtualRow<T> foundRow = null;
        for (int i1 = 0; i1 < rows.size(); i1++) {
            if(rows.get(i).getLastCell().getIndex() > i){
                foundRow = rows.get(i);
                break;
            }
        }
        T foundCell = null;
        if(foundRow != null){
            List<T> cells = foundRow.getCells();

            for (T cell : cells) {
                if(cell.getIndex() == i){
                    foundCell = cell;

                    break;
                }
            }
        }

        if(foundCell != null){
            foundRow.remove(foundCell);
            dumpCell(foundCell);
            return foundCell;
        }

        return null;

    }

    private void dumpCell(T foundCell) {
        cellsPiles.addLast(foundCell);
        foundCell.updateIndex(-1);
    }

    boolean addFirst(T cell) {
        new NullPointerException("Trace it");
        VirtualRow<T> firstRow = getFirstRow();
        double cellWidth = computeCellWidth(cell);
        cell.setPrefWidth(cellWidth);
        if(firstRow == null){
            return addLast(cell);
        }
        return firstRow.addLeadingCell(cell);
    }

    private double computeCellWidth(T cell) {
        if(cell == null){
            return 0;
        }

        if(cell.getScene() == null){
            testParent.getChildren().setAll(cell);
            return cell.getLayoutBounds().getWidth();
        }

        return cell.getLayoutBounds().getWidth();
    }

    public boolean addLast(T cell) {
        new NullPointerException("Trace it");
        VirtualRow<T> last = getLastRow();
        if(last == null || !last.isAddAble(cell)){
            last = getOrCreateRow();
            addTrailingRow(last);
        }

        if(last.isAddAble(cell)){
            return last.addTrailingCell(cell);
        }
        return false;
    }


    T removeFirst() {
        VirtualRow<T> firstRow = getFirstRow();
        if(firstRow == null){
            return null;
        }
        T cell =  firstRow.removeFirst();
        if(cell != null){
            cellsPiles.addFirst(cell);
        }
        return cell;
    }

    T getAndRemoveCellFromPile(int prefIndex){
        T cell = null;

        for (int i = 0, max = cellsPiles.size(); i < max; i++) {
            T _cell = cellsPiles.get(i);
            assert _cell != null;
            if (_cell.getIndex() == prefIndex) {
                cell = _cell;
                cellsPiles.remove(i);
                break;
            }
        }
        if (cell == null && !cellsPiles.isEmpty()) {
            cell = cellsPiles.removeLast();
        }

        return  cell;
    }

    /**
     * Puts the given cell onto the pile. This is called whenever a cell has
     * fallen off the flow's start.
     */
     void addToPile(T cell) {
        assert cell != null;
        cellsPiles.addLast(cell);
    }

    /**
     * This method will remove all cells from the VirtualFlow and remove them,
     * adding them to the 'pile' (that is, a place from where cells can be used
     * at a later date). This method is protected to allow subclasses to clean up
     * appropriately.
     */
     void addAllToPile() {
        for (int i = 0, max = size(); i < max; i++) {
            addToPile(removeFirst());
        }
    }

    void cleanPile() {
        boolean wasFocusOwner = false;

        for (int i = 0, max = cellsPiles.size(); i < max; i++) {
            T cell = cellsPiles.get(i);
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
     * @param index the index
     * @return the visible cell
     */
    public T getVisibleCell(int index) {
        if (isEmpty()) return null;

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
    T getAvailableCell(int index) {
        // If there are cells, then we will attempt to get an existing cell
        if (!isEmpty()) {
            // First check the cells that have already been created and are
            // in use. If this call returns a value, then we can use it
            T cell = getVisibleCell(index);
            if (cell != null) {
                return cell;
            }
        }

        // check the pile
        for (int i = 0; i < cellsPiles.size(); i++) {
            T cell = cellsPiles.get(i);
            if (cell.getIndex() == index) {
                // Note that we don't remove from the pile: if we do it leads
                // to a severe performance decrease. This seems to be OK, as
                // getCell() is only used for cell measurement purposes.
                // pile.remove(i);
                return cell;
            }
        }

        return null;
    }

     boolean doesCellContainFocus(Cell<?> c) {
        Scene scene = c.getScene();
        final Node focusOwner = scene == null ? null : scene.getFocusOwner();

        if (focusOwner != null) {
            if (c.equals(focusOwner)) {
                return true;
            }

            Parent p = focusOwner.getParent();
            while (p != null && ! (p instanceof VirtualFlow)) {
                if (c.equals(p)) {
                    return true;
                }
                p = p.getParent();
            }
        }

        return false;
    }

    public T getDumpCell() {
       return cellsPiles.getFirst();
    }
}
