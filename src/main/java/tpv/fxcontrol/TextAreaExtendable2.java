
package tpv.fxcontrol;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

public class TextAreaExtendable2 extends TextArea {
    private final static Scene COMPUTE_SCENE = new Scene(new Group());
    private final static Text COMPUTE_TEXT = new Text();
    private final static double FONT_EXT = 2.75;
    static {
        ((Group) COMPUTE_SCENE.getRoot()).getChildren().setAll(COMPUTE_TEXT);
    }
    public TextAreaExtendable2() {
        this.setWrapText(true);
        textProperty().addListener((observable, oldValue, newValue) -> {
            COMPUTE_TEXT.setText(newValue);
            COMPUTE_TEXT.applyCss();
            setPrefWidth( this.getFont().getSize()*FONT_EXT + COMPUTE_TEXT.getBoundsInLocal().getWidth());
        });
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                this.applyCss();
                setWrapText(true);
                Node text = this.lookup(".text");
                this.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> {
                    return this.getFont().getSize() + text.getBoundsInLocal().getHeight();
                }, new Observable[]{text.boundsInLocalProperty()}));

                setPrefWidth(text.getBoundsInLocal().getWidth() + this.getFont().getSize()*FONT_EXT);

                text.boundsInLocalProperty().addListener((observableBoundsAfter, boundsBefore, boundsAfter) -> {
                    Platform.runLater(() -> {
                        this.requestLayout();
                    });
                });
            }

        });
    }
}