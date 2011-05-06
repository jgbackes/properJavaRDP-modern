/* TriBltOrder.java
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
import net.propero.rdp.RasterOp;
import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.Utilities;
import net.propero.rdp.cached.CacheManager;
import net.propero.rdp.cached.WrappedImage;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPEGDI] 2.2.2.2.1.1.2.10 Mem3Blt (MEM3BLT_ORDER)
 * <p/>
 * The Mem3Blt Primary Drawing Order is used to render a bitmap stored in the bitmap
 * cache or offscreen bitmap cache to the screen by using a specified brush and three-way raster operation.
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc241588(v=prot.10).aspx">[MS-RDPEGDI]: 2.2.2.2.1.1.2.10 Mem3Blt (MEM3BLT_ORDER)</a>
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc241602(v=PROT.10).aspx">[MS-RDPEGDI]: 2.2.2.2.1.1.2.3 PatBlt (PATBLT_ORDER)</a>
 * @since ProperJavaRDP 3.0
 */
public class Mem3BltOrder extends PatBltOrder {

    protected static Logger logger = Logger.getLogger(Mem3BltOrder.class);

    public static final int ROP2_COPY = 12;
    private static final int ROP2_XOR = 6;
    private static final int ROP2_AND = 8;
    private static final int ROP2_NXOR = 9;
    private static final int ROP2_OR = 14;

    /**
     * Index of the color table entry to use
     */
    private int colorTable = 0;

    /**
     * The cacheId field contains the encoded bitmap cache ID and Color Table Cache entry.
     */
    private int cacheId = 0;

    /**
     * ID of the bitmap cache in which the source bitmap is stored
     */
    private int cacheIndex = 0;

    /**
     * The x-coordinate of the source rectangle within the source bitmap specified by using a Coord Field
     */
    private int nXSrc = 0;

    /**
     * The inverted y-coordinate of the source rectangle within the source bitmap specified using a Coord Field (section 2.2.2.2.1.1.1.1). The actual value of the y-coordinate MUST be computed using the following formula:
     * <code> ActualYSrc = (SourceBitmapHeight - nHeight) - nYSrc</code>
     */
    private int nYSrc = 0;

    public Mem3BltOrder() {
        super();
    }

    public int getColorTable() {
        logger.debug("Mem3BltOrder:getColorTable");
        return this.colorTable;
    }

    public int getCacheID() {
        logger.debug("Mem3BltOrder:getCacheID");
        return this.cacheId;
    }

    public int getCacheIDX() {
        logger.debug("Mem3BltOrder:getCacheIDX");
        return this.cacheIndex;
    }

    public int getSrcX() {
        logger.debug("Mem3BltOrder:getSrcX");
        return this.nXSrc;
    }

    public int getSrcY() {
        logger.debug("Mem3BltOrder:getSrcY");
        return this.nYSrc;
    }

    public void setColorTable(int color_table) {
        logger.debug("Mem3BltOrder:setColorTable");
        this.colorTable = color_table;
    }

    public void setCacheID(int cache_id) {
        logger.debug("Mem3BltOrder:setCacheID");
        this.cacheId = cache_id;
    }

    public void setCacheIDX(int cache_idx) {
        logger.debug("Mem3BltOrder:setCacheIDX");
        this.cacheIndex = cache_idx;
    }

    public void setSrcX(int srcx) {
        logger.debug("Mem3BltOrder:setSrcX");
        this.nXSrc = srcx; // corrected
    }

    public void setSrcY(int srcy) {
        logger.debug("Mem3BltOrder:setSrcY");
        this.nYSrc = srcy; // corrected
    }

    public void reset() {
        super.reset();
        logger.debug("Mem3BltOrder:reset");
        colorTable = 0;
        cacheId = 0;
        cacheIndex = 0;
        nXSrc = 0;
        nYSrc = 0;
    }

