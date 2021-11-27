package tpv.fxcontrol;

import javafx.beans.property.StringProperty;

interface Filterable {
    StringProperty filterProperty();
    default String getFilter() {
        return filterProperty().get();
    }
    default void setFilter(String filter) {
        filterProperty().set(filter);
    }

}
