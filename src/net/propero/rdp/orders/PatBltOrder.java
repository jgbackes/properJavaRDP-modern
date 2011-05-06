/* PatBltOrder.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:40 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: 
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
package net.propero.rdp.orders;

import net.propero.rdp.Bitmap;
import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.Rdp;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.Utilities;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;

public class PatBltOrder extends DestBltOrder {

    static Logger logger = Logger.getLogger(Rdp.class);
    static boolean drawDebuggingRectangles = false;

    static {
        logger.setLevel(Level.WARN);
        //drawDebuggingRectangles = true;
    }

    private int bgcolor = 0;
    private int fgcolor = 0;
    private Brush brush = null;

    public PatBltOrder() {
        super();
        brush = new Brush();
    }

    public int getBackgroundColor() {
        return this.bgcolor;
    }

    public int getForegroundColor() {
        return this.fgcolor;
    }

    public Brush getBrush() {
        return this.brush;
    }

    public void setBackgroundColor(int bgcolor) {
        this.bgcolor = bgcolor;
    }

    public void setForegroundColor(int fgcolor) {
        this.fgcolor = fgcolor;
    }

    public void reset() {
        super.reset();
        bgcolor = 0;
        fgcolor = 0;
        brush.reset();
    }

    public void processOrder(RdesktopCanvas surface, RdpPacket data, int present, boolean delta) {
        if ((present & 0x01) != 0)
            setX(Utilities.setCoordinate(data, getX(), delta));
        if ((present & 0x02) != 0)
            setY(Utilities.setCoordinate(data, getY(), delta));
        if ((present & 0x04) != 0)
            setCX(Utilities.setCoordinate(data, getCX(), delta));
        if ((present & 0x08) != 0)
            setCY(Utilities.setCoordinate(data, getCY(), delta));
        if ((present & 0x10) != 0)
            setOpcode(Utilities.ROP2_P(data.get8()));
        if ((present & 0x20) != 0)
            setBackgroundColor(Utilities.setColor(data));
        if ((present & 0x40) != 0)
            setForegroundColor(Utilities.setColor(data));

        Utilities.parseBrush(data, getBrush(), present >> 7);

        // if(logger.isInfoEnabled()) logger.info("opcode="+patblt.getOpcode());
        this.drawPatBltOrder(surface);
    }

    /**
     * Perform a pattern blit on the screen
     *
     * @param surface Rendering canvas
     */
    public void drawPatBltOrder(RdesktopCanvas surface) {
        Brush brush = this.getBrush();
        int x = this.getX();
        int y = this.getY();

        if (x <= surface.getRight() && y <= surface.getBottom()) {

            int cx = this.getCX();
            int cy = this.getCY();
            int foregroundColor = this.getForegroundColor();
            int backgroundColor = this.getBackgroundColor();
            int opcode = this.getOpcode();

            patBltOrder(surface, opcode, x, y, cx, cy, foregroundColor, backgroundColor, brush);
        }
    }

    /**
     * Draw a pattern to the screen (pattern blit)
     *
     * @param surface         Rendering surface
     * @param opcode          Code defining operation to be performed
     * @param x               x coordinate for left of blit area
     * @param y               y coordinate for top of blit area
     * @param cx              Width of blit area
     * @param cy              Height of blit area
     * @param foregroundColor Foreground color for pattern
     * @param backgroundColor Background color for pattern
     * @param brush           Brush object defining pattern to be drawn
     */
    public void patBltOrder(RdesktopCanvas surface, int opcode, int x, int y, int cx, int cy,
                            int foregroundColor, int backgroundColor, Brush brush) {

        // convert to 24-bit color
        foregroundColor = Bitmap.convertTo24(foregroundColor);
        backgroundColor = Bitmap.convertTo24(backgroundColor);

        // Perform standard clipping checks, x-axis
        int clipRight = x + cx - 1;
        clipRight = Math.min(clipRight, surface.getRight());
        x = Math.max(x, surface.getLeft());
        cx = clipRight - x + 1;

        // Perform standard clipping checks, y-axis
        int clipBottom = y + cy - 1;
        clipBottom = Math.min(clipBottom, surface.getBottom());
        y = Math.max(y, surface.getTop());
        cy = clipBottom - y + 1;

        int i;
        int[] src;
        switch (brush.getStyle()) {
            case 0: // solid
                // make efficient version of rop later with int foregroundColor and boolean
                // usearray set to false for single color
                src = new int[cx * cy];
                for (i = 0; i < src.length; i++) {
                    src[i] = foregroundColor;
                }
                surface.getRasterOp().doArrayOperation(opcode,
                        surface.getBackingStore(),
                        surface.width,
                        x, y, cx, cy, src, cx, 0, 0);

                /* ********* Useful test for identifying image boundaries ************ */
                if (drawDebuggingRectangles) {
                    Graphics g = surface.getBackingStore().getGraphics();
                    Color oldColor = g.getColor();
                    g.setColor(Color.RED);
                    g.drawRect(x, y, cx, cy);
                    g.setColor(oldColor);
                    g.dispose();
                }

                surface.repaint(x, y, cx, cy);

                break;

            case 2: // hatch
                logger.warn("Unsupported brush style hatch");
                break;

            case 3: // pattern
                int brushXOrigin = brush.getXOrigin();
                int brushYOrigin = brush.getYOrigin();
                byte[] brushPattern = brush.getPattern();

                src = new int[cx * cy];
                int psrc = 0;
                for (i = 0; i < cy; i++) {
                    for (int j = 0; j < cx; j++) {
                        if ((brushPattern[(i + brushYOrigin) % 8] & (0x01 << ((j + brushXOrigin) % 8))) == 0) {
                            src[psrc] = foregroundColor;
                        } else {
                            src[psrc] = backgroundColor;
                        }
                        psrc++;
                    }
                }
                surface.getRasterOp().doArrayOperation(opcode,
                        surface.getBackingStore(),
                        surface.width,
                        x, y, cx, cy, src, cx, 0, 0);

                /* ********* Useful test for identifying image boundaries ************ */
                if (drawDebuggingRectangles) {
                    Graphics g = surface.getBackingStore().getGraphics();
                    Color oldColor = g.getColor();
                    g.setColor(Color.RED);
                    g.drawRect(x, y, cx, cy);
                    g.setColor(oldColor);
                    g.dispose();
                }
                
                surface.repaint(x, y, cx, cy);
                break;
            default:
                logger.warn("Unsupported brush style " + brush.getStyle());
        }
    }
}
