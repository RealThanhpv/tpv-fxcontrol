package tpv.fxcontrol;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TableViewFilterable<T> extends TableView<T> implements Filterable {

    private final ListFilterMediator mediator;
    private final StringProperty filter = new SimpleStringProperty() {
        @Override
        protected void invalidated() {
            if (get().isEmpty()) {
                reset();
                return;
            }

            doFilter(get());
        }
    };

    public TableViewFilterable(Callback<TableView<T>, TableRow<T>> rowCallback){
        this();
        setRowFactory(rowCallback);
    }

    public TableViewFilterable() {
        mediator = new ListFilterMediator(new ArrayList());
    }

    final public List<T> getSource() {
        return mediator.getSource();
    }

    public void setSource(final List<T> items) {
        mediator.setSource(items);
        reFilter();

    }

    public List<T> getFilteredItems() {
        return new ArrayList<>(getItems());
    }

    public void addItems(ObservableList<T> items) {
        mediator.getSource().addAll(items);
        reFilter();
    }

    public void addItems(T... items) {
        mediator.getSource().addAll(Arrays.asList(items));
        reFilter();

    }

    public boolean removeItem(T item) {
        boolean r = mediator.getSource().remove(item);
        reFilter();
        return r;
    }

    public void addItem(int index, T item) {
        mediator.getSource().add(index, item);
        reFilter();
    }

    @Override
    public final StringProperty filterProperty() {
        return filter;
    }

    private void doFilter(String filter) {
        getItems().setAll(mediator.filter(filter));
    }

    private void reFilter() {
        doFilter(filter.get());
    }

    private void reset() {
        doFilter("");
    }

    final public void clear() {
        mediator.getSource().clear();
        reFilter();
    }


}
