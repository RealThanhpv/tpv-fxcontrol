package tpv.fxcontrol.cell; /**
 * @author Thanh
 */

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

public class TextFieldTableCell<S, T> extends TableCell<S, T> {

    private TextField textField;
    private ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<>(this, "converter");

    public TextFieldTableCell() {
        this(null);
    }

    public TextFieldTableCell(StringConverter<T> converter) {
        this.getStyleClass().add("text-area-table-cell");
        setConverter(converter);
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
        return forTableColumn(new DefaultStringConverter());
    }

    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(final StringConverter<T> converter) {
        return list -> new TextFieldTableCell<>(converter);
    }

    private static <T> String getItemText(Cell<T> cell, StringConverter<T> converter) {
        return converter == null ? cell.getItem() == null ? "" : cell.getItem()
                .toString() : converter.toString(cell.getItem());
    }

    private static <T> TextField createTextField(final Cell<T> cell, final StringConverter<T> converter) {
        TextField textField = new TextField(getItemText(cell, converter));

        textField.setOnKeyReleased(t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                cell.cancelEdit();
                t.consume();
            }
        });
        textField.setOnKeyPressed(t -> {
            KeyCode keyCode = t.getCode();
            if (keyCode == KeyCode.ENTER && t.isAltDown()) {
                t.consume();
            } else if (t.getCode() == KeyCode.ENTER && !t.isAltDown()) {
                if (converter == null) {
                    throw new IllegalStateException(
                            "Attempting to convert text input into Object, but provided "
                                    + "StringConverter is null. Be sure to set a StringConverter "
                                    + "in your cell factory.");
                }
                cell.commitEdit(converter.fromString(textField.getText()));
                t.consume();
            }

        });
        return textField;
    }

    private static <T> void cancelEdit(Cell<T> cell, final StringConverter<T> converter) {
        cell.setText(getItemText(cell, converter));
        cell.setGraphic(null);
    }

    private void startEdit(final Cell<T> cell, final StringConverter<T> converter) {
        textField.setText(getItemText(cell, converter));

        cell.setText(null);
        cell.setGraphic(textField);

        textField.selectAll();
        textField.requestFocus();
    }

    private void updateItem(final Cell<T> cell, final StringConverter<T> converter) {

        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);

        } else {
            if (cell.isEditing()) {
                if (textField != null) {
                    textField.setText(getItemText(cell, converter));
                }
                cell.setText(null);
                cell.setGraphic(textField);
            } else {
                cell.setText(getItemText(cell, converter));
                cell.setGraphic(null);
            }
        }
    }

    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    public final StringConverter<T> getConverter() {
        return converterProperty().get();
    }

    public final void setConverter(StringConverter<T> value) {
        converterProperty().set(value);
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }

        super.startEdit();

        if (isEditing()) {
            if (textField == null) {
                textField = createTextField(this, getConverter());
            }

            startEdit(this, getConverter());
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        cancelEdit(this, getConverter());
    }

    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        updateItem(this, getConverter());
    }

}