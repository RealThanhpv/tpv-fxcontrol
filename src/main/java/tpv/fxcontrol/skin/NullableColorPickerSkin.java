/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package tpv.fxcontrol.skin;


import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.skin.Utils;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.css.*;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.SizeConverter;
import javafx.geometry.*;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.WindowEvent;
import tpv.fxcontrol.NullableColorPicker;
import tpv.fxcontrol.behavior.NullableColorPickerBehavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static javafx.scene.paint.Color.*;
import static javafx.scene.paint.Color.YELLOW;


/**
 * Default skin implementation for the {@link ColorPicker} control.
 *
 * @see ColorPicker
 * @since 9
 */
public class NullableColorPickerSkin extends SkinBase<NullableColorPicker> {
    private static final int PICKER_SIZE = 15;
    private static final int BOUNDARY_WIDTH = 4;
    private static final double NULL_STROKE_WIDTH = 2;
    private final NullableColorPicker control;
    private  NullableColorPickerBehavior behavior;
    private NullableColorPalette popupContent;

    /* *************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/
    private PopupControl popup;

    private boolean popupNeedsReconfiguring = true;

    private StackPane root;
    private Rectangle colorRect;
    private Line nullLine;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new instance of NullableColorPickerSkin, although note that this
     * instance does not handle any behavior / input mappings - this needs to be
     * handled appropriately by subclasses.
     *
     * @param control The control that this skin should be installed onto.
     */
    public NullableColorPickerSkin(NullableColorPicker control) {
        super(control);

        behavior = new NullableColorPickerBehavior(control);

        this.control = control;

        getChildren().clear();

        root = new StackPane();
        root.setFocusTraversable(false);
        root.setId("arrow-button");
        root.getStyleClass().setAll("arrow-button");

        getChildren().add(root);

        Rectangle boundary = new Rectangle();
        boundary.setWidth(PICKER_SIZE+BOUNDARY_WIDTH);
        boundary.setHeight(PICKER_SIZE+BOUNDARY_WIDTH);
        boundary.setStrokeWidth(BOUNDARY_WIDTH);
        boundary.setStroke(WHITE);
        boundary.setMouseTransparent(true);
        root.getChildren().add(boundary);


        colorRect = new Rectangle();
        colorRect.setWidth(PICKER_SIZE);
        colorRect.setHeight(PICKER_SIZE);
        colorRect.setFill(control.getValue());
        colorRect.setMouseTransparent(true);
        root.getChildren().add(colorRect);


        nullLine = new Line();
        nullLine.setStartY(PICKER_SIZE+BOUNDARY_WIDTH-NULL_STROKE_WIDTH/2);
        nullLine.setEndX(PICKER_SIZE+BOUNDARY_WIDTH-NULL_STROKE_WIDTH/2);
        nullLine.setStrokeWidth(NULL_STROKE_WIDTH);
        nullLine.setStrokeLineCap(StrokeLineCap.ROUND);
        nullLine.setStroke(RED);
        nullLine.setMouseTransparent(true);
        if(control.getValue() == null) {
            root.getChildren().add(nullLine);
        }

        control.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
                colorRect.setFill(newValue);
                root.getChildren().remove(nullLine);
            }
            else {
                if(!root.getChildren().contains(nullLine)) {
                    root.getChildren().add(nullLine);
                }
            }
        });


        root.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
                event.consume();
                if(control.getValue() != null) {
                    control.setValue(null);
                }
                else {
                    control.setValue((Color)colorRect.getFill());
                }

        });

        getSkinnable().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if(!popupContent.isCustomColorDialogShowing()){
                    focusLost();
                }


            }
        });

        registerChangeListener(control.showingProperty(), e -> {
            if (getSkinnable().isShowing()) {
                show();
            } else {
                hide();
            }
        });
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/





    /** {@inheritDoc} */
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefWidth(height);
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefHeight(width);
    }

    // Overridden so that we use the displayNode as the baseline, rather than the arrow.
    // See RT-30754 for more information.
    /** {@inheritDoc} */
    @Override protected double computeBaselineOffset(double topInset, double rightInset, double bottomInset, double leftInset) {
        return super.computeBaselineOffset(topInset, rightInset, bottomInset, leftInset);
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/


    void focusLost() {
        getSkinnable().hide();
    }












    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/




    /** {@inheritDoc} */
    public void show() {
        if (getSkinnable() == null) {
            throw new IllegalStateException("NullableColorPicker is null");
        }

        Node content = getPopupContent();
        if (content == null) {
            throw new IllegalStateException("Popup node is null");
        }

        if (getPopup().isShowing()) return;

        positionAndShowPopup();
    }

    /** {@inheritDoc} */
     public void hide() {
        if (popup != null && popup.isShowing()) {
            popup.hide();
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    PopupControl getPopup() {
        if (popup == null) {
            createPopup();
        }
        return popup;
    }

    private Point2D getPrefPopupPosition() {
        return com.sun.javafx.util.Utils.pointRelativeTo(getSkinnable(), getPopupContent(), HPos.CENTER, VPos.BOTTOM, 0, 0, true);
    }

    private void positionAndShowPopup() {
        final NullableColorPicker control = getSkinnable();
        if (control.getScene() == null) {
            return;
        }

        final PopupControl _popup = getPopup();
        _popup.getScene().setNodeOrientation(getSkinnable().getEffectiveNodeOrientation());

        final Node popupContent = getPopupContent();
        sizePopup();

        Point2D p = getPrefPopupPosition();

        popupNeedsReconfiguring = true;
        reconfigurePopup();

        _popup.show(control.getScene().getWindow(),
                snapPositionX(p.getX()),
                snapPositionY(p.getY()));

        popupContent.requestFocus();

        // second call to sizePopup here to enable proper sizing _after_ the popup
        // has been displayed. See RT-37622 for more detail.
        sizePopup();
    }

    private void sizePopup() {
        final Node popupContent = getPopupContent();

        if (popupContent instanceof Region) {
            // snap to pixel
            final Region r = (Region) popupContent;

            // 0 is used here for the width due to RT-46097
            double prefHeight = snapSizeY(r.prefHeight(0));
            double minHeight = snapSizeY(r.minHeight(0));
            double maxHeight = snapSizeY(r.maxHeight(0));
            double h = snapSizeY(Math.min(Math.max(prefHeight, minHeight), Math.max(minHeight, maxHeight)));

            double prefWidth = snapSizeX(r.prefWidth(h));
            double minWidth = snapSizeX(r.minWidth(h));
            double maxWidth = snapSizeX(r.maxWidth(h));
            double w = snapSizeX(Math.min(Math.max(prefWidth, minWidth), Math.max(minWidth, maxWidth)));

            popupContent.resize(w, h);
        } else {
            popupContent.autosize();
        }
    }

    private void createPopup() {
        popup = new PopupControl() {
            @Override public Styleable getStyleableParent() {
                return NullableColorPickerSkin.this.getSkinnable();
            }
            {
                setSkin(new Skin<Skinnable>() {
                    @Override public Skinnable getSkinnable() { return NullableColorPickerSkin.this.getSkinnable(); }
                    @Override public Node getNode() { return getPopupContent(); }
                    @Override public void dispose() { }
                });
            }
        };
        popup.getStyleClass().add(Properties.COMBO_BOX_STYLE_CLASS);
        popup.setConsumeAutoHidingEvents(false);
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);
        popup.setOnAutoHide(e -> getBehavior().onAutoHide(popup));
        popup.addEventHandler(MouseEvent.MOUSE_CLICKED, t -> {
            // RT-18529: We listen to mouse input that is received by the popup
            // but that is not consumed, and assume that this is due to the mouse
            // clicking outside of the node, but in areas such as the
            // dropshadow.
            getBehavior().onAutoHide(popup);
        });
        popup.addEventHandler(WindowEvent.WINDOW_HIDDEN, t -> {
            // Make sure the accessibility focus returns to the combo box
            // after the window closes.
            getSkinnable().notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_NODE);
        });

        // Fix for RT-21207
        InvalidationListener layoutPosListener = o -> {
            popupNeedsReconfiguring = true;
            reconfigurePopup();
        };
        getSkinnable().layoutXProperty().addListener(layoutPosListener);
        getSkinnable().layoutYProperty().addListener(layoutPosListener);
        getSkinnable().widthProperty().addListener(layoutPosListener);
        getSkinnable().heightProperty().addListener(layoutPosListener);

        getSkinnable().sceneProperty().addListener(o -> {
            if (((ObservableValue)o).getValue() == null) {
                hide();
            } else if (getSkinnable().isShowing()) {
                show();
            }
        });

    }

    void reconfigurePopup() {
        // RT-26861. Don't call getPopup() here because it may cause the popup
        // to be created too early, which leads to memory leaks like those noted
        // in RT-32827.
        if (popup == null) return;

        final boolean isShowing = popup.isShowing();
        if (! isShowing) return;

        if (! popupNeedsReconfiguring) return;
        popupNeedsReconfiguring = false;

        final Point2D p = getPrefPopupPosition();

        final Node popupContent = getPopupContent();
        final double minWidth = popupContent.prefWidth(Region.USE_COMPUTED_SIZE);
        final double minHeight = popupContent.prefHeight(Region.USE_COMPUTED_SIZE);

        if (p.getX() > -1) popup.setAnchorX(p.getX());
        if (p.getY() > -1) popup.setAnchorY(p.getY());
        if (minWidth > -1) popup.setMinWidth(minWidth);
        if (minHeight > -1) popup.setMinHeight(minHeight);

        final Bounds b = popupContent.getLayoutBounds();
        final double currentWidth = b.getWidth();
        final double currentHeight = b.getHeight();
        final double newWidth  = currentWidth < minWidth ? minWidth : currentWidth;
        final double newHeight = currentHeight < minHeight ? minHeight : currentHeight;

        if (newWidth != currentWidth || newHeight != currentHeight) {
            // Resizing content to resolve issues such as RT-32582 and RT-33700
            // (where RT-33700 was introduced due to a previous fix for RT-32582)
            popupContent.resize(newWidth, newHeight);
            if (popupContent instanceof Region) {
                ((Region)popupContent).setMinSize(newWidth, newHeight);
                ((Region)popupContent).setPrefSize(newWidth, newHeight);
            }
        }
    }


































    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/




    // --- color rect width
    private final StyleableDoubleProperty colorRectWidth =  new StyleableDoubleProperty(12) {
        @Override protected void invalidated() {
            if(root!=null) root.requestLayout();
        }
        @Override public CssMetaData<NullableColorPicker,Number> getCssMetaData() {
            return NullableColorPickerSkin.StyleableProperties.COLOR_RECT_WIDTH;
        }
        @Override public Object getBean() {
            return NullableColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectWidth";
        }
    };

    // --- color rect height
    private final StyleableDoubleProperty colorRectHeight =  new StyleableDoubleProperty(12) {
        @Override protected void invalidated() {
            if(root!=null) root.requestLayout();
        }
        @Override public CssMetaData<NullableColorPicker,Number> getCssMetaData() {
            return NullableColorPickerSkin.StyleableProperties.COLOR_RECT_HEIGHT;
        }
        @Override public Object getBean() {
            return NullableColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectHeight";
        }
    };

    // --- color rect X
    private final StyleableDoubleProperty colorRectX =  new StyleableDoubleProperty(0) {
        @Override protected void invalidated() {
            if(root!=null) root.requestLayout();
        }
        @Override public CssMetaData<NullableColorPicker,Number> getCssMetaData() {
            return NullableColorPickerSkin.StyleableProperties.COLOR_RECT_X;
        }
        @Override public Object getBean() {
            return NullableColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectX";
        }
    };

    // --- color rect Y
    private final StyleableDoubleProperty colorRectY =  new StyleableDoubleProperty(0) {
        @Override protected void invalidated() {
            if(root!=null) root.requestLayout();
        }
        @Override public CssMetaData<NullableColorPicker,Number> getCssMetaData() {
            return NullableColorPickerSkin.StyleableProperties.COLOR_RECT_Y;
        }
        @Override public Object getBean() {
            return NullableColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectY";
        }
    };



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }



    /** {@inheritDoc} */
     protected Node getPopupContent() {
        if (popupContent == null) {
            popupContent = new NullableColorPalette(getSkinnable());
            popupContent.setPopupControl(getPopup());
        }
        return popupContent;
    }





    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/



    /** {@inheritDoc} */
     NullableColorPickerBehavior getBehavior() {
        return behavior;
    }



    // Translatable display names for the most common colors
    private static final Map<Color, String> COLOR_NAME_MAP = Map.ofEntries(
            Map.entry(TRANSPARENT, Properties.getColorPickerString("colorName.transparent")),
            Map.entry(BLACK,       Properties.getColorPickerString("colorName.black")),
            Map.entry(BLUE,        Properties.getColorPickerString("colorName.blue")),
            Map.entry(CYAN,        Properties.getColorPickerString("colorName.cyan")),
            Map.entry(DARKBLUE,    Properties.getColorPickerString("colorName.darkblue")),
            Map.entry(DARKCYAN,    Properties.getColorPickerString("colorName.darkcyan")),
            Map.entry(DARKGRAY,    Properties.getColorPickerString("colorName.darkgray")),
            Map.entry(DARKGREEN,   Properties.getColorPickerString("colorName.darkgreen")),
            Map.entry(DARKMAGENTA, Properties.getColorPickerString("colorName.darkmagenta")),
            Map.entry(DARKRED,     Properties.getColorPickerString("colorName.darkred")),
            Map.entry(GRAY,        Properties.getColorPickerString("colorName.gray")),
            Map.entry(GREEN,       Properties.getColorPickerString("colorName.green")),
            Map.entry(LIGHTBLUE,   Properties.getColorPickerString("colorName.lightblue")),
            Map.entry(LIGHTCYAN,   Properties.getColorPickerString("colorName.lightcyan")),
            Map.entry(LIGHTGRAY,   Properties.getColorPickerString("colorName.lightgray")),
            Map.entry(LIGHTGREEN,  Properties.getColorPickerString("colorName.lightgreen")),
            Map.entry(LIGHTYELLOW, Properties.getColorPickerString("colorName.lightyellow")),
            Map.entry(LIME,        Properties.getColorPickerString("colorName.lime")),
            Map.entry(MAGENTA,     Properties.getColorPickerString("colorName.magenta")),
            Map.entry(MAROON,      Properties.getColorPickerString("colorName.maroon")),
            Map.entry(MEDIUMBLUE,  Properties.getColorPickerString("colorName.mediumblue")),
            Map.entry(NAVY,        Properties.getColorPickerString("colorName.navy")),
            Map.entry(OLIVE,       Properties.getColorPickerString("colorName.olive")),
            Map.entry(ORANGE,      Properties.getColorPickerString("colorName.orange")),
            Map.entry(PINK,        Properties.getColorPickerString("colorName.pink")),
            Map.entry(PURPLE,      Properties.getColorPickerString("colorName.purple")),
            Map.entry(RED,         Properties.getColorPickerString("colorName.red")),
            Map.entry(TEAL,        Properties.getColorPickerString("colorName.teal")),
            Map.entry(WHITE,       Properties.getColorPickerString("colorName.white")),
            Map.entry(YELLOW,      Properties.getColorPickerString("colorName.yellow")));

    // CSS names.
    // Note that synonyms (such as "grey") have been removed here,
    // since a color can be presented with only one name in this
    // skin. If a reverse map is created for parsing names in the
    // future, then the synonyms should be included there. For a
    // full list of CSS names, see Color.java.
    private static final Map<Color, String> CSS_NAME_MAP = Map.ofEntries(
            Map.entry(ALICEBLUE,            "aliceblue"),
            Map.entry(ANTIQUEWHITE,         "antiquewhite"),
            Map.entry(AQUAMARINE,           "aquamarine"),
            Map.entry(AZURE,                "azure"),
            Map.entry(BEIGE,                "beige"),
            Map.entry(BISQUE,               "bisque"),
            Map.entry(BLACK,                "black"),
            Map.entry(BLANCHEDALMOND,       "blanchedalmond"),
            Map.entry(BLUE,                 "blue"),
            Map.entry(BLUEVIOLET,           "blueviolet"),
            Map.entry(BROWN,                "brown"),
            Map.entry(BURLYWOOD,            "burlywood"),
            Map.entry(CADETBLUE,            "cadetblue"),
            Map.entry(CHARTREUSE,           "chartreuse"),
            Map.entry(CHOCOLATE,            "chocolate"),
            Map.entry(CORAL,                "coral"),
            Map.entry(CORNFLOWERBLUE,       "cornflowerblue"),
            Map.entry(CORNSILK,             "cornsilk"),
            Map.entry(CRIMSON,              "crimson"),
            Map.entry(CYAN,                 "cyan"),
            Map.entry(DARKBLUE,             "darkblue"),
            Map.entry(DARKCYAN,             "darkcyan"),
            Map.entry(DARKGOLDENROD,        "darkgoldenrod"),
            Map.entry(DARKGRAY,             "darkgray"),
            Map.entry(DARKGREEN,            "darkgreen"),
            Map.entry(DARKKHAKI,            "darkkhaki"),
            Map.entry(DARKMAGENTA,          "darkmagenta"),
            Map.entry(DARKOLIVEGREEN,       "darkolivegreen"),
            Map.entry(DARKORANGE,           "darkorange"),
            Map.entry(DARKORCHID,           "darkorchid"),
            Map.entry(DARKRED,              "darkred"),
            Map.entry(DARKSALMON,           "darksalmon"),
            Map.entry(DARKSEAGREEN,         "darkseagreen"),
            Map.entry(DARKSLATEBLUE,        "darkslateblue"),
            Map.entry(DARKSLATEGRAY,        "darkslategray"),
            Map.entry(DARKTURQUOISE,        "darkturquoise"),
            Map.entry(DARKVIOLET,           "darkviolet"),
            Map.entry(DEEPPINK,             "deeppink"),
            Map.entry(DEEPSKYBLUE,          "deepskyblue"),
            Map.entry(DIMGRAY,              "dimgray"),
            Map.entry(DODGERBLUE,           "dodgerblue"),
            Map.entry(FIREBRICK,            "firebrick"),
            Map.entry(FLORALWHITE,          "floralwhite"),
            Map.entry(FORESTGREEN,          "forestgreen"),
            Map.entry(GAINSBORO,            "gainsboro"),
            Map.entry(GHOSTWHITE,           "ghostwhite"),
            Map.entry(GOLD,                 "gold"),
            Map.entry(GOLDENROD,            "goldenrod"),
            Map.entry(GRAY,                 "gray"),
            Map.entry(GREEN,                "green"),
            Map.entry(GREENYELLOW,          "greenyellow"),
            Map.entry(HONEYDEW,             "honeydew"),
            Map.entry(HOTPINK,              "hotpink"),
            Map.entry(INDIANRED,            "indianred"),
            Map.entry(INDIGO,               "indigo"),
            Map.entry(IVORY,                "ivory"),
            Map.entry(KHAKI,                "khaki"),
            Map.entry(LAVENDER,             "lavender"),
            Map.entry(LAVENDERBLUSH,        "lavenderblush"),
            Map.entry(LAWNGREEN,            "lawngreen"),
            Map.entry(LEMONCHIFFON,         "lemonchiffon"),
            Map.entry(LIGHTBLUE,            "lightblue"),
            Map.entry(LIGHTCORAL,           "lightcoral"),
            Map.entry(LIGHTCYAN,            "lightcyan"),
            Map.entry(LIGHTGOLDENRODYELLOW, "lightgoldenrodyellow"),
            Map.entry(LIGHTGRAY,            "lightgray"),
            Map.entry(LIGHTGREEN,           "lightgreen"),
            Map.entry(LIGHTPINK,            "lightpink"),
            Map.entry(LIGHTSALMON,          "lightsalmon"),
            Map.entry(LIGHTSEAGREEN,        "lightseagreen"),
            Map.entry(LIGHTSKYBLUE,         "lightskyblue"),
            Map.entry(LIGHTSLATEGRAY,       "lightslategray"),
            Map.entry(LIGHTSTEELBLUE,       "lightsteelblue"),
            Map.entry(LIGHTYELLOW,          "lightyellow"),
            Map.entry(LIME,                 "lime"),
            Map.entry(LIMEGREEN,            "limegreen"),
            Map.entry(LINEN,                "linen"),
            Map.entry(MAGENTA,              "magenta"),
            Map.entry(MAROON,               "maroon"),
            Map.entry(MEDIUMAQUAMARINE,     "mediumaquamarine"),
            Map.entry(MEDIUMBLUE,           "mediumblue"),
            Map.entry(MEDIUMORCHID,         "mediumorchid"),
            Map.entry(MEDIUMPURPLE,         "mediumpurple"),
            Map.entry(MEDIUMSEAGREEN,       "mediumseagreen"),
            Map.entry(MEDIUMSLATEBLUE,      "mediumslateblue"),
            Map.entry(MEDIUMSPRINGGREEN,    "mediumspringgreen"),
            Map.entry(MEDIUMTURQUOISE,      "mediumturquoise"),
            Map.entry(MEDIUMVIOLETRED,      "mediumvioletred"),
            Map.entry(MIDNIGHTBLUE,         "midnightblue"),
            Map.entry(MINTCREAM,            "mintcream"),
            Map.entry(MISTYROSE,            "mistyrose"),
            Map.entry(MOCCASIN,             "moccasin"),
            Map.entry(NAVAJOWHITE,          "navajowhite"),
            Map.entry(NAVY,                 "navy"),
            Map.entry(OLDLACE,              "oldlace"),
            Map.entry(OLIVE,                "olive"),
            Map.entry(OLIVEDRAB,            "olivedrab"),
            Map.entry(ORANGE,               "orange"),
            Map.entry(ORANGERED,            "orangered"),
            Map.entry(ORCHID,               "orchid"),
            Map.entry(PALEGOLDENROD,        "palegoldenrod"),
            Map.entry(PALEGREEN,            "palegreen"),
            Map.entry(PALETURQUOISE,        "paleturquoise"),
            Map.entry(PALEVIOLETRED,        "palevioletred"),
            Map.entry(PAPAYAWHIP,           "papayawhip"),
            Map.entry(PEACHPUFF,            "peachpuff"),
            Map.entry(PERU,                 "peru"),
            Map.entry(PINK,                 "pink"),
            Map.entry(PLUM,                 "plum"),
            Map.entry(POWDERBLUE,           "powderblue"),
            Map.entry(PURPLE,               "purple"),
            Map.entry(RED,                  "red"),
            Map.entry(ROSYBROWN,            "rosybrown"),
            Map.entry(ROYALBLUE,            "royalblue"),
            Map.entry(SADDLEBROWN,          "saddlebrown"),
            Map.entry(SALMON,               "salmon"),
            Map.entry(SANDYBROWN,           "sandybrown"),
            Map.entry(SEAGREEN,             "seagreen"),
            Map.entry(SEASHELL,             "seashell"),
            Map.entry(SIENNA,               "sienna"),
            Map.entry(SILVER,               "silver"),
            Map.entry(SKYBLUE,              "skyblue"),
            Map.entry(SLATEBLUE,            "slateblue"),
            Map.entry(SLATEGRAY,            "slategray"),
            Map.entry(SNOW,                 "snow"),
            Map.entry(SPRINGGREEN,          "springgreen"),
            Map.entry(STEELBLUE,            "steelblue"),
            Map.entry(TAN,                  "tan"),
            Map.entry(TEAL,                 "teal"),
            Map.entry(THISTLE,              "thistle"),
            Map.entry(TOMATO,               "tomato"),
            Map.entry(TRANSPARENT,          "transparent"),
            Map.entry(TURQUOISE,            "turquoise"),
            Map.entry(VIOLET,               "violet"),
            Map.entry(WHEAT,                "wheat"),
            Map.entry(WHITE,                "white"),
            Map.entry(WHITESMOKE,           "whitesmoke"),
            Map.entry(YELLOW,               "yellow"),
            Map.entry(YELLOWGREEN,          "yellowgreen"));



    static String tooltipString(Color c) {
        if (c != null) {
            String tooltipStr = "";
            String displayName = COLOR_NAME_MAP.get(c);
            if (displayName != null) {
                tooltipStr += displayName + " ";
            }

            tooltipStr += Utils.formatHexString(c);

            String cssName = CSS_NAME_MAP.get(c);
            if (cssName != null) {
                tooltipStr += " (css: " + cssName + ")";
            }
            return tooltipStr;
        } else {
            return null;
        }
    }


    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static class StyleableProperties {

        private static final CssMetaData<NullableColorPicker,Number> COLOR_RECT_WIDTH =
                new CssMetaData<NullableColorPicker,Number>("-fx-color-rect-width", SizeConverter.getInstance(), 12d) {
                    @Override public boolean isSettable(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return !skin.colorRectWidth.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return skin.colorRectWidth;
                    }
                };
        private static final CssMetaData<NullableColorPicker,Number> COLOR_RECT_HEIGHT =
                new CssMetaData<NullableColorPicker,Number>("-fx-color-rect-height", SizeConverter.getInstance(), 12d) {
                    @Override public boolean isSettable(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return !skin.colorRectHeight.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return skin.colorRectHeight;
                    }
                };
        private static final CssMetaData<NullableColorPicker,Number> COLOR_RECT_X =
                new CssMetaData<NullableColorPicker,Number>("-fx-color-rect-x", SizeConverter.getInstance(), 0) {
                    @Override public boolean isSettable(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return !skin.colorRectX.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return skin.colorRectX;
                    }
                };
        private static final CssMetaData<NullableColorPicker,Number> COLOR_RECT_Y =
                new CssMetaData<NullableColorPicker,Number>("-fx-color-rect-y", SizeConverter.getInstance(), 0) {
                    @Override public boolean isSettable(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return !skin.colorRectY.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return skin.colorRectY;
                    }
                };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<CssMetaData<? extends Styleable, ?>>(ComboBoxBaseSkin.getClassCssMetaData());
            styleables.add(COLOR_RECT_WIDTH);
            styleables.add(COLOR_RECT_HEIGHT);
            styleables.add(COLOR_RECT_X);
            styleables.add(COLOR_RECT_Y);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Returns the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @return the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return NullableColorPickerSkin.StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }



}
