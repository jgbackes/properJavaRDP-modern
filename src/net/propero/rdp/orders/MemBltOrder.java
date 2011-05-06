/* MemBltOrder.java
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
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.Utilities;
import net.propero.rdp.cached.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;

public class MemBltOrder extends ScreenBltOrder {

    protected static Logger logger = Logger.getLogger(MemBltOrder.class);
    private static boolean drawDebuggingRectangles = false;

    static {
        logger.setLevel(Level.WARN);
        //drawDebuggingRectangles = true;
    }

    private int color_table = 0;
    private int cache_id = 0;
    private int cache_idx = 0;

    public MemBltOrder() {
        super();
    }

    public int getColorTable() {
        return this.color_table;
    }

    public int getCacheID() {
        return this.cache_id;
    }

    public int getCacheIDX() {
        return this.cache_idx;
    }

    public void setColorTable(int color_table) {
        this.color_table = color_table;
    }

    public void setCacheID(int cache_id) {
        this.cache_id = cache_id;
    }

    public void setCacheIDX(int cache_idx) {
        this.cache_idx = cache_idx;
    }

    public void reset() {
        super.reset();
        color_table = 0;
        cache_id = 0;
        cache_idx = 0;
    }

    /**
     * Process data describing a memory blit, and perform blit on drawing surface
     *
     * @param data    Packet containing mem blit order
     * @param present Flags defining information available in packet
     * @param delta   True if destination coordinates are described as relative to the source
     */
    public void processOrder(RdesktopCanvas surface, RdpPacket data,
                             int present, boolean delta) {
        if ((present & 0x01) != 0) {
            setCacheID(data.get8());
            setColorTable(data.get8());
        }
        if ((present & 0x02) != 0)
            setX(Utilities.setCoordinate(data, getX(), delta));
        if ((present & 0x04) != 0)
            setY(Utilities.setCoordinate(data, getY(), delta));
        if ((present & 0x08) != 0)
            setCX(Utilities.setCoordinate(data, getCX(), delta));
        if ((present & 0x10) != 0)
            setCY(Utilities.setCoordinate(data, getCY(), delta));
        if ((present & 0x20) != 0)
            setOpcode(Utilities.ROP2_S(data.get8()));
        if ((present & 0x40) != 0)
            setSrcX(Utilities.setCoordinate(data, getSrcX(), delta));
        if ((present & 0x80) != 0)
            setSrcY(Utilities.setCoordinate(data, getSrcY(), delta));
        if ((present & 0x0100) != 0)
            setCacheIDX(data.getLittleEndian16());

        if (logger.isInfoEnabled()) {
            logger.info("MemBltOrder:processOrder");
        }
        drawMemBltOrder(surface);
    }

    /**
     * Perform a memory blit
     *
     * @param surface The RdesktopCanvas we are drawing into
     */
    public void drawMemBltOrder(RdesktopCanvas surface) {
        int x = this.getX();
        int y = this.getY();

        if (x <= surface.getRight() && y <= surface.getBottom()) {

            int cx = this.getCX();
            int cy = this.getCY();
            int srcX = this.getSrcX();
            int srcY = this.getSrcY();

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

            srcX += x - this.getX();
            srcY += y - this.getY();

            if (logger.isInfoEnabled())
                logger.info("MEMBLT x=" + x + " y=" + y + " cx=" + cx + " cy=" + cy
                        + " srcX=" + srcX + " srcY=" + srcY + " opcode="
                        + this.getOpcode());
            try {
                Bitmap bitmap = CacheManager.getInstance().getBitmap(this.getCacheID(), this.getCacheIDX());

                // should use the colorMap, but requires high color backingStore...
                surface.getRasterOp().doArrayOperation(this.getOpcode(),
                        surface.getBackingStore(),
                        surface.width, x, y, cx, cy,
                        bitmap.getBitmapData(),
                        bitmap.getWidth(), srcX, srcY);

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
            } catch (RdesktopException ignore) {
            }
        }
    }
}
