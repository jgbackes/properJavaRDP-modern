/* DestBltOrder.java
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

import java.awt.*;

/**
 * [MS-RDPEGDI] Section 2.2.2.2.1.1.2.1 DstBlt (DSTBLT_ORDER)
 * <p/>
 * The DstBlt Primary Drawing Order is used to paint a rectangle by using
 * a destination-only raster operation.
 */
public class DestBltOrder implements Order {

    static boolean drawDebuggingRectangles = false;

    static {
        //drawDebuggingRectangles = true;
    }

    private int x = 0;
    private int y = 0;
    private int cx = 0;
    private int cy = 0;
    private int opcode = 0;

    public DestBltOrder() {
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

    public int getOpcode() {
        return this.opcode;
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

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public void reset() {
        x = 0;
        y = 0;
        cx = 0;
        cy = 0;
        opcode = 0;
    }

    public void processOrder(RdesktopCanvas surface, RdpPacket data, int present, boolean delta) {
        if ((present & 0x01) != 0) {
            setX(net.propero.rdp.Utilities.setCoordinate(data, getX(), delta));
        }
        if ((present & 0x02) != 0) {
            setY(net.propero.rdp.Utilities.setCoordinate(data, getY(), delta));
        }
        if ((present & 0x04) != 0) {
            setCX(net.propero.rdp.Utilities.setCoordinate(data, getCX(), delta));
        }
        if ((present & 0x08) != 0) {
            setCY(net.propero.rdp.Utilities.setCoordinate(data, getCY(), delta));
        }
        if ((present & 0x10) != 0) {
            setOpcode(net.propero.rdp.Utilities.ROP2_S(data.get8()));
        }

        if (x <= surface.getRight() && y <= surface.getBottom()) {
            int cx = this.getCX();
            int cy = this.getCY();

            int clipRight = x + cx - 1;
            if (clipRight > surface.getRight())
                clipRight = surface.getRight();
            if (x < surface.getLeft())
                x = surface.getLeft();
            cx = clipRight - x + 1;

            int clipBottom = y + cy - 1;
            if (clipBottom > surface.getBottom())
                clipBottom = surface.getBottom();
            if (y < surface.getTop())
                y = surface.getTop();
            cy = clipBottom - y + 1;

            surface.getRasterOp().doArrayOperation(this.getOpcode(),
                    surface.getBackingStore(), surface.width, x, y, cx, cy, null, 0, 0, 0);

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
        }
    }
}
