/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

package tpv.fxcontrol.behavior;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.TwoLevelFocusComboBehavior;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import tpv.fxcontrol.NullableColorPicker;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCode.F10;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;


public class NullableColorPickerBehavior extends BehaviorBase<NullableColorPicker> {

    private final InputMap<NullableColorPicker> inputMap;
    private InvalidationListener focusListener = this::focusChanged;

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    private TwoLevelFocusComboBehavior tlFocus;

    /**
     *
     */
    public NullableColorPickerBehavior(final NullableColorPicker control) {
        super(control);

        // create a map for comboBox-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        inputMap = createInputMap();

        final EventHandler<KeyEvent> togglePopup = e -> {
            // If popup is shown, KeyEvent causes popup to close
            showPopupOnMouseRelease = true;

            if (getNode().isShowing()) {
                hide();
            } else {
                show();
            }
        };

        // comboBox-specific mappings for key and mouse input
        InputMap.KeyMapping enterPressed, enterReleased;
        addDefaultMapping(inputMap,
                new InputMap.KeyMapping(F4, KEY_RELEASED, togglePopup),
                new InputMap.KeyMapping(new KeyBinding(UP).alt(), togglePopup),
                new InputMap.KeyMapping(new KeyBinding(DOWN).alt(), togglePopup),

                new InputMap.KeyMapping(SPACE, KEY_PRESSED, this::keyPressed),
                new InputMap.KeyMapping(SPACE, KEY_RELEASED, this::keyReleased),

                enterPressed = new InputMap.KeyMapping(ENTER, KEY_PRESSED, this::keyPressed),
                enterReleased = new InputMap.KeyMapping(ENTER, KEY_RELEASED, this::keyReleased),

                // The following keys are forwarded to the parent container
                new InputMap.KeyMapping(ESCAPE, KEY_PRESSED, this::cancelEdit),
                new InputMap.KeyMapping(F10, KEY_PRESSED, this::forwardToParent),

                new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed),
                new InputMap.MouseMapping(MouseEvent.MOUSE_RELEASED, this::mouseReleased),
                new InputMap.MouseMapping(MouseEvent.MOUSE_ENTERED, this::mouseEntered),
                new InputMap.MouseMapping(MouseEvent.MOUSE_EXITED, this::mouseExited)
        );

        // we don't want to consume events on enter press - let them carry on through
        enterPressed.setAutoConsume(false);
        enterReleased.setAutoConsume(false);

        // ComboBoxBase also cares about focus
        control.focusedProperty().addListener(focusListener);

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusComboBehavior(control); // needs to be last.
        }
    }

    @Override
    public void dispose() {
        if (tlFocus != null) tlFocus.dispose();
        getNode().focusedProperty().removeListener(focusListener);
        super.dispose();
    }

    @Override
    public InputMap<NullableColorPicker> getInputMap() {
        return inputMap;
    }

    /***************************************************************************
     *                                                                         *
     * Focus change handling                                                   *
     *                                                                         *
     **************************************************************************/

    protected void focusChanged(Observable o) {
        // If we did have the key down, but are now not focused, then we must
        // disarm the box.
        final NullableColorPicker box = getNode();
        if (keyDown && !box.isFocused()) {
            keyDown = false;
            box.disarm();
        }
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    /**
     * Indicates that a keyboard key has been pressed which represents the
     * event (this could be space bar for example). As long as keyDown is true,
     * we are also armed, and will ignore mouse events related to arming.
     * Note this is made package private solely for the sake of testing.
     */
    private boolean keyDown;

    /**
     * This function is invoked when an appropriate keystroke occurs which
     * causes this button to be armed if it is not already armed by a mouse
     * press.
     */
    private void keyPressed(KeyEvent e) {
        // If popup is shown, KeyEvent causes popup to close
        showPopupOnMouseRelease = true;

        if (Utils.isTwoLevelFocus()) {
            show();
            if (tlFocus != null) {
                tlFocus.setExternalFocus(false);
            }
        } else {
            if (!getNode().isPressed() && !getNode().isArmed()) {
                keyDown = true;
                getNode().arm();
            }
        }
    }

    /**
     * Invoked when a valid keystroke release occurs which causes the button
     * to fire if it was armed by a keyPress.
     */
    private void keyReleased(KeyEvent e) {
        // If popup is shown, KeyEvent causes popup to close
        showPopupOnMouseRelease = true;

        if (!Utils.isTwoLevelFocus()) {
            if (keyDown) {
                keyDown = false;
                if (getNode().isArmed()) {
                    getNode().disarm();
                }
            }
        }
    }

    private void forwardToParent(KeyEvent event) {
        if (getNode().getParent() != null) {
            getNode().getParent().fireEvent(event);
        }
    }

    private void cancelEdit(KeyEvent event) {
        /**
         * This can be cleaned up if the editor property is moved up
         * to ComboBoxBase.
         */
        NullableColorPicker comboBoxBase = getNode();

        TextField textField = null;


        if (textField != null && textField.getTextFormatter() != null) {
            textField.cancelEdit();
        } else {
            forwardToParent(event);
        }
    }


    /**************************************************************************
     *                                                                        *
     * Mouse Events                                                           *
     *                                                                        *
     *************************************************************************/

    public void mousePressed(MouseEvent e) {
        arm(e);
    }

    public void mouseReleased(MouseEvent e) {
        disarm();

        // The showPopupOnMouseRelease boolean was added to resolve
        // RT-18151: namely, clicking on the comboBox button shouldn't hide,
        // and then immediately show the popup, which was occurring because we
        // can't know whether the popup auto-hide was coming because of a MOUSE_PRESS
        // since PopupWindow calls hide() before it calls onAutoHide().
        if (showPopupOnMouseRelease) {
            show();
        } else {
            showPopupOnMouseRelease = true;
            hide();
        }
    }

    public void mouseEntered(MouseEvent e) {
        arm();
    }

    public void mouseExited(MouseEvent e) {
        disarm();
    }

//    private void getFocus() {
//        if (! getNode().isFocused() && getNode().isFocusTraversable()) {
//            getNode().requestFocus();
//        }
//    }

    private void arm(MouseEvent e) {
        boolean valid = (e.getButton() == MouseButton.PRIMARY &&
                !(e.isMiddleButtonDown() || e.isSecondaryButtonDown() ||
                        e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown()));

        if (!getNode().isArmed() && valid) {
            getNode().arm();
        }
    }

    public void show() {
        if (!getNode().isShowing()) {
            if (getNode().isFocusTraversable()) {
                getNode().requestFocus();
            }
            getNode().show();
        }
    }

    public void hide() {
        if (getNode().isShowing()) {
            getNode().hide();
        }
    }

    private boolean showPopupOnMouseRelease = true;

    public void onAutoHide(PopupControl popup) {
        // when we click on some non  interactive part of the
        // Color Palette - we do not want to hide.
        if (!popup.isShowing() && getNode().isShowing()) {
            // Popup was dismissed. Maybe user clicked outside or typed ESCAPE.
            // Make sure DatePicker button is in sync.
            getNode().hide();
        }
        // if the ColorPicker is no longer showing, then invoke the super method
        // to keep its show/hide state in sync.
        if (!getNode().isShowing()) {
            hide();
        }
    }

    public void arm() {
        if (getNode().isPressed()) {
            getNode().arm();
        }
    }

    public void disarm() {
        if (!keyDown && getNode().isArmed()) {
            getNode().disarm();
        }
    }
}
