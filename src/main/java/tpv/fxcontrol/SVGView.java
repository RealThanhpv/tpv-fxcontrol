package tpv.fxcontrol;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import tpv.jfxsvg.SVGLoader;
import javafx.scene.layout.StackPane;

public class SVGView extends StackPane {

    private final StringProperty svgUrlProp = new SimpleStringProperty(this, "url", null){
        @Override
        protected void invalidated() {
            loadSVGNode(get());
        }
    };


    public SVGView(String url){
        setSvgUrl(url);
    }

    public SVGView(){
        if(!(getSvgUrl() == null || getSvgUrl().isEmpty())) {
            loadSVGNode(getSvgUrl());
        }
    }

    private void loadSVGNode(String url) {
        try {
            Node node = SVGLoader.loadSVG(url);
            getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public StringProperty svgUrlProperty(){
        return svgUrlProp;
    }

    public String getSvgUrl(){
        return svgUrlProperty().get();
    }

    public void setSvgUrl(String url){
        svgUrlProperty().set(url);
    }




}
