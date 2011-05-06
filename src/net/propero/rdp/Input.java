/* Input.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handles input events and sends relevant input data
 *          to server
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 * 
 * (See gpl.txt for details of the GNU General Public License.)
 * 
 */

package net.propero.rdp;

import net.propero.rdp.keymapping.KeyCode;
import net.propero.rdp.keymapping.KeyCode_FileBased;
import net.propero.rdp.keymapping.KeyMapException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Collections;
import java.util.Set;
import java.util.Vector;

@SuppressWarnings({"UnusedDeclaration"})
public class Input {

    protected static Logger logger = Logger.getLogger(Input.class);

    KeyCode_FileBased newKeyMapper = null;

    private long lastMouseMoved;

    protected Vector<Integer> pressedKeys;

    protected static boolean capsLockOn = false;
    protected static boolean numLockOn = false;
    protected static boolean scrollLockOn = false;

    protected static boolean serverAltDown = false;
    protected static boolean altDown = false;
    protected static boolean ctrlDown = false;

    // Using this flag value (0x0001) seems to do nothing, and after running
    // through other possible values, the RIGHT flag does not appear to be
    // implemented
    protected static final int KBD_FLAG_RIGHT = 0x0001;
    protected static final int KBD_FLAG_EXT = 0x0100;

    // QUIET flag is actually as below (not 0x1000 as in rdesktop)
    protected static final int KBD_FLAG_QUIET = 0x200;
    protected static final int KBD_FLAG_DOWN = 0x4000;
    protected static final int KBD_FLAG_UP = 0x8000;

    protected static final int RDP_KEYPRESS = 0;
    protected static final int RDP_KEYRELEASE = KBD_FLAG_DOWN | KBD_FLAG_UP;
    protected static final int MOUSE_FLAG_MOVE = 0x0800;

    protected static final int MOUSE_FLAG_BUTTON1 = 0x1000;
    protected static final int MOUSE_FLAG_BUTTON2 = 0x2000;
    protected static final int MOUSE_FLAG_BUTTON3 = 0x4000;

    protected static final int MOUSE_FLAG_BUTTON4 = 0x0280; // wheel up - rdesktop 1.2.0
    protected static final int MOUSE_FLAG_BUTTON5 = 0x0380; // wheel down - rdesktop 1.2.0
    protected static final int MOUSE_FLAG_DOWN = 0x8000;

    protected static final int RDP_INPUT_SYNCHRONIZE = 0;
    protected static final int RDP_INPUT_CODEPOINT = 1;
    protected static final int RDP_INPUT_VIRTKEY = 2;
    protected static final int RDP_INPUT_SCANCODE = 4;
    protected static final int RDP_INPUT_MOUSE = 0x8001;

    protected static int time = 0;

    public KeyEvent lastKeyEvent = null;
    public boolean modifiersValid = false;
    public boolean keyDownWindows = false;

    protected RdesktopCanvas canvas = null;
    protected Rdp rdp = null;
    KeyCode keys = null;

