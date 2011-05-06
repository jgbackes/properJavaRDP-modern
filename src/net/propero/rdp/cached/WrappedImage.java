/* WrappedImage.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.3 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Adds functionality to the BufferedImage class, allowing
 *          manipulation of color indices, making the RGB values
 *          invisible (in the case of Indexed Color only).
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
package net.propero.rdp.cached;

import net.propero.rdp.Rdp;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

public class WrappedImage {
    private static Logger logger = Logger.getLogger(Rdp.class);

    private IndexColorModel colorModel = null;
    private BufferedImage bufferedImage = null;

    /**
     * Constructor for WrappedImage of a given width, height and imageType
     *
     * @param width     Width of the warped image
     * @param height    Height of the warped image
     * @param imageType The image type
     */
    public WrappedImage(int width, int height, int imageType) {
        bufferedImage = new BufferedImage(width, height, imageType);
    }

    /**
     * Constructor for WrappedImage of a given width, height, colorModel
     *
     * @param width
     * @param height
     * @param colorModel
     */
    public WrappedImage(int width, int height, IndexColorModel colorModel) {
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); //super(width, height, BufferedImage.TYPE_INT_RGB);
        this.colorModel = colorModel;
    }

    public int getWidth() {
        return bufferedImage.getWidth();
    }

    public int getHeight() {
        return bufferedImage.getHeight();
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public Graphics getGraphics() {
        return bufferedImage.getGraphics();
    }

    public BufferedImage getSubImage(int x, int y, int width, int height) {
        BufferedImage result = null;

        try {
            result = bufferedImage.getSubimage(x, y, width, height);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        return result;
    }

    /**
     * Force a color to its true RGB representation (extracting from color model if indexed color)
     *
     * @param color
     * @return
     */
    public int checkColor(int color) {
        int result = color;
        if (colorModel != null) {
            result = colorModel.getRGB(color);
        }
        return result;
    }

    /**
     * Set the color model for this Image
     *
     * @param colorModel Color model for use with this image
     */
    public void setIndexColorModel(IndexColorModel colorModel) {
        this.colorModel = colorModel;
    }

    /**
     * Set the color at a given xy position
     *
     * @param x     location on the x axis
     * @param y     location on the y axis
     * @param color color to set
     */
    public void setRGB(int x, int y, int color) {
        if (x < bufferedImage.getWidth() && x >= 0 && y < bufferedImage.getHeight() && y >= 0) {

            if (colorModel != null) {
                color = colorModel.getRGB(color);
            }
            bufferedImage.setRGB(x, y, color);
        }
    }

    /**
     * Apply a given array of color values to an area of pixels in the image, do not convert for color model
     *
     * @param x      x-coordinate for left of area to set
     * @param y      y-coordinate for top of area to set
     * @param cx     width of area to set
     * @param cy     height of area to set
     * @param data   array of pixel color values to apply to area
     * @param offset offset to pixel data in data
     * @param w      width of a line in data (measured in pixels)
     */
    public void setRGBNoConversion(int x, int y, int cx, int cy, int[] data, int offset, int w) {
        bufferedImage.setRGB(x, y, cx, cy, data, offset, w);
    }

    /**
     * Apply a given array of color values to an area of pixels in the image.
     *
     * @param x      Coordinate for the left of area to set
     * @param y      Coordinate for the top of area to set
     * @param cx     width of the area to set
     * @param cy     height of the area to set
     * @param data   array of pixel color values to apply to area
     * @param offset offset to pixel data in data
     * @param w      width of line in data (measured in pixels)
     */
    public void setRGB(int x, int y, int cx, int cy, int[] data, int offset, int w) {
        if (colorModel != null && data != null && data.length > 0) {
            for (int i = 0; i < data.length; i++) {
                data[i] = colorModel.getRGB(data[i]);
            }
        }
        bufferedImage.setRGB(x, y, cx, cy, data, offset, w);
    }

    /**
     * Get the RGB color data from the buffered image
     *
     * @param x      Coordinate for the left of area to get
     * @param y      Coordinate for the top of area to get
     * @param cx     width of the area to get
     * @param cy     height of the area to get
     * @param data   array of pixel color values to get from area
     * @param offset offset to pixel data in data
     * @param width  width of line in data (measured in pixels)
     * @return RGB data from the bufferedImage
     */
    public int[] getRGB(int x,
                        int y,
                        int cx,
                        int cy,
                        int[] data,
                        int offset,
                        int width) {
        return bufferedImage.getRGB(x, y, cx, cy, data, offset, width);
    }

    /**
     * @param x horizontal location of pixel
     * @param y vertical location of pixel
     * @return RGB value at this locaion
     */
    public int getRGB(int x, int y) {
        int result = 0;

        if (x < this.getWidth() && x >= 0 && y < this.getHeight() && y >= 0) {

            if (colorModel == null)
                return bufferedImage.getRGB(x, y);
            else {
                int pix = bufferedImage.getRGB(x, y) & 0xFFFFFF;
                int[] values = {(pix >> 16) & 0xFF,
                        (pix >> 8) & 0xFF,
                        (pix) & 0xFF};
                result = colorModel.getDataElement(values, 0);
            }
        }

        return result;
    }
}
