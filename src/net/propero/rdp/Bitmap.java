/* Bitmap.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Provide a class for storage of Bitmap images, along with
 *          static methods for decompression and conversion of bitmaps.
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
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

public class Bitmap {

    protected static Logger logger = Logger.getLogger(Rdp.class);

    private final static int FILL = 0;
    private final static int MIX = 1;
    private final static int FILL_OR_MIX = 2;
    private final static int COLOR = 3;
    private final static int COPY = 4;
    private final static int SET_MIX_MIX = 6;
    private final static int SET_MIX_FILL_OR_MIX = 7;
    private final static int BI_COLOR = 8;
    private final static int FILL_OR_MIX_1 = 9;
    private final static int FILL_OR_MIX_2 = 10;
    private final static int WHITE = 13;
    private final static int BLACK = 14;


    public int usage;

    private int[] highData = null;

    private int width = 0;
    private int height = 0;
    private int x = 0;
    private int y = 0;

    /**
     * Convert the color passed in to 24 bit color space
     *
     * @param color Color that needs converting
     * @return color after being converted
     */
    public static int convertTo24(int color) {
        if (Options.getServerBpp() == 15) {
            return convert15to24(color);
        }
        if (Options.getServerBpp() == 16) {
            return convert16to24(color);
        }
        return color;
    }

    /**
     * @param color16 Color in 15 bit color space
     * @return Color in 24 bit space
     */
    public static int convert15to24(int color16) {
        int r24 = (color16 >> 7) & 0xF8;
        int g24 = (color16 >> 2) & 0xF8;
        int b24 = (color16 << 3) & 0xFF;

        r24 |= r24 >> 5;
        g24 |= g24 >> 5;
        b24 |= b24 >> 5;

        return (r24 << 16) | (g24 << 8) | b24;
    }

    /**
     * @param color16 Color in 16 bit color space
     * @return Color in 24 bit space
     */
    public static int convert16to24(int color16) {
        int r24 = (color16 >> 8) & 0xF8;
        int g24 = (color16 >> 3) & 0xFC;
        int b24 = (color16 << 3) & 0xFF;

        r24 |= r24 >> 5;
        g24 |= g24 >> 6;
        b24 |= b24 >> 5;

        return (r24 << 16) | (g24 << 8) | b24;
    }

    /**
     * Read integer of a specified byte-length from byte array
     *
     * @param data   Array to read from
     * @param offset Offset in array to read from
     * @param Bpp    Number of bytes to read
     * @return value
     */
    static int cvalx(byte[] data, int offset, int Bpp) {
        int rv = 0;
        if (Options.getServerBpp() == 15) {
            int lower = data[offset] & 0xFF;
            int full = (data[offset + 1] & 0xFF) << 8 | lower;

            int r24 = (full >> 7) & 0xF8;
            r24 |= r24 >> 5;
            int g24 = (full >> 2) & 0xF8;
            g24 |= g24 >> 5;
            int b24 = (lower << 3) & 0xFF;
            b24 |= b24 >> 5;

            return (r24 << 16) | (g24 << 8) | b24;

        } else if (Options.getServerBpp() == 16) {
            int lower = data[offset] & 0xFF;
            int full = (data[offset + 1] & 0xFF) << 8 | lower;

            int r24 = (full >> 8) & 0xF8;
            r24 |= r24 >> 5;
            int g24 = (full >> 3) & 0xFC;
            g24 |= g24 >> 6;
            int b24 = (lower << 3) & 0xFF;
            b24 |= b24 >> 5;

            return (r24 << 16) | (g24 << 8) | b24;

        } else {
            for (int i = (Bpp - 1); i >= 0; i--) {
                rv = rv << 8;
                rv |= data[offset + i] & 0xFF;
            }
        }

        return rv;
    }

    /**
     * @param input       where the line data is stored
     * @param startOffset offset into line data
     * @param offset      offset
     * @param Bpp         pixel depth
     * @return value
     */
    static int getli(byte[] input, int startOffset, int offset, int Bpp) {
        int rv = 0;

        int location = startOffset + (offset * Bpp);
        for (int i = 0; i < Bpp; i++) {
            rv = rv << 8;
            rv |= (input[location + (Bpp - i - 1)]) & 0xFF;
        }
        return rv;
    }

    /**
     * @param input         Byte array of line to set
     * @param startLocation Starting location
     * @param offset        byte offset
     * @param value         value to set
     * @param Bpp           pixel depth
     */
    static void setli(byte[] input, int startLocation, int offset, int value, int Bpp) {
        int location = startLocation + offset * Bpp;

        input[location] = (byte) (value & 0xFF);
        if (Bpp > 1) {
            input[location + 1] = (byte) ((value & 0xFF00) >> 8);
        }
        if (Bpp > 2) {
            input[location + 2] = (byte) ((value & 0xFF0000) >> 16);
        }
    }

    /**
     * Convert byte array representing a bitmap into integer array of pixels
     *
     * @param bitmap Byte array of bitmap data
     * @param Bpp    Bytes-per-pixel for bitmap
     * @return Integer array of pixel data representing input image data
     */
    public static int[] convertImage(byte[] bitmap, int Bpp) {
        int[] out = new int[bitmap.length / Bpp];

        for (int i = 0; i < out.length; i++) {
            if (Bpp == 1) {
                out[i] = bitmap[i] & 0xFF;
            } else if (Bpp == 2) {
                out[i] = ((bitmap[i * Bpp + 1] & 0xFF) << 8)
                        | (bitmap[i * Bpp] & 0xFF);
            } else if (Bpp == 3) {
                out[i] = ((bitmap[i * Bpp + 2] & 0xFF) << 16)
                        | ((bitmap[i * Bpp + 1] & 0xFF) << 8)
                        | (bitmap[i * Bpp] & 0xFF);
            }
            out[i] = Bitmap.convertTo24(out[i]);
        }
        return out;
    }

    /**
     * Constructor for Bitmap based on integer pixel values
     *
     * @param data   Array of pixel data, one integer per pixel. Should have a length of width*height.
     * @param width  Width of bitmap represented by data
     * @param height Height of bitmap represented by data
     * @param x      Desired x-coordinate of bitmap
     * @param y      Desired y-coordinate of bitmap
     */
    public Bitmap(int[] data, int width, int height, int x, int y) {
        this.highData = data;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    /**
     * Constructor for Bitmap based on
     *
     * @param data   Array of pixel data, each pixel represented by Bpp bytes. Should have a length of width*height*Bpp.
     * @param width  Width of bitmap represented by data
     * @param height Height of bitmap represented by data
     * @param x      Desired x-coordinate of bitmap
     * @param y      Desired y-coordinate of bitmap
     * @param Bpp    Number of bytes per pixel in image represented by data
     */
    public Bitmap(byte[] data, int width, int height, int x, int y, int Bpp) {
        this.highData = Bitmap.convertImage(data, Bpp);
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    /**
     * Retrieve data representing this Bitmap, as an array of integer pixel values
     *
     * @return Bitmap pixel data
     */
    public int[] getBitmapData() {
        return this.highData;
    }

    /**
     * Retrieve width of the bitmap represented by this object
     *
     * @return Bitmap width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Retrieve height of the bitmap represented by this object
     *
     * @return Bitmap height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Retrieve desired x-coordinate of the bitmap represented by this object
     *
     * @return x-coordinate of this bitmap
     */
    public int getX() {
        return this.x;
    }

    /**
     * Retrieve desired y-coordinate of the bitmap represented by this object
     *
     * @return y-coordinate of this bitmap
     */
    public int getY() {
        return this.y;
    }

    /**
     * Decompress bitmap data from packet and output directly to supplied image object
     *
     * @param width  Width of bitmap to decompress
     * @param height Height of bitmap to decompress
     * @param size   Size of compressed data in bytes
     * @param data   Packet containing bitmap data
     * @param Bpp    Bytes per-pixel for bitmap
     * @param left   X offset for drawing bitmap
     * @param top    Y offset for drawing bitmap
     * @param w      Image to draw bitmap to
     * @return Original image object, with decompressed bitmap drawn at specified coordinates
     * @throws RdesktopException Protocol error
     */
    public static WrappedImage decompressImgDirect(int width, int height, int size, RdpPacket data,
                                                   int Bpp, int left, int top, WrappedImage w)
            throws RdesktopException {

        //WrappedImage w = null;

        byte[] compressed_pixel = new byte[size];
        data.copyToByteArray(compressed_pixel, 0, data.getPosition(), size);
        data.incrementPosition(size);

        int previous = -1;
        int line = 0;
        int prevY = 0;
        int input = 0;
        int opCode;
        int count;
        int offset;
        int x = width;
        int lastOpCode = -1;
        int fom_mask;
        int code;
        int color1 = 0;
        int color2 = 0;
        byte mixMask;
        int mask = 0;
        int mix = 0xffffffff;

        boolean insertMix = false;
        boolean biColor = false;
        boolean isFillOrMix;

        while (input < size) {
            fom_mask = 0;
            code = (compressed_pixel[input++] & 0x000000ff);
            opCode = code >> 4;

            /* Handle different opCode forms */
            switch (opCode) {
                case 0xc:
                case WHITE:
                case BLACK:
                    opCode -= 6;
                    count = code & 0xf;
                    offset = 16;
                    break;

                case 0xf:
                    opCode = code & 0xf;
                    if (opCode < 9) {
                        count = (compressed_pixel[input++] & 0xff);
                        count |= ((compressed_pixel[input++] & 0xff) << 8);
                    } else {
                        count = (opCode < 0xb) ? 8 : 1;
                    }
                    offset = 0;
                    break;

                default:
                    opCode >>= 1;
                    count = code & 0x1f;
                    offset = 32;
                    break;
            }

            /* Handle strange cases for counts */
            if (offset != 0) {
                isFillOrMix = ((opCode == 2) || (opCode == 7));

                if (count == 0) {
                    if (isFillOrMix)
                        count = (compressed_pixel[input++] & 0x000000ff) + 1;
                    else
                        count = (compressed_pixel[input++] & 0x000000ff) + offset;
                } else if (isFillOrMix) {
                    count <<= 3;
                }
            }

            switch (opCode) {
                case FILL:
                    if ((lastOpCode == opCode)
                            && !((x == width) && (previous == -1)))
                        insertMix = true;
                    break;
                case BI_COLOR:
                    color1 = cvalx(compressed_pixel, input, Bpp);
                    input += Bpp;
                case COLOR:
                    color2 = cvalx(compressed_pixel, input, Bpp);
                    input += Bpp;
                    break;
                case SET_MIX_MIX:
                case SET_MIX_FILL_OR_MIX:
                    mix = cvalx(compressed_pixel, input, Bpp);
                    input += Bpp;
                    opCode -= 5;
                    break;
                case FILL_OR_MIX_1:
                    mask = 0x03;
                    opCode = 0x02;
                    fom_mask = 3;
                    break;
                case FILL_OR_MIX_2:
                    mask = 0x05;
                    opCode = 0x02;
                    fom_mask = 5;
                    break;

            }

            lastOpCode = opCode;
            mixMask = 0;

            /* Output body */
            while (count > 0) {
                if (x >= width) {
                    if (height <= 0)
                        throw new RdesktopException("Decompressing bitmap failed! Height = " + height);
                    x = 0;
                    height--;

                    previous = line;
                    prevY = previous / width;
                    line = height * width;
                }

                switch (opCode) {
                    case FILL:
                        if (insertMix) {
                            if (previous == -1) {
                                w.setRGB(left + x, top + height, mix);
                            } else {
                                w.setRGB(left + x, top + height, w.getRGB(left + x, top + prevY) ^ mix);
                            }

                            insertMix = false;
                            count--;
                            x++;
                        }

                        if (previous == -1) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    w.setRGB(left + x, top + height, 0);
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                w.setRGB(left + x, top + height, 0);
                                count--;
                                x++;
                            }
                        } else {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    w.setRGB(left + x, top + height, w.getRGB(left + x, top + prevY));
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                w.setRGB(left + x, top + height, w.getRGB(left + x, top + prevY));
                                count--;
                                x++;
                            }
                        }
                        break;

                    case MIX:
                        if (previous == -1) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    w.setRGB(left + x, top + height, mix);
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                w.setRGB(left + x, top + height, mix);
                                count--;
                                x++;
                            }
                        } else {

                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    w.setRGB(left + x, top + height, w.getRGB(left + x, top + prevY) ^ mix);
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                w.setRGB(left + x, top + height, w.getRGB(left + x, top + prevY) ^ mix);
                                count--;
                                x++;
                            }

                        }
                        break;
                    case FILL_OR_MIX:
                        if (previous == -1) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    mixMask <<= 1;
                                    if (mixMask == 0) {
                                        mask = (fom_mask != 0) ? (byte) fom_mask
                                                : compressed_pixel[input++];
                                        mixMask = 1;
                                    }
                                    if ((mask & mixMask) != 0) {
                                        w.setRGB(left + x, top + height, (byte) mix);
                                    } else {
                                        w.setRGB(left + x, top + height, 0);
                                    }
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                mixMask <<= 1;
                                if (mixMask == 0) {
                                    mask = (fom_mask != 0) ? (byte) fom_mask
                                            : compressed_pixel[input++];
                                    mixMask = 1;
                                }
                                if ((mask & mixMask) != 0) {
                                    w.setRGB(left + x, top + height, mix);
                                } else {
                                    w.setRGB(left + x, top + height, 0);
                                }
                                count--;
                                x++;
                            }
                        } else {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    mixMask <<= 1;
                                    if (mixMask == 0) {
                                        mask = (fom_mask != 0) ? (byte) fom_mask
                                                : compressed_pixel[input++];
                                        mixMask = 1;
                                    }
                                    if ((mask & mixMask) != 0) {
                                        w.setRGB(left + x, top + height, w.getRGB(left + x, prevY + top) ^ mix);
                                    } else {
                                        w.setRGB(left + x, top + height, w.getRGB(left + x, prevY + top));
                                    }
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                mixMask <<= 1;
                                if (mixMask == 0) {
                                    mask = (fom_mask != 0) ? (byte) fom_mask
                                            : compressed_pixel[input++];
                                    mixMask = 1;
                                }
                                if ((mask & mixMask) != 0) {
                                    w.setRGB(left + x, top + height, w.getRGB(left + x, prevY + top) ^ mix);
                                } else {
                                    w.setRGB(left + x, top + height, w.getRGB(left + x, prevY + top));
                                }
                                count--;
                                x++;
                            }

                        }
                        break;

                    case COLOR:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                w.setRGB(left + x, top + height, color2);
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            w.setRGB(left + x, top + height, color2);
                            count--;
                            x++;
                        }

                        break;

                    case COPY:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                w.setRGB(left + x, top + height, cvalx(compressed_pixel, input, Bpp));
                                input += Bpp;
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            w.setRGB(left + x, top + height, cvalx(compressed_pixel, input, Bpp));
                            input += Bpp;
                            count--;
                            x++;
                        }
                        break;

                    case BI_COLOR:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                if (biColor) {
                                    w.setRGB(left + x, top + height, color2);
                                    biColor = false;
                                } else {
                                    w.setRGB(left + x, top + height, color1);
                                    biColor = true;
                                    count++;
                                }
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            if (biColor) {
                                w.setRGB(left + x, top + height, color2);
                                biColor = false;
                            } else {
                                w.setRGB(left + x, top + height, color1);
                                biColor = true;
                                count++;
                            }
                            count--;
                            x++;
                        }

                        break;

                    case WHITE:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                w.setRGB(left + x, top + height, 0xffffff);
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            w.setRGB(left + x, top + height, 0xffffff);
                            count--;
                            x++;
                        }
                        break;

                    case BLACK:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                w.setRGB(left + x, top + height, 0x00);
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            w.setRGB(left + x, top + height, 0x00);
                            count--;
                            x++;
                        }

                        break;
                    default:
                        throw new RdesktopException(
                                "Unimplemented decompress opCode " + opCode);// ;
                }
            }
        }

        return w;
    }

    /**
     * Decompress bitmap data from packet and output as an Image
     *
     * @param width  Width of bitmap
     * @param height Height of bitmap
     * @param size   Size of compressed data in bytes
     * @param data   Packet containing bitmap data
     * @param Bpp    Bytes per-pixel for bitmap
     * @param cm     Color model for bitmap (if using indexed palette)
     * @return Decompressed bitmap as Image object
     * @throws RdesktopException Protocol error
     */
    public static Image decompressImg(int width, int height, int size, RdpPacket data, int Bpp, IndexColorModel cm) throws RdesktopException {

        WrappedImage w;

        byte[] compressed_pixel = new byte[size];
        data.copyToByteArray(compressed_pixel, 0, data.getPosition(), size);
        data.incrementPosition(size);

        int previous = -1;
        int line = 0;
        int prevY = 0;
        int input = 0;
        int opCode;
        int count;
        int offset;
        int x = width;
        int lastOpCode = -1;
        int fom_mask;
        int code;
        int color1 = 0;
        int color2 = 0;
        byte mixMask;
        int mask = 0;
        int mix = 0xffffffff;

        boolean insertMix = false;
        boolean biColor = false;
        boolean isFillOrMix;

        if (cm == null) {
            w = new WrappedImage(width, height, BufferedImage.TYPE_INT_RGB);
        } else {
            w = new WrappedImage(width, height, cm);
        }

        while (input < size) {
            fom_mask = 0;
            code = (compressed_pixel[input++] & 0x000000ff);
            opCode = code >> 4;

            /* Handle different opCode forms */
            switch (opCode) {
                case 0xc:
                case WHITE:
                case BLACK:
                    opCode -= 6;
                    count = code & 0xf;
                    offset = 16;
                    break;

                case 0xf:
                    opCode = code & 0xf;
                    if (opCode < 9) {
                        count = (compressed_pixel[input++] & 0xff);
                        count |= ((compressed_pixel[input++] & 0xff) << 8);
                    } else {
                        count = (opCode < 0xb) ? 8 : 1;
                    }
                    offset = 0;
                    break;

                default:
                    opCode >>= 1;
                    count = code & 0x1f;
                    offset = 32;
                    break;
            }

            /* Handle strange cases for counts */
            if (offset != 0) {
                isFillOrMix = ((opCode == 2) || (opCode == 7));

                if (count == 0) {
                    if (isFillOrMix) {
                        count = (compressed_pixel[input++] & 0x000000ff) + 1;
                    } else {
                        count = (compressed_pixel[input++] & 0x000000ff) + offset;
                    }
                } else if (isFillOrMix) {
                    count <<= 3;
                }
            }

            switch (opCode) {
                case FILL:
                    if ((lastOpCode == opCode)
                            && !((x == width) && (previous == -1)))
                        insertMix = true;
                    break;
                case BI_COLOR:
                    color1 = cvalx(compressed_pixel, input, Bpp);
                    input += Bpp;
                case COLOR:
                    color2 = cvalx(compressed_pixel, input, Bpp);
                    input += Bpp;
                    break;
                case SET_MIX_MIX:
                case SET_MIX_FILL_OR_MIX:
                    mix = cvalx(compressed_pixel, input, Bpp);
                    input += Bpp;
                    opCode -= 5;
                    break;
                case FILL_OR_MIX_1:
                    mask = 0x03;
                    opCode = 0x02;
                    fom_mask = 3;
                    break;
                case FILL_OR_MIX_2:
                    mask = 0x05;
                    opCode = 0x02;
                    fom_mask = 5;
                    break;

            }

            lastOpCode = opCode;
            mixMask = 0;

            /* Output body */
            while (count > 0) {
                if (x >= width) {
                    if (height <= 0) {
                        throw new RdesktopException("Decompressing bitmap failed! Height = " + height);
                    }
                    x = 0;
                    height--;

                    previous = line;
                    prevY = previous / width;
                    line = height * width;
                }

                switch (opCode) {
                    case FILL:
                        if (insertMix) {
                            if (previous == -1) {
                                w.setRGB(x, height, mix);
                            } else {
                                w.setRGB(x, height, w.getRGB(x, prevY) ^ mix);
                            }

                            insertMix = false;
                            count--;
                            x++;
                        }

                        if (previous == -1) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    w.setRGB(x, height, 0);
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                w.setRGB(x, height, 0);
                                count--;
                                x++;
                            }
                        } else {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    w.setRGB(x, height, w.getRGB(x, prevY));
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                w.setRGB(x, height, w.getRGB(x, prevY));
                                count--;
                                x++;
                            }
                        }
                        break;

                    case MIX:
                        if (previous == -1) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    w.setRGB(x, height, mix);
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                w.setRGB(x, height, mix);
                                count--;
                                x++;
                            }
                        } else {

                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    w.setRGB(x, height, w.getRGB(x, prevY) ^ mix);
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                w.setRGB(x, height, w.getRGB(x, prevY) ^ mix);
                                count--;
                                x++;
                            }

                        }
                        break;
                    case FILL_OR_MIX:
                        if (previous == -1) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    mixMask <<= 1;
                                    if (mixMask == 0) {
                                        mask = (fom_mask != 0) ? (byte) fom_mask
                                                : compressed_pixel[input++];
                                        mixMask = 1;
                                    }
                                    if ((mask & mixMask) != 0) {
                                        w.setRGB(x, height, (byte) mix);
                                    } else {
                                        w.setRGB(x, height, 0);
                                    }
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                mixMask <<= 1;
                                if (mixMask == 0) {
                                    mask = (fom_mask != 0) ? (byte) fom_mask
                                            : compressed_pixel[input++];
                                    mixMask = 1;
                                }
                                if ((mask & mixMask) != 0) {
                                    w.setRGB(x, height, mix);
                                } else {
                                    w.setRGB(x, height, 0);
                                }
                                count--;
                                x++;
                            }
                        } else {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    mixMask <<= 1;
                                    if (mixMask == 0) {
                                        mask = (fom_mask != 0) ? (byte) fom_mask
                                                : compressed_pixel[input++];
                                        mixMask = 1;
                                    }
                                    if ((mask & mixMask) != 0) {
                                        w.setRGB(x, height, w.getRGB(x, prevY) ^ mix);
                                    } else {
                                        w.setRGB(x, height, w.getRGB(x, prevY));
                                    }
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                mixMask <<= 1;
                                if (mixMask == 0) {
                                    mask = (fom_mask != 0) ? (byte) fom_mask
                                            : compressed_pixel[input++];
                                    mixMask = 1;
                                }
                                if ((mask & mixMask) != 0) {
                                    w.setRGB(x, height, w.getRGB(x, prevY) ^ mix);
                                } else {
                                    w.setRGB(x, height, w.getRGB(x, prevY));
                                }
                                count--;
                                x++;
                            }

                        }
                        break;

                    case COLOR:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                // pixel[line + x] = color2;
                                w.setRGB(x, height, color2);
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            w.setRGB(x, height, color2);
                            count--;
                            x++;
                        }

                        break;

                    case COPY:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                w.setRGB(x, height, cvalx(compressed_pixel, input, Bpp));
                                input += Bpp;
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            w.setRGB(x, height, cvalx(compressed_pixel, input, Bpp));
                            input += Bpp;
                            count--;
                            x++;
                        }
                        break;

                    case BI_COLOR:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                if (biColor) {
                                    w.setRGB(x, height, color2);
                                    biColor = false;
                                } else {
                                    w.setRGB(x, height, color1);
                                    biColor = true;
                                    count++;
                                }
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            if (biColor) {
                                w.setRGB(x, height, color2);
                                biColor = false;
                            } else {
                                w.setRGB(x, height, color1);
                                biColor = true;
                                count++;
                            }
                            count--;
                            x++;
                        }

                        break;

                    case WHITE:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                w.setRGB(x, height, 0xffffff);
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            w.setRGB(x, height, 0xffffff);
                            count--;
                            x++;
                        }
                        break;

                    case BLACK:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                w.setRGB(x, height, 0x00);
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            w.setRGB(x, height, 0x00);
                            count--;
                            x++;
                        }

                        break;
                    default:
                        throw new RdesktopException(
                                "Unimplemented decompress opCode " + opCode);// ;
                }
            }
        }

        /* if(Options.getServerBpp == 16){
            for(int i = 0; i < pixel.length; i++) pixel[i] = Bitmap.convert16to24(pixel[i]);
        }*/

        return w.getBufferedImage();
    }

    /**
     * Decompress bitmap data from packet and store in array of integers
     *
     * @param width  Width of bitmap
     * @param height Height of bitmap
     * @param size   Size of compressed data in bytes
     * @param data   Packet containing bitmap data
     * @param Bpp    Bytes per-pixel for bitmap
     * @return Integer array of pixels containing decompressed bitmap data
     * @throws RdesktopException Protocol error
     */
    public static int[] decompressInt(int width, int height, int size, RdpPacket data, int Bpp)
            throws RdesktopException {

        byte[] compressed_pixel = new byte[size];
        data.copyToByteArray(compressed_pixel, 0, data.getPosition(), size);
        data.incrementPosition(size);

        int previous = -1;
        int line = 0;
        int input = 0;
        int output = 0;
        int opCode;
        int count;
        int offset;
        int x = width;
        int lastOpCode = -1;
        int fom_mask;
        int code;
        int color1 = 0;
        int color2 = 0;
        byte mixMask;
        int mask = 0;
        int mix = 0xffffffff;

        boolean insertMix = false;
        boolean biColor = false;
        boolean isFillOrMix;

        int[] pixels = new int[width * height];
        while (input < size) {
            fom_mask = 0;
            code = (compressed_pixel[input++] & 0x000000ff);
            opCode = code >> 4;

            /* Handle different opCode forms */
            switch (opCode) {
                case 0xc:
                case WHITE:
                case BLACK:
                    opCode -= 6;
                    count = code & 0xf;
                    offset = 16;
                    break;

                case 0xf:
                    opCode = code & 0xf;
                    if (opCode < 9) {
                        count = (compressed_pixel[input++] & 0xff);
                        count |= ((compressed_pixel[input++] & 0xff) << 8);
                    } else {
                        count = (opCode < 0xb) ? 8 : 1;
                    }
                    offset = 0;
                    break;

                default:
                    opCode >>= 1;
                    count = code & 0x1f;
                    offset = 32;
                    break;
            }

            /* Handle strange cases for counts */
            if (offset != 0) {
                isFillOrMix = ((opCode == 2) || (opCode == 7));

                if (count == 0) {
                    if (isFillOrMix) {
                        count = (compressed_pixel[input++] & 0x000000ff) + 1;
                    } else {
                        count = (compressed_pixel[input++] & 0x000000ff) + offset;
                    }
                } else if (isFillOrMix) {
                    count <<= 3;
                }
            }

            switch (opCode) {
                case FILL:
                    if ((lastOpCode == opCode)
                            && !((x == width) && (previous == -1)))
                        insertMix = true;
                    break;
                case BI_COLOR:
                    color1 = cvalx(compressed_pixel, input, Bpp);
                    //(compressed_pixel[input++]&0x000000ff);
                    input += Bpp;
                case COLOR:
                    color2 = cvalx(compressed_pixel, input, Bpp);
                    //color2 = (compressed_pixel[input++]&0x000000ff);
                    input += Bpp;
                    break;
                case SET_MIX_MIX:
                case SET_MIX_FILL_OR_MIX:
                    mix = cvalx(compressed_pixel, input, Bpp);
                    input += Bpp;
                    opCode -= 5;
                    break;
                case FILL_OR_MIX_1:
                    mask = 0x03;
                    opCode = 0x02;
                    fom_mask = 3;
                    break;
                case FILL_OR_MIX_2:
                    mask = 0x05;
                    opCode = 0x02;
                    fom_mask = 5;
                    break;

            }

            lastOpCode = opCode;
            mixMask = 0;

            /* Output body */
            while (count > 0) {
                if (x >= width) {
                    if (height <= 0)
                        throw new RdesktopException("Decompressing bitmap failed! Height = " + height);

                    x = 0;
                    height--;

                    previous = line;
                    line = output + height * width;
                }

                switch (opCode) {
                    case FILL:
                        if (insertMix) {
                            if (previous == -1) {
                                pixels[line + x] = mix;
                            } else {
                                pixels[line + x] = (pixels[previous + x] ^ mix);
                            }

                            insertMix = false;
                            count--;
                            x++;
                        }

                        if (previous == -1) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    pixels[line + x] = 0;
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                pixels[line + x] = 0;
                                count--;
                                x++;
                            }
                        } else {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    pixels[line + x] = pixels[previous + x];
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                pixels[line + x] = pixels[previous + x];
                                count--;
                                x++;
                            }
                        }
                        break;

                    case MIX:
                        if (previous == -1) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    pixels[line + x] = mix;
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                pixels[line + x] = mix;
                                count--;
                                x++;
                            }
                        } else {

                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    pixels[line + x] = pixels[previous + x] ^ mix;
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                pixels[line + x] = pixels[previous + x] ^ mix;
                                count--;
                                x++;
                            }

                        }
                        break;
                    case FILL_OR_MIX:
                        if (previous == -1) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    mixMask <<= 1;
                                    if (mixMask == 0) {
                                        mask = (fom_mask != 0) ? (byte) fom_mask
                                                : compressed_pixel[input++];
                                        mixMask = 1;
                                    }
                                    if ((mask & mixMask) != 0)
                                        pixels[line + x] = (byte) mix;
                                    else
                                        pixels[line + x] = 0;
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                mixMask <<= 1;
                                if (mixMask == 0) {
                                    mask = (fom_mask != 0) ? (byte) fom_mask
                                            : compressed_pixel[input++];
                                    mixMask = 1;
                                }
                                if ((mask & mixMask) != 0)
                                    pixels[line + x] = mix;
                                else
                                    pixels[line + x] = 0;
                                count--;
                                x++;
                            }
                        } else {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    mixMask <<= 1;
                                    if (mixMask == 0) {
                                        mask = (fom_mask != 0) ? (byte) fom_mask
                                                : compressed_pixel[input++];
                                        mixMask = 1;
                                    }
                                    if ((mask & mixMask) != 0)
                                        pixels[line + x] = (pixels[previous + x] ^ mix);
                                    else
                                        pixels[line + x] = pixels[previous + x];
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                mixMask <<= 1;
                                if (mixMask == 0) {
                                    mask = (fom_mask != 0) ? (byte) fom_mask
                                            : compressed_pixel[input++];
                                    mixMask = 1;
                                }
                                if ((mask & mixMask) != 0)
                                    pixels[line + x] = (pixels[previous + x] ^ mix);
                                else
                                    pixels[line + x] = pixels[previous + x];
                                count--;
                                x++;
                            }

                        }
                        break;

                    case COLOR:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                pixels[line + x] = color2;
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            pixels[line + x] = color2;
                            count--;
                            x++;
                        }

                        break;

                    case COPY:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                pixels[line + x] = cvalx(compressed_pixel, input,
                                        Bpp);
                                input += Bpp;
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            pixels[line + x] = cvalx(compressed_pixel, input, Bpp);
                            input += Bpp;
                            count--;
                            x++;
                        }
                        break;

                    case BI_COLOR:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                if (biColor) {
                                    pixels[line + x] = color2;
                                    biColor = false;
                                } else {
                                    pixels[line + x] = color1;
                                    biColor = true;
                                    count++;
                                }
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            if (biColor) {
                                pixels[line + x] = color2;
                                biColor = false;
                            } else {
                                pixels[line + x] = color1;
                                biColor = true;
                                count++;
                            }
                            count--;
                            x++;
                        }

                        break;

                    case WHITE:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                pixels[line + x] = 0xffffff;
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            pixels[line + x] = 0xffffff;
                            count--;
                            x++;
                        }
                        break;

                    case BLACK:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                pixels[line + x] = 0x00;
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            pixels[line + x] = 0x00;
                            count--;
                            x++;
                        }

                        break;
                    default:
                        throw new RdesktopException(
                                "Unimplemented decompress opCode " + opCode);// ;
                }
            }
        }

        return pixels;
    }

    /**
     * Decompress bitmap data from packet and store in array of bytes
     *
     * @param width  Width of bitmap
     * @param height Height of bitmap
     * @param size   Size of compressed data in bytes
     * @param data   Packet containing bitmap data
     * @return Byte array of pixels containing decompressed bitmap data
     * @throws RdesktopException Protocol exception
     */
    public static byte[] decompress(int width, int height, int size, RdpPacket data) throws RdesktopException {

        byte[] compressed_pixel = new byte[size];
        data.copyToByteArray(compressed_pixel, 0, data.getPosition(), size);
        data.incrementPosition(size);

        int previous = 0;
        int line = 0;
        int input = 0;
        int output = 0;
        int opCode;
        int count;
        int offset;
        int x = width;
        int lastOpCode = -1;
        int fom_mask;
        int code;
        int color1 = 0;
        int color2 = 0;
        byte mixMask;
        int mask = 0;
        int mix = 0xffffffff;

        boolean insertMix = false;
        boolean biColor = false;
        boolean isFillOrMix;

        byte[] pixel = new byte[width * height];
        while (input < size) {
            fom_mask = 0;
            code = (compressed_pixel[input++] & 0x000000ff);
            opCode = code >> 4;

            /* Handle different opCode forms */
            switch (opCode) {
                case 0xc:
                case WHITE:
                case BLACK:
                    opCode -= 6;
                    count = code & 0xf;
                    offset = 16;
                    break;

                case 0xf:
                    opCode = code & 0xf;
                    if (opCode < 9) {
                        count = (compressed_pixel[input++] & 0xff);
                        count |= ((compressed_pixel[input++] & 0xff) << 8);
                    } else {
                        count = (opCode < 0xb) ? 8 : 1;
                    }
                    offset = 0;
                    break;

                default:
                    opCode >>= 1;
                    count = code & 0x1f;
                    offset = 32;
                    break;
            }

            /* Handle strange cases for counts */
            if (offset != 0) {
                isFillOrMix = ((opCode == 2) || (opCode == 7));

                if (count == 0) {
                    if (isFillOrMix)
                        count = (compressed_pixel[input++] & 0x000000ff) + 1;
                    else
                        count = (compressed_pixel[input++] & 0x000000ff)
                                + offset;
                } else if (isFillOrMix) {
                    count <<= 3;
                }
            }

            switch (opCode) {
                case FILL:
                    if ((lastOpCode == opCode) && !((x == width) && (previous == 0))) {
                        insertMix = true;
                    }
                    break;
                case BI_COLOR:
                    color1 = (compressed_pixel[input++] & 0x000000ff);
                case COLOR:
                    color2 = (compressed_pixel[input++] & 0x000000ff);
                    break;
                case SET_MIX_MIX:
                case SET_MIX_FILL_OR_MIX:
                    mix = compressed_pixel[input++];
                    opCode -= 5;
                    break;
                case FILL_OR_MIX_1:
                    mask = 0x03;
                    opCode = 0x02;
                    fom_mask = 3;
                    break;
                case FILL_OR_MIX_2:
                    mask = 0x05;
                    opCode = 0x02;
                    fom_mask = 5;
                    break;

            }

            lastOpCode = opCode;
            mixMask = 0;

            /* Output body */
            while (count > 0) {
                if (x >= width) {
                    if (height <= 0) {
                        throw new RdesktopException("Decompressing bitmap failed! Height = " + height);
                    }

                    x = 0;
                    height--;

                    previous = line;
                    line = output + height * width;
                }

                switch (opCode) {
                    case FILL:
                        if (insertMix) {
                            if (previous == 0) {
                                pixel[line + x] = (byte) mix;
                            } else {
                                pixel[line + x] = (byte) (pixel[previous + x] ^ (byte) mix);
                            }

                            insertMix = false;
                            count--;
                            x++;
                        }

                        if (previous == 0) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    pixel[line + x] = 0;
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                pixel[line + x] = 0;
                                count--;
                                x++;
                            }
                        } else {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    pixel[line + x] = pixel[previous + x];
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                pixel[line + x] = pixel[previous + x];
                                count--;
                                x++;
                            }
                        }
                        break;

                    case MIX:
                        if (previous == 0) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    pixel[line + x] = (byte) mix;
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                pixel[line + x] = (byte) mix;
                                count--;
                                x++;
                            }
                        } else {

                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    setli(pixel, line, x, getli(pixel, previous, x, 1) ^ mix, 1);
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                setli(pixel, line, x, getli(pixel, previous, x, 1) ^ mix, 1);
                                count--;
                                x++;
                            }

                        }
                        break;
                    case FILL_OR_MIX:
                        if (previous == 0) {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    mixMask <<= 1;
                                    if (mixMask == 0) {
                                        mask = (fom_mask != 0) ? (byte) fom_mask
                                                : compressed_pixel[input++];
                                        mixMask = 1;
                                    }
                                    if ((mask & mixMask) != 0)
                                        pixel[line + x] = (byte) mix;
                                    else
                                        pixel[line + x] = 0;
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                mixMask <<= 1;
                                if (mixMask == 0) {
                                    mask = (fom_mask != 0) ? (byte) fom_mask
                                            : compressed_pixel[input++];
                                    mixMask = 1;
                                }
                                if ((mask & mixMask) != 0)
                                    pixel[line + x] = (byte) mix;
                                else
                                    pixel[line + x] = 0;
                                count--;
                                x++;
                            }
                        } else {
                            while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                                for (int i = 0; i < 8; i++) {
                                    mixMask <<= 1;
                                    if (mixMask == 0) {
                                        mask = (fom_mask != 0) ? (byte) fom_mask
                                                : compressed_pixel[input++];
                                        mixMask = 1;
                                    }
                                    if ((mask & mixMask) != 0)
                                        pixel[line + x] = (byte) (pixel[previous + x] ^
                                                (byte) mix);
                                    else
                                        pixel[line + x] = pixel[previous + x];
                                    count--;
                                    x++;
                                }
                            }
                            while ((count > 0) && (x < width)) {
                                mixMask <<= 1;
                                if (mixMask == 0) {
                                    mask = (fom_mask != 0) ? (byte) fom_mask
                                            : compressed_pixel[input++];
                                    mixMask = 1;
                                }
                                if ((mask & mixMask) != 0)
                                    pixel[line + x] = (byte) (pixel[previous + x] ^
                                            (byte) mix);
                                else
                                    pixel[line + x] = pixel[previous + x];
                                count--;
                                x++;
                            }

                        }
                        break;

                    case COLOR:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                pixel[line + x] = (byte) color2;
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            pixel[line + x] = (byte) color2;
                            count--;
                            x++;
                        }

                        break;

                    case COPY:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                pixel[line + x] = compressed_pixel[input++];
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            pixel[line + x] = compressed_pixel[input++];
                            count--;
                            x++;
                        }
                        break;

                    case BI_COLOR:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                if (biColor) {
                                    pixel[line + x] = (byte) color2;
                                    biColor = false;
                                } else {
                                    pixel[line + x] = (byte) color1;
                                    biColor = true;
                                    count++;
                                }
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            if (biColor) {
                                pixel[line + x] = (byte) color2;
                                biColor = false;
                            } else {
                                pixel[line + x] = (byte) color1;
                                biColor = true;
                                count++;
                            }
                            count--;
                            x++;
                        }

                        break;

                    case WHITE:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                pixel[line + x] = (byte) 0xff;
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            pixel[line + x] = (byte) 0xff;
                            count--;
                            x++;
                        }
                        break;

                    case BLACK:
                        while (((count & ~0x7) != 0) && ((x + 8) < width)) {
                            for (int i = 0; i < 8; i++) {
                                pixel[line + x] = (byte) 0x00;
                                count--;
                                x++;
                            }
                        }
                        while ((count > 0) && (x < width)) {
                            pixel[line + x] = (byte) 0x00;
                            count--;
                            x++;
                        }

                        break;
                    default:
                        throw new RdesktopException("Unimplemented decompress opCode " + opCode);// ;
                }
            }
        }

        return pixel;
    }
}
