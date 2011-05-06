/* RasterOp.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Set of operations used in displaying raster graphics
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
// Created on 01-Jul-2003

package net.propero.rdp;


import net.propero.rdp.cached.WrappedImage;
import org.apache.log4j.Logger;

public class RasterOp {
    static Logger logger = Logger.getLogger(Rdp.class);

    /**
     * Perform an operation on a rectangular area of a WrappedImage, using an integer array of color values as
     * source if necessary
     *
     * @param opcode      Code defining operation to perform
     * @param biDst       Destination image for operation
     * @param destWidth   Width of destination image
     * @param x           X-offset of destination area within destination image
     * @param y           Y-offset of destination area within destination image
     * @param cx          Width of destination area
     * @param cy          Height of destination area
     * @param src         Source data, represented as an array of integer pixel values
     * @param sourceWidth Width of source data
     * @param sourceX     X-offset of source area within source data
     * @param sourceY     Y-offset of source area within source data
     */
    public void doArrayOperation(int opcode, WrappedImage biDst, int destWidth, int x, int y,
                                 int cx, int cy, int[] src, int sourceWidth, int sourceX, int sourceY) {

        if (cx == 63) {
            System.out.println(opcode);
        }
        switch (opcode) {
            case 0x0:
                ropClear(biDst, x, y, cx, cy);
                break;
            case 0x1:
                ropNor(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0x2:
                ropAndInverted(biDst, x, y, cx, cy, src, sourceWidth, sourceX,
                        sourceY);
                break;
            case 0x3: // CopyInverted
                ropInvert(biDst, src, sourceWidth, sourceX, sourceY, cx, cy);
                ropCopy(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0x4: // AndReverse
                ropInvert(biDst, null, destWidth, x, y, cx, cy);
                ropAnd(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0x5:
                ropInvert(biDst, null, destWidth, x, y, cx, cy);
                break;
            case 0x6:
                ropXor(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0x7:
                ropNand(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0x8:
                ropAnd(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0x9:
                ropEquiv(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0xa: // Noop
                break;
            case 0xb:
                ropOrInverted(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0xc:
                ropCopy(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0xd: // OrReverse
                ropInvert(biDst, null, destWidth, x, y, cx, cy);
                ropOr(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0xe:
                ropOr(biDst, x, y, cx, cy, src, sourceWidth, sourceX, sourceY);
                break;
            case 0xf:
                ropSet(biDst, x, y, cx, cy);
                break;
            default:
                logger.warn("doArrayOperation unsupported opcode: " + opcode);
                // rop_array(opcode,dst,destWidth,x,y,cx,cy,src,sourceWidth,sourceX,sourceY);
        }
    }

    /**
     * Perform an operation on a single pixel in a WrappedImage
     *
     * @param opcode Opcode defining operation to perform
     * @param dst    Image on which to perform the operation
     * @param x      X-coordinate of pixel to modify
     * @param y      Y-coordinate of pixel to modify
     * @param color  Color to use in operation (unused for some operations)
     */
    public void doPixelOperation(int opcode, WrappedImage dst, int x, int y, int color) {
        int mask = Options.getBppMask();

        if (dst == null)
            return;

        int c = dst.getRGB(x, y);

        switch (opcode) {
            case 0x0:
                dst.setRGB(x, y, 0);
                break;
            case 0x1:
                dst.setRGB(x, y, (~(c | color)) & mask);
                break;
            case 0x2:
                dst.setRGB(x, y, c & ((~color) & mask));
                break;
            case 0x3:
                dst.setRGB(x, y, (~color) & mask);
                break;
            case 0x4:
                dst.setRGB(x, y, (~c & color) * mask);
                break;
            case 0x5:
                dst.setRGB(x, y, (~c) & mask);
                break;
            case 0x6:
                dst.setRGB(x, y, c ^ ((color) & mask));
                break;
            case 0x7:
                dst.setRGB(x, y, (~c & color) & mask);
                break;
            case 0x8:
                dst.setRGB(x, y, c & (color & mask));
                break;
            case 0x9:
                dst.setRGB(x, y, c ^ (~color & mask));
                break;
            case 0xa: /* Noop */
                break;
            case 0xb:
                dst.setRGB(x, y, c | (~color & mask));
                break;
            case 0xc:
                dst.setRGB(x, y, color);
                break;
            case 0xd:
                dst.setRGB(x, y, (~c | color) & mask);
                break;
            case 0xe:
                dst.setRGB(x, y, c | (color & mask));
                break;
            case 0xf:
                dst.setRGB(x, y, mask);
                break;
            default:
                logger.warn("do_byte unsupported opcode: " + opcode);
        }
    }

    private void ropInvert(WrappedImage biDst, int[] dest, int width, int x, int y, int cx, int cy) {
        int mask = Options.getBppMask();
        int pdest = (y * width + x);
        for (int i = 0; i < cy; i++) {
            for (int j = 0; j < cx; j++) {
                if (biDst != null) {
                    int c = biDst.getRGB(x + j, y + i);
                    biDst.setRGB(x + j, y + i, ~c & mask);
                } else
                    dest[pdest] = (~dest[pdest]) & mask;
                pdest++;
            }
            pdest += (width - cx);
        }
    }

    private void ropClear(WrappedImage biDst, int x, int y, int cx, int cy) {

        for (int i = x; i < x + cx; i++) {
            for (int j = y; j < y + cy; j++)
                biDst.setRGB(i, j, 0);
        }
    }

    private void ropSet(WrappedImage biDst, int x, int y, int cx, int cy) {

        int mask = Options.getBppMask();

        for (int i = x; i < x + cx; i++) {
            for (int j = y; j < y + cy; j++) {
                biDst.setRGB(i, j, mask);
            }
        }

    }

    private void ropCopy(WrappedImage biDst, int x, int y, int cx, int cy,
                         int[] src, int sourceWidth, int srcx, int srcy) {

        if (src == null) { // special case - copy to self
            biDst.getGraphics().copyArea(srcx, srcy, cx, cy, x - srcx, y - srcy);
        } else {
            biDst.setRGB(x, y, cx, cy, src, 0, sourceWidth);
        }
    }

    private void ropNor(WrappedImage biDst, int x, int y, int cx, int cy,
                        int[] src, int srcwidth, int srcx, int srcy) {
        // opcode 0x1
        int mask = Options.getBppMask();
        int psrc = (srcy * srcwidth + srcx);

        for (int row = 0; row < cy; row++) {
            for (int col = 0; col < cx; col++) {
                biDst.setRGB(x + cx, y + cy, (~(biDst.getRGB(x + cx, y + cy) | src[psrc])) & mask);
            }
            psrc += (srcwidth - cx);
        }
    }

    private void ropAndInverted(WrappedImage biDst, int x, int y, int cx,
                                int cy, int[] src, int srcwidth, int srcx, int srcy) {
        // opcode 0x2
        int mask = Options.getBppMask();
        int psrc = (srcy * srcwidth + srcx);
        for (int row = 0; row < cy; row++) {
            for (int col = 0; col < cx; col++) {
                int c = biDst.getRGB(x + cx, y + cy);
                biDst.setRGB(x + cx, y + cy, c & ((~src[psrc]) & mask));
                psrc++;
            }
            psrc += (srcwidth - cx);
        }
    }

    private void ropXor(WrappedImage biDst, int x, int y, int cx, int cy,
                        int[] src, int srcwidth, int srcx, int srcy) {
        // opcode 0x6
        int mask = Options.getBppMask();
        int psrc = (srcy * srcwidth + srcx);
        for (int row = 0; row < cy; row++) {
            for (int col = 0; col < cx; col++) {
                int c = biDst.getRGB(x + col, y + row);
                biDst.setRGB(x + col, y + row, c ^ ((src[psrc]) & mask));
                psrc++;
            }
            psrc += (srcwidth - cx);
        }
    }

    private void ropNand(WrappedImage biDst, int x, int y, int cx, int cy,
                         int[] src, int srcwidth, int srcx, int srcy) {
        // opcode 0x7
        int mask = Options.getBppMask();
        int psrc = (srcy * srcwidth + srcx);
        for (int row = 0; row < cy; row++) {
            for (int col = 0; col < cx; col++) {
                int c = biDst.getRGB(x + col, y + row);
                biDst.setRGB(x + col, y + row, (~(c & src[psrc])) & mask);
                psrc++;
            }
            psrc += (srcwidth - cx);
        }
    }

    private void ropAnd(WrappedImage biDst, int x, int y, int cx, int cy,
                        int[] src, int srcwidth, int srcx, int srcy) {
        // opcode 0x8
        int mask = Options.getBppMask();
        int psrc = (srcy * srcwidth + srcx);
        for (int row = 0; row < cy; row++) {
            for (int col = 0; col < cx; col++) {
                int c = biDst.getRGB(x + col, y + row);
                biDst.setRGB(x + col, y + row, c & ((src[psrc]) & mask));
                psrc++;
            }
            psrc += (srcwidth - cx);
        }
    }

    private void ropEquiv(WrappedImage biDst, int x, int y, int cx,
                          int cy, int[] src, int srcwidth, int srcx, int srcy) {
        // opcode 0x9
        int mask = Options.getBppMask();
        int psrc = (srcy * srcwidth + srcx);
        for (int row = 0; row < cy; row++) {
            for (int col = 0; col < cx; col++) {
                int c = biDst.getRGB(x + col, y + row);
                biDst.setRGB(x + col, y + row, c ^ ((~src[psrc]) & mask));
                psrc++;
            }
            psrc += (srcwidth - cx);
        }
    }

    private void ropOrInverted(WrappedImage biDst, int x, int y, int cx,
                               int cy, int[] src, int srcwidth, int srcx, int srcy) {
        // opcode 0xb
        int mask = Options.getBppMask();
        int psrc = (srcy * srcwidth + srcx);
        for (int row = 0; row < cy; row++) {
            for (int col = 0; col < cx; col++) {
                int c = biDst.getRGB(x + col, y + row);
                biDst.setRGB(x + col, y + row, c | ((~src[psrc]) & mask));
                psrc++;
            }
            psrc += (srcwidth - cx);
        }
    }

    private void ropOr(WrappedImage biDst, int x, int y, int cx, int cy,
                       int[] src, int srcwidth, int srcx, int srcy) {
        // opcode 0xe
        int mask = Options.getBppMask();
        int psrc = (srcy * srcwidth + srcx);
        for (int row = 0; row < cy; row++) {
            for (int col = 0; col < cx; col++) {
                int c = biDst.getRGB(x + col, y + row);
                biDst.setRGB(x + col, y + row, c | (src[psrc] & mask));
                psrc++;
            }
            psrc += (srcwidth - cx);
        }
    }
}
