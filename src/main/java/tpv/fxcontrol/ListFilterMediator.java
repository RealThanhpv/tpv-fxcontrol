package tpv.fxcontrol;

import java.util.ArrayList;
import java.util.List;

public final class ListFilterMediator<E> {

    private List<E> source;

    public ListFilterMediator(final List<E> source) {
        this.source = source;
    }

    final public List<E> filter(String s) {
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

    public List<E> getSource() {
        return source;
    }

    public void setSource(List<E> list) {
        this.source = list;
    }
}
