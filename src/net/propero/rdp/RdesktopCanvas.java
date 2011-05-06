/* RdesktopCanvas.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.8 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Canvas component, handles drawing requests from server,
 *          and passes user input to Input class.
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

import net.propero.rdp.cached.WrappedImage;
import net.propero.rdp.keymapping.KeyCode_FileBased;
import net.propero.rdp.orders.BoundsOrder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;

@SuppressWarnings({"UnusedDeclaration"})
public class RdesktopCanvas extends Canvas {
    static Logger logger = Logger.getLogger(Rdp.class);
    static boolean drawDebuggingRectangles = false;

    static {
        logger.setLevel(Level.WARN);
        // drawDebuggingRectangles = true;
    }


    private RasterOp rop = null;

    WrappedImage backingStore;
    // BufferedImage apex_backstore = null;


    private Input input = null;

    public static final int MIX_OPAQUE = 1;
    public static final int TEXT2_VERTICAL = 0x04;
    public static final int TEXT2_IMPLICIT_X = 0x20;

    public KeyCode_FileBased fbKeys = null;

    public int width = 0;

    public int height = 0;

    protected IndexColorModel colorMap = null;

    public Rdp rdp = null;

    // Clip region
    private int top = 0;
    private int left = 0;
    private int right = 0;
    private int bottom = 0;

    /**
     * Initialise this canvas to specified width and height, also initialise
     * backingStore
     *
     * @param width  Desired width of canvas
     * @param height Desired height of canvas
     */
    public RdesktopCanvas(int width, int height) {
        super();
        rop = new RasterOp();
        this.width = width;
        this.height = height;
        this.right = width - 1; // changed
        this.bottom = height - 1; // changed
        setSize(width, height);

        backingStore = new WrappedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public void paint(Graphics g) {
        update(g);
    }

    @Override
    public void update(Graphics g) {
        Rectangle r = g.getClipBounds();
        g.drawImage(backingStore.getSubImage(r.x, r.y, r.width, r.height), r.x, r.y, null);

        if (Options.isSave_graphics()) {
            RdesktopCanvas.saveToFile(backingStore.getSubImage(r.x, r.y, r.width, r.height));
        }
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    public RasterOp getRasterOp() {
        return rop;
    }

    public WrappedImage getBackingStore() {
        return backingStore;
    }

    /**
     * Register a color palette with this canvas
     *
     * @param cm Color model to be used with this canvas
     */
    public void registerPalette(IndexColorModel cm) {
        this.colorMap = cm;

        backingStore.setIndexColorModel(cm);
    }

    /**
     * Register the Rdp layer to act as the communications interface to this
     * canvas
     *
     * @param rdp Rdp object controlling Rdp layer communication
     */
    public void registerCommLayer(Rdp rdp) {
        this.rdp = rdp;
        if (fbKeys != null)
            input = new Input(this, rdp, fbKeys);

    }

    /**
     * Register keymap
     *
     * @param keys Keymapping object for use in handling keyboard events
     */
    public void registerKeyboard(KeyCode_FileBased keys) {
        this.fbKeys = keys;
        if (rdp != null) {
            // rdp and keys have been registered...
            input = new Input(this, rdp, keys);
        }
    }


    /**
     * Display a compressed bitmap direct to the backingStore NOTE: Currently not
     * functioning correctly, see Bitmap.decompressImgDirect Does not call
     * repaint. Image is drawn to canvas on next update
     *
     * @param x      x coordinate within backingStore for drawing of bitmap
     * @param y      y coordinate within backingStore for drawing of bitmap
     * @param width  Width of bitmap
     * @param height Height of bitmap
     * @param size   Size (bytes) of compressed bitmap data
     * @param data   Packet containing compressed bitmap data at current read
     *               position
     * @param Bpp    Bytes-per-pixel for bitmap
     * @throws RdesktopException Protocol error
     */
    public void displayCompressed(int x, int y, int width, int height,
                                  int size, RdpPacket data, int Bpp)
            throws RdesktopException {
        backingStore = Bitmap.decompressImgDirect(width, height, size, data, Bpp, x, y, backingStore);
    }

    /**
     * Draw an image object to the backingStore, does not call repaint. Image is
     * drawn to canvas on next update.
     *
     * @param img Image to draw to backingStore
     * @param x   x coordinate for drawing location
     * @param y   y coordinate for drawing location
     * @throws RdesktopException Protocol error
     */
    public void displayImage(Image img, int x, int y) throws RdesktopException {

        Graphics g = backingStore.getGraphics();
        g.drawImage(img, x, y, null);

        /* ********* Useful test for identifying image boundaries ************ */
        if (drawDebuggingRectangles) {
            Color oldColor = g.getColor();
            g.setColor(Color.YELLOW);
            g.drawRect(x, y, img.getWidth(null), img.getHeight(null));
            g.setColor(oldColor);
        }

        g.dispose();
    }

    /**
     * Draw an image (from an integer array of color data) to the backingStore,
     * does not call repaint. Image is drawn to canvas on next update.
     *
     * @param data Integer array of pixel color information
     * @param w    Width of image
     * @param h    Height of image
     * @param x    x coordinate for drawing location
     * @param y    y coordinate for drawing location
     * @param cx   Width of drawn image (clips, does not scale)
     * @param cy   Height of drawn image (clips, does not scale)
     * @throws RdesktopException Protocol error
     */
    public void displayImage(int[] data, int w, int h, int x, int y, int cx,
                             int cy) throws RdesktopException {

        backingStore.setRGB(x, y, cx, cy, data, 0, w);

        /* ********* Useful test for identifying image boundaries ************ */
        if (drawDebuggingRectangles) {
            Graphics g = backingStore.getGraphics();
            Color oldColor = g.getColor();
            g.setColor(Color.RED);
            g.drawRect(x, y, cx, cy);
            g.setColor(oldColor);
            g.dispose();
        }
    }

    /**
     * Retrieve an image from the backingStore, as integer pixel information
     *
     * @param x  x coordinate of image to retrieve
     * @param y  y coordinage of image to retrieve
     * @param cx width of image to retrieve
     * @param cy height of image to retrieve
     * @return Requested area of backingStore, as an array of integer pixel colors
     */
    public int[] getImage(int x, int y, int cx, int cy) {

        int[] data;

        // no existing image data to add to retrieving as complete image, no offset needed
        data = backingStore.getRGB(x, y, cx, cy, null, 0, cx);

        return data;
    }

    /**
     * Draw an image (from an integer array of color data) to the backingStore,
     * also calls repaint to draw image to canvas
     *
     * @param x    x coordinate at which to draw image
     * @param y    y coordinate at which to draw image
     * @param cx   Width of drawn image (clips, does not scale)
     * @param cy   Height of drawn image (clips, does not scale)
     * @param data Image to draw, represented as an array of integer pixel
     *             colors
     */
    public void putImage(int x, int y, int cx, int cy, int[] data) {

        // drawing entire image, no offset needed
        backingStore.setRGBNoConversion(x, y, cx, cy, data, 0, cx);

        /* ********* Useful test for identifying image boundaries ************ */
        if (drawDebuggingRectangles) {
            Graphics g = this.getBackingStore().getGraphics();
            Color oldColor = g.getColor();
            g.setColor(Color.BLUE);
            g.drawRect(x, y, cx, cy);
            g.setColor(oldColor);
            g.dispose();
        }
        this.repaint(x, y, cx, cy);
    }

    /**
     * Reset clipping boundaries for canvas
     */
    public void resetClip() {
        Graphics g = this.getGraphics();
        Rectangle bounds = this.getBounds();
        g.setClip(bounds.x, bounds.y, bounds.width, bounds.height);
        this.top = 0;
        this.left = 0;
        this.right = this.width - 1; // changed
        this.bottom = this.height - 1; // changed
    }

    /**
     * Set clipping boundaries for canvas, based on a bounds order
     *
     * @param bounds Order defining new boundaries
     */
    public void setClip(BoundsOrder bounds) {
        Graphics g = this.getGraphics();
        g.setClip(bounds.getLeft(), bounds.getTop(), bounds.getRight()
                - bounds.getLeft(), bounds.getBottom() - bounds.getTop());
        this.top = bounds.getTop();
        this.left = bounds.getLeft();
        this.right = bounds.getRight();
        this.bottom = bounds.getBottom();
    }

    /**
     * Move the mouse pointer (only available in Java 1.3+)
     *
     * @param x x coordinate for mouse move
     * @param y y coordinate for mouse move
     */
    public void movePointer(int x, int y) {
        Point p = this.getLocationOnScreen();
        x = x + p.x;
        y = y + p.y;
        robot.mouseMove(x, y);
    }

    /**
     * Draw a filled rectangle to the screen
     *
     * @param x     x coordinate (left) of rectangle
     * @param y     y coordinate (top) of rectangle
     * @param cx    Width of rectangle
     * @param cy    Height of rectangle
     * @param color Color of rectangle
     */
    public void fillRectangle(int x, int y, int cx, int cy, int color) {
        if (x <= right && y <= bottom) {

            int Bpp = Options.getBpp();

            // convert to 24-bit color
            color = Bitmap.convertTo24(color);

            // correction for 24-bit color
            if (Bpp == 3) {
                color = ((color & 0xFF) << 16) | (color & 0xFF00) | ((color & 0xFF0000) >> 16);
            }

            // Perform standard clipping checks, x-axis
            int clipRight = x + cx - 1;
            clipRight = Math.min(clipRight, right);
            x = Math.max(x, left);
            cx = clipRight - x + 1;

            // Perform standard clipping checks, y-axis
            int clipBottom = y + cy - 1;
            clipBottom = Math.min(clipBottom, bottom);
            y = Math.max(y, top);
            cy = clipBottom - y + 1;

            // construct rectangle as integer array, filled with color
            int[] rect = new int[cx * cy];
            for (int i = 0; i < rect.length; i++) {
                rect[i] = color;
            }
            // draw rectangle to backingStore
            backingStore.setRGB(x, y, cx, cy, rect, 0, cx);

            /* ********* Useful test for identifying image boundaries ************ */
            if (drawDebuggingRectangles) {
                Graphics g = this.getBackingStore().getGraphics();
                Color oldColor = g.getColor();
                g.setColor(Color.GREEN);
                g.drawRect(x, y, cx, cy);
                g.setColor(oldColor);
                g.dispose();
            }

            this.repaint(x, y, cx, cy); // seems to be faster than Graphics.fillRect according to JProbe
        }
    }


    /**
     * Create an AWT Cursor object
     *
     * @param x         Horizontal location
     * @param y         Vertical location
     * @param w         Width
     * @param h         Height
     * @param andMask   Drawing mask
     * @param xorMask   Drawing mask
     * @param cache_idx Item in the cache
     * @return Created Cursor
     */
    public Cursor createCursor(int x, int y,
                               int w, int h, byte[] andMask,
                               byte[] xorMask, int cache_idx) {
        int pXORMask = 0;
        int pANDMask = 0;
        Point p = new Point(x, y);
        int size = w * h;
        int scanLine = w / 8;
        int offset;
        byte[] mask = new byte[size];
        int[] cursor = new int[size];
        int pCursor;
        int pMask;

        offset = size;

        for (int i = 0; i < h; i++) {
            offset -= w;
            pMask = offset;
            for (int j = 0; j < scanLine; j++) {
                for (int bit = 0x80; bit > 0; bit >>= 1) {
                    if ((andMask[pANDMask] & bit) != 0) {
                        mask[pMask] = 0;
                    } else {
                        mask[pMask] = 1;
                    }
                    pMask++;
                }
                pANDMask++;
            }
        }

        offset = size;

        for (int i = 0; i < h; i++) {
            offset -= w;
            pCursor = offset;
            for (int j = 0; j < w; j++) {
                cursor[pCursor] = ((xorMask[pXORMask + 2] << 16) & 0x00ff0000)
                        | ((xorMask[pXORMask + 1] << 8) & 0x0000ff00)
                        | (xorMask[pXORMask] & 0x000000ff);
                pXORMask += 3;
                pCursor++;
            }

        }

        pMask = 0;
        pCursor = 0;

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                if ((mask[pMask] == 0) && (cursor[pCursor] != 0)) {
                    cursor[pCursor] = ~(cursor[pCursor]);
                    cursor[pCursor] |= 0xff000000;
                } else if ((mask[pMask] == 1) || (cursor[pCursor] != 0)) {
                    cursor[pCursor] |= 0xff000000;
                }
                pCursor++;
                pMask++;
            }
        }

        Image winCursor = this.createImage(new MemoryImageSource(w, h, cursor, 0, w));
        return createCustomCursor(winCursor, p, "", cache_idx);
    }

    /**
     * Handle the window losing focus, notify input classes
     */
    public void lostFocus() {
        if (input != null)
            input.lostFocus();
    }

    /**
     * Handle the window gaining focus, notify input classes
     */
    public void gainedFocus() {
        if (input != null)
            input.gainedFocus();
    }

    /**
     * Notify the input classes that the connection is ready for sending messages
     */
    public void triggerReadyToSend() {
        input.triggerReadyToSend();
    }

    private Robot robot = null;

    public static void saveToFile(Image image) {
        if (Options.getServerBpp() == 8)
            return;

        BufferedImage img;

        img = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.drawImage(image, 0, 0, null);

        // Write generated image to a file
        try {
            // Save as JPEG
            File file = new File("/Users/jbackes/testimages/RDPTesting-" + Options.getImgCountAndIncrement() + ".jpg");

            ImageIO.write(img, "jpg", file);
        } catch (IOException ignored) {
            System.out.println("Error printing file # " + Options.getImgCount());
        }

        g.dispose();
    }

    protected Cursor createCustomCursor(Image windowCursor, Point p, String s, int cache_idx) {
        return Toolkit.getDefaultToolkit().createCustomCursor(windowCursor, p, "");
    }

    public void addNotify() {
        super.addNotify();

        if (robot == null) {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                logger.warn("Pointer movement not allowed");
            }
        }
    }
}
