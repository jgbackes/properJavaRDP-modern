/* LineOrder.java
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
import net.propero.rdp.Options;
import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.Utilities;

public class LineToOrder implements Order {

    private int mixmode = 0;
    private int startx = 0;
    private int starty = 0;
    private int endx = 0;
    private int endy = 0;
    private int bgcolor = 0;
    private int opcode = 0;
    Pen pen = null;

    public LineToOrder() {
        pen = new Pen();
    }

    public int getMixmode() {
        return this.mixmode;
    }

    public int getStartX() {
        return this.startx;
    }

    public int getStartY() {
        return this.starty;
    }

    public int getEndX() {
        return this.endx;
    }

    public int getEndY() {
        return this.endy;
    }

    public int getBackgroundColor() {
        return this.bgcolor;
    }

    public int getOpcode() {
        return this.opcode;
    }

    public Pen getPen() {
        return this.pen;
    }

    public void setMixmode(int mixmode) {
        this.mixmode = mixmode;
    }

    public void setStartX(int startx) {
        this.startx = startx;
    }

    public void setStartY(int starty) {
        this.starty = starty;
    }

    public void setEndX(int endx) {
        this.endx = endx;
    }

    public void setEndY(int endy) {
        this.endy = endy;
    }

    public void setBackgroundColor(int bgColor) {
        this.bgcolor = bgColor;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public void reset() {
        mixmode = 0;
        startx = 0;
        starty = 0;
        endx = 0;
        endy = 0;
        bgcolor = 0;
        opcode = 0;
        pen.reset();
    }

    /**
     * Parse data describing a rectangle order, and draw the rectangle to the drawing surface
     *
     * @param data    Packet containing rectangle order
     * @param present Flags defining information available in packet
     * @param delta   True if the rectangle is described as (x,y,width,height), as opposed to (x1,y1,x2,y2)
     */
    public void processOrder(RdesktopCanvas surface, RdpPacket data,
                             int present, boolean delta) {
        if ((present & 0x01) != 0)
            setMixmode(data.getLittleEndian16());
        if ((present & 0x02) != 0)
            setStartX(Utilities.setCoordinate(data, getStartX(), delta));
        if ((present & 0x04) != 0)
            setStartY(Utilities.setCoordinate(data, getStartY(), delta));
        if ((present & 0x08) != 0)
            setEndX(Utilities.setCoordinate(data, getEndX(), delta));
        if ((present & 0x10) != 0)
            setEndY(Utilities.setCoordinate(data, getEndY(), delta));
        if ((present & 0x20) != 0)
            setBackgroundColor(Utilities.setColor(data));
        if ((present & 0x40) != 0)
            setOpcode(data.get8());

        Utilities.parsePen(data, this.getPen(), present >> 7);

        // if(logger.isInfoEnabled()) logger.info("Line from
        // ("+lineTo.getStartX()+","+lineTo.getStartY()+") to
        // ("+lineTo.getEndX()+","+lineTo.getEndY()+")");


        this.drawLine(surface, startx, starty, endx, endy, pen.getColor(), opcode - 1);
    }

    /**
     * Draw a line to the screen
     *
     * @param x1     x coordinate of start point of line
     * @param y1     y coordinate of start point of line
     * @param x2     x coordinate of end point of line
     * @param y2     y coordinate of end point of line
     * @param color  color of line
     * @param opcode Operation code defining operation to perform on pixels within
     *               the line
     */
    public void drawLine(RdesktopCanvas surface, int x1, int y1, int x2, int y2, int color, int opcode) {
        // convert to 24-bit color
        color = Bitmap.convertTo24(color);

        if (x1 == x2 || y1 == y2) {
            drawLineVerticalHorizontal(surface, x1, y1, x2, y2, color, opcode);
            return;
        }

        int deltaX = Math.abs(x2 - x1); // The difference between the x's
        int deltaY = Math.abs(y2 - y1); // The difference between the y's
        int x = x1; // Start x off at the first pixel
        int y = y1; // Start y off at the first pixel
        int xIncrement1;
        int xIncrement2;
        int yIncrement1;
        int yIncrement2;
        int numerator;
        int denominator;
        int numeratorAdd;
        int pixelCount;

        if (x2 >= x1) { // The x-values are increasing
            xIncrement1 = 1;
            xIncrement2 = 1;
        } else { // The x-values are decreasing
            xIncrement1 = -1;
            xIncrement2 = -1;
        }

        if (y2 >= y1) { // The y-values are increasing
            yIncrement1 = 1;
            yIncrement2 = 1;
        } else { // The y-values are decreasing
            yIncrement1 = -1;
            yIncrement2 = -1;
        }

        if (deltaX >= deltaY) { // There is at least one x-value for every y-value
            xIncrement1 = 0; // Don't change the x when numerator >= denominator
            yIncrement2 = 0; // Don't change the y for every iteration
            denominator = deltaX;
            numerator = deltaX / 2;
            numeratorAdd = deltaY;
            pixelCount = deltaX; // There are more x-values than y-values
        } else { // There is at least one y-value for every x-value
            xIncrement2 = 0; // Don't change the x for every iteration
            yIncrement1 = 0; // Don't change the y when numerator >= denominator
            denominator = deltaY;
            numerator = deltaY / 2;
            numeratorAdd = deltaX;
            pixelCount = deltaY; // There are more y-values than x-values
        }

        for (int currentPixel = 0; currentPixel <= pixelCount; currentPixel++) {
            setPixel(surface, opcode, x, y, color); // Draw the current pixel
            numerator += numeratorAdd; // Increase the numerator by the top of the fraction
            if (numerator >= denominator) { // Check if numerator >= denominator
                numerator -= denominator; // Calculate the new numerator value
                x += xIncrement1; // Change the x as appropriate
                y += yIncrement1; // Change the y as appropriate
            }
            x += xIncrement2; // Change the x as appropriate
            y += yIncrement2; // Change the y as appropriate
        }

        int xMin = x1 < x2 ? x1 : x2;
        int xMax = x1 > x2 ? x1 : x2;
        int yMin = y1 < y2 ? y1 : y2;
        int yMax = y1 > y2 ? y1 : y2;

        surface.repaint(xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);
    }

    /**
     * Helper function for drawLine, draws a horizontal or vertical line using a
     * much faster method than used for diagonal lines
     *
     * @param x1     x coordinate of start point of line
     * @param y1     y coordinate of start point of line
     * @param x2     x coordinate of end point of line
     * @param y2     y coordinate of end point of line
     * @param color  color of line
     * @param opcode Operation code defining operation to perform on pixels within
     *               the line
     */
    public void drawLineVerticalHorizontal(RdesktopCanvas surface, int x1, int y1, int x2, int y2,
                                           int color, int opcode) {
        int i;
        // only vertical or horizontal lines
        if (y1 == y2) {
            if (y1 >= surface.getTop() && y1 <= surface.getBottom()) { // visible
                if (x2 > x1) {
                    x1 = Math.max(x1, surface.getLeft());
                    x2 = Math.min(x2, surface.getRight());

                    for (i = 0; i < x2 - x1; i++) {
                        surface.getRasterOp().doPixelOperation(opcode, surface.getBackingStore(), x1 + i, y1, color);
                    }
                    surface.repaint(x1, y1, x2 - x1 + 1, 1);
                } else {
                    x2 = Math.max(x2, surface.getLeft());
                    x1 = Math.min(x1, surface.getRight());
                    for (i = 0; i < x1 - x2; i++) {
                        surface.getRasterOp().doPixelOperation(opcode, surface.getBackingStore(), x2 + i, y1, color);
                    }
                    surface.repaint(x2, y1, x1 - x2 + 1, 1);
                }
            }
        } else {
            if (x1 >= surface.getLeft() && x1 <= surface.getRight()) { // visible
                if (y2 > y1) {
                    y1 = Math.max(y1, surface.getTop());
                    y2 = Math.min(y2, surface.getBottom());
                    for (i = 0; i < y2 - y1; i++) {
                        surface.getRasterOp().doPixelOperation(opcode, surface.getBackingStore(), x1, y1 + i, color);
                    }
                    surface.repaint(x1, y1, 1, y2 - y1 + 1);
                } else {
                    y2 = Math.max(y2, surface.getTop());
                    y1 = Math.min(y1, surface.getBottom());
                    for (i = 0; i < y1 - y2; i++) {
                        surface.getRasterOp().doPixelOperation(opcode, surface.getBackingStore(), x1, y2 + i, color);
                    }
                    surface.repaint(x1, y2, 1, y1 - y2 + 1);
                }
            }
        }
    }

    /**
     * Perform an operation on a pixel in the backingStore
     *
     * @param surface Rendering canvas
     * @param opcode ID of operation to perform
     * @param x      x coordinate of pixel
     * @param y      y coordinate of pixel
     * @param color  Color value to be used in operation
     */
    public void setPixel(RdesktopCanvas surface, int opcode, int x, int y, int color) {
        int Bpp = Options.getBpp();

        // correction for 24-bit color
        if (Bpp == 3)
            color = ((color & 0xFF) << 16) | (color & 0xFF00)
                    | ((color & 0xFF0000) >> 16);

        if ((x < surface.getLeft()) || (x > surface.getRight()) || (y < surface.getTop())
                || (y > surface.getBottom())) { // Clip
        } else {
            surface.getRasterOp().doPixelOperation(opcode, surface.getBackingStore(), x, y, color);
        }
    }
}
