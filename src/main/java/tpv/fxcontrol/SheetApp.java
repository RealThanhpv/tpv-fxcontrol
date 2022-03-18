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
import tpv.fxcontrol.skin.Sheet;

import java.util.Random;

public class SheetApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        AnchorPane root = new AnchorPane();
        Scene scene = new Scene(root, 100, 300);

        Sheet<FlowIndexedCell<Rectangle>> sheet = new Sheet<>();
        sheet.setWidth(100);

        root.getChildren().add(sheet);

        scene.setFill(Color.DARKGRAY);

        stage.setScene(scene);

        for (int i = 0; i < 20; i++) {
            Rectangle rect = new Rectangle();
            rect.setWidth(50);
            rect.setHeight(50);
            FlowIndexedCell cell = new FlowIndexedCell<>();
            cell.setGraphic(rect);
            sheet.addLast(cell);
        }


        org.scenicview.ScenicView.show(scene);
        CSSFX.start(scene);
        stage.show();

    }


}
