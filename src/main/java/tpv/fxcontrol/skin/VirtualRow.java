package tpv.fxcontrol.skin;

import javafx.scene.Group;
import javafx.scene.Node;
import tpv.fxcontrol.FlowIndexedCell;

import java.util.List;
import java.util.stream.Collectors;

public class VirtualRow<T extends FlowIndexedCell> extends Group {
    private Sheet<T> sheet;
    private int index = -1;

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

    boolean isAddAble(T cell){
        return getLayoutBounds().getWidth() + cell.getLayoutBounds().getWidth() < getWidth();
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
        if((layoutX + cell.getLayoutBounds().getWidth() ) > getWidth()){
            return false;
        }

        getChildren().add(rowPos, cell);

        double cellLayoutX  = 0;

        for (int i = 0; i < rowPos; i++) {
           cellLayoutX += getChildren().get(i).getLayoutBounds().getWidth();
        }
        cell.setLayoutX(cellLayoutX);

        double w = cell.getLayoutBounds().getWidth();

        for (int i = rowPos ; i < getChildren().size(); i++) {
            cellLayoutX = getChildren().get(i).getLayoutBounds().getWidth() + w;
            getChildren().get(i).setLayoutX(cellLayoutX);
        }
        return true;
    }

    boolean addLeadingCell(T cell){

        if((cell.getWidth() + cell.getPrefWidth()) > getWidth()){
            return false;
        }

        double shiftRight = cell.getPrefWidth();
        for (Node child : getChildren()) {
            child.setLayoutX(child.getLayoutX() + shiftRight);
        }
        getChildren().add(cell);
        cell.setLayoutX(0);
        return true;
    }

    public T getFirstCell() {
        if(getChildren().isEmpty()){
            return null;
        }
        return (T) getChildren().get(0);
    }


    double getCellPosition(T cell) {
        List<T> cells = getCells();
        T found = null;
        for (T t : cells) {
            if(t.getIndex() == cell.getIndex()){
                found = t;
                break;
            }
        }

        if(found != null){
            return found.getLayoutX();
        }
        return  - 1;
    }

    private List<T> getCells() {
        return getChildren().stream().map(n->(T)n).collect(Collectors.toList());
    }

    public T getLastCell() {
        if(getChildren().isEmpty()){
            return null;
        }

        return (T) getChildren().get(getChildren().size()-1);
    }

    public T removeFirst() {
        return (T) getChildren().remove(0);
    }
}
