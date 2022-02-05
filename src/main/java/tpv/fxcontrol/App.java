package tpv.fxcontrol;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        AnchorPane container = new AnchorPane();


        SubScene subScene = new SubScene(container, 800, 600);
        Group root = new Group();
        root.getChildren().setAll(subScene);
        Scene scene  = new Scene(root);

        TextAreaExtendable2 ta = new TextAreaExtendable2();
        Node ta2 = new TextAreaResizable();
//        container.setScaleX(2);
//        container.setScaleY(2);
        ta.setPrefWidth(100);
        container.getChildren().setAll(ta);

        container.getChildren().add(ta2);

        ColorOptionNullable colorOptionNullable = new ColorOptionNullable();

        container.getChildren().add(colorOptionNullable);

        ta2.setTranslateX(300);
        ta2.setTranslateY(500);


        stage.setScene(scene);
        stage.show();

    }
}
