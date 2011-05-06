/* Text2Order.java
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
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.Utilities;
import net.propero.rdp.cached.CacheManager;
import net.propero.rdp.cached.DataBlob;
import net.propero.rdp.cached.Glyph;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;


// MS-RDPEGDI Section 2.2.2.2.1.1.2.13 GlyphIndex (GLYPHINDEX_ORDER)
public class GlyphIndexOrder implements Order {

    protected static Logger logger = Logger.getLogger(GlyphIndexOrder.class);
    private static boolean drawDebuggingRectangles = false;

    static {
        logger.setLevel(Level.WARN);
        //drawDebuggingRectangles = true;
    }
    
    private static final int MIX_TRANSPARENT = 0;

    private int cacheId = 0;        // The ID of the glyph cache in which the glyph data MUST be stored.
    // This value MUST be in the range 0 to 9 (inclusive).

    private int flAccel = 0;        // Accelerator flags. For glyph related terminology, see [YUAN]
    // figures 14-17 and 15-1. For information about string widths and heights,
    // see [MSDN-SWH]. For information about character widths, see [MSDN-CW].

    private int ulCharInc = 0;      // Specifies whether or not the font is a fixed-pitch (monospace) font.
    // If so, this member is equal to the advance width of the glyphs in pixels
    // (see [YUAN] figures 14-17); if not, this field is set to 0x00.
    // The minimum value for this field is 0x00 (inclusive), and the maximum
    // value is 0xFF (inclusive).

    private int fOpRedundant = 0;   // A Boolean value indicating whether or not the opaque rectangle is redundant.
    // Redundant, in this context, means that the text background is transparent.

    private int bgColor = 0;        // The text color described by using a Generic Color (section 2.2.2.2.1.1.1.8) structure.
    private int fgColor = 0;        // Color of the opaque rectangle described by using a Generic Color (section 2.2.2.2.1.1.1.8) structure.

    private int clipLeft = 0;       // The left coordinate of the text background rectangle.
    private int clipTop = 0;        // The top coordinate of the text background rectangle.
    private int clipRight = 0;      // The right coordinate of the text background rectangle.
    private int clipBottom = 0;     // The bottom coordinate of the text background rectangle.

    private int opLeft = 0;         // The left coordinate of the opaque rectangle.
    private int opTop = 0;          // The top coordinate of the opaque rectangle.
    private int opRight = 0;        // The right coordinate of the opaque rectangle.
    private int opBottom = 0;       // The bottom coordinate of the opaque rectangle.

    private int brushOrgX = 0;      // The x-coordinate of the point where the top leftmost pixel of a brush pattern MUST be anchored.
    private int brushOrgY = 0;      // The y-coordinate of the point where the top leftmost pixel of a brush pattern MUST be anchored.
    private int brushStyle = 0;     // This field MUST be set to BS_SOLID (0x00),
    private int brushHatch = 0;     // This field MUST be set to 0x00
    private byte[] brushExtra = new byte[7]; // This field is not used

    private int x = 0;              // The x-coordinate of the point where the origin of the starting glyph MUST be positioned.
    private int y = 0;              // The y-coordinate of the point where the origin of the starting glyph MUST be positioned.

    private int opcode = 0;

    private int length = 0;
    byte[] text = new byte[256];

    public GlyphIndexOrder() {
    }

    public int getCacheId() {
        return this.cacheId;
    }

    public int getClipLeft() {
        return this.clipLeft;
    }

    public int getClipRight() {
        return this.clipRight;
    }

    public int getClipTop() {
        return this.clipTop;
    }

    public int getClipBottom() {
        return this.clipBottom;
    }

    public int getBoxLeft() {
        return this.opLeft;
    }

    public int getBoxRight() {
        return this.opRight;
    }

    public int getBoxTop() {
        return this.opTop;
    }

    public int getBoxBottom() {
        return this.opBottom;
    }

    public int getX() {
        return this.x;
    }

    public int getUlCharInc() {
        return this.ulCharInc;
    }

    public int getY() {
        return this.y;
    }

    public int getFlAccel() {
        return this.flAccel;
    }

    public int getfOpRedundant() {
        return this.fOpRedundant;
    }

    public int getForegroundColor() {
        return this.fgColor;
    }

    public int getBackgroundColor() {
        return this.bgColor;
    }

    public int getLength() {
        return this.length;
    }

    public byte[] getText() {
        return this.text;
    }

    public void setCacheId(int cacheId) {
        this.cacheId = cacheId;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setUlCharInc(int ulCharInc) {
        this.ulCharInc = ulCharInc;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setfOpRedundant(int fOpRedundant) {
        this.fOpRedundant = fOpRedundant;
    }

    public void setFlAccel(int flAccel) {
        this.flAccel = flAccel;
    }

    public void setForegroundColor(int fgcolor) {
        this.fgColor = fgcolor;
    }

    public void setBackgroundColor(int bgcolor) {
        this.bgColor = bgcolor;
    }

    public void setClipLeft(int clipleft) {
        this.clipLeft = clipleft;
    }

    public void setClipRight(int clipright) {
        this.clipRight = clipright;
    }

    public void setClipTop(int cliptop) {
        this.clipTop = cliptop;
    }

    public void setClipBottom(int clipbottom) {
        this.clipBottom = clipbottom;
    }

    public void setBoxLeft(int boxleft) {
        this.opLeft = boxleft;
    }

    public void setBoxRight(int boxright) {
        this.opRight = boxright;
    }

    public void setBoxTop(int boxtop) {
        this.opTop = boxtop;
    }

    public void setBoxBottom(int boxbottom) {
        this.opBottom = boxbottom;
    }

    public void setText(byte[] text) {
        this.text = text;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void reset() {
        cacheId = 0;
        flAccel = 0;
        fOpRedundant = 0;
        ulCharInc = 0;
        fgColor = 0;
        bgColor = 0;
        clipLeft = 0;
        clipTop = 0;
        clipRight = 0;
        clipBottom = 0;
        opLeft = 0;
        opTop = 0;
        opRight = 0;
        opBottom = 0;
        x = 0;
        y = 0;
        length = 0;
        opcode = 0;
        text = new byte[256];
    }

    public int getOpcode() {
        return opcode;
    }

    public void setOpcode(int name) {
        opcode = name;
    }

    /**
     * Process a glyphIndex order and output to drawing surface
     *
     * @param data    Packet containing glyphIndex order
     * @param present Flags defining information available in packet
     * @param delta   Unused
     * @throws net.propero.rdp.RdesktopException
     *          Problem processing GlyphIndex
     */
    public void processOrder(RdesktopCanvas surface, RdpPacket data,
                             int present, boolean delta) throws RdesktopException {
        if ((present & 0x000001) != 0) {
            setCacheId(data.get8());
        }
        if ((present & 0x000002) != 0) {
            setFlAccel(data.get8());
        }

        if ((present & 0x000004) != 0) {
            setOpcode(data.get8()); // setUlCharInc(data.get8());
        }

        if ((present & 0x000008) != 0) {
            setfOpRedundant(data.get8());
        }

        if ((present & 0x000010) != 0) {
            setForegroundColor(Utilities.setColor(data));
        }

        if ((present & 0x000020) != 0) {
            setBackgroundColor(Utilities.setColor(data));
        }

        if ((present & 0x000040) != 0) {
            setClipLeft(data.getLittleEndian16());
        }

        if ((present & 0x000080) != 0) {
            setClipTop(data.getLittleEndian16());
        }

        if ((present & 0x000100) != 0) {
            setClipRight(data.getLittleEndian16());
        }

        if ((present & 0x000200) != 0) {
            setClipBottom(data.getLittleEndian16());
        }

        if ((present & 0x000400) != 0) {
            setBoxLeft(data.getLittleEndian16());
        }

        if ((present & 0x000800) != 0) {
            setBoxTop(data.getLittleEndian16());
        }

        if ((present & 0x001000) != 0) {
            setBoxRight(data.getLittleEndian16());
        }

        if ((present & 0x002000) != 0) {
            setBoxBottom(data.getLittleEndian16());
        }

        if ((present & 0x004000) != 0) {
            data.incrementPosition(1);
        }

        if ((present & 0x008000) != 0) {
            data.incrementPosition(1);
        }

        if ((present & 0x020000) != 0) {
            data.incrementPosition(4);
        }

        if ((present & 0x040000) != 0) {
            data.incrementPosition(4);
        }

        if ((present & 0x080000) != 0) {
            setX(data.getLittleEndian16());
        }

        if ((present & 0x100000) != 0) {
            setY(data.getLittleEndian16());
        }

        if ((present & 0x200000) != 0) {
            setLength(data.get8());

            byte[] text = new byte[getLength()];
            data.copyToByteArray(text, 0, data.getPosition(), text.length);
            data.incrementPosition(text.length);
            setText(text);

            /*
             * if(logger.isInfoEnabled()) logger.info("X: " + glyphIndex.getX() + "
             * Y: " + glyphIndex.getY() + " Left Clip: " + glyphIndex.getClipLeft() + "
             * Top Clip: " + glyphIndex.getClipTop() + " Right Clip: " +
             * glyphIndex.getClipRight() + " Bottom Clip: " + glyphIndex.getClipBottom() + "
             * Left Box: " + glyphIndex.getBoxLeft() + " Top Box: " +
             * glyphIndex.getBoxTop() + " Right Box: " + glyphIndex.getBoxRight() + "
             * Bottom Box: " + glyphIndex.getBoxBottom() + " Foreground Color: " +
             * glyphIndex.getForegroundColor() + " Background Color: " +
             * glyphIndex.getBackgroundColor() + " Font: " + glyphIndex.getCacheId() + "
             * Flags: " + glyphIndex.getFlAccel() + " Mixmode: " + glyphIndex.getfOpRedundant() + "
             * Unknown: " + glyphIndex.getUlCharInc() + " Length: " +
             * glyphIndex.getLength());
             */
        }

        drawText(surface, getClipRight() - getClipLeft(), getClipBottom()
                - getClipTop(), getBoxRight() - getBoxLeft(),
                getBoxBottom() - getBoxTop());

    }


    /**
     * Draw a glyphIndex order to the drawing surface
     *
     * @param surface Drawing surface
     * @param clipCX  Width of clipping area
     * @param clipCY  Height of clipping area
     * @param boxCY   Width of bounding box (to draw if > 1)
     * @param boxCX   Height of bounding box (to draw if boxCY > 1)
     * @throws net.propero.rdp.RdesktopException
     *          Error drawing
     */
    private void drawText(RdesktopCanvas surface,
                          int clipCX, int clipCY, int boxCY, int boxCX)
            throws RdesktopException {
        byte[] text = getText();
        DataBlob entry;
        Glyph glyph;
        int offset;
        int ptext = 0;
        int length = getLength();
        int x = getX();
        int y = getY();

        if (boxCY > 1) {
            surface.fillRectangle(getBoxLeft(), getBoxTop(), boxCY, boxCX, getBackgroundColor());
        } else if (getfOpRedundant() == RdesktopCanvas.MIX_OPAQUE) {
            surface.fillRectangle(getClipLeft(), getClipTop(), clipCX, clipCY, getBackgroundColor());
        }

        /*
        * logger.debug("X: " + glyphIndex.getX() + " Y: " + glyphIndex.getY() + " Left Clip: " +
        * glyphIndex.getClipLeft() + " Top Clip: " + glyphIndex.getClipTop() + " Right Clip: " +
        * glyphIndex.getClipRight() + " Bottom Clip: " + glyphIndex.getClipBottom() + " Left
        * Box: " + glyphIndex.getBoxLeft() + " Top Box: " + glyphIndex.getBoxTop() + " Right
        * Box: " + glyphIndex.getBoxRight() + " Bottom Box: " + glyphIndex.getBoxBottom() + "
        * Foreground Color: " + glyphIndex.getForegroundColor() + " Background Color: " +
        * glyphIndex.getBackgroundColor() + " Font: " + glyphIndex.getCacheId() + " Flags: " +
        * glyphIndex.getFlAccel() + " Mixmode: " + glyphIndex.getfOpRedundant() + " Unknown: " +
        * glyphIndex.getUlCharInc() + " Length: " + glyphIndex.getLength());
        */
        for (int i = 0; i < length;) {
            switch (text[ptext + i] & 0x000000ff) {
                case (0xff):
                    if (i + 2 < length) {
                        byte[] data = new byte[text[ptext + i + 2] & 0x000000ff];
                        System.arraycopy(text, ptext, data, 0, text[ptext + i + 2] & 0x000000ff);
                        DataBlob db = new DataBlob(text[ptext + i + 2] & 0x000000ff, data);
                        CacheManager.getInstance().putText(text[ptext + i + 1] & 0x000000ff, db);
                    } else {
                        throw new RdesktopException();
                    }
                    length -= i + 3;
                    ptext = i + 3;
                    i = 0;
                    break;

                case (0xfe):
                    entry = CacheManager.getInstance().getText(text[ptext + i + 1] & 0x000000ff);
                    if (entry != null) {
                        if ((entry.getData()[1] == 0) && ((getFlAccel() & RdesktopCanvas.TEXT2_IMPLICIT_X) == 0)) {
                            if ((getFlAccel() & 0x04) != 0) {
                                y += text[ptext + i + 2] & 0x000000ff;
                            } else {
                                x += text[ptext + i + 2] & 0x000000ff;
                            }
                        }
                    }
                    if (i + 2 < length) {
                        i += 3;
                    } else {
                        i += 2;
                    }
                    length -= i;
                    ptext = i;
                    i = 0;
                    // break;

                    byte[] data = entry.getData();
                    for (int j = 0; j < entry.getSize(); j++) {
                        glyph = CacheManager.getInstance().getFont(getCacheId(), data[j] & 0x000000ff);
                        if ((getFlAccel() & RdesktopCanvas.TEXT2_IMPLICIT_X) == 0) {
                            offset = data[++j] & 0x000000ff;
                            if ((offset & 0x80) != 0) {
                                int var = Utilities.twosComplement16((data[j + 1] & 0xff) | ((data[j + 2] & 0xff) << 8));
                                j += 2;
                                if ((getFlAccel() & RdesktopCanvas.TEXT2_VERTICAL) != 0) {
                                    y += var;
                                } else {
                                    x += var;
                                }
                            } else {
                                if ((getFlAccel() & RdesktopCanvas.TEXT2_VERTICAL) != 0) {
                                    y += offset;
                                } else {
                                    x += offset;
                                }
                            }
                        }
                        if (glyph != null) {
                            //if((glyphIndex.getFlAccel() & TEXT2_VERTICAL) != 0) logger.info("Drawing glyph: (" + (x + (short)glyph.getOffset()) + ", " + (y + (short)glyph.getBaseLine()) + ")"  );
                            drawGlyph(surface, getfOpRedundant(), x + (short) glyph.getOffset(),
                                    y + (short) glyph.getBaseLine(), glyph.getWidth(),
                                    glyph.getHeight(), glyph.getFontData(),
                                    getBackgroundColor(), getForegroundColor());

                            if ((getFlAccel() & RdesktopCanvas.TEXT2_IMPLICIT_X) != 0) {
                                x += glyph.getWidth();
                            }
                        }
                    }
                    break;

                default:
                    glyph = CacheManager.getInstance().getFont(getCacheId(), text[ptext + i] & 0x000000ff);
                    if ((getFlAccel() & RdesktopCanvas.TEXT2_IMPLICIT_X) == 0) {
                        offset = text[ptext + (++i)] & 0x000000ff;
                        if ((offset & 0x80) != 0) {
                            int var = Utilities.twosComplement16((text[ptext + i + 1] & 0x000000ff) | ((text[ptext + i + 2] & 0x000000ff) << 8));
                            i += 2;
                            if ((getFlAccel() & RdesktopCanvas.TEXT2_VERTICAL) != 0) {
                                logger.info("y +=" + (text[ptext + (i + 1)] & 0x000000ff) + " | " + ((text[ptext + (i + 2)] & 0x000000ff) << 8));
                                y += var;
                            } else {
                                x += var;
                            }
                        } else {
                            if ((getFlAccel() & RdesktopCanvas.TEXT2_VERTICAL) != 0) {
                                y += offset;
                            } else {
                                x += offset;
                            }
                        }
                    }
                    if (glyph != null) {
                        drawGlyph(surface, getfOpRedundant(), x + (short) glyph.getOffset(),
                                y + (short) glyph.getBaseLine(), glyph.getWidth(),
                                glyph.getHeight(), glyph.getFontData(),
                                getBackgroundColor(), getForegroundColor());

                        if ((getFlAccel() & RdesktopCanvas.TEXT2_IMPLICIT_X) != 0)
                            x += glyph.getWidth();
                    }
                    i++;
                    break;
            }
        }
    }

    /**
     * Draw a single glyph to the screen
     *
     * @param mixMode 0 for transparent background, specified color for background otherwide
     * @param x       x coordinate on screen at which to draw glyph
     * @param y       y coordinate on screen at which to draw glyph
     * @param cx      Width of clipping area for glyph
     * @param cy      Height of clipping area for glyph
     * @param data    Set of values defining glyph's pattern
     * @param bgColor Background color for glyph pattern
     * @param fgColor Foreground color for glyph pattern
     */
    public void drawGlyph(RdesktopCanvas surface, int mixMode, int x, int y, int cx, int cy,
                          byte[] data, int bgColor, int fgColor) {

        int pData;
        int index = 0x80;

        int bytes_per_row = (cx - 1) / 8 + 1;
        int newX;
        int newY;
        int newCX;
        int newCY;

        int Bpp = Options.getBpp();

        // convert to 24-bit color
        fgColor = Bitmap.convertTo24(fgColor);
        bgColor = Bitmap.convertTo24(bgColor);

        // correction for 24-bit color
        if (Bpp == 3) {
            fgColor = ((fgColor & 0xFF) << 16) | (fgColor & 0xFF00) | ((fgColor & 0xFF0000) >> 16);
            bgColor = ((bgColor & 0xFF) << 16) | (bgColor & 0xFF00) | ((bgColor & 0xFF0000) >> 16);
        }

        // clip here instead

        if (x > surface.getRight() || y > surface.getBottom()) {
            return; // off screen
        }

        int clipright = x + cx - 1;
        if (clipright > surface.getRight()) {
            clipright = surface.getRight();
        }
        if (x < surface.getLeft()) {
            newX = surface.getLeft();
        } else {
            newX = x;
        }
        newCX = clipright - x + 1; // not clipRight - newX - 1

        int clipbottom = y + cy - 1;
        if (clipbottom > surface.getBottom()) {
            clipbottom = surface.getBottom();
        }
        if (y < surface.getTop()) {
            newY = surface.getTop();
        } else {
            newY = y;
        }

        newCY = clipbottom - newY + 1;

        // int pbackstore = (newY * this.width) + x;
        pData = bytes_per_row * (newY - y); // offset y, but not x

        if (mixMode == MIX_TRANSPARENT) { // FillStippled
            for (int i = 0; i < newCY; i++) {
                for (int j = 0; j < newCX; j++) {
                    if (index == 0) { // next row
                        pData++;
                        index = 0x80;
                    }

                    if ((data[pData] & index) != 0) {
                        if ((x + j >= newX) && (newX + j > 0) && (newY + i > 0))
                            // since haven't offset x
                            surface.getBackingStore().setRGB(newX + j, newY + i, fgColor);
                    }
                    index >>= 1;
                }
                pData++;
                index = 0x80;
                // pbackstore += this.width;
                if (pData == data.length) {
                    pData = 0;
                }
            }
        } else { // FillOpaqueStippled
            for (int i = 0; i < newCY; i++) {
                for (int j = 0; j < newCX; j++) {
                    if (index == 0) { // next row
                        pData++;
                        index = 0x80;
                    }

                    if (x + j >= newX) {
                        if ((x + j > 0) && (y + i > 0)) {
                            if ((data[pData] & index) != 0)
                                surface.getBackingStore().setRGB(x + j, y + i, fgColor);
                            else
                                surface.getBackingStore().setRGB(x + j, y + i, bgColor);
                        }
                    }
                    index >>= 1;
                }
                pData++;
                index = 0x80;
                // pbackstore += this.width;
                if (pData == data.length) {
                    pData = 0;
                }
            }
        }

        /* ********* Useful test for identifying image boundaries ************ */
        if (drawDebuggingRectangles) {
            Graphics g = surface.getBackingStore().getGraphics();
            g.setColor(Color.ORANGE);
            g.drawRect(x, y, cx, cy);
            g.dispose();
        }
        surface.repaint(newX, newY, newCX, newCY);
    }
}
