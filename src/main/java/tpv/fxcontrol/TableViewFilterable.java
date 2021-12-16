package tpv.fxcontrol;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;


public class TableViewFilterable<T> extends TableView<T> implements CollectionFilterable<T> {

    private final FilterMediator<T> mediator;
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
        mediator = new FilterMediator(FXCollections.observableArrayList());
        mediator.sourceProperty().addListener((observable, oldValue, newValue) -> {
            doFilter(getFilter());
        });
    }

    @Override
    public FilterMediator<T> getMediator() {
        return mediator;
    }

    public List<T> getFilteredItems() {
        return new ArrayList<>(getItems());
    }

    @Override
    public final StringProperty filterProperty() {
        return filter;
    }

    @Override
    public void doFilter(String filter) {
        TableViewFilterable.this.getItems().setAll(getMediator().filter(filter));
    }





}
