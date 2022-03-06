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

package tpv.fxcontrol;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.css.*;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.*;
import javafx.scene.control.skin.ColorPickerSkin;
import javafx.scene.paint.Color;
import tpv.fxcontrol.skin.NullableColorPickerSkin;

/**
 * <p>ColorPicker control allows the user to select a color from either a standard
 * palette of colors with a simple one click selection OR define their own custom color.
 *
 * <p>The {@link #valueProperty() value} is the currently selected {@link javafx.scene.paint.Color}.
 * An initial color can be set by calling setColor or via the constructor. If nothing
 * is specified, a default initial color is used.
 *
 * <p>The ColorPicker control provides a color palette with a predefined set of colors. If
 * the user does not want to choose from the predefined set, they can create a custom
 * color by interacting with a custom color dialog. This dialog provides RGB,
 * HSB and Web modes of interaction, to create new colors. It also lets the opacity
 * of the color to be modified.
 *
 * <p>Once a new color is defined, users can choose whether they want to save it
 * or just use it. If the new color is saved, this color will then appear in the
 * custom colors area on the color palette. Also {@link #getCustomColors() getCustomColors}
 * returns the list of saved custom colors.
 *
 * <p>The {@link #promptTextProperty() promptText} is not supported and hence is a no-op.
 * But it may be supported in the future.
 *
 * <pre><code> ColorPicker colorPicker = new ColorPicker();
 * colorPicker.setOnAction(e {@literal ->} {
 *     Color c = colorPicker.getValue();
 *     System.out.println("New Color's RGB = "+c.getRed()+" "+c.getGreen()+" "+c.getBlue());
 * });</code></pre>
 *
 * <img src="doc-files/ColorPicker.png" alt="Image of the ColorPicker control">
 *
 * <p>The ColorPicker control's appearance can be styled in three ways: a simple Button mode,
 * MenuButton mode or SplitMenuButton mode. The default is MenuButton mode.
 * For a Button like appearance the style class to use is {@link #STYLE_CLASS_BUTTON STYLE_CLASS_BUTTON}
 * and for SplitMenuButton appearance and behavior, the style class to use is
 * {@link #STYLE_CLASS_SPLIT_BUTTON STYLE_CLASS_SPLIT_BUTTON}.
 *
 * <pre>colorPicker.getStyleClass().add("button");</pre>
 * <p>or
 * <pre>colorPicker.getStyleClass().add("split-button");</pre>
 *
 * @since JavaFX 2.2
 */
public class NullableColorPicker extends Control {

    /* *************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * <p>Called prior to the ComboBox showing its popup/display after the user
     * has clicked or otherwise interacted with the ComboBox.
     * @since JavaFX 2.2
     */
    public static final EventType<Event> ON_SHOWING =
            new EventType<Event>(Event.ANY, "NULLABLE_COLOR_PICKER_ON_SHOWING");

    /**
     * <p>Called after the ComboBox has shown its popup/display.
     * @since JavaFX 2.2
     */
    public static final EventType<Event> ON_SHOWN =
            new EventType<Event>(Event.ANY, "NULLABLE_COLOR_PICKER_ON_SHOWN");

    /**
     * <p>Called when the ComboBox popup/display <b>will</b> be hidden.
     * @since JavaFX 2.2
     */
    public static final EventType<Event> ON_HIDING =
            new EventType<Event>(Event.ANY, "NULLABLE_COLOR_PICKER_ON_HIDING");

    /**
     * <p>Called when the ComboBox popup/display has been hidden.
     * @since JavaFX 2.2
     */
    public static final EventType<Event> ON_HIDDEN =
            new EventType<Event>(Event.ANY, "NULLABLE_COLOR_PICKER_ON_HIDDEN");



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default ComboBoxBase instance.
     */


    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- value
    /**
     * The value of this ComboBox is defined as the selected item if the input
     * is not editable, or if it is editable, the most recent user action:
     * either the value input by the user, or the last selected item.
     * @return the value property
     */
    public ObjectProperty<Color> valueProperty() { return value; }
    private ObjectProperty<Color> value = new SimpleObjectProperty<Color>(this, "value");

