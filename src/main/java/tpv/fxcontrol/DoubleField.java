
package tpv.fxcontrol;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.util.StringConverter;

public final class DoubleField extends TextField {
    private static final Pattern validEditingState = Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?");
    private static final UnaryOperator<Change> filter = (c) -> {
        String text = c.getControlNewText();
        return validEditingState.matcher(text).matches() ? c : null;
    };
    private static final StringConverter<Double> converter = new StringConverter<>() {
        public Double fromString(String s) {
            return !s.isEmpty() && !"-".equals(s) && !".".equals(s) && !"-.".equals(s) ? Double.valueOf(s) : 0.0D;
        }

        public String toString(Double d) {
            return d.toString();
        }
    };
    private final TextFormatter<Double> textFormatter;

    public DoubleField() {
        this.textFormatter = new TextFormatter(converter, 0.0D, filter);
        this.setTextFormatter(this.textFormatter);
    }

    public double getValue() {
        return this.textFormatter.getValue();
    }

    public void setValue(double value) {
        this.textFormatter.setValue(value);
    }

    public ObjectProperty<Double> valueProperty() {
        return this.textFormatter.valueProperty();
    }
}