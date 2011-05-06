/* RdesktopFrame.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Window for RDP session
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

import net.propero.rdp.keymapping.KeyCode_FileBased;
import net.propero.rdp.menu.RdpMenu;
import net.propero.rdp.virtualChannels.cliprdr.ClipChannel;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

public class RdesktopFrame extends Frame implements Printable {

    static Logger logger = Logger.getLogger(Rdp.class);

    public RdesktopCanvas canvas = null;
    public Rdp rdp = null;
    public RdpMenu menu = null;

    private boolean menuVisible = true;
    protected boolean inFullscreen = false;

    /**
     * Register the clipboard channel
     *
     * @param c ClipChannel object for controlling clipboard mapping
     */
    public void setClip(ClipChannel c) {
        canvas.addFocusListener(c);
    }

    // TODO: replace deprecated method
    public boolean action(Event event, Object arg) {
        if (menu != null)
            return menu.action(event, arg);
        return false;
    }

    /**
     * Switch to fullscreen mode
     */
    public void goFullScreen() {
        inFullscreen = true;

        if (Options.isFullScreen()) {

            inFullscreen = true;

            //if (this.isDisplayable()) this.dispose();
            this.setVisible(false);
            this.setLocation(0, 0);
            //this.setUndecorated(true);
            this.setVisible(true);

            this.pack();
        }
    }

    /**
     * Exit fullscreen mode
     */
    public void leaveFullScreen() {
        inFullscreen = false;
        if (Options.isFullScreen()) {

            inFullscreen = false;

            if (this.isDisplayable())
                this.dispose();

            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice myDevice = env.getDefaultScreenDevice();
            if (myDevice.isFullScreenSupported())
                myDevice.setFullScreenWindow(null);

            this.setLocation(10, 10);
            //this.setUndecorated(false);
            this.setVisible(true);
            //setExtendedState (Frame.NORMAL);
            this.pack();
        }
    }

    /**
     * Switch in/out of fullscreen mode
     */
    public void toggleFullScreen() {
        if (inFullscreen)
            leaveFullScreen();
        else
            goFullScreen();
    }


    /**
     * Display the menu bar
     */
    public void showMenu() {
        if (menu == null)
            menu = new RdpMenu(this);

        if (!menuVisible && Options.isEnableMenu())
            this.setMenuBar(menu);
        canvas.repaint();
        menuVisible = true;
    }

    /**
     * Hide the menu bar
     */
    public void hideMenu() {
        if (menuVisible && Options.isEnableMenu())
            this.setMenuBar(null);
        //canvas.setSize(this.WIDTH, this.HEIGHT);
        canvas.repaint();
        menuVisible = false;
    }

    /**
     * Toggle the menu on/off (show if hidden, hide if visible)
     */
    public void toggleMenu() {
        if (!menuVisible)
            showMenu();
        else
            hideMenu();
    }

    /**
     * Create a new RdesktopFrame.
     * Size defined by Options.width and Options.height
     * Creates RdesktopCanvas occupying entire frame
     */
    public RdesktopFrame() {
        super();
        Common.frame = this;
        setLayout(new BorderLayout());
        canvas = new RdesktopCanvas(Options.getWidth(), Options.getHeight());

        add(new Toolbar(this), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);

        setTitle(Options.getWindowTitle());

        if (Constants.OS == Constants.TS_OSMAJORTYPE_WINDOWS) {
            // Windows has to setResizable(false) before pack,
            // else draws on the frame
            setResizable(false);
        }

        if (Options.isFullScreen()) {
            goFullScreen();
            pack();
            setLocation(0, 0);
        } else {// centre
            pack();
            setLocationRelativeTo(null);
        }

        if (Constants.OS != Constants.TS_OSMAJORTYPE_WINDOWS) {
            // Linux Java 1.3 needs pack() before setResizeable
            setResizable(true);
        }

        addWindowListener(new RdesktopWindowAdapter());
        canvas.addFocusListener(new RdesktopFocusListener());

        if (Constants.OS == Constants.TS_OSMAJORTYPE_WINDOWS) {
            // redraws screen on window move
            addComponentListener(new RdesktopComponentAdapter());
        }

        canvas.requestFocus();
        FocusRequester.requestFocus(this, canvas);
    }


    /**
     * Retrieve the canvas contained within this frame
     *
     * @return RdesktopCanvas object associated with this frame
     */
    public RdesktopCanvas getCanvas() {
        return this.canvas;
    }

    /**
     * Register the RDP communications layer with this frame
     *
     * @param rdp Rdp object encapsulating the RDP comms layer
     */
    public void registerCommLayer(Rdp rdp) {
        this.rdp = rdp;
        canvas.registerCommLayer(rdp);
    }

    /**
     * Register keymap
     *
     * @param keys Keymapping object for use in handling keyboard events
     */
    public void registerKeyboard(KeyCode_FileBased keys) {
        canvas.registerKeyboard(keys);
    }

    class RdesktopFocusListener implements FocusListener {

        public void focusGained(FocusEvent arg0) {
            if (Constants.OS == Constants.TS_OSMAJORTYPE_WINDOWS) {
                canvas.repaint(0, 0, Options.getWidth(), Options.getHeight());
            }
            // gained focus..need to check state of locking keys
            canvas.gainedFocus();
        }

        public void focusLost(FocusEvent arg0) {
            //  lost focus - need clear keys that are down
            canvas.lostFocus();
        }
    }

    class RdesktopWindowAdapter extends WindowAdapter {

        public void windowClosing(WindowEvent e) {
            setVisible(false);
            Rdesktop.exit(0, rdp, (RdesktopFrame) e.getWindow(), true);
        }

        public void windowLostFocus(WindowEvent e) {
            logger.info("windowLostFocus");
            // lost focus - need clear keys that are down
            canvas.lostFocus();
        }

        public void windowDeiconified(WindowEvent e) {
            if (Constants.OS == Constants.TS_OSMAJORTYPE_WINDOWS) {
                // canvas.repaint();
                canvas.repaint(0, 0, Options.getWidth(), Options.getHeight());
            }
            canvas.gainedFocus();
        }

        public void windowActivated(WindowEvent e) {
            if (Constants.OS == Constants.TS_OSMAJORTYPE_WINDOWS) {
                // canvas.repaint();
                canvas.repaint(0, 0, Options.getWidth(), Options.getHeight());
            }
            // gained focus..need to check state of locking keys
            canvas.gainedFocus();
        }

        public void windowGainedFocus(WindowEvent e) {
            if (Constants.OS == Constants.TS_OSMAJORTYPE_WINDOWS) {
                // canvas.repaint();
                canvas.repaint(0, 0, Options.getWidth(), Options.getHeight());
            }
            // gained focus..need to check state of locking keys
            canvas.gainedFocus();
        }
    }

    class RdesktopComponentAdapter extends ComponentAdapter {
        public void componentMoved(ComponentEvent e) {
            canvas.repaint(0, 0, Options.getWidth(), Options.getHeight());
        }
    }


    /**
     * Display an error dialog with "Yes" and "No" buttons and the title "properJavaRDP error"
     *
     * @param msg Array of message lines to display in dialog box
     * @return True if "Yes" was clicked to dismiss box
     */
    public boolean showYesNoErrorDialog(String[] msg) {
        int result = JOptionPane.showConfirmDialog(this, msg, "ERROR!", JOptionPane.YES_NO_OPTION);
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Display an error dialog with the title "properJavaRDP error"
     *
     * @param msg Array of message lines to display in dialog box
     */
    public void showErrorDialog(String[] msg) {
        JOptionPane.showConfirmDialog(this, msg, "ERROR!", JOptionPane.DEFAULT_OPTION);
    }

    /**
     * Notify the canvas that the connection is ready for sending messages
     */
    public void triggerReadyToSend() {
        this.setVisible(true);
        canvas.triggerReadyToSend();
    }

    /**
     * Method: print
     * <p/>
     * <p/>
     * This class is responsible for rendering a page using the provided
     * parameters. The result will be a grid where each cell will be half an
     * inch by half an inch.
     *
     * @param pageFormat a value of type PageFormat
     * @param page       a value of type int
     * @return a value of type int
     */
    public int print(Graphics g, PageFormat pageFormat, int page) {

        int i;
        Graphics2D g2d;
        Line2D.Double line = new Line2D.Double();

        //--- Validate the page number, we only print the first page
        if (page == 0) {  //--- Create a graphic2D object a set the default parameters
            g2d = (Graphics2D) g;
            g2d.setColor(Color.black);

            //--- Translate the origin to be (0,0)
            g2d.translate(pageFormat.getImageableX(), pageFormat
                    .getImageableY());

            //--- Print the vertical lines
            for (i = 0; i < pageFormat.getWidth(); i += 72 / 2) {
                line.setLine(i, 0, i, pageFormat.getHeight());
                g2d.draw(line);
            }

            //--- Print the horizontal lines
            for (i = 0; i < pageFormat.getHeight(); i += 72 / 2) {
                line.setLine(0, i, pageFormat.getWidth(), i);
                g2d.draw(line);
            }

            return (PAGE_EXISTS);
        } else
            return (NO_SUCH_PAGE);
    }

    protected void fullscreen() {
        setUndecorated(true);
        setExtendedState(Frame.MAXIMIZED_BOTH);
    }
}