    public final void setValue(Color value) { valueProperty().set(value); }
    public final Color getValue() { return valueProperty().get(); }




    // --- showing
    /**
     * Represents the current state of the ComboBox popup, and whether it is
     * currently visible on screen (although it may be hidden behind other windows).
     */
    private ReadOnlyBooleanWrapper showing;
    public ReadOnlyBooleanProperty showingProperty() { return showingPropertyImpl().getReadOnlyProperty(); }
    public final boolean isShowing() { return showingPropertyImpl().get(); }
    private void setShowing(boolean value) {
        // these events will not fire if the showing property is bound
        Event.fireEvent(this, value ? new Event(NullableColorPicker.ON_SHOWING) :
                new Event(NullableColorPicker.ON_HIDING));
        showingPropertyImpl().set(value);
        Event.fireEvent(this, value ? new Event(NullableColorPicker.ON_SHOWN) :
                new Event(NullableColorPicker.ON_HIDDEN));
    }
    private ReadOnlyBooleanWrapper showingPropertyImpl() {
        if (showing == null) {
            showing = new ReadOnlyBooleanWrapper(false) {
                @Override protected void invalidated() {
                    pseudoClassStateChanged(PSEUDO_CLASS_SHOWING, get());
                    notifyAccessibleAttributeChanged(AccessibleAttribute.EXPANDED);
                }

                @Override
                public Object getBean() {
                    return NullableColorPicker.this;
                }

                @Override
                public String getName() {
                    return "showing";
                }
            };
        }
        return showing;
    }



    // --- armed
    /**
     * Indicates that the ComboBox has been "armed" such that a mouse release
     * will cause the ComboBox {@link #show()} method to be invoked. This is
     * subtly different from pressed. Pressed indicates that the mouse has been
     * pressed on a Node and has not yet been released. {@code arm} however
     * also takes into account whether the mouse is actually over the
     * ComboBox and pressed.
     * @return the armed property
     */
    public BooleanProperty armedProperty() { return armed; }
    private final void setArmed(boolean value) { armedProperty().set(value); }
    public final boolean isArmed() { return armedProperty().get(); }
    private BooleanProperty armed = new SimpleBooleanProperty(this, "armed", false) {
        @Override protected void invalidated() {
            pseudoClassStateChanged(PSEUDO_CLASS_ARMED, get());
        }
    };


