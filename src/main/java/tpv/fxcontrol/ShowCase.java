package tpv.fxcontrol;

import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import tpv.fxcontrol.skin.TextAreaSimpleSkin;

import java.util.Random;


public class ShowCase extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        AnchorPane root = new AnchorPane();
        Scene scene = new Scene(root, 600, 400);

        GridView<String> gridView = new GridView();
        gridView.setPrefWidth(600);
        gridView.setCellHeight(50);

        Random randInt = new Random();

        for (int i = 0; i < 100; i++) {
            String v = "This is the number\n"+
                    String.valueOf(randInt.nextInt(100));
            gridView.getItems().add(v);

        }
        gridView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        gridView.getSelectionModel().getSelectedIndices().addListener(new ListChangeListener<Integer>() {
            @Override
            public void onChanged(Change<? extends Integer> c) {
                while (c.next()){
                    System.out.println(c.getList());
                }
            }
        });

        root.getChildren().add(gridView);

        org.scenicview.ScenicView.show(scene);
        stage.setScene(scene);
        stage.show();

    }
}
