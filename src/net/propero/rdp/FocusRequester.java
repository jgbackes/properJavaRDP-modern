// FocusRequester solves a problem where calls to requestFocus() fail - this seems to happen
// in some cases even when the call is made after setting a window visible and also even
// if using EventQueue.invokeLater().
// The solution seems to be to act on a windowOpened event and only then set the focus and
// still by using invokeLater().
//
// Usage:
// FocusRequester.requestFocus(win, beanToFocusOn);

package net.propero.rdp;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FocusRequester implements Runnable {
    private static Component beanToFocusOn;
    private static FocusRequester theFocusRequester = null;
    private static WindowAdapter theWindowOpenListener = null;

    public static void requestFocus(Window win, Component bean) {
        if (theFocusRequester == null) {
            theFocusRequester = new FocusRequester();

            // Create a WindowAdaptor which calls invokeLater on theFocusRequester when win is opened.
            theWindowOpenListener = new WindowAdapter() {
                public void windowOpened(WindowEvent e) {
                    EventQueue.invokeLater(theFocusRequester);
                }
            };
        }

        beanToFocusOn = bean;

        if (win != null) {
            win.addWindowListener(theWindowOpenListener);
        }

        // Call now in case window is already opened or win was passed as null;
        // normally the effective call is the
        // one made in theWindowOpenListener.windowOpened()
        EventQueue.invokeLater(theFocusRequester);
    }

    public void run() {
        beanToFocusOn.requestFocus();
    }
}