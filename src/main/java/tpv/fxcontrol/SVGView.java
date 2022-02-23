package tpv.fxcontrol;

import javafx.beans.property.StringProperty;
import javafx.css.*;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import tpv.jfxsvg.SVGLoader;
import javafx.scene.layout.StackPane;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SVGView extends StackPane {


    private static final StyleablePropertyFactory<SVGView> FACTORY = new StyleablePropertyFactory<>(StackPane.getClassCssMetaData());
    private static CssMetaData<SVGView, String> URL_CLASS_CSS_META_DATA = FACTORY.createUrlCssMetaData("-url", s->s.url, null, false);
    private final StyleableStringProperty url = new SimpleStyleableStringProperty(URL_CLASS_CSS_META_DATA, this, "url"){
        @Override
        protected void invalidated() {
            loadSVGNode(get());
        }
    };

    private final static List<CssMetaData<? extends Styleable, ?>> CLASS_CSS_META_DATA;

    static {
        // combine already available properties in Rectangle with new properties
        List<CssMetaData<? extends Styleable, ?>> parent = StackPane.getClassCssMetaData();
        List<CssMetaData<? extends Styleable, ?>> additional = Arrays.asList(URL_CLASS_CSS_META_DATA);

        // create arraylist with suitable capacity
        List<CssMetaData<? extends Styleable, ?>> own = new ArrayList(parent.size()+ additional.size());

        // fill list with old and new metadata
        own.addAll(parent);
        own.addAll(additional);

        // make sure the metadata list is not modifiable
        CLASS_CSS_META_DATA = Collections.unmodifiableList(own);
    }

    // make metadata available for extending the class
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CLASS_CSS_META_DATA;
    }

    // returns a list of the css metadata for the stylable properties of the Node
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CLASS_CSS_META_DATA;
    }

    public SVGView(String url){
        setUrl(url);
    }

    public SVGView(){
        if(!(getUrl() == null || getUrl().isEmpty() || getUrl().isBlank())) {
            loadSVGNode(getUrl());
        }
    }

    private void loadSVGNode(String urlString) {

        try {
            URL url = new URL (urlString);

            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

            StringBuffer sb = new StringBuffer();

            String line;
            while ((line = in.readLine()) != null) {
               sb.append(line);
            }
            in.close();

            Node node = SVGLoader.parse(sb.toString());
            getChildren().setAll(node);

        } catch (Exception e) {
            System.out.println("Failed with url: "+ url);
            e.printStackTrace();
        }

    }


    public StyleableStringProperty urlProperty(){
        return url;
    }

    public String getUrl(){
        return urlProperty().get();
    }

    public void setUrl(String url){
        urlProperty().set(url);
    }




}
