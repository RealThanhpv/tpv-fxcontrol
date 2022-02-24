package tpv.fxcontrol;

import javafx.css.*;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import tpv.jfxsvg.SVGLoader;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SVGView extends StackPane {


    private static final StyleablePropertyFactory<SVGView> FACTORY = new StyleablePropertyFactory<>(StackPane.getClassCssMetaData());
    private final static List<CssMetaData<? extends Styleable, ?>> CLASS_CSS_META_DATA;
    private static final CssMetaData<SVGView, String> URL_CLASS_CSS_META_DATA = FACTORY.createUrlCssMetaData("-url", s -> s.url, null, false);

    static {
        // combine already available properties in Rectangle with new properties
        List<CssMetaData<? extends Styleable, ?>> parent = StackPane.getClassCssMetaData();
        List<CssMetaData<? extends Styleable, ?>> additional = Arrays.asList(URL_CLASS_CSS_META_DATA);

        // create arraylist with suitable capacity
        List<CssMetaData<? extends Styleable, ?>> own = new ArrayList(parent.size() + additional.size());

        // fill list with old and new metadata
        own.addAll(parent);
        own.addAll(additional);

        // make sure the metadata list is not modifiable
        CLASS_CSS_META_DATA = Collections.unmodifiableList(own);
    }

    private final StyleableStringProperty url = new SimpleStyleableStringProperty(URL_CLASS_CSS_META_DATA, this, "url") {
        @Override
        protected void invalidated() {
            loadSVGNode(get());
        }
    };

    public SVGView(String url) {
        setUrl(url);
    }

    public SVGView() {
        loadSVGNode(getUrl());
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

    private void loadSVGNode(String urlString) {
        if (urlString == null || urlString.isEmpty() || urlString.isBlank()) {
            return;
        }

        File file = new File(urlString);
        if (file.exists()) {
            try {
                parseSVGFromFile(file);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } else {
            try {
                URL url = new URL(urlString);

                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

                StringBuffer sb = new StringBuffer();

                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                in.close();

                Node node = SVGLoader.parse(sb.toString());
                getChildren().setAll(node);


            } catch (IOException e) {

            }
        }
    }

    private void parseSVGFromFile(File file) throws IOException {
        byte[] buf = null;

        try {
            FileInputStream inFile = new FileInputStream(file);
            buf = new byte[inFile.available()];
            inFile.read(buf);

            inFile.close();


        } catch (Exception ex) {
            ex.printStackTrace();
        }


        Node node = SVGLoader.parse(new String(buf));
        getChildren().setAll(node);
    }


    public StyleableStringProperty urlProperty() {
        return url;
    }

    public String getUrl() {
        return urlProperty().get();
    }

    public void setUrl(String url) {
        urlProperty().set(url);
    }


}
