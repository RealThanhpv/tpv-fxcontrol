package tpv.fxcontrol;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;


public class ListViewFilterable<E> extends ListView<E> implements CollectionFilterable {

    private final FilterMediator<E> meditor;
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

    public ListViewFilterable(Callback<ListView<E>, ListCell<E>> cellCallBack){
        this();
        setCellFactory(cellCallBack);
    }

    public ListViewFilterable() {
        meditor = new FilterMediator(FXCollections.observableArrayList());
        meditor.sourceProperty().addListener((observable, oldValue, newValue) -> {
            doFilter(getFilter());
        });
    }

    @Override
    public final StringProperty filterProperty() {
        return filter;
    }

    @Override
    public FilterMediator getMediator() {
        return meditor;
    }

    @Override
    public void doFilter(String filter) {
        ListViewFilterable.this.getItems().setAll(getMediator().filter(filter));
    }

    public List<E> getFilteredItems() {
        return new ArrayList<>(getItems());
    }




}
