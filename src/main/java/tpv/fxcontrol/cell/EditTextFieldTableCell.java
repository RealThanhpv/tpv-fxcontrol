package tpv.fxcontrol.cell;

import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.util.function.Function;

/**
 *
 * Edit from https://gist.github.com/james-d/be5bbd6255a4640a5357
 */
public class EditTextFieldTableCell<S, T> extends TableCell<S, T> {

    /**
     * Convenience converter that does nothing (converts Strings to themselves and vice-versa...).
     */
    public static final StringConverter<String> IDENTITY_CONVERTER = new StringConverter<String>() {

        @Override
        public String toString(String object) {
            return object;
        }

        @Override
        public String fromString(String string) {
            return string;
        }

    };
    // Text field for editing
    // TODO: allow this to be a plugable control.
    private final TextField editor = new TextField();
    // Converter for converting the text in the text field to the user type, and vice-versa:
    private final StringConverter<T> converter;

    public EditTextFieldTableCell(StringConverter<T> converter) {
        this.converter = converter;

        itemProperty().addListener((obx, oldItem, newItem) -> {
            if (newItem == null) {
                setText(null);
            } else {
                setText(converter.toString(newItem));
            }
        });
        setGraphic(editor);
        setContentDisplay(ContentDisplay.TEXT_ONLY);

        editor.setOnAction(evt -> {
            commitEdit(this.converter.fromString(editor.getText()));
        });
        editor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitEdit(this.converter.fromString(editor.getText()));
            }
        });
        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                editor.setText(converter.toString(getItem()));
                cancelEdit();
                event.consume();
            }
//            else if (event.getCode() == KeyCode.RIGHT) {
//                getTableView().getSelectionModel().selectRightCell();
//                event.consume();
//            } else if (event.getCode() == KeyCode.LEFT) {
//                getTableView().getSelectionModel().selectLeftCell();
//                event.consume();
//            } else if (event.getCode() == KeyCode.UP) {
//                getTableView().getSelectionModel().selectAboveCell();
//                event.consume();
//            } else if (event.getCode() == KeyCode.DOWN) {
//                getTableView().getSelectionModel().selectBelowCell();
//                event.consume();
//            }
        });
    }

    /**
     * Convenience method for creating an EditCell for a String value.
     *
     * @return
     */
    public static <S> EditTextFieldTableCell<S, String> createStringEditCell() {
        return new EditTextFieldTableCell<S, String>(IDENTITY_CONVERTER);
    }

    public static <T> TableColumn<T, String> createColumn(String title, Function<T, StringProperty> property) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cellData -> property.apply(cellData.getValue()));

        col.setCellFactory(column -> EditTextFieldTableCell.createStringEditCell());
        return col;
    }

    // set the text of the text field and display the graphic
    @Override
    public void startEdit() {
        super.startEdit();
        editor.setText(converter.toString(getItem()));
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        editor.requestFocus();
    }

    // revert to text display
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    // commits the edit. Update property if possible and revert to text display
    @Override
    public void commitEdit(T item) {

        // This block is necessary to support commit on losing focus, because the baked-in mechanism
        // sets our editing state to false before we can intercept the loss of focus.
        // The default commitEdit(...) method simply bails if we are not editing...
        if (!isEditing() && !item.equals(getItem())) {
            TableView<S> table = getTableView();
            if (table != null) {
                TableColumn<S, T> column = getTableColumn();
                CellEditEvent<S, T> event = new CellEditEvent<>(table,
                        new TablePosition<S, T>(table, getIndex(), column),
                        TableColumn.editCommitEvent(), item);
                Event.fireEvent(column, event);
            }
        }

        super.commitEdit(item);

        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

}