    /**
     * Create a new Input object with a given keymap object
     *
     * @param c Canvas on which to listen for input events
     * @param r Rdp layer on which to send input messages
     * @param k Key map to use in handling keyboard events
     */
    public Input(RdesktopCanvas c, Rdp r, KeyCode_FileBased k) {
        newKeyMapper = k;
        canvas = c;
        rdp = r;
        if (Options.isDebugKeyboard()) {
            logger.setLevel(Level.DEBUG);
        } else {
            logger.setLevel(Level.WARN);
        }
        addInputListeners();
        pressedKeys = new Vector<Integer>();

        Set<AWTKeyStroke> emptyKeystrokeSet = Collections.emptySet();

        KeyboardFocusManager.getCurrentKeyboardFocusManager().setDefaultFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptyKeystrokeSet);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().setDefaultFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, emptyKeystrokeSet);
    }

    /**
     * Create a new Input object, using a keymap generated from a specified file
     *
     * @param canvas     Canvas on which to listen for input events
     * @param rdp        Rdp layer on which to send input messages
     * @param keymapFile Path to file containing keymap data
     */
    public Input(RdesktopCanvas canvas, Rdp rdp, String keymapFile) {
        try {
            newKeyMapper = new KeyCode_FileBased(keymapFile);
        } catch (KeyMapException kmEx) {
            System.err.println(kmEx.getMessage());
            if (!Common.underApplet)
                System.exit(-1);
        }

        this.canvas = canvas;
        this.rdp = rdp;
        if (Options.isDebugKeyboard()) {
            logger.setLevel(Level.WARN);
        }
        addInputListeners();
        pressedKeys = new Vector<Integer>();

        Set<AWTKeyStroke> emptyKeystrokeSet = Collections.emptySet();

        KeyboardFocusManager.getCurrentKeyboardFocusManager().setDefaultFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptyKeystrokeSet);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().setDefaultFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, emptyKeystrokeSet);
    }

    /**
     * Add all relevant input listeners to the canvas
     */
    public void addInputListeners() {
        canvas.addMouseListener(new RdesktopMouseAdapter());
        canvas.addMouseMotionListener(new RdesktopMouseMotionAdapter());
        canvas.addKeyListener(new RdesktopKeyAdapter());
        canvas.addMouseWheelListener(new RdesktopMouseWheelAdapter());
    }

    /**
     * Send a sequence of key actions to the server
     *
     * @param pressSequence String representing a sequence of key actions.
     *                      Actions are represented as a pair of consecutive characters,
     *                      the first character's value (cast to integer) being the scancode
     *                      to send, the second (cast to integer) of the pair representing the action
     *                      (0 == UP, 1 == DOWN, 2 == QUIET UP, 3 == QUIET DOWN).
     */
    public void sendKeyPresses(String pressSequence) {
        try {
            String debugString = "Sending key presses: ";
            for (int i = 0; i < pressSequence.length(); i += 2) {
                int scanCode = (int) pressSequence.charAt(i);
                int action = (int) pressSequence.charAt(i + 1);
                int flags = 0;

                if (action == KeyCode_FileBased.UP) {
                    flags = RDP_KEYRELEASE;
                } else if (action == KeyCode_FileBased.DOWN) {
                    flags = RDP_KEYPRESS;
                } else if (action == KeyCode_FileBased.QUIETUP) {
                    flags = RDP_KEYRELEASE | KBD_FLAG_QUIET;
                } else if (action == KeyCode_FileBased.QUIETDOWN) {
                    flags = KBD_FLAG_QUIET;
                }

                long t = getTime();

                sendScanCode(t, flags, scanCode);
            }

            if (pressSequence.length() > 0)
                logger.debug(debugString);
        } catch (Exception ignore) {
        }
    }

    /**
     * Retrieve the next "timestamp", by incrementing previous
     * stamp (up to the maximum value of an integer, at which the
     * timestamp is reverted to 1)
     *
     * @return New timestamp value
     */
    public static int getTime() {
        time++;
        if (time == Integer.MAX_VALUE)
            time = 1;
        return time;
    }

    /**
     * Handle loss of focus to the main canvas.
     * Clears all depressed keys (sending release messages
     * to the server.
     */
    public void lostFocus() {
        clearKeys();
        modifiersValid = false;
    }

    /**
     * Handle the main canvas gaining focus.
     * Check locking key states.
     */
    public void gainedFocus() {
        doLockKeys(); // ensure lock key states are correct
    }

    /**
     * Send a keyboard event to the server
     *
     * @param time     Time stamp to identify this event
     * @param flags    Flags defining the nature of the event (eg: press/release/quiet/extended)
     * @param scanCode Scancode value identifying the key in question
     */
    public void sendScanCode(long time, int flags, int scanCode) {

        if (scanCode == 0x38) { // be careful with alt
            if ((flags & RDP_KEYRELEASE) != 0) {
                logger.info("Alt release, serverAltDown = " + serverAltDown);
                serverAltDown = false;
            }
            if ((flags == RDP_KEYPRESS)) {
                logger.info("Alt press, serverAltDown = " + serverAltDown);
                serverAltDown = true;
            }
        }

        if ((scanCode & KeyCode.SCANCODE_EXTENDED) != 0) {
            rdp.sendInput((int) time, RDP_INPUT_SCANCODE, flags | KBD_FLAG_EXT,
                    scanCode & ~KeyCode.SCANCODE_EXTENDED, 0);
        } else
            rdp.sendInput((int) time, RDP_INPUT_SCANCODE, flags, scanCode, 0);
    }

    /**
     * Release any modifier keys that may be depressed.
     */
    public void clearKeys() {
        if (!modifiersValid)
            return;

        altDown = false;
        ctrlDown = false;

        if (lastKeyEvent == null)
            return;

        if (lastKeyEvent.isShiftDown())
            sendScanCode(getTime(), RDP_KEYRELEASE, 0x2a); // shift
        if (lastKeyEvent.isAltDown() || serverAltDown) {
            sendScanCode(getTime(), RDP_KEYRELEASE, 0x38); // ALT
            sendScanCode(getTime(), KBD_FLAG_QUIET, 0x38); // ALT
            sendScanCode(getTime(), RDP_KEYRELEASE | KBD_FLAG_QUIET, 0x38); // l.alt
        }
        if (lastKeyEvent.isControlDown()) {
            sendScanCode(getTime(), RDP_KEYRELEASE, 0x1d); // l.ctrl
            //sendScanCode(getTime(), RDP_KEYPRESS | KBD_FLAG_QUIET, 0x1d); // Ctrl
            //sendScanCode(getTime(), RDP_KEYRELEASE | KBD_FLAG_QUIET, 0x1d); // ctrl
        }
        if (lastKeyEvent != null && lastKeyEvent.isAltGraphDown()) {
            sendScanCode(getTime(), RDP_KEYRELEASE, 0x38 | KeyCode.SCANCODE_EXTENDED); //r.alt
        }
    }

    /**
     * Send keypress events for any modifier keys that are currently down
     */
    public void setKeys() {
        if (!modifiersValid) {
            return;
        }


        if (lastKeyEvent == null) {
            return;
        }

        if (lastKeyEvent.isShiftDown()) {
            sendScanCode(getTime(), RDP_KEYPRESS, 0x2a); // shift
        }
        if (lastKeyEvent.isAltDown()) {
            sendScanCode(getTime(), RDP_KEYPRESS, 0x38); // l.alt
        }
        if (lastKeyEvent.isControlDown()) {
            sendScanCode(getTime(), RDP_KEYPRESS, 0x1d); // l.ctrl
        }
        if (lastKeyEvent != null && lastKeyEvent.isAltGraphDown()) {
            sendScanCode(getTime(), RDP_KEYPRESS, 0x38 | KeyCode.SCANCODE_EXTENDED); //r.alt
        }
    }

    class RdesktopKeyAdapter extends KeyAdapter {

        /**
         * Construct an RdesktopKeyAdapter based on the parent KeyAdapter class
         */
        public RdesktopKeyAdapter() {
            super();
        }

        /**
         * Handle a keyPressed event, sending any relevant keypresses to the server
         */
        public void keyPressed(KeyEvent e) {
            lastKeyEvent = e;
            modifiersValid = true;
            long time = getTime();

            // Some java versions have keys that don't generate keyPresses -
            // here we add the key so we can later check if it happened
            pressedKeys.addElement(e.getKeyCode());

            logger.debug("PRESSED keychar='" + e.getKeyChar() + "' keycode=0x"
                    + Integer.toHexString(e.getKeyCode()) + " char='"
                    + ((char) e.getKeyCode()) + "'");

            if (rdp != null) {
                if (!handleSpecialKeys(time, e, true)) {
                    sendKeyPresses(newKeyMapper.getKeyStrokes(e));
                }
                // sendScanCode(time, RDP_KEYPRESS, keys.getScancode(e));
            }
        }

        /**
         * Handle a keyTyped event, sending any relevant keypresses to the server
         */
        public void keyTyped(KeyEvent e) {
            lastKeyEvent = e;
            modifiersValid = true;
            long time = getTime();

            // Some java versions have keys that don't generate keyPresses -
            // here we add the key so we can later check if it happened
            pressedKeys.addElement(e.getKeyCode());

            logger.debug("TYPED keychar='" + e.getKeyChar() + "' keycode=0x"
                    + Integer.toHexString(e.getKeyCode()) + " char='"
                    + ((char) e.getKeyCode()) + "'");

            if (rdp != null) {
                if (!handleSpecialKeys(time, e, true))
                    sendKeyPresses(newKeyMapper.getKeyStrokes(e));
                // sendScanCode(time, RDP_KEYPRESS, keys.getScancode(e));
            }
        }

        /**
         * Handle a keyReleased event, sending any relevent key events to the server
         */
        public void keyReleased(KeyEvent e) {
            // Some java versions have keys that don't generate keyPresses -
            // we added the key to the vector in keyPressed so here we check for
            // it
            Integer keycode = e.getKeyCode();
            if (!pressedKeys.contains(keycode)) {
                this.keyPressed(e);
            }

            pressedKeys.removeElement(keycode);

            lastKeyEvent = e;
            modifiersValid = true;
            long time = getTime();

            logger.debug("RELEASED keychar='" + e.getKeyChar() + "' keycode=0x"
                    + Integer.toHexString(e.getKeyCode()) + " char='"
                    + ((char) e.getKeyCode()) + "'");
            if (rdp != null) {
                if (!handleSpecialKeys(time, e, false))
                    sendKeyPresses(newKeyMapper.getKeyStrokes(e));
                // sendScanCode(time, RDP_KEYRELEASE, keys.getScancode(e));
            }
        }

    }

    /**
     * Act on any keyboard shortcuts that a specified KeyEvent may describe
     *
     * @param time    Time stamp for event to send to server
     * @param e       Keyboard event to be checked for shortcut keys
     * @param pressed True if key was pressed, false if released
     * @return True if a shortcut key combination was detected and acted upon, false otherwise
     */
    public boolean handleShortcutKeys(long time, KeyEvent e, boolean pressed) {
        if (!e.isAltDown())
            return false;

        if (!altDown)
            return false; // all of the below have ALT on

        switch (e.getKeyCode()) {

            /* case KeyEvent.VK_M:
               if(pressed) ((RdesktopFrame) canvas.getParent()).toggleMenu();
               break; */

            case KeyEvent.VK_ENTER:
                sendScanCode(time, RDP_KEYRELEASE, 0x38);
                altDown = false;
                ((RdesktopFrame) canvas.getParent()).toggleFullScreen();
                break;

            /*
                * The below case block handles "real" ALT+TAB events. Once the TAB in
                * an ALT+TAB combination has been pressed, the TAB is sent to the
                * server with the quiet flag on, as is the subsequent ALT-up.
                *
                * This ensures that the initial ALT press is "undone" by the server.
                *
                * --- Tom Elliott, 7/04/05
                */

            case KeyEvent.VK_TAB: // Alt+Tab received, quiet combination

                sendScanCode(time, (pressed ? RDP_KEYPRESS : RDP_KEYRELEASE)
                        | KBD_FLAG_QUIET, 0x0f);
                if (!pressed) {
                    sendScanCode(time, RDP_KEYRELEASE | KBD_FLAG_QUIET, 0x38); // Release Alt
                }

                if (pressed)
                    logger.debug("Alt + Tab pressed, ignoring, releasing tab");
                break;
            case KeyEvent.VK_PAGE_UP: // Alt + PgUp = Alt-Tab
                sendScanCode(time, pressed ? RDP_KEYPRESS : RDP_KEYRELEASE, 0x0f); // TAB
                if (pressed)
                    logger.debug("shortcut pressed: sent ALT+TAB");
                break;
            case KeyEvent.VK_PAGE_DOWN: // Alt + PgDown = Alt-Shift-Tab
                if (pressed) {
                    sendScanCode(time, RDP_KEYPRESS, 0x2a); // Shift
                    sendScanCode(time, RDP_KEYPRESS, 0x0f); // TAB
                    logger.debug("shortcut pressed: sent ALT+SHIFT+TAB");
                } else {
                    sendScanCode(time, RDP_KEYRELEASE, 0x0f); // TAB
                    sendScanCode(time, RDP_KEYRELEASE, 0x2a); // Shift
                }

                break;
            case KeyEvent.VK_INSERT: // Alt + Insert = Alt + Esc
                sendScanCode(time, pressed ? RDP_KEYPRESS : RDP_KEYRELEASE, 0x01); // ESC
                if (pressed)
                    logger.debug("shortcut pressed: sent ALT+ESC");
                break;
            case KeyEvent.VK_HOME: // Alt + Home = Ctrl + Esc (Start)
                if (pressed) {
                    sendScanCode(time, RDP_KEYRELEASE, 0x38); // ALT
                    sendScanCode(time, RDP_KEYPRESS, 0x1d); // left Ctrl
                    sendScanCode(time, RDP_KEYPRESS, 0x01); // Esc
                    logger.debug("shortcut pressed: sent CTRL+ESC (Start)");

                } else {
                    sendScanCode(time, RDP_KEYRELEASE, 0x01); // escape
                    sendScanCode(time, RDP_KEYRELEASE, 0x1d); // left ctrl
                    // sendScanCode(time,RDP_KEYPRESS,0x38); // ALT
                }

                break;
            case KeyEvent.VK_END: // Ctrl+Alt+End = Ctrl+Alt+Del
                if (ctrlDown) {
                    sendScanCode(time, pressed ? RDP_KEYPRESS : RDP_KEYRELEASE,
                            0x53 | KeyCode.SCANCODE_EXTENDED); // DEL
                    if (pressed)
                        logger.debug("shortcut pressed: sent CTRL+ALT+DEL");
                }
                break;
            case KeyEvent.VK_DELETE: // Alt + Delete = Menu
                if (pressed) {
                    sendScanCode(time, RDP_KEYRELEASE, 0x38); // ALT
                    // need to do another press and release to shift focus from
                    // to/from menu bar
                    sendScanCode(time, RDP_KEYPRESS, 0x38); // ALT
                    sendScanCode(time, RDP_KEYRELEASE, 0x38); // ALT
                    sendScanCode(time, RDP_KEYPRESS,
                            0x5d | KeyCode.SCANCODE_EXTENDED); // Menu
                    logger.debug("shortcut pressed: sent MENU");
                } else {
                    sendScanCode(time, RDP_KEYRELEASE,
                            0x5d | KeyCode.SCANCODE_EXTENDED); // Menu
                    // sendScanCode(time,RDP_KEYPRESS,0x38); // ALT
                }
                break;
            case KeyEvent.VK_SUBTRACT: // Ctrl + Alt + Minus (on NUM KEYPAD) =
                // Alt+PrtSc
                if (ctrlDown) {
                    if (pressed) {
                        sendScanCode(time, RDP_KEYRELEASE, 0x1d); // Ctrl
                        sendScanCode(time, RDP_KEYPRESS,
                                0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
                        logger.debug("shortcut pressed: sent ALT+PRTSC");
                    } else {
                        sendScanCode(time, RDP_KEYRELEASE,
                                0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
                        sendScanCode(time, RDP_KEYPRESS, 0x1d); // Ctrl
                    }
                }
                break;
            case KeyEvent.VK_ADD: // Ctrl + ALt + Plus (on NUM KEYPAD) = PrtSc
            case KeyEvent.VK_EQUALS: // for laptops that can't do Ctrl-Alt+Plus
                if (ctrlDown) {
                    if (pressed) {
                        sendScanCode(time, RDP_KEYRELEASE, 0x38); // Alt
                        sendScanCode(time, RDP_KEYRELEASE, 0x1d); // Ctrl
                        sendScanCode(time, RDP_KEYPRESS,
                                0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
                        logger.debug("shortcut pressed: sent PRTSC");
                    } else {
                        sendScanCode(time, RDP_KEYRELEASE,
                                0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
                        sendScanCode(time, RDP_KEYPRESS, 0x1d); // Ctrl
                        sendScanCode(time, RDP_KEYPRESS, 0x38); // Alt
                    }
                }
                break;
            default:
                return false;
        }
        if (!altDown)
            return false; // all of the below have ALT on

        switch (e.getKeyCode()) {
            case KeyEvent.VK_MINUS: // for laptops that can't do Ctrl+Alt+Minus
                if (ctrlDown) {
                    if (pressed) {
                        sendScanCode(time, RDP_KEYRELEASE, 0x1d); // Ctrl
                        sendScanCode(time, RDP_KEYPRESS, 0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
                        logger.debug("shortcut pressed: sent ALT+PRTSC");
                    } else {
                        sendScanCode(time, RDP_KEYRELEASE, 0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
                        sendScanCode(time, RDP_KEYPRESS, 0x1d); // Ctrl
                    }
                }
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Deal with modifier keys as control, alt or caps lock
     *
     * @param time    Time stamp for key event
     * @param e       Key event to check for special keys
     * @param pressed True if key was pressed, false if released
     * @return true if the event was handled
     */
    public boolean handleSpecialKeys(long time, KeyEvent e, boolean pressed) {

        if (handleShortcutKeys(time, e, pressed)) {
            return true;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                ctrlDown = pressed;
                return false;
            case KeyEvent.VK_ALT:
                altDown = pressed;
                return false;
            case KeyEvent.VK_CAPS_LOCK:
                if (pressed && Options.isCapsSendsUpAndDown()) {
                    capsLockOn = !capsLockOn;
                }
                if (!Options.isCapsSendsUpAndDown()) {
                    capsLockOn = pressed;
                }
                return false;
            case KeyEvent.VK_NUM_LOCK:
                if (pressed) {
                    numLockOn = !numLockOn;
                }
                return false;
            case KeyEvent.VK_SCROLL_LOCK:
                if (pressed) {
                    scrollLockOn = !scrollLockOn;
                }
                return false;
            case KeyEvent.VK_PAUSE: // untested
                if (pressed) { // E1 1D 45 E1 9D C5
                    rdp.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0xe1, 0);
                    rdp.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0x1d, 0);
                    rdp.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0x45, 0);
                    rdp.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0xe1, 0);
                    rdp.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0x9d, 0);
                    rdp.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0xc5, 0);
                } else { // release left ctrl
                    rdp.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYRELEASE, 0x1d, 0);
                }
                break;

            default:
                return false; // not handled - use sendScanCode instead
        }
        return true; // handled - no need to use sendScanCode
    }

    /**
     * Turn off any locking key, check states if available
     */
    public void triggerReadyToSend() {
        capsLockOn = false;
        numLockOn = false;
        scrollLockOn = false;
        doLockKeys(); // ensure lock key states are correct
    }

    /**
     * Handle pressing of the middle mouse button, sending relevent event data to the server
     *
     * @param e MouseEvent detailing circumstances under which middle button was pressed
     */
    protected void middleButtonPressed(MouseEvent e) {
        rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON3 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
    }

    /**
     * Handle release of the middle mouse button, sending relevent event data to the server
     *
     * @param e MouseEvent detailing circumstances under which middle button was released
     */
    protected void middleButtonReleased(MouseEvent e) {
        /* if (!Options.paste_hack || !ctrlDown) */
        rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON3, e.getX(), e
                .getY());
    }

    /**
     * Send the mouse button information to the remote machine
     */
    class RdesktopMouseAdapter extends MouseAdapter {

        public RdesktopMouseAdapter() {
            super();
        }

        public void mousePressed(MouseEvent e) {
            if (e.getY() != 0)
                ((RdesktopFrame) canvas.getParent()).hideMenu();

            int time = getTime();
            if (rdp != null) {
                if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
                    logger.debug("Mouse Button 1 Pressed.");
                    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON1
                            | MOUSE_FLAG_DOWN, e.getX(), e.getY());
                } else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                    logger.debug("Mouse Button 3 Pressed.");
                    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON2
                            | MOUSE_FLAG_DOWN, e.getX(), e.getY());
                } else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) {
                    logger.debug("Middle Mouse Button Pressed.");
                    middleButtonPressed(e);
                }
            }
        }

        public void mouseReleased(MouseEvent e) {
            int time = getTime();
            if (rdp != null) {
                if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
                    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON1, e.getX(), e.getY());
                } else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON2, e.getX(), e.getY());
                } else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) {
                    middleButtonReleased(e);
                }
            }
        }
    }

    /**
     * Send the mouse location information to the remote machine
     */

    class RdesktopMouseMotionAdapter extends MouseMotionAdapter {

        public RdesktopMouseMotionAdapter() {
            super();
            lastMouseMoved = System.currentTimeMillis();
        }

        /**
         * This method is used to send the current mouse position to the remote machine
         *
         * @param e Event that contains the new mouse information
         */
        public void mouseMoved(MouseEvent e) {
            // Code to limit mouse events to 4 per second.
            if ((System.currentTimeMillis() - lastMouseMoved) > 250) {
                lastMouseMoved = System.currentTimeMillis();

                logger.debug("mouseMoved to " + e.getX() + ", " + e.getY() + " at " + time);

                // TODO: complete menu show/hide section
                if (e.getY() == 0) {
                    ((RdesktopFrame) canvas.getParent()).showMenu();
                } else {
                    ((RdesktopFrame) canvas.getParent()).hideMenu();
                }

                if (rdp != null) {
                    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_MOVE, e.getX(), e.getY());
                }
            }
        }

        public void mouseDragged(MouseEvent e) {
            int time = getTime();

            logger.debug("mouseMoved to " + e.getX() + ", " + e.getY() + " at " + time);

            if (rdp != null) {
                rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_MOVE, e.getX(),
                        e.getY());
            }
        }
    }

    protected void doLockKeys() {
        // 	doesn't work on Java 1.4.1_02 or 1.4.2 on Linux, there is a bug in java....
        // does work on the same version on Windows.
        if (!Rdesktop.readyToSend)
            return;
        if (!Options.isUseLockingKeyState())
            return;
        if (Constants.OS == Constants.TS_OSMAJORTYPE_OS2)
            return; // broken for linux
        if (Constants.OS == Constants.TS_OSMAJORTYPE_MACINTOSH)
            return; // unsupported operation for mac
        logger.debug("doLockKeys");

        try {
            Toolkit tk = Toolkit.getDefaultToolkit();
            if (tk.getLockingKeyState(KeyEvent.VK_CAPS_LOCK) != capsLockOn) {
                capsLockOn = !capsLockOn;
                logger.debug("CAPS LOCK toggle");
                sendScanCode(getTime(), RDP_KEYPRESS, 0x3a);
                sendScanCode(getTime(), RDP_KEYRELEASE, 0x3a);

            }
            if (tk.getLockingKeyState(KeyEvent.VK_NUM_LOCK) != numLockOn) {
                numLockOn = !numLockOn;
                logger.debug("NUM LOCK toggle");
                sendScanCode(getTime(), RDP_KEYPRESS, 0x45);
                sendScanCode(getTime(), RDP_KEYRELEASE, 0x45);

            }
            if (tk.getLockingKeyState(KeyEvent.VK_SCROLL_LOCK) != scrollLockOn) {
                scrollLockOn = !scrollLockOn;
                logger.debug("SCROLL LOCK toggle");
                sendScanCode(getTime(), RDP_KEYPRESS, 0x46);
                sendScanCode(getTime(), RDP_KEYRELEASE, 0x46);
            }
        } catch (Exception e) {
            Options.setUseLockingKeyState(false);
        }
    }

    private class RdesktopMouseWheelAdapter implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            int time = getTime();
            //   if(logger.isInfoEnabled()) logger.info("mousePressed at "+time);
            if (rdp != null) {
                if (e.getWheelRotation() < 0) { // up
                    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON4 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
                } else { // down
                    rdp.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON5 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
                }
            }
        }
    }
}
