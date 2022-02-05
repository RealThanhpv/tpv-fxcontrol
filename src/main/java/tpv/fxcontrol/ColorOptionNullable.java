package tpv.fxcontrol;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class ColorOptionNullable extends StackPane {

    private final ColorPicker colorPicker = new ColorPicker();
    private final Line stroke = new Line();
    private final Rectangle eventTaker = new Rectangle();
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>();

    public ColorOptionNullable() {
        getChildren().addAll(colorPicker);
        colorPicker.setStyle(
                "-fx-color-label-visible:false;"

        );

        colorPicker.getStylesheets().add(getUserAgentStylesheet());


        eventTaker.setWidth(getMaxWidth()-1);
        eventTaker.setHeight(getMaxHeight()-1);
        eventTaker.setStrokeWidth(4);
        eventTaker.setStroke(Color.WHITE);
        eventTaker.setFill(Color.TRANSPARENT);

        stroke.setStartY(getMaxWidth());
        stroke.setEndX(getMaxHeight());
        stroke.setMouseTransparent(true);
        stroke.setStrokeWidth(2);

        eventTaker.widthProperty().bind(maxWidthProperty());
        eventTaker.heightProperty().bind(maxHeightProperty());
        stroke.startYProperty().bind(maxWidthProperty());
        stroke.endXProperty().bind(maxHeightProperty().subtract(1));



        setMaxWidth(14);
        setMaxHeight(14);

        stroke.setStroke(Color.RED);

        getChildren().add(eventTaker);

        color.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                getChildren().remove(stroke);
            } else {
                if (!getChildren().contains(stroke)) {
                    getChildren().add(stroke);
                }
            }
        });

        color.set(colorPicker.getValue());

        eventTaker.setOnMouseClicked(event -> {
            if (event.getClickCount() < 2) {
                if (color.get() == null) {
                    color.set(colorPicker.getValue());
                } else {
                    color.set(null);
                }
                event.consume();

            } else {
                colorPicker.show();
            }

        });

        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            color.set(newValue);
        });

    }
    @Override
    public String getUserAgentStylesheet() {
        return ColorOptionNullable.class.getResource("/tpv/fxcontrol/coloroption.css").toExternalForm();
    }

    public ObjectProperty<Color> valueProperty() {
        return color;
    }

    public Color getValue() {
        return color.get();
    }

    public void setValue(Color color) {
        colorPicker.setValue(color);
    }
}
