package tpv.fxcontrol;

import javafx.collections.ObservableList;

import java.util.Arrays;
import java.util.List;

interface CollectionFilterable<E> extends Filterable {
    FilterMediator getMediator();


    default List<E> getSource() {
        return getMediator().getSource();
    }

    default void setSource(ObservableList<E> source){
        getMediator().setSource(source);
    }
    default void setSource(List<E> items) {
        getMediator().getSource().setAll(items);
        reFilter();
    }


    default void addItems(List<E> items) {
        getMediator().getSource().addAll(items);
        reFilter();
    }

    default void addItems(E... items) {
        getMediator().getSource().addAll(Arrays.asList(items));
        reFilter();

    }

    default void removeItems(E... items) {
        getMediator().getSource().removeAll(items);
        reFilter();
    }

    default void removeItems(List<E> items){
        getMediator().getSource().removeAll(items);
        reFilter();
    }

    default void clear() {
        getMediator().getSource().clear();
        reFilter();
    }

    default void reset() {
        doFilter("");
    }

    void doFilter(String filter);

    private void reFilter() {
        doFilter(getFilter());
    }

}