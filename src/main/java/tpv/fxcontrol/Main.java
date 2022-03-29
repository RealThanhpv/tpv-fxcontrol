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


public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        AnchorPane root = new AnchorPane();


        Scene scene = new Scene(root, 100, 300);

        TextAreaExtendable2 ta = new TextAreaExtendable2();
        Node ta2 = new TextAreaResizable();

        ta.setPrefWidth(100);


        NullableColorPicker colorOptionNullable = new NullableColorPicker();
        colorOptionNullable.setTranslateX(200);
        colorOptionNullable.setTranslateY(200);
        ta2.setTranslateX(300);
        ta2.setTranslateY(500);
        FlowView<Rectangle> flView = new FlowView<>();
        AnchorPane.setTopAnchor(flView, 0.0);
        AnchorPane.setRightAnchor(flView, 0.0);
        AnchorPane.setBottomAnchor(flView, 0.0);
        AnchorPane.setLeftAnchor(flView, 0.0);
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            Rectangle rectangle = new Rectangle();
            rectangle.setWidth(100);
            rectangle.setHeight(50);
            rectangle.setFill(Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
//            flView.getItems().add("Number "+i);
            flView.getItems().add(rectangle);
        }
        flView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


        root.getChildren().add(flView);
        flView.prefWidthProperty().bind(scene.widthProperty());


        scene.setFill(Color.DARKGRAY);

        stage.setScene(scene);
//        org.scenicview.ScenicView.show(scene);
        CSSFX.start(scene);
        stage.show();

    }


}
