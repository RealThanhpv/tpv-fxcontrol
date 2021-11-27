package tpv.fxcontrol;

import java.util.ArrayList;
import java.util.List;

final class ListFilterMediator<E> {

    private List<E> source;

     ListFilterMediator(final List<E> source) {
        this.source = source;
    }

    final  List<E> filter(String s) {
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

        for (E o : source) {
            if (isMatch(o, filter)) {
                filters.add(o);
            }
        }

        return filters;

    }

    final List<E> getSource() {
        return source;
    }

    final void setSource(List<E> list) {
        this.source = list;
    }
}