    // --- On Action
    /**
     * The ComboBox action, which is invoked whenever the ComboBox
     * {@link #valueProperty() value} property is changed. This
     * may be due to the value property being programmatically changed, when the
     * user selects an item in a popup list or dialog, or, in the case of
     * {@link #editableProperty() editable} ComboBoxes, it may be when the user
     * provides their own input (be that via a {@link TextField} or some other
     * input mechanism.
     * @return the on action property
     */
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() { return onAction; }
    public final void setOnAction(EventHandler<ActionEvent> value) { onActionProperty().set(value); }
    public final EventHandler<ActionEvent> getOnAction() { return onActionProperty().get(); }
    private ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
        @Override protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return NullableColorPicker.this;
        }

        @Override
        public String getName() {
            return "onAction";
        }
    };


    // --- On Showing
    public final ObjectProperty<EventHandler<Event>> onShowingProperty() { return onShowing; }
    public final void setOnShowing(EventHandler<Event> value) { onShowingProperty().set(value); }
    public final EventHandler<Event> getOnShowing() { return onShowingProperty().get(); }
    /**
     * Called just prior to the {@code ComboBoxBase} popup/display being shown.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<Event>> onShowing = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            setEventHandler(ON_SHOWING, get());
        }

        @Override public Object getBean() {
            return NullableColorPicker.this;
        }

        @Override public String getName() {
            return "onShowing";
        }
    };


    // -- On Shown
    public final ObjectProperty<EventHandler<Event>> onShownProperty() { return onShown; }
    public final void setOnShown(EventHandler<Event> value) { onShownProperty().set(value); }
    public final EventHandler<Event> getOnShown() { return onShownProperty().get(); }
    /**
     * Called just after the {@link ComboBoxBase} popup/display is shown.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<Event>> onShown = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            setEventHandler(ON_SHOWN, get());
        }

        @Override public Object getBean() {
            return NullableColorPicker.this;
        }

        @Override public String getName() {
            return "onShown";
        }
    };


    // --- On Hiding
    public final ObjectProperty<EventHandler<Event>> onHidingProperty() { return onHiding; }
    public final void setOnHiding(EventHandler<Event> value) { onHidingProperty().set(value); }
    public final EventHandler<Event> getOnHiding() { return onHidingProperty().get(); }
    /**
     * Called just prior to the {@link ComboBox} popup/display being hidden.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<Event>> onHiding = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            setEventHandler(ON_HIDING, get());
        }

        @Override public Object getBean() {
            return NullableColorPicker.this;
        }

        @Override public String getName() {
            return "onHiding";
        }
    };


    // --- On Hidden
    public final ObjectProperty<EventHandler<Event>> onHiddenProperty() { return onHidden; }
    public final void setOnHidden(EventHandler<Event> value) { onHiddenProperty().set(value); }
    public final EventHandler<Event> getOnHidden() { return onHiddenProperty().get(); }
    /**
     * Called just after the {@link ComboBoxBase} popup/display has been hidden.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<Event>> onHidden = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            setEventHandler(ON_HIDDEN, get());
        }

        @Override public Object getBean() {
            return NullableColorPicker.this;
        }

        @Override public String getName() {
            return "onHidden";
        }
    };


    /* *************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Requests that the ComboBox display the popup aspect of the user interface.
     * As mentioned in the {@link ComboBoxBase} class javadoc, what is actually
     * shown when this method is called is undefined, but commonly it is some
     * form of popup or dialog window.
     */
    public void show() {
        if (!isDisabled()) {
            setShowing(true);
        }
    }

    /**
     * Closes the popup / dialog that was shown when {@link #show()} was called.
     */
    public void hide() {
        if (isShowing()) {
            setShowing(false);
        }
    }

    /**
     * Arms the ComboBox. An armed ComboBox will show a popup list on the next
     * expected UI gesture.
     *
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins or Behaviors. It is not common
     *       for developers or designers to access this function directly.
     */
    public void arm() {
        if (! armedProperty().isBound()) {
            setArmed(true);
        }
    }

    /**
     * Disarms the ComboBox. See {@link #arm()}.
     *
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins or Behaviors. It is not common
     *       for developers or designers to access this function directly.
     */
    public void disarm() {
        if (! armedProperty().isBound()) {
            setArmed(false);
        }
    }


    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "nullable-color-picker";


    private static final PseudoClass PSEUDO_CLASS_SHOWING =
            PseudoClass.getPseudoClass("showing");
    private static final PseudoClass PSEUDO_CLASS_ARMED =
            PseudoClass.getPseudoClass("armed");


    /* *************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case EXPANDED: return isShowing();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case EXPAND: show(); break;
            case COLLAPSE: hide(); break;
            default: super.executeAccessibleAction(action); break;
        }
    }


    /**
     * The custom colors added to the Color Palette by the user.
     */
    private ObservableList<Color> customColors = FXCollections.<Color>observableArrayList();
    /**
     * Gets the list of custom colors added to the Color Palette by the user.
     * @return the list of custom colors
     */
    public final ObservableList<Color>  getCustomColors() {
        return customColors;
    }

    /**
     * Creates a default ColorPicker instance with a selected color set to white.
     */
    public NullableColorPicker() {
        this(Color.WHITE);
    }

    /**
     * Creates a ColorPicker instance and sets the selected color to the given color.
     * @param color to be set as the currently selected color of the ColorPicker.
     */
    public NullableColorPicker(Color color) {
        setValue(color);
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        // Fix for RT-29885
        getProperties().addListener((MapChangeListener<Object, Object>) change -> {
            if (change.wasAdded()) {
                if (change.getKey() == "FOCUSED") {
                    setFocused((Boolean)change.getValueAdded());
                    getProperties().remove("FOCUSED");
                }
            }
        });
    }

    /* *************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new NullableColorPickerSkin(this);
    }

    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/


}
