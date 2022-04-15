package tpv.fxcontrol;

import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Random;


public class TextExtendableApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        AnchorPane root = new AnchorPane();


        Scene scene = new Scene(root, 100, 300);

        TextAreaExtendable2 ta = new TextAreaExtendable2();
        root.getChildren().add(ta);


        scene.setFill(Color.DARKGRAY);

        stage.setScene(scene);
//        org.scenicview.ScenicView.show(scene);
        CSSFX.start(scene);
        stage.show();

    }


}
