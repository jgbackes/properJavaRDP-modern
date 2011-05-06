/* RectangleOrder.java
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

import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.Utilities;

public class RectangleOrder implements Order {

    private int rect_color;

    private int x = 0;
    private int y = 0;
    private int cx = 0;
    private int cy = 0;
    public int color = 0;

    public RectangleOrder() {
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getCX() {
        return this.cx;
    }

    public int getCY() {
        return this.cy;
    }

    public int getColor() {
        return this.color;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setCX(int cx) {
        this.cx = cx;
    }

    public void setCY(int cy) {
        this.cy = cy;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void reset() {
        x = 0;
        y = 0;
        cx = 0;
        cy = 0;
        color = 0;
    }

    /**
     * Parse data describing a rectangle order, and draw the rectangle to the drawing surface
     *
     * @param surface Rendering canvas
     * @param data    Packet containing rectangle order
     * @param present Flags defining information available in packet
     * @param delta   True if the rectangle is described as (x,y,width,height), as opposed to (x1,y1,x2,y2)
     */
    public void processOrder(RdesktopCanvas surface, RdpPacket data,
                             int present, boolean delta) {

        if ((present & 0x01) != 0)
            setX(Utilities.setCoordinate(data, getX(), delta));
        if ((present & 0x02) != 0)
            setY(Utilities.setCoordinate(data, getY(), delta));
        if ((present & 0x04) != 0)
            setCX(Utilities.setCoordinate(data, getCX(), delta));
        if ((present & 0x08) != 0)
            setCY(Utilities.setCoordinate(data, getCY(), delta));
        if ((present & 0x10) != 0)
            rect_color = (rect_color & 0xffffff00) | data.get8(); // rect.setColor(setColor(data));
        if ((present & 0x20) != 0)
            rect_color = (rect_color & 0xffff00ff)
                    | (data.get8() << 8); // rect.setColor(setColor(data));
        if ((present & 0x40) != 0)
            rect_color = (rect_color & 0xff00ffff)
                    | (data.get8() << 16);

        setColor(rect_color);

        surface.fillRectangle(x, y,cx, cy, color);
    }
}
	
