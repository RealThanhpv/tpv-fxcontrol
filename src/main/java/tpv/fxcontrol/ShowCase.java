package tpv.fxcontrol;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import tpv.fxcontrol.skin.TextAreaSimpleSkin;


public class ShowCase extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        AnchorPane root = new AnchorPane();
        Scene scene = new Scene(root, 600, 400);
        TextArea textArea = new TextArea();

        TextAreaSimpleSkin skin = new TextAreaSimpleSkin(textArea);
        textArea.setSkin(skin);

        root.getChildren().add(textArea);

        org.scenicview.ScenicView.show(scene);
        stage.setScene(scene);
        stage.show();

    }
}
