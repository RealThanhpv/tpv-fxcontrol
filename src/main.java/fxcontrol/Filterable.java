package ptk.fxcontrols;

import javafx.beans.property.StringProperty;

public interface Filterable {
    StringProperty filterProperty();
    default String getFilter() {
        return filterProperty().get();
    }
    default void setFilter(String filter) {
        filterProperty().set(filter);
    }

}
