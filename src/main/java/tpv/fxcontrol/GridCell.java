/**
 * Copyright (c) 2013, 2015, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tpv.fxcontrol;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.control.*;
import tpv.fxcontrol.skin.GridCellSkin;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A GridCell is created to represent items in the {@link GridView}
 * {@link GridView#getItems() items list}. As with other JavaFX UI controls
 * (like {@link ListView}, {@link TableView}, etc), the {@link GridView} control
 * is virtualised, meaning it is exceedingly memory and CPU efficient. Refer to
 * the {@link GridView} class documentation for more details.
 *
 * @see GridView
 */
public class GridCell<T> extends IndexedCell<T> {

    /**************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    /**
     * Creates a default GridCell instance.
     */


    public GridCell() {
        getStyleClass().add("grid-cell"); //$NON-NLS-1$
        setAccessibleRole(AccessibleRole.LIST_ITEM);
        setEditable(false);


//		itemProperty().addListener(new ChangeListener<T>() {
//            @Override public void changed(ObservableValue<? extends T> arg0, T oldItem, T newItem) {
//                updateItem(newItem, newItem == null);
//            }
//        });

        // TODO listen for index change and update index and item, rather than
        // listen to just item update as above. This requires the GridCell to
        // know about its containing GridRow (and the GridRow to know its
        // containing GridView)
        indexProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                final GridView<T> gridView = getGridView();
                if (gridView == null) return;

                if(getIndex() < 0) {
                    updateItem(null, true);
                    return;
                }
                T item = gridView.getItems().get(getIndex());

//                updateIndex(getIndex());
                updateItem(item, item == null);
            }
        });
    }


    /**
     * {@inheritDoc}
     */
    @Override protected Skin<?> createDefaultSkin() {
        return new GridCellSkin<>(this);
    }



    /**************************************************************************
     *
     * Properties
     *
     **************************************************************************/

//	/**
//     * The {@link GridView} that this GridCell exists within.
//     */
//    public SimpleObjectProperty<GridView<T>> gridViewProperty() {
//        return gridView;
//    }
//    private final SimpleObjectProperty<GridView<T>> gridView =
//            new SimpleObjectProperty<>(this, "gridView"); //$NON-NLS-1$

    /**
     * Sets the {@link GridView} that this GridCell exists within.
     */
    public final void updateGridView(GridView<T> gridView) {
        this.gridView.set(gridView);
    }

