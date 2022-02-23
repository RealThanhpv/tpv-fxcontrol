package tpv.fxcontrol;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class Test extends AnchorPane {

    public Test (){
        FXMLLoader loader = new FXMLLoader(Test.class.getResource("Test.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
