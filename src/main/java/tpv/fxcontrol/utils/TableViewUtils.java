package tpv.fxcontrol.utils;

import java.lang.reflect.Field;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.input.MouseEvent;



/**
 * https://stackoverflow.com/questions/27739833/adapt-tableview-menu-button
 */
public class TableViewUtils {

    /**
     * Make table menu button visible and replace the context menu with a custom context menu via reflection.
     * The preferred height is modified so that an empty header row remains visible. This is needed in case you remove all columns, so that the menu button won't disappear with the row header.
     * IMPORTANT: Modification is only possible AFTER the table has been made visible, otherwise you'd get a NullPointerException
     * @param tableView
     */
    public static void addCustomTableMenu(TableView tableView) {
        tableView.getContextMenu();
        // enable table menu
        tableView.setTableMenuButtonVisible(true);

        // get the table  header row
        TableHeaderRow tableHeaderRow = getTableHeaderRow((TableViewSkin) tableView.getSkin());

        // get context menu via reflection
        ContextMenu contextMenu = getContextMenu(tableHeaderRow);

        // setting the preferred height for the table header row
        // if the preferred height isn't set, then the table header would disappear if there are no visible columns
        // and with it the table menu button
        // by setting the preferred height the header will always be visible
        // note: this may need adjustments in case you have different heights in columns (eg when you use grouping)
        double defaultHeight = tableHeaderRow.getHeight();
        tableHeaderRow.setPrefHeight(defaultHeight);

        // modify the table menu
        contextMenu.getItems().clear();

        addCustomMenuItems( contextMenu, tableView);

    }

    /**
     * Create a menu with custom items. The important thing is that the menu remains open while you click on the menu items.
     * @param cm
     * @param table
     */
    private static void addCustomMenuItems( ContextMenu cm, TableView table) {

        // create new context menu
        CustomMenuItem cmi;

        // select all item
        Label selectAll = new Label("Select all");
        selectAll.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                for (Object obj : table.getColumns()) {
                    ((TableColumn<?, ?>) obj).setVisible(true);
                }
            }

        });

        cmi = new CustomMenuItem(selectAll);
        cmi.setHideOnClick(false);
        cm.getItems().add(cmi);

        // deselect all item
        Label deselectAll = new Label("Deselect all");
        deselectAll.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {

                for (Object obj : table.getColumns()) {
                    ((TableColumn<?, ?>) obj).setVisible(false);
                }
            }

        });

        cmi = new CustomMenuItem(deselectAll);
        cmi.setHideOnClick(false);
        cm.getItems().add(cmi);

        // separator
        cm.getItems().add(new SeparatorMenuItem());

        // menu item for each of the available columns
        for (Object obj : table.getColumns()) {

            TableColumn<?, ?> tableColumn = (TableColumn<?, ?>) obj;

            CheckBox cb = new CheckBox(tableColumn.getText());
            cb.selectedProperty().bindBidirectional(tableColumn.visibleProperty());

            cmi = new CustomMenuItem(cb);
            cmi.setHideOnClick(false);

            cm.getItems().add(cmi);
        }

    }

    /**
     * Find the TableHeaderRow of the TableViewSkin
     * 
     * @param tableSkin
     * @return
     */
    private static TableHeaderRow getTableHeaderRow(TableViewSkin<?> tableSkin) {

        // get all children of the skin
        ObservableList<Node> children = tableSkin.getChildren();

        // find the TableHeaderRow child
        for (int i = 0; i < children.size(); i++) {

            Node node = children.get(i);

            if (node instanceof TableHeaderRow) {
                return (TableHeaderRow) node;
            }

        }
        return null;
    }

    /**
     * Get the table menu, i. e. the ContextMenu of the given TableHeaderRow via
     * reflection
     * 
     * @param headerRow
     * @return
     */
    private static ContextMenu getContextMenu(TableHeaderRow headerRow) {

        try {

            // get columnPopupMenu field
            Field privateContextMenuField = TableHeaderRow.class.getDeclaredField("columnPopupMenu");

            // make field public
            privateContextMenuField.setAccessible(true);

            // get field
            ContextMenu contextMenu = (ContextMenu) privateContextMenuField.get(headerRow);

            return contextMenu;

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

}