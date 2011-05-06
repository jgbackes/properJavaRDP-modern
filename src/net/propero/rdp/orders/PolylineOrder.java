/* PolyLineOrder.java
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
import net.propero.rdp.RdpPacket;
import net.propero.rdp.Utilities;

public class PolylineOrder extends LineToOrder {

    private int x = 0;
    private int y = 0;
    private int flags = 0;
    private int fgcolor = 0;
    private int lines = 0;
    private int opcode = 0;
    private int datasize = 0;
    byte[] data = new byte[256];

    public PolylineOrder() {
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getFlags() {
        return this.flags;
    }

    public int getForegroundColor() {
        return this.fgcolor;
    }

    public int getLines() {
        return this.lines;
    }

    public int getDataSize() {
        return this.datasize;
    }

    public byte[] getData() {
        return this.data;
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

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void setForegroundColor(int fgcolor) {
        this.fgcolor = fgcolor;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }

    public void setDataSize(int datasize) {
        this.datasize = datasize;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public void reset() {
        x = 0;
        y = 0;
        flags = 0;
        fgcolor = 0;
        lines = 0;
        datasize = 0;
        opcode = 0;
        data = new byte[256];
    }

    /**
     * Parse data describing a multi-line order, and draw to registered surface
     *
     * @param surface Rendering canvas
     * @param data    Packet containing polyline order
     * @param present Flags defining information available in packet
     * @param delta   True if each set of coordinates is described relative to previous set
     */
    public void processOrder(RdesktopCanvas surface, RdpPacket data,
                             int present, boolean delta) {

        if ((present & 0x01) != 0)
            setX(Utilities.setCoordinate(data, getX(), delta));
        if ((present & 0x02) != 0)
            setY(Utilities.setCoordinate(data, getY(), delta));
        if ((present & 0x04) != 0)
            setOpcode(data.get8());
        if ((present & 0x10) != 0)
            setForegroundColor(Utilities.setColor(data));
        if ((present & 0x20) != 0)
            setLines(data.get8());
        if ((present & 0x40) != 0) {
            int datasize = data.get8();
            setDataSize(datasize);
            byte[] databytes = new byte[datasize];
            for (int i = 0; i < datasize; i++)
                databytes[i] = (byte) data.get8();
            setData(databytes);
        }
        // logger.info("polyline delta="+delta);
        // if(logger.isInfoEnabled()) logger.info("Line from
        // ("+line.getStartX()+","+line.getStartY()+") to
        // ("+line.getEndX()+","+line.getEndY()+")");
        // now draw the line
        this.drawPolyLineOrder(surface);
    }

    /**
     * Draw a multi-point set of lines to the screen
     *
     * @param surface Rendering canvas
     */
    public void drawPolyLineOrder(RdesktopCanvas surface) {
        int x = this.getX();
        int y = this.getY();
        int foregroundColor = this.getForegroundColor();
        int dataSize = this.getDataSize();
        byte[] dataBytes = this.getData();
        int lines = this.getLines();

        // convert to 24-bit color
        foregroundColor = Bitmap.convertTo24(foregroundColor);

        // hack - data as single element byte array so can pass by ref to
        // parseDelta
        // see http://www.rgagnon.com/javadetails/java-0035.html

        int[] data = new int[1];
        data[0] = ((lines - 1) / 4) + 1;
        int flags = 0;
        int index = 0;

        int opcode = this.getOpcode() - 1;

        for (int line = 0; (line < lines) && (data[0] < dataSize); line++) {
            int xFrom = x;
            int yFrom = y;

            if (line % 4 == 0) {
                flags = dataBytes[index++];
            }

            if ((flags & 0xc0) == 0) {
                flags |= 0xc0; /* none = both */
            }

            if ((flags & 0x40) != 0) {
                x += parseDelta(dataBytes, data);
            }

            if ((flags & 0x80) != 0) {
                y += parseDelta(dataBytes, data);
            }
            
            // logger.info("polyline
            // "+line+","+xFrom+","+yFrom+","+x+","+y+","+foregroundColor+","+opcode);

            drawLine(surface, xFrom, yFrom, x, y, foregroundColor, opcode);
            flags <<= 2;
        }
    }

    /**
     * Parse a delta co-ordinate in polyline order form
     *
     * @param buffer Holds the delta in pre parsed format
     * @param offset Offset into the buffer where the polyline order is located
     * @return Parsed value
     */
    public int parseDelta(byte[] buffer, int[] offset) {
        int value = buffer[offset[0]++] & 0xff;
        int two_byte = value & 0x80;

        if ((value & 0x40) != 0) {  /* sign bit */
            value |= ~0x3f;
        } else {
            value &= 0x3f;
        }

        if (two_byte != 0) {
            value = (value << 8) | (buffer[offset[0]++] & 0xff);
        }

        return value;
    }
}
