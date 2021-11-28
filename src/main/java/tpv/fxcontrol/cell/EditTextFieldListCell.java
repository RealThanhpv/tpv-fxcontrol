package tpv.fxcontrol.cell;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;

/**
 *
 * Extend from https://gist.github.com/james-d/be5bbd6255a4640a5357
 */
abstract public class EditTextFieldListCell<T>  extends ListCell<T> implements Committer<T> {

    private final TextField editor = new TextField();

    public EditTextFieldListCell() {
        itemProperty().addListener((obx, oldItem, newItem) -> {
            if (newItem == null) {
                setText(null);
            } else {
                setText(getText(newItem));
            }
        });

        setGraphic(editor);
        setContentDisplay(ContentDisplay.TEXT_ONLY);

        editor.setOnAction(evt -> {
            commitEdit(getItem());
        });
        editor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitEdit(getItem());
            }
        });
//        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
//            if (event.getCode() == KeyCode.ESCAPE) {
//                editor.setText(getItem().toString());
//                cancelEdit();
//                event.consume();
//            }
//            else if(event.getCode() == KeyCode.ENTER){
//                commitEdit();
//                event.consume();
//            }
//
////            else if (event.getCode() == KeyCode.RIGHT) {
////                getTableView().getSelectionModel().selectRightCell();
////                event.consume();
////            } else if (event.getCode() == KeyCode.LEFT) {
////                getTableView().getSelectionModel().selectLeftCell();
////                event.consume();
////            } else if (event.getCode() == KeyCode.UP) {
////                getTableView().getSelectionModel().selectAboveCell();
////                event.consume();
////            } else if (event.getCode() == KeyCode.DOWN) {
////                getTableView().getSelectionModel().selectBelowCell();
////                event.consume();
////            }
//        });
    }



    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    @Override
    public void startEdit() {
        super.startEdit();
        editor.setText(getText(getItem()));
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        editor.requestFocus();

    }

    @Override
    public void commitEdit(T item) {
        commit(editor.getText(), item);
        super.commitEdit(item);
        setText(getText(item));
        setContentDisplay(ContentDisplay.TEXT_ONLY);


    }


}

