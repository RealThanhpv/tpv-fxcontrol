package tpv.fxcontrol.skin;

import java.util.AbstractList;
import java.util.ArrayList;

/**
     * A List-like implementation that is exceedingly efficient for the purposes
     * of the VirtualFlow. Typically there is not much variance in the number of
     * cells -- it is always some reasonably consistent number. Yet for efficiency
     * in code, we like to use a linked list implementation so as to append to
     * start or append to end. However, at times when we need to iterate, LinkedList
     * is expensive computationally as well as requiring the construction of
     * temporary iterators.
     * <p>
     * This linked list like implementation is done using an array. It begins by
     * putting the first item in the center of the allocated array, and then grows
     * outward (either towards the first or last of the array depending on whether
     * we are inserting at the head or tail). It maintains an index to the start
     * and end of the array, so that it can efficiently expose iteration.
     * <p>
     * This class is package private solely for the sake of testing.
     */
     class ArrayLinkedList<T> extends AbstractList<T> {
        /**
         * The array list backing this class. We default the size of the array
         * list to be fairly large so as not to require resizing during normal
         * use, and since that many ArrayLinkedLists won't be created it isn't
         * very painful to do so.
         */
        private final ArrayList<T> array;

        private int firstIndex = -1;
        private int lastIndex = -1;

        public ArrayLinkedList() {
            array = new ArrayList<T>(50);
            for (int i = 0; i < 50; i++) {
                array.add(null);
            }
        }

        public T getFirst() {
            return firstIndex == -1 ? null : array.get(firstIndex);
        }

        public T getLast() {
            return lastIndex == -1 ? null : array.get(lastIndex);
        }

        public void addFirst(T cell) {
            // if firstIndex == -1 then that means this is the first item in the
            // list and we need to initialize firstIndex and lastIndex
            if (firstIndex == -1) {
                firstIndex = lastIndex = array.size() / 2;
                array.set(firstIndex, cell);
            } else if (firstIndex == 0) {
                // we're already at the head of the array, so insert at position
                // 0 and then increment the lastIndex to compensate
                array.add(0, cell);
                lastIndex++;
            } else {
                // we're not yet at the head of the array, so insert at the
                // firstIndex - 1 position and decrement first position
                array.set(--firstIndex, cell);
            }
        }

        public void addLast(T cell) {

            // if lastIndex == -1 then that means this is the first item in the
            // list and we need to initialize the firstIndex and lastIndex
            if (firstIndex == -1) {
                firstIndex = lastIndex = array.size() / 2;
                array.set(lastIndex, cell);
            } else if (lastIndex == array.size() - 1) {
                // we're at the end of the array so need to "add" so as to force
                // the array to be expanded in size
                array.add(++lastIndex, cell);
            } else {
                array.set(++lastIndex, cell);
            }
        }

        public int size() {
            return firstIndex == -1 ? 0 : lastIndex - firstIndex + 1;
        }

        public boolean isEmpty() {
            return firstIndex == -1;
        }

        public T get(int index) {
            if (index > (lastIndex - firstIndex) || index < 0) {
                // Commented out exception due to RT-29111
                // throw new java.lang.ArrayIndexOutOfBoundsException();
                return null;
            }

            return array.get(firstIndex + index);
        }

        public void clear() {
            for (int i = 0; i < array.size(); i++) {
                array.set(i, null);
            }

            firstIndex = lastIndex = -1;
        }

        public T removeFirst() {
            if (isEmpty()) return null;
            return remove(0);
        }

        public T removeLast() {
            if (isEmpty()) return null;
            return remove(lastIndex - firstIndex);
        }

        public T remove(int index) {
            if (index > lastIndex - firstIndex || index < 0) {
                throw new ArrayIndexOutOfBoundsException();
            }

            // if the index == 0, then we're removing the first
            // item and can simply set it to null in the array and increment
            // the firstIndex unless there is only one item, in which case
            // we have to also set first & last index to -1.
            if (index == 0) {
                T cell = array.get(firstIndex);
                array.set(firstIndex, null);
                if (firstIndex == lastIndex) {
                    firstIndex = lastIndex = -1;
                } else {
                    firstIndex++;
                }
                return cell;
            } else if (index == lastIndex - firstIndex) {
                // if the index == lastIndex - firstIndex, then we're removing the
                // last item and can simply set it to null in the array and
                // decrement the lastIndex
                T cell = array.get(lastIndex);
                array.set(lastIndex--, null);
                return cell;
            } else {
                // if the index is somewhere in between, then we have to remove the
                // item and decrement the lastIndex
                T cell = array.get(firstIndex + index);
                array.set(firstIndex + index, null);
                for (int i = (firstIndex + index + 1); i <= lastIndex; i++) {
                    array.set(i - 1, array.get(i));
                }
                array.set(lastIndex--, null);
                return cell;
            }
        }
    }