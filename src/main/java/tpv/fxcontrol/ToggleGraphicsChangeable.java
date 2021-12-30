package tpv.fxcontrol;

import javafx.scene.Node;
import javafx.scene.control.ToggleButton;

public class ToggleGraphicsChangeable extends ToggleButton {
    private Node g1;
    private Node g2;
    public ToggleGraphicsChangeable(){}
    public ToggleGraphicsChangeable(Node g1, Node g2){
        this.g1 = g1;
        this.g2 = g2;
        setGraphic(g1);
        selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                setGraphic(g2);
            }
            else {
                setGraphic(g1);
            }
        });
    }
}


