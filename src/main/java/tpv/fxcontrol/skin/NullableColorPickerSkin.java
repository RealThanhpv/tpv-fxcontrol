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
import com.sun.javafx.scene.control.behavior.TextInputControlBehavior;
import com.sun.javafx.scene.control.skin.Utils;
import com.sun.javafx.scene.input.ExtendedInputMethodRequests;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.css.*;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.SizeConverter;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
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
    private final NullableColorPicker comboBoxBase;
    private  NullableColorPickerBehavior behavior;

    /* *************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

//    private Label displayNode = new Label(); // this is normally either label or textField

    private StackPane root;
//    private Region arrow;

    /** The mode in which this control will be represented. */
    private ComboBoxMode mode = ComboBoxMode.COMBOBOX;


    private final EventHandler<MouseEvent> mouseEnteredEventHandler  = e ->   getBehavior().mouseEntered(e);
    private final EventHandler<MouseEvent> mousePressedEventHandler  = e -> { getBehavior().mousePressed(e);  e.consume(); };
    private final EventHandler<MouseEvent> mouseReleasedEventHandler = e -> { getBehavior().mouseReleased(e); e.consume(); };
    private final EventHandler<MouseEvent> mouseExitedEventHandler   = e ->   getBehavior().mouseExited(e);



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new instance of ComboBoxBaseSkin, although note that this
     * instance does not handle any behavior / input mappings - this needs to be
     * handled appropriately by subclasses.
     *
     * @param control The control that this skin should be installed onto.
     */
    public NullableColorPickerSkin(NullableColorPicker control) {
        super(control);

        behavior = new NullableColorPickerBehavior(control);

        comboBoxBase = control;

        getChildren().clear();

        // open button / arrow
//        arrow = new Region();
//        arrow.setFocusTraversable(false);
//        arrow.getStyleClass().setAll("arrow");
//        arrow.setId("arrow");
//        arrow.setMaxWidth(Region.USE_PREF_SIZE);
//        arrow.setMaxHeight(Region.USE_PREF_SIZE);
//        arrow.setMouseTransparent(true);

        root = new StackPane();
        root.setFocusTraversable(false);
        root.setId("arrow-button");
        root.getStyleClass().setAll("arrow-button");

        getChildren().add(root);

        Rectangle rectangle = new Rectangle();
        rectangle.setWidth(16);
        rectangle.setHeight(16);
        root.getChildren().add(rectangle);

        // When ComboBoxBase focus shifts to another node, it should hide.
        getSkinnable().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                focusLost();
            }
        });

        // Register listeners
        updateArrowButtonListeners();
        registerChangeListener(control.editableProperty(), e -> {
            updateArrowButtonListeners();
            updateDisplayArea();
        });
        registerChangeListener(control.showingProperty(), e -> {
            if (getSkinnable().isShowing()) {
                show();
            } else {
                hide();
            }
        });
        registerChangeListener(control.valueProperty(), e -> updateDisplayArea());
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/





    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
//        if (displayNode == null) {
            updateDisplayArea();
//        }

//        final double arrowWidth = snapSizeX(arrow.prefWidth(-1));
        final double arrowButtonWidth = (isButton()) ? 0 :
                root.snappedLeftInset() + 0 +
                        root.snappedRightInset();

//        if (displayNode != null) {
//            displayNode.resizeRelocate(x, y, w - arrowButtonWidth, h);
//        }

//        arrowButton.setVisible(! isButton());
        if (! isButton()) {
            root.resize(arrowButtonWidth, h);
            positionInArea(root, (x + w) - arrowButtonWidth, y,
                    arrowButtonWidth, h, 0, HPos.CENTER, VPos.CENTER);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
//        if (displayNode == null) {
            updateDisplayArea();
//        }

        double ph;
//        if (displayNode == null) {
            final int DEFAULT_HEIGHT = 21;
            double arrowHeight = 0;// (isButton()) ? 0 :
                   // (root.snappedTopInset() + arrow.prefHeight(-1) + root.snappedBottomInset());
            ph = Math.max(DEFAULT_HEIGHT, arrowHeight);
//        } else {
//            ph = displayNode.prefHeight(width);
//        }

        return topInset+ ph + bottomInset;
    }

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
//        if (displayNode == null) {
            updateDisplayArea();
