package tpv.fxcontrol;

import javafx.beans.property.StringProperty;
import javafx.css.*;
import javafx.scene.Node;
import tpv.jfxsvg.SVGLoader;
import javafx.scene.layout.StackPane;

public class SVGView extends StackPane {
    private static final StyleablePropertyFactory<SVGView> FACTORY =
            new StyleablePropertyFactory<>(StackPane.getClassCssMetaData());
    CssMetaData<SVGView, String> URL = FACTORY.createUrlCssMetaData("-url", s->s.url, null, false);

    private final StyleableStringProperty url = new SimpleStyleableStringProperty(URL, this, "url"){
        @Override
        protected void invalidated() {
            loadSVGNode(get());
        }
    };


    public SVGView(String url){
        setUrl(url);
    }

    public SVGView(){
        if(!(getUrl() == null || getUrl().isEmpty())) {
            loadSVGNode(getUrl());
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


    public StringProperty urlProperty(){
        return url;
    }

    public String getUrl(){
        return urlProperty().get();
    }

    public void setUrl(String url){
        urlProperty().set(url);
    }




}