    /**
     * Parse data describing a tri blit order, and perform blit on drawing surface
     *
     * @param data    Packet containing tri blit order
     * @param present Flags defining information available in packet
     * @param delta   True if destination coordinates are described as relative to the source
     */
    public void processOrder(RdesktopCanvas surface, RdpPacket data, int present, boolean delta) {

        logger.debug("Mem3BltOrder:processOrder");

        // The cacheId
        if ((present & 0x01) != 0) {
            setCacheID(data.get8());
            setColorTable(data.get8());
        }

        // The left coordinate of the blit rectangle
        if ((present & 0x02) != 0) {
            setX(Utilities.setCoordinate(data, getX(), delta));
        }

        // The top coordinate of the blit rectangle
        if ((present & 0x04) != 0) {
            setY(Utilities.setCoordinate(data, getY(), delta));
        }

        // The width of the blit rectangle
        if ((present & 0x08) != 0) {
            setCX(Utilities.setCoordinate(data, getCX(), delta));
        }

        // The height of the blit rectangle
        if ((present & 0x10) != 0) {
            setCY(Utilities.setCoordinate(data, getCY(), delta));
        }

        // The index of the ternary raster operation to perform
        if ((present & 0x20) != 0) {
            setOpcode(Utilities.ROP2_S(data.get8()));
        }

        // The x-coordinate of the source rectangle within
        // the source bitmap
        if ((present & 0x40) != 0) {
            setSrcX(Utilities.setCoordinate(data, getSrcX(), delta));
        }

        // The inverted y-coordinate of the source rectangle within
        // the source bitmap
        if ((present & 0x80) != 0) {
            setSrcY(Utilities.setCoordinate(data, getSrcY(), delta));
        }

        // Background color
        if ((present & 0x0100) != 0) {
            setBackgroundColor(Utilities.setColor(data));
        }

        // Foreground color
        if ((present & 0x0200) != 0) {
            setForegroundColor(Utilities.setColor(data));
        }

        // Brush data
        Utilities.parseBrush(data, getBrush(), present >> 10);

        // The index of the source bitmap within the bitmap cache specified by the cacheId field.
        if ((present & 0x8000) != 0)
            setCacheIDX(data.getLittleEndian16());

        drawTriBltOrder(surface);
    }

    /**
     * Perform a tri blit on the screen
     *
     * @param surface The RdesktopCanvas we are drawing into
     */
    public void drawTriBltOrder(RdesktopCanvas surface) {
        int x = this.getX();
        int y = this.getY();
        RasterOp rop = surface.getRasterOp();
        WrappedImage backingStore = surface.getBackingStore();
        
        if (x <= surface.getRight() && y <= surface.getBottom()) {

            int cx = this.getCX();
            int cy = this.getCY();
            int srcx = this.getSrcX();
            int srcy = this.getSrcY();
            int fgcolor = this.getForegroundColor();
            int bgcolor = this.getBackgroundColor();
            Brush brush = this.getBrush();

            // convert to 24-bit color
            fgcolor = Bitmap.convertTo24(fgcolor);
            bgcolor = Bitmap.convertTo24(bgcolor);

            // Perform standard clipping checks, x-axis
            int clipright = x + cx - 1;
            if (clipright > surface.getRight())
                clipright = surface.getRight();
            if (x < surface.getLeft())
                x = surface.getLeft();
            cx = clipright - x + 1;

            // Perform standard clipping checks, y-axis
            int clipbottom = y + cy - 1;
            if (clipbottom > surface.getBottom())
                clipbottom = surface.getBottom();
            if (y < surface.getTop())
                y = surface.getTop();
            cy = clipbottom - y + 1;

            try {
                Bitmap bitmap = CacheManager.getInstance().getBitmap(this.getCacheID(), this
                        .getCacheIDX());
                switch (this.getOpcode()) {
                    case 0x69: // PDSxxn
                        rop.doArrayOperation(ROP2_XOR, backingStore, surface.width, x, y, cx, cy,
                                bitmap.getBitmapData(), bitmap.getWidth(), srcx, srcy);
                        patBltOrder(surface, ROP2_NXOR, x, y, cx, cy, fgcolor, bgcolor, brush);
                        break;
                    case 0xb8: // PSDPxax
                        patBltOrder(surface, ROP2_XOR, x, y, cx, cy, fgcolor, bgcolor, brush);
                        rop.doArrayOperation(ROP2_AND, backingStore, surface.width, x, y, cx, cy,
                                bitmap.getBitmapData(), bitmap.getWidth(), srcx, srcy);
                        patBltOrder(surface, ROP2_XOR, x, y, cx, cy, fgcolor, bgcolor, brush);
                        break;
                    case 0xc0: // PSa
                        rop.doArrayOperation(ROP2_COPY, backingStore, surface.width, x, y, cx, cy,
                                bitmap.getBitmapData(), bitmap.getWidth(), srcx, srcy);
                        patBltOrder(surface, ROP2_AND, x, y, cx, cy, fgcolor, bgcolor, brush);
                        break;

                    default:
                        logger.warn("Unimplemented Triblt opcode:"
                                + this.getOpcode());
                        rop.doArrayOperation(ROP2_COPY, backingStore, surface.width, x, y, cx, cy,
                                bitmap.getBitmapData(), bitmap.getWidth(), srcx, srcy);
                }
            } catch (RdesktopException e) {
                logger.warn(e.getMessage());
            }
        }
    }
}
