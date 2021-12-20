//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package tpv.fxcontrol;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.TextArea;

public class TextAreaExtandable extends TextArea {
    public TextAreaExtandable() {
        this.setWrapText(true);
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                this.applyCss();
                Node text = this.lookup(".text");
                this.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> {
                    return this.getFont().getSize() + text.getBoundsInLocal().getHeight();
                }, new Observable[]{text.boundsInLocalProperty()}));
                text.boundsInLocalProperty().addListener((observableBoundsAfter, boundsBefore, boundsAfter) -> {
                    Platform.runLater(() -> {
                        this.requestLayout();
                    });
                });
            }

        });
    }
}