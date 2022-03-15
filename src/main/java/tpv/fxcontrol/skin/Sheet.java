package tpv.fxcontrol.skin;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import tpv.fxcontrol.FlowIndexedCell;

public class Sheet<T extends FlowIndexedCell> extends Group {
    /**
     * The list of cells representing those cells which actually make up the
     * current view. The cells are ordered such that the first cell in this
     * list is the first in the view, and the last cell is the last in the
     * view. When pixel scrolling, the list is simply shifted and items drop
     * off the beginning or the end, depending on the order of scrolling.
     * <p>
     * This is package private ONLY FOR TESTING
     */
    final VirtualFlow.ArrayLinkedList<T> cells = new VirtualFlow.ArrayLinkedList<T>();



     T get(int i) {
        return cells.get(i);
    }

     int size() {
        return cells.size();
    }

     void clear() {
        cells.clear();
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
}
