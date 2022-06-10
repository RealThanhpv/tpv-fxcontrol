package tpv.fxcontrol;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import tpv.fxcontrol.skin.TextAreaExtendableSkin;
import tpv.fxcontrol.skin.TextInputControlSkin;

public class ShowCase extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        AnchorPane root = new AnchorPane();
        Scene scene = new Scene(root, 600, 400);
        TextArea textArea = new TextArea();

        TextAreaExtendableSkin skin = new TextAreaExtendableSkin(textArea);
        textArea.setSkin(skin);

        root.getChildren().add(textArea);

//        org.scenicview.ScenicView.show(scene);
        stage.setScene(scene);
        stage.show();

    }
}
