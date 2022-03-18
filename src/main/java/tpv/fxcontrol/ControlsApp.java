package tpv.fxcontrol;

import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Random;

public class ControlsApp extends Application {

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
        FlowView<String> flView = new FlowView<>();
        AnchorPane.setTopAnchor(flView, 0.0);
        AnchorPane.setRightAnchor(flView, 0.0);
        AnchorPane.setBottomAnchor(flView, 0.0);
        AnchorPane.setLeftAnchor(flView, 0.0);

        Random random = new Random();

        flView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        System.out.println(flView.getItems().size());

        root.getChildren().add(flView);
        flView.prefWidthProperty().bind(scene.widthProperty());

        scene.setFill(Color.DARKGRAY);

        stage.setScene(scene);

        for (int i = 0; i < 500; i++) {
            flView.getItems().add("1 " + random.nextInt());
        }

        org.scenicview.ScenicView.show(scene);
        CSSFX.start(scene);
        stage.show();

    }


}
