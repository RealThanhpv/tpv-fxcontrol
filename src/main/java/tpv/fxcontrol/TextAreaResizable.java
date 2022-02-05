
package tpv.fxcontrol;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

public class TextAreaResizable extends Control {
    private final TextArea editor;
    private final BooleanProperty resizeVertical;
    private final BooleanProperty resizeHorizontal;
    private final StringProperty text;

    public TextAreaResizable() {
        this.editor = new TextArea();
        this.resizeVertical = new SimpleBooleanProperty(this, "resizeVertical", true);
        this.resizeHorizontal = new SimpleBooleanProperty(this, "resizeHorizontal", true);
        this.text = new SimpleStringProperty(this, "text");
        this.getStyleClass().add("resizable-text-area");
        this.getStylesheets().add(this.getUserAgentStylesheet());
        this.editor.textProperty().bindBidirectional(this.textProperty());
    }

    public TextAreaResizable(String text) {
        this();
        this.setText(text);
    }

    protected Skin<?> createDefaultSkin() {
        return new TextAreaResizable.TextAreaResizableSkin(this);
    }

    public String getUserAgentStylesheet() {
        return TextAreaResizable.class.getResource("/tpv/fxcontrol/resizable-text-area.css").toExternalForm();
    }

    public final TextArea getEditor() {
        return this.editor;
    }

    public final boolean isResizeVertical() {
        return this.resizeVertical.get();
    }

    public final void setResizeVertical(boolean resizeVertical) {
        this.resizeVertical.set(resizeVertical);
    }

    public final BooleanProperty resizeVerticalProperty() {
        return this.resizeVertical;
    }

    public final boolean isResizeHorizontal() {
        return this.resizeHorizontal.get();
    }

    public final void setResizeHorizontal(boolean resizeHorizontal) {
        this.resizeHorizontal.set(resizeHorizontal);
    }

    public final BooleanProperty resizeHorizontalProperty() {
        return this.resizeHorizontal;
    }

    public final String getText() {
        return (String)this.text.get();
    }

    public final void setText(String text) {
        this.text.set(text);
    }

    public final StringProperty textProperty() {
        return this.text;
    }

    private static class TextAreaResizableSkin extends SkinBase<TextAreaResizable> {
        private double startX;
        private double startY;
        private double startW;
        private double startH;

        public TextAreaResizableSkin(TextAreaResizable area) {
            super(area);
            TextArea editor = area.getEditor();
            FontIcon resizeIcon = new FontIcon(MaterialDesign.MDI_RESIZE_BOTTOM_RIGHT);
            StackPane resizeCorner = new StackPane(new Node[]{resizeIcon});
            resizeCorner.getStyleClass().add("resize-corner");
            resizeCorner.setPrefSize(10.0D, 10.0D);
            resizeCorner.setMaxSize(10.0D, 10.0D);
            resizeCorner.setOnMousePressed((evt) -> {
                editor.requestFocus();
                this.startX = evt.getScreenX();
                this.startY = evt.getScreenY();
                this.startW = editor.getWidth();
                this.startH = editor.getHeight();
            });
            resizeCorner.setOnMouseDragged((evt) -> {
                double screenX = evt.getScreenX();
                double screenY = evt.getScreenY();
                double deltaX = screenX - this.startX;
                double deltaY = screenY - this.startY;
                double w = this.startW + deltaX;
                double h = this.startH + deltaY;
                if (editor.getMaxWidth() > 0.0D) {
                    w = Math.min(editor.getMaxWidth(), w);
                }

                if (editor.getMaxHeight() > 0.0D) {
                    h = Math.min(editor.getMaxHeight(), h);
                }

                if (area.isResizeHorizontal()) {
                    editor.setPrefWidth(w);
                }

                if (area.isResizeVertical()) {
                    editor.setPrefHeight(h);
                }

            });
            StackPane.setAlignment(resizeCorner, Pos.BOTTOM_RIGHT);
            editor.focusedProperty().addListener((it) -> {
                resizeIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), editor.isFocused());
            });
            StackPane pane = new StackPane(new Node[]{editor, resizeCorner});
            this.getChildren().setAll(new Node[]{pane});
        }
    }
}