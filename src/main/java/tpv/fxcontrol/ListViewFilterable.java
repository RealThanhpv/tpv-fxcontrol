package tpv.fxcontrol;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListViewFilterable<E> extends ListView<E> implements Filterable {

    //    private final ObservableList<E> origins = FXCollections.observableArrayList();
    private final ListFilterMediator<E> filterMediator;
    private final StringProperty filter = new SimpleStringProperty("") {
        @Override
        protected void invalidated() {
            if (get().isEmpty()) {
                reset();
                return;
            }

            doFilter(get());
        }
    };

    public ListViewFilterable() {
        filterMediator = new ListFilterMediator(new ArrayList());
    }

    @Override
    public final StringProperty filterProperty() {
        return filter;
    }

    public final List<E> getSource() {
        return filterMediator.getSource();
    }

    public final void setSource(List<E> items) {
        filterMediator.setSource(items);
        reFilter();
    }


    public void addItems(List<E> items) {
        filterMediator.getSource().addAll(items);
        reFilter();
    }

    public void addItems(E... items) {
        filterMediator.getSource().addAll(Arrays.asList(items));
        reFilter();

    }

    public void removeItem(E item) {
        filterMediator.getSource().remove(item);
        reFilter();
    }

    public void clear() {
        filterMediator.getSource().clear();
        reFilter();
    }

    private void reset() {
        doFilter("");
    }

    private void doFilter(String filter) {
        ListViewFilterable.this.getItems().setAll(filterMediator.filter(filter));
    }

    private void reFilter() {
        doFilter(filter.get());
    }

}
