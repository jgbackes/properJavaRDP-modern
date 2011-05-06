/* ScreenBltOrder.java
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

import java.awt.*;

public class ScreenBltOrder extends DestBltOrder {

    private int srcx = 0;
    private int srcy = 0;

    public ScreenBltOrder() {
        super();
    }

    public int getSrcX() {
        return this.srcx;
    }

    public int getSrcY() {
        return this.srcy;
    }

    public void setSrcX(int srcx) {
        this.srcx = srcx;
    }

    public void setSrcY(int srcy) {
        this.srcy = srcy;
    }

    public void reset() {
        super.reset();
        srcx = 0;
        srcy = 0;
    }

    /**
     * Parse data describing a screen blit, and perform blit on drawing surface
     *
     * @param data    Packet containing blit data
     * @param present Flags defining the information available within the packet
     * @param delta   True if the coordinates of the blit destination are described as relative to the source
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
            setOpcode(Utilities.ROP2_S(data.get8()));
        if ((present & 0x20) != 0)
            setSrcX(Utilities.setCoordinate(data, getSrcX(), delta));
        if ((present & 0x40) != 0)
            setSrcY(Utilities.setCoordinate(data, getSrcY(), delta));

        this.drawScreenBltOrder(surface);
    }

    /**
     * Perform a screen blit
     *
     * @param surface Rendering canvas
     */
    public void drawScreenBltOrder(RdesktopCanvas surface) {
        int x = this.getX();
        int y = this.getY();

        if (x <= surface.getRight() && y <= surface.getBottom()) {

            int cx = this.getCX();
            int cy = this.getCY();
            int srcX = this.getSrcX();
            int srcY = this.getSrcY();

            int clipRight = x + cx - 1;
            clipRight = Math.min(clipRight, surface.getRight());
            x = Math.max(x, surface.getLeft());
            cx = clipRight - x + 1;

            int clipBottom = y + cy - 1;
            clipBottom = Math.min(clipBottom, surface.getBottom());
            y = Math.max(y, surface.getTop());
            cy = clipBottom - y + 1;

            srcX += x - this.getX();
            srcY += y - this.getY();

            surface.getRasterOp().doArrayOperation(this.getOpcode(),
                    surface.getBackingStore(),
                    surface.getWidth(),
                    x, y, cx, cy,
                    null, surface.getWidth(), srcX, srcY);

            /* ********* Useful test for identifying image boundaries ************ */
            if (drawDebuggingRectangles) {
                Graphics g = surface.getBackingStore().getGraphics();
                Color oldColor = g.getColor();
                g.setColor(Color.GRAY);
                g.drawRect(x, y, cx, cy);
                g.setColor(oldColor);
                g.dispose();
            }
            surface.repaint(x, y, cx, cy);
        }
    }
}
