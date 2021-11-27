package tpv.fxcontrol;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

final class FilterMediator<E> {

    private final ObjectProperty<ObservableList<E>> source = new SimpleObjectProperty<>();
    FilterMediator(){
        source.set(FXCollections.observableArrayList());
    }

    FilterMediator(final ObservableList<E> source) {
        this.source.set(source);
    }

    public ObservableList<E> getSource() {
        return source.get();
    }

    public void setSource(ObservableList<E> source) {
        this.source.set(source);
    }

    public ObjectProperty<ObservableList<E>> sourceProperty() {
        return source;
    }

    final List<E> filter(String s) {
        if (s == null) {
            s = "";
        }

        return doFilter(s);
    }

    private boolean isMatch(E e, String filter) {
        return e.toString().toUpperCase().contains(filter.toUpperCase());
    }

    private List<E> doFilter(String filter) {
        List<E> filters = new ArrayList<>();

        for (E o : getSource()) {
            if (isMatch(o, filter)) {
                filters.add(o);
            }
        }

        return filters;

    }


}
