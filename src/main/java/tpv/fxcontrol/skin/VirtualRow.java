package tpv.fxcontrol.skin;

import javafx.scene.Group;
import javafx.scene.Node;
import tpv.fxcontrol.FlowIndexedCell;

class VirtualRow<T extends FlowIndexedCell> extends Group {
    private Sheet<T> sheet;
    private double height;

    VirtualRow(Sheet<T> sheet){
        this.sheet = sheet;
    }

    double getWidth(){
        return sheet.getViewPortWidth();
    }



    boolean isAddable(T cell){
       double[] size = sheet.getOrCreateCacheCellSize(cell.getIndex());
       double cw = size !=  null? size[0]: 0;
       double w = getLayoutBounds().getWidth() + cw;
       return w < sheet.getViewPortWidth();
    }



    boolean appendCell(T cell){
        boolean addable = isAddable(cell);
        if(!addable){
            return false;
        }
        double layoutX  = getLayoutBounds().getWidth();
        double cellHeight  = cell.getLayoutBounds().getHeight();

        getChildren().add(cell);
        cell.setLayoutX(layoutX);
        if(cellHeight > height){
            height  = cellHeight;
        }
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

    T prependFallOff(T cell){
        throw new NullPointerException("Need implementation");
    }

}
