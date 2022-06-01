package tpv.fxcontrol;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ShowCase extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        AnchorPane root = new AnchorPane();
        Scene scene = new Scene(root, 600, 400);
        root.getChildren().add(new NullableColorPicker());

        org.scenicview.ScenicView.show(scene);
        stage.setScene(scene);
        stage.show();

    }
}
