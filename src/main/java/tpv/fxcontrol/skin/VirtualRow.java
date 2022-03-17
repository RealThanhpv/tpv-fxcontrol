package tpv.fxcontrol.skin;

import javafx.scene.Group;
import javafx.scene.Node;
import tpv.fxcontrol.FlowIndexedCell;

public class VirtualRow<T extends FlowIndexedCell> extends Group {
    private Sheet<T> sheet;
    private int index = -1;
    private double height = 1;

    public int getIndex() {
        return index;
    }

    public void updateIndex(int index) {
        this.index = index;
    }

    VirtualRow(Sheet<T> sheet){
        this.sheet = sheet;
    }

    double getWidth() {
        return sheet.getWidth();
    }



    double getHeight() {
        return height;
    }

    void setHeight(double height) {
        if(this.height ==  height){
            return;
        }
        this.height = height;
    }

    boolean isAddAble(T cell){
        return getLayoutBounds().getWidth() + cell.getWidth() < getWidth();
    }

    /**
     *
     * @param cell
     * @return
     */
    boolean addTrailingCell(T cell){
        double layoutX = getLayoutBounds().getWidth();
        if((layoutX + cell.getWidth() ) > getWidth()){
            return false;
        }
        getChildren().add(cell);
        cell.setLayoutX(layoutX);
        cell.setVisible(true);
        return true;
    }


    boolean insertCell( int rowPos, T cell){
        double layoutX = getLayoutBounds().getWidth();
        if((layoutX + cell.getWidth() ) > getWidth()){
            return false;
        }

        getChildren().add(rowPos, cell);

        double cellLayoutX  = 0;

        for (int i = 0; i < rowPos; i++) {
           cellLayoutX += getChildren().get(i).getLayoutBounds().getWidth();
        }
        cell.setLayoutX(cellLayoutX);

        double w = cell.getLayoutBounds().getWidth();

        for (int i = +1; i < getChildren().size(); i++) {
            cellLayoutX = getChildren().get(i).getLayoutBounds().getWidth() + w;
            getChildren().get(i).setLayoutX(cellLayoutX);
        }
        return true;
    }

    boolean addLeadingCell(T cell){
        if((cell.getWidth() + getLayoutBounds().getWidth()) > getWidth()){
            return false;
        }

        double shiftRight = cell.getWidth();
        for (Node child : getChildren()) {
            child.setLayoutX(child.getLayoutX() + shiftRight);
        }
        getChildren().add(cell);
        cell.setLayoutX(0);
        return true;
    }



}
