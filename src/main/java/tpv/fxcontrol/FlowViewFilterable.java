
package tpv.fxcontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;

public class FlowViewFilterable<E> extends ScrollPane implements Filterable {
    private final FilterMediator<E> mediator = new FilterMediator();
    private final FlowPane flowPane;
    private final StringProperty filter = new SimpleStringProperty();
    private DataViewConverter<E, Node> converter;

    public FlowViewFilterable(double hgap, double vgap) {
        this.flowPane = new FlowPane(hgap, vgap);
        this.setContent(this.flowPane);
        this.flowPane.prefWidthProperty().bind(this.widthProperty());
        this.setHbarPolicy(ScrollBarPolicy.NEVER);
    }

    public final void setHGap(double hgap) {
        this.flowPane.setHgap(hgap);
    }

    public final void setVGap(double vgap) {
        this.flowPane.setVgap(vgap);
    }

    public final void setDataViewConverter(DataViewConverter<E, Node> converter) {
        this.converter = converter;
    }

    public final StringProperty filterProperty() {
        return this.filter;
    }

    public final List<E> getFilteredItems() {
        return this.revertList(this.flowPane.getChildren());
    }

    private void reset() {
        this.convertAndSetAll(this.mediator.getSource());
    }

    private List<E> revertList(List<Node> nodes) {
        return (List)nodes.stream().map((n) -> {
            return this.converter.toData(n);
        }).collect(Collectors.toList());
    }

    private void convertAndAdd(E item) {
        this.flowPane.getChildren().add(this.converter.toView(item));
    }

    private void convertAndAddAll(List<E> items) {
        Iterator<E> iterator = items.iterator();

        while(iterator.hasNext()) {
            E item = iterator.next();
            this.convertAndAdd(item);
        }

    }

    private void convertAndSetAll(List<E> items) {
        this.flowPane.getChildren().clear();
        this.convertAndAddAll(items);
    }

    private void reFilter() {
        this.convertAndSetAll(this.mediator.filter((String)this.filter.get()));
    }

    private void doFilter(String filter) {
        this.convertAndSetAll(this.mediator.filter(filter));
    }

    public final void clear() {
        this.mediator.getSource().clear();
        this.reFilter();
    }

    public final void addItems(List<E> items) {
        this.mediator.getSource().addAll(items);
        this.convertAndAddAll(items);
    }

    public final void addItems(E... items) {
        this.mediator.getSource().addAll(Arrays.asList(items));
        this.convertAndAddAll(Arrays.asList(items));
    }

    public final void removeItem(E item) {
        this.mediator.getSource().remove(item);
        this.reFilter();
    }

    public final List<E> getSource() {
        return this.mediator.getSource();
    }
}