//    /**
//     * Returns the {@link GridView} that this GridCell exists within.
//     */
//    public GridView<T> getGridView() {
//        return gridView.get();
//    }

    /**
     * Listens to the selectionModel property on the ListView. Whenever the entire model is changed,
     * we have to unhook the weakSelectedListener and update the selection.
     */
    private final ChangeListener<MultipleSelectionModel<T>> selectionModelPropertyListener = new ChangeListener<MultipleSelectionModel<T>>() {
        @Override
        public void changed(
                ObservableValue<? extends MultipleSelectionModel<T>> observable,
                MultipleSelectionModel<T> oldValue,
                MultipleSelectionModel<T> newValue) {

            if (oldValue != null) {
                oldValue.getSelectedIndices().removeListener(weakSelectedListener);
            }

            if (newValue != null) {
                newValue.getSelectedIndices().addListener(weakSelectedListener);
            }
            updateSelection();
        }

    };
    private void updateSelection() {
        if (isEmpty()) return;
        int index = getIndex();
        GridView<T> gridView = getGridView();
        if (index == -1 || gridView == null) return;

        SelectionModel<T> sm = gridView.getSelectionModel();
        if (sm == null) {
            updateSelected(false);
            return;
        }

        boolean isSelected = sm.isSelected(index);
        if (isSelected() == isSelected) return;

        updateSelected(isSelected);
    }


    private void updateEditing() {
        final int index = getIndex();
        final GridView<T> list = getGridView();
        final int editIndex = list == null ? -1 : list.getEditingIndex();
        final boolean editing = isEditing();
        final boolean match = (list != null) && (index != -1) && (index == editIndex);

        if (match && !editing) {
            startEdit();
        } else if (!match && editing) {
            // If my index is not the one being edited then I need to cancel
            // the edit. The tricky thing here is that as part of this call
            // I cannot end up calling list.edit(-1) the way that the standard
            // cancelEdit method would do. Yet, I need to call cancelEdit
            // so that subclasses which override cancelEdit can execute. So,
            // I have to use a kind of hacky flag workaround.
            try {
                // try-finally to make certain that the flag is reliably reset to true
                updateEditingIndex = false;
                cancelEdit();
            } finally {
                updateEditingIndex = true;
            }
        }
    }
    private void updateFocus() {
        int index = getIndex();
        GridView<T> gridView = getGridView();
        if (index == -1 || gridView == null) return;

        FocusModel<T> fm = gridView.getFocusModel();
        if (fm == null) {
            setFocused(false);
            return;
        }

        setFocused(fm.isFocused(index));
    }
    private boolean firstRun = true;
    private void updateItem(int oldIndex) {
        final GridView<T> lv = getGridView();
        final List<T> items = lv == null ? null : lv.getItems();
        final int index = getIndex();
        final int itemCount = items == null ? -1 : items.size();

        // Compute whether the index for this cell is for a real item
        boolean valid = items != null && index >=0 && index < itemCount;

        final T oldValue = getItem();
        final boolean isEmpty = isEmpty();

        // Cause the cell to update itself
        outer: if (valid) {
            final T newValue = items.get(index);

            // RT-35864 - if the index didn't change, then avoid calling updateItem
            // unless the item has changed.
            if (oldIndex == index) {
                if (!isItemChanged(oldValue, newValue)) {
                    // RT-37054:  we break out of the if/else code here and
                    // proceed with the code following this, so that we may
                    // still update references, listeners, etc as required.
                    break outer;
                }
            }
            updateItem(newValue, false);
        } else {
            // RT-30484 We need to allow a first run to be special-cased to allow
            // for the updateItem method to be called at least once to allow for
            // the correct visual state to be set up. In particular, in RT-30484
            // refer to Ensemble8PopUpTree.png - in this case the arrows are being
            // shown as the new cells are instantiated with the arrows in the
            // children list, and are only hidden in updateItem.
            if (!isEmpty || firstRun) {
                updateItem(null, true);
                firstRun = false;
            }
        }
    }
    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The ListView associated with this Cell.
     */
    private ReadOnlyObjectWrapper<GridView<T>> gridView = new ReadOnlyObjectWrapper<GridView<T>>(this, "gridView") {
        /**
         * A weak reference to the ListView itself, such that whenever the ...
         */
        private WeakReference<GridView<T>> weakListViewRef = new WeakReference<GridView<T>>(null);

        @Override protected void invalidated() {
            // Get the current and old list view references
            final GridView<T> currentGridView = get();
            final GridView<T> oldListView = weakListViewRef.get();

            // If the currentListView is the same as the oldListView, then
            // there is nothing to be done.
            if (currentGridView == oldListView) return;

            // If the old list view is not null, then we must unhook all its listeners
            if (oldListView != null) {
                // If the old selection model isn't null, unhook it
                final MultipleSelectionModel<T> sm = oldListView.getSelectionModel();
                if (sm != null) {
                    sm.getSelectedIndices().removeListener(weakSelectedListener);
                }

                // If the old focus model isn't null, unhook it
                final FocusModel<T> fm = oldListView.getFocusModel();
                if (fm != null) {
                    fm.focusedIndexProperty().removeListener(weakFocusedListener);
                }

                // If the old items isn't null, unhook the listener
                final ObservableList<T> items = oldListView.getItems();
                if (items != null) {
                    items.removeListener(weakItemsListener);
                }

                // Remove the listeners of the properties on ListView
                oldListView.editingIndexProperty().removeListener(weakEditingListener);
                oldListView.itemsProperty().removeListener(weakItemsPropertyListener);
                oldListView.focusModelProperty().removeListener(weakFocusModelPropertyListener);
                oldListView.selectionModelProperty().removeListener(weakSelectionModelPropertyListener);
            }

            if (currentGridView != null) {
                final MultipleSelectionModel<T> sm = currentGridView.getSelectionModel();
                if (sm != null) {
                    sm.getSelectedIndices().addListener(weakSelectedListener);
                }

                final FocusModel<T> fm = currentGridView.getFocusModel();
                if (fm != null) {
                    fm.focusedIndexProperty().addListener(weakFocusedListener);
                }

                final ObservableList<T> items = currentGridView.getItems();
                if (items != null) {
                    items.addListener(weakItemsListener);
                }

//                currentGridView.editingIndexProperty().addListener(weakEditingListener);
                currentGridView.itemsProperty().addListener(weakItemsPropertyListener);
                currentGridView.focusModelProperty().addListener(weakFocusModelPropertyListener);
                currentGridView.selectionModelProperty().addListener(weakSelectionModelPropertyListener);

                weakListViewRef = new WeakReference<GridView<T>>(currentGridView);
            }

            updateItem(-1);
            updateSelection();
            updateFocus();
            requestLayout();
        }
    };
    private void setGridView(GridView<T> value) { gridView.set(value); }
    public final GridView<T> getGridView() { return gridView.get(); }
    public final ReadOnlyObjectProperty<GridView<T>> gridViewProperty() { return gridView.getReadOnlyProperty(); }





    /* *************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *     We have to listen to a number of properties on the ListView itself  *
     *     as well as attach listeners to a couple different ObservableLists.  *
     *     We have to be sure to unhook these listeners whenever the reference *
     *     to the ListView changes, or whenever one of the ObservableList      *
     *     references changes (such as setting the selectionModel, focusModel, *
     *     or items).                                                          *
     *                                                                         *
     **************************************************************************/

    /**
     * Listens to the editing index on the ListView. It is possible for the developer
     * to call the ListView#edit(int) method and cause a specific cell to start
     * editing. In such a case, we need to be notified so we can call startEdit
     * on our side.
     */
    private final InvalidationListener editingListener = value -> {
        updateEditing();
    };
    private boolean updateEditingIndex = true;

    /**
     * Listens to the selection model on the ListView. Whenever the selection model
     * is changed (updated), the selected property on the ListCell is updated accordingly.
     */
    private final ListChangeListener<Integer> selectedListener = c -> {

        updateSelection();
    };


    /**
     * Listens to the items on the ListView. Whenever the items are changed in such a way that
     * it impacts the index of this ListCell, then we must update the item.
     */
    private final ListChangeListener<T> itemsListener = c -> {
        boolean doUpdate = false;
        while (c.next()) {
            // RT-35395: We only update the item in this cell if the current cell
            // index is within the range of the change and certain changes to the
            // list have occurred.
            final int currentIndex = getIndex();
            final GridView<T> lv = getGridView();
            final List<T> items = lv == null ? null : lv.getItems();
            final int itemCount = items == null ? 0 : items.size();

            final boolean indexAfterChangeFromIndex = currentIndex >= c.getFrom();
            final boolean indexBeforeChangeToIndex = currentIndex < c.getTo() || currentIndex == itemCount;
            final boolean indexInRange = indexAfterChangeFromIndex && indexBeforeChangeToIndex;

            doUpdate = indexInRange || (indexAfterChangeFromIndex && !c.wasReplaced() && (c.wasRemoved() || c.wasAdded()));
        }

        if (doUpdate) {
            updateItem(-1);
        }
    };

    /**
     * Listens to the items property on the ListView. Whenever the entire list is changed,
     * we have to unhook the weakItemsListener and update the item.
     */
    private final InvalidationListener itemsPropertyListener = new InvalidationListener() {
        private WeakReference<ObservableList<T>> weakItemsRef = new WeakReference<>(null);

        @Override public void invalidated(Observable observable) {
            ObservableList<T> oldItems = weakItemsRef.get();
            if (oldItems != null) {
                oldItems.removeListener(weakItemsListener);
            }

            GridView<T> listView = getGridView();
            ObservableList<T> items = listView == null ? null : listView.getItems();
            weakItemsRef = new WeakReference<>(items);

            if (items != null) {
                items.addListener(weakItemsListener);
            }
            updateItem(-1);
        }
    };

    /**
     * Listens to the focus model on the ListView. Whenever the focus model changes,
     * the focused property on the ListCell is updated
     */
    private final InvalidationListener focusedListener = value -> {
        updateFocus();
    };

    /**
     * Listens to the focusModel property on the ListView. Whenever the entire model is changed,
     * we have to unhook the weakFocusedListener and update the focus.
     */
    private final ChangeListener<FocusModel<T>> focusModelPropertyListener = new ChangeListener<FocusModel<T>>() {
        @Override public void changed(ObservableValue<? extends FocusModel<T>> observable,
                                      FocusModel<T> oldValue,
                                      FocusModel<T> newValue) {
            if (oldValue != null) {
                oldValue.focusedIndexProperty().removeListener(weakFocusedListener);
            }
            if (newValue != null) {
                newValue.focusedIndexProperty().addListener(weakFocusedListener);
            }
            updateFocus();
        }
    };


    private final WeakInvalidationListener weakEditingListener = new WeakInvalidationListener(editingListener);
    private final WeakListChangeListener<Integer> weakSelectedListener = new WeakListChangeListener<Integer>(selectedListener);
    private final WeakChangeListener<MultipleSelectionModel<T>> weakSelectionModelPropertyListener = new WeakChangeListener<MultipleSelectionModel<T>>(selectionModelPropertyListener);
    private final WeakListChangeListener<T> weakItemsListener = new WeakListChangeListener<T>(itemsListener);
    private final WeakInvalidationListener weakItemsPropertyListener = new WeakInvalidationListener(itemsPropertyListener);
    private final WeakInvalidationListener weakFocusedListener = new WeakInvalidationListener(focusedListener);
    private final WeakChangeListener<FocusModel<T>> weakFocusModelPropertyListener = new WeakChangeListener<FocusModel<T>>(focusModelPropertyListener);

}
