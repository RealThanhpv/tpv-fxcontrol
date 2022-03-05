package tpv.fxcontrol;

import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        HBox container = new HBox();
        container.setSpacing(10);


        SubScene subScene = new SubScene(container, 800, 600);
        Group root = new Group();
        root.getChildren().setAll(subScene);
        Scene scene  = new Scene(root);

        TextAreaExtendable2 ta = new TextAreaExtendable2();
        Node ta2 = new TextAreaResizable();
//        container.setScaleX(2);
//        container.setScaleY(2);
        ta.setPrefWidth(100);
//        container.getChildren().setAll(ta);

//        container.getChildren().add(ta2);

        NullableColorPicker colorOptionNullable = new NullableColorPicker();
//        colorOptionNullable.setTranslateX(200);
//        colorOptionNullable.setTranslateY(200);
//        colorOptionNullable.setValue(null);
        ta2.setTranslateX(300);
        ta2.setTranslateY(500);

        container.getChildren().add(colorOptionNullable);
        container.getChildren().add(new ColorPicker());

//        SVGView svgView = new SVGView();
//        svgView.setUrl("https://upload.wikimedia.org/wikipedia/commons/f/f7/Bananas.svg");
//        svgView.setUrl("tiger.svg");
//        container.getChildren().add(svgView);
//        container.getChildren().add(new Test());

        subScene.setFill(Color.DARKGRAY);

        stage.setScene(scene);
//        org.scenicview.ScenicView.show(scene);
        CSSFX.start(scene);
        stage.show();

    }
}
