package tpv.fxcontrol.skin;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.layout.HBox;
import tpv.fxcontrol.FlowIndexedCell;

class VirtualRow<T extends FlowIndexedCell> extends Group {
    private Sheet<T> sheet;

    VirtualRow(Sheet<T> sheet){
        this.sheet = sheet;
    }

    boolean isAddable(T cell){
       double[] size = sheet.getOrCreateCacheCellSize(cell.getIndex());
       double w = getLayoutBounds().getWidth() + size[0];
       return w < sheet.getViewPortWidth();
    }

    boolean appendCell(T cell){
        boolean addable = isAddable(cell);
        if(!addable){
            return false;
        }
        double layoutX  = getLayoutBounds().getWidth();
        getChildren().add(cell);
        cell.setLayoutX(layoutX);
        return true;
    }

    /**
     *
     * @param cell
     * @return The fall off cell (if any)
     */
    boolean prependCell(T cell){
        boolean addable = isAddable(cell);
        if(!addable){
            return false;
        }

        double[] size = sheet.getOrCreateCacheCellSize(cell.getIndex());
        double width  =  size[1];

        for (Node child : getChildren()) {
            child.setLayoutX(child.getLayoutX()+width);
        }
        getChildren().add(0, cell);

        return true;
    }

    T prependFalloff(T cell){
        throw new NullPointerException("Need implementation");
    }

}