//        }

////        if (displayNode != null) {
//            return displayNode.getLayoutBounds().getMinY() + displayNode.getLayoutY() + displayNode.getBaselineOffset();
//        }

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


    private void updateArrowButtonListeners() {
//        if (getSkinnable().isEditable()) {
//            //
//            // arrowButton behaves like a button.
//            // This is strongly tied to the implementation in ComboBoxBaseBehavior.
//            //
//            displayNode.addEventHandler(MouseEvent.MOUSE_ENTERED,  mouseEnteredEventHandler);
//            displayNode.addEventHandler(MouseEvent.MOUSE_PRESSED,  mousePressedEventHandler);
//            displayNode.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedEventHandler);
//            displayNode.addEventHandler(MouseEvent.MOUSE_EXITED,   mouseExitedEventHandler);
//        } else {
//            displayNode.removeEventHandler(MouseEvent.MOUSE_ENTERED,  mouseEnteredEventHandler);
//            displayNode.removeEventHandler(MouseEvent.MOUSE_PRESSED,  mousePressedEventHandler);
//            displayNode.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedEventHandler);
//            displayNode.removeEventHandler(MouseEvent.MOUSE_EXITED,   mouseExitedEventHandler);
//        }
    }

//    /** {@inheritDoc} */
//     public Label getDisplayNode() {
//        return displayNode;
//    }

    void updateDisplayArea() {
        final List<Node> children = getChildren();
//        final Node oldDisplayNode = displayNode;
//        displayNode = getDisplayNode();
//
//        // don't remove displayNode if it hasn't changed.
//        if (oldDisplayNode != null && oldDisplayNode != displayNode) {
//            children.remove(oldDisplayNode);
//        }
//
//        if (displayNode != null && !children.contains(displayNode)) {
//            children.add(displayNode);
//            displayNode.applyCss();
//        }
    }


    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    PopupControl popup;

    private boolean popupNeedsReconfiguring = true;

    private TextField textField;

    private String initialTextFieldValue = null;



    /* *************************************************************************
     *                                                                         *
     * TextField Listeners                                                     *
     *                                                                         *
     **************************************************************************/

    private EventHandler<MouseEvent> textFieldMouseEventHandler = event -> {
        NullableColorPicker comboBoxBase = getSkinnable();
        if (!event.getTarget().equals(comboBoxBase)) {
            comboBoxBase.fireEvent(event.copyFor(comboBoxBase, comboBoxBase));
            event.consume();
        }
    };
    private EventHandler<DragEvent> textFieldDragEventHandler = event -> {
        NullableColorPicker comboBoxBase = getSkinnable();
        if (!event.getTarget().equals(comboBoxBase)) {
            comboBoxBase.fireEvent(event.copyFor(comboBoxBase, comboBoxBase));
            event.consume();
        }
    };



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/




    /** {@inheritDoc} */
    public void show() {
        if (getSkinnable() == null) {
            throw new IllegalStateException("ComboBox is null");
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

    TextField getEditableInputNode() {
        if (textField == null && getEditor() != null) {
            textField = getEditor();
            textField.setFocusTraversable(false);
//            textField.promptTextProperty().bind(comboBoxBase.promptTextProperty());
            textField.tooltipProperty().bind(comboBoxBase.tooltipProperty());

            // Fix for JDK-8145515 - in short the ComboBox was firing the event down to
            // the TextField, and then the TextField was firing it back up to the
            // ComboBox, resulting in stack overflows.
            textField.getProperties().put(TextInputControlBehavior.DISABLE_FORWARD_TO_PARENT, true);

            // Fix for RT-21406: ComboBox do not show initial text value
            initialTextFieldValue = textField.getText();
            // End of fix (see updateDisplayNode below for the related code)
        }

        return textField;
    }

    void setTextFromTextFieldIntoComboBoxValue() {
        if (getEditor() != null) {
            StringConverter<Color> c = getConverter();
            if (c != null) {
                Color oldValue = comboBoxBase.getValue();
                Color value = oldValue;
                String text = textField.getText();

                // conditional check here added due to RT-28245
                if (oldValue == null && (text == null || text.isEmpty())) {
                    value = null;
                } else {
                    try {
                        value = c.fromString(text);
                    } catch (Exception ex) {
                        // Most likely a parsing error, such as DateTimeParseException
                    }
                }

                if ((value != null || oldValue != null) && (value == null || !value.equals(oldValue))) {
                    // no point updating values needlessly if they are the same
                    comboBoxBase.setValue(value);
                }

                updateDisplayNode();
            }
        }
    }

    void updateDisplayNode() {
        if (textField != null && getEditor() != null) {
            Color value = comboBoxBase.getValue();
            StringConverter<Color> c = getConverter();

            if (initialTextFieldValue != null && ! initialTextFieldValue.isEmpty()) {
                // Remainder of fix for RT-21406: ComboBox do not show initial text value
                textField.setText(initialTextFieldValue);
                initialTextFieldValue = null;
                // end of fix
            } else {
                String stringValue = c.toString(value);
                if (value == null || stringValue == null) {
                    textField.setText("");
                } else if (! stringValue.equals(textField.getText())) {
                    textField.setText(stringValue);
                }
            }
        }
    }

    void updateEditable() {
        TextField newTextField = getEditor();

        if (getEditor() == null) {
            // remove event filters
            if (textField != null) {
                textField.removeEventFilter(MouseEvent.DRAG_DETECTED, textFieldMouseEventHandler);
                textField.removeEventFilter(DragEvent.ANY, textFieldDragEventHandler);

                comboBoxBase.setInputMethodRequests(null);
            }
        } else if (newTextField != null) {
            // add event filters

            // Fix for RT-31093 - drag events from the textfield were not surfacing
            // properly for the ComboBox.
            newTextField.addEventFilter(MouseEvent.DRAG_DETECTED, textFieldMouseEventHandler);
            newTextField.addEventFilter(DragEvent.ANY, textFieldDragEventHandler);

            // RT-38978: Forward input method requests to TextField.
            comboBoxBase.setInputMethodRequests(new ExtendedInputMethodRequests() {
                 public Point2D getTextLocation(int offset) {
                    return newTextField.getInputMethodRequests().getTextLocation(offset);
                }

                 public int getLocationOffset(int x, int y) {
                    return newTextField.getInputMethodRequests().getLocationOffset(x, y);
                }

                 public void cancelLatestCommittedText() {
                    newTextField.getInputMethodRequests().cancelLatestCommittedText();
                }

                 public String getSelectedText() {
                    return newTextField.getInputMethodRequests().getSelectedText();
                }

                 public int getInsertPositionOffset() {
                    return ((ExtendedInputMethodRequests)newTextField.getInputMethodRequests()).getInsertPositionOffset();
                }

                public String getCommittedText(int begin, int end) {
                    return ((ExtendedInputMethodRequests)newTextField.getInputMethodRequests()).getCommittedText(begin, end);
                }

                 public int getCommittedTextLength() {
                    return ((ExtendedInputMethodRequests)newTextField.getInputMethodRequests()).getCommittedTextLength();
                }
            });
        }

        textField = newTextField;
    }

    private Point2D getPrefPopupPosition() {
        return com.sun.javafx.util.Utils.pointRelativeTo(getSkinnable(), getPopupContent(), HPos.CENTER, VPos.BOTTOM, 0, 0, true);
    }

    private void positionAndShowPopup() {
        final NullableColorPicker comboBoxBase = getSkinnable();
        if (comboBoxBase.getScene() == null) {
            return;
        }

        final PopupControl _popup = getPopup();
        _popup.getScene().setNodeOrientation(getSkinnable().getEffectiveNodeOrientation());


        final Node popupContent = getPopupContent();
        sizePopup();

        Point2D p = getPrefPopupPosition();

        popupNeedsReconfiguring = true;
        reconfigurePopup();

        _popup.show(comboBoxBase.getScene().getWindow(),
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

        // RT-36966 - if skinnable's scene becomes null, ensure popup is closed
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

    private void handleKeyEvent(KeyEvent ke, boolean doConsume) {
        // When the user hits the enter key, we respond before
        // ever giving the event to the TextField.
        if (ke.getCode() == KeyCode.ENTER) {
            if (ke.isConsumed() || ke.getEventType() != KeyEvent.KEY_RELEASED) {
                return;
            }
            setTextFromTextFieldIntoComboBoxValue();

            if (doConsume && comboBoxBase.getOnAction() != null) {
                ke.consume();
            } else if (textField != null) {
                textField.fireEvent(ke);
            }
        } else if (ke.getCode() == KeyCode.F10 || ke.getCode() == KeyCode.ESCAPE) {
            // RT-23275: The TextField fires F10 and ESCAPE key events
            // up to the parent, which are then fired back at the
            // TextField, and this ends up in an infinite loop until
            // the stack overflows. So, here we consume these two
            // events and stop them from going any further.
            if (doConsume) ke.consume();
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/





    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    /* *************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/



    final ComboBoxMode getMode() { return mode; }
    final void setMode(ComboBoxMode value) { mode = value; }




    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/














    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/




    private boolean isButton() {
        return getMode() == ComboBoxMode.BUTTON;
    }





    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private StackPane pickerColorBox;
    private Rectangle colorRect;
    private NullableColorPalette popupContent;




    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- color label visible
    BooleanProperty colorLabelVisible = new StyleableBooleanProperty(true) {
        @Override public void invalidated() {
//            if (displayNode != null) {
//                if (colorLabelVisible.get()) {
//                    displayNode.setText(colorDisplayName(((NullableColorPicker)getSkinnable()).getValue()));
//                } else {
//                    displayNode.setText("");
//                }
//            }
        }
        @Override public Object getBean() {
            return NullableColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorLabelVisible";
        }
        @Override public CssMetaData<NullableColorPicker,Boolean> getCssMetaData() {
            return NullableColorPickerSkin.StyleableProperties.COLOR_LABEL_VISIBLE;
        }
    };

    // --- image url
    private final StringProperty imageUrlProperty() { return imageUrl; }
    private final StyleableStringProperty imageUrl = new StyleableStringProperty() {
        @Override public void applyStyle(StyleOrigin origin, String v) {
            super.applyStyle(origin, v);
            if (v == null) {
                // remove old image view
                if (pickerColorBox.getChildren().size() == 2) pickerColorBox.getChildren().remove(1);
            } else {
                if (pickerColorBox.getChildren().size() == 2) {
                    ImageView imageView = (ImageView)pickerColorBox.getChildren().get(1);
                    imageView.setImage(StyleManager.getInstance().getCachedImage(v));
                } else {
                    pickerColorBox.getChildren().add(new ImageView(StyleManager.getInstance().getCachedImage(v)));
                }
            }
        }
        @Override public Object getBean() {
            return NullableColorPickerSkin.this;
        }
        @Override public String getName() {
            return "imageUrl";
        }
        @Override public CssMetaData<NullableColorPicker,String> getCssMetaData() {
            return NullableColorPickerSkin.StyleableProperties.GRAPHIC;
        }
    };

    // --- color rect width
    private final StyleableDoubleProperty colorRectWidth =  new StyleableDoubleProperty(12) {
        @Override protected void invalidated() {
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
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
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
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
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
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
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
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
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (!colorLabelVisible.get()) {
            return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
        }
//        String displayNodeText = displayNode.getText();
        double width = 0;
//        for (String name : COLOR_NAME_MAP.values()) {
//            displayNode.setText(name);
//            width = Math.max(width, super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset));
//        }
//        displayNode.setText(Utils.formatHexString(Color.BLACK)); // #000000
//        width = Math.max(width, super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset));
//        displayNode.setText(displayNodeText);
        return width;
    }

    /** {@inheritDoc} */
     protected Node getPopupContent() {
        if (popupContent == null) {
//            popupContent = new ColorPalette(colorPicker.getValue(), colorPicker);
            popupContent = new NullableColorPalette((NullableColorPicker)getSkinnable());
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

    private void updateComboBoxMode() {
        List<String> styleClass = getSkinnable().getStyleClass();
        if (styleClass.contains(ColorPicker.STYLE_CLASS_BUTTON)) {
            setMode(ComboBoxMode.BUTTON);
        } else if (styleClass.contains(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)) {
            setMode(ComboBoxMode.SPLITBUTTON);
        }
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

    static String colorDisplayName(Color c) {
        if (c != null) {
            String displayName = COLOR_NAME_MAP.get(c);
            if (displayName == null) {
                displayName = Utils.formatHexString(c);
            }
            return displayName;
        } else {
            return null;
        }
    }

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

    private void updateColor() {
        final NullableColorPicker colorPicker = (NullableColorPicker)getSkinnable();
        colorRect.setFill(colorPicker.getValue());
//        if (colorLabelVisible.get()) {
//            displayNode.setText(colorDisplayName(colorPicker.getValue()));
//        } else {
//            displayNode.setText("");
//        }
    }



    /* *************************************************************************
     *                                                                         *
     *                         picker-color-cell                               *
     *                                                                         *
     **************************************************************************/

    private class PickerColorBox extends StackPane {
        @Override protected void layoutChildren() {
            final double top = snappedTopInset();
            final double left = snappedLeftInset();
            final double width = getWidth();
            final double height = getHeight();
            final double right = snappedRightInset();
            final double bottom = snappedBottomInset();
            colorRect.setX(snapPositionX(colorRectX.get()));
            colorRect.setY(snapPositionY(colorRectY.get()));
            colorRect.setWidth(snapSizeX(colorRectWidth.get()));
            colorRect.setHeight(snapSizeY(colorRectHeight.get()));
            if (getChildren().size() == 2) {
                final ImageView icon = (ImageView) getChildren().get(1);
                Pos childAlignment = StackPane.getAlignment(icon);
                layoutInArea(icon, left, top,
                        width - left - right, height - top - bottom,
                        0, getMargin(icon),
                        childAlignment != null? childAlignment.getHpos() : getAlignment().getHpos(),
                        childAlignment != null? childAlignment.getVpos() : getAlignment().getVpos());
                colorRect.setLayoutX(icon.getLayoutX());
                colorRect.setLayoutY(icon.getLayoutY());
            } else {
                Pos childAlignment = StackPane.getAlignment(colorRect);
                layoutInArea(colorRect, left, top,
                        width - left - right, height - top - bottom,
                        0, getMargin(colorRect),
                        childAlignment != null? childAlignment.getHpos() : getAlignment().getHpos(),
                        childAlignment != null? childAlignment.getVpos() : getAlignment().getVpos());
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static class StyleableProperties {
        private static final CssMetaData<NullableColorPicker,Boolean> COLOR_LABEL_VISIBLE =
                new CssMetaData<NullableColorPicker,Boolean>("-fx-color-label-visible",
                        BooleanConverter.getInstance(), Boolean.TRUE) {

                    @Override public boolean isSettable(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return skin.colorLabelVisible == null || !skin.colorLabelVisible.isBound();
                    }

                    @Override public StyleableProperty<Boolean> getStyleableProperty(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return (StyleableProperty<Boolean>)(WritableValue<Boolean>)skin.colorLabelVisible;
                    }
                };
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
        private static final CssMetaData<NullableColorPicker,String> GRAPHIC =
                new CssMetaData<NullableColorPicker,String>("-fx-graphic", javafx.css.converter.StringConverter.getInstance()) {
                    @Override public boolean isSettable(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return !skin.imageUrl.isBound();
                    }
                    @Override public StyleableProperty<String> getStyleableProperty(NullableColorPicker n) {
                        final NullableColorPickerSkin skin = (NullableColorPickerSkin) n.getSkin();
                        return skin.imageUrl;
                    }
                };
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<CssMetaData<? extends Styleable, ?>>(ComboBoxBaseSkin.getClassCssMetaData());
            styleables.add(COLOR_LABEL_VISIBLE);
            styleables.add(COLOR_RECT_WIDTH);
            styleables.add(COLOR_RECT_HEIGHT);
            styleables.add(COLOR_RECT_X);
            styleables.add(COLOR_RECT_Y);
            styleables.add(GRAPHIC);
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

    /** {@inheritDoc} */
     protected javafx.util.StringConverter<Color> getConverter() {
        return null;
    }

    /**
     * ColorPicker does not use a main text field, so this method has been
     * overridden to return null.
     */
     protected TextField getEditor() {
        return null;
    }
}
