package net.propero.rdp.pdus;

import net.propero.rdp.Bitmap;
import net.propero.rdp.Options;
import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;

import java.awt.*;

/**
 * [MS-RDPBCGR] Section 2.2.9.1.1.3.1.2.1 Bitmap PointerUpdate Data (TS_UPDATE_BITMAP_DATA)
 * <p/>
 * The TS_UPDATE_BITMAP structure contains one or more rectangular clippings taken
 * from the server-side screen frame buffer (see [T128] section 8.17).
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/dd306368(v=PROT.10).aspx">
 *      [MS-RDPBCGR] Section 2.2.9.1.1.3.1.2.1 Bitmap PointerUpdate Data (TS_UPDATE_BITMAP_DATA)</a>
 */

public class BitmapUpdatePDU extends IncomingPDU {

    static boolean drawDebuggingRectangles = false;

    static {
        //drawDebuggingRectangles = true;
    }

    /**
     * Indicates that the bitmap data is compressed. The bitmapComprHdr
     * field MUST be present if the NO_BITMAP_COMPRESSION_HDR (0x0400) flag is not set.
     */
    private final static int BITMAP_COMPRESSION = 0x0001;

    /**
     * Indicates that the bitmapComprHdr field is not present (removed for bandwidth efficiency to save 8 bytes).
     */
    private final static int NO_BITMAP_COMPRESSION_HDR = 0x0400;


    /**
     * Process the bitmap updates received on this channel
     *
     * @param surface Canvas that we are drawing on
     * @param data    data that was received
     * @throws RdesktopException Protocol error
     */
    @Override
    public void process(RdesktopCanvas surface, RdpPacket data)
            throws RdesktopException {

        int numberRectangles;
        int destLeft;
        int destTop;
        int destRight;
        int destBottom;
        int width;
        int height;
        int cx;
        int cy;
        int bitsPerPixel;
        int flags;              // The flags describing the format of the bitmap data in the bitmapDataStream field.
        int bitmapLength;
        int size;
        byte[] pixel;

        int minX;
        int minY;
        int maxX;
        int maxY;

        maxX = maxY = 0;
        minX = surface.getWidth();
        minY = surface.getHeight();

        numberRectangles = data.getLittleEndian16();

        /**
         * Variable-length array of TS_BITMAP_DATA (section 2.2.9.1.1.3.1.2.2) structures,
         *   each of which contains a rectangular clipping taken from the server-side screen
         *   frame buffer. The number of screen clippings in the array is specified by
         *   the numberRectangles field.
         */
        for (int i = 0; i < numberRectangles; i++) {

            destLeft = data.getLittleEndian16();
            destTop = data.getLittleEndian16();
            destRight = data.getLittleEndian16();
            destBottom = data.getLittleEndian16();

            width = data.getLittleEndian16();
            height = data.getLittleEndian16();
            bitsPerPixel = data.getLittleEndian16();
            flags = data.getLittleEndian16();
            bitmapLength = data.getLittleEndian16();

            int Bpp = (bitsPerPixel + 7) / 8;

            cx = destRight - destLeft + 1;
            cy = destBottom - destTop + 1;

            minX = Math.min(minX, destLeft);
            minY = Math.min(minY, destTop);
            maxX = Math.max(maxX, destRight);
            maxY = Math.max(maxY, destBottom);

            /* Server may limit bpp - this is how we find out */
            if (Options.getServerBpp() != bitsPerPixel) {
                logger.warn("Server limited color depth to " + bitsPerPixel + " bits");
                Options.setBpp(bitsPerPixel);
            }

            if (flags == 0) {
                logger.debug("flags == 0");

                int widthDepth = width * Bpp;

                pixel = new byte[widthDepth * height];

                for (int y = 0; y < height; y++) {
                    data.copyToByteArray(pixel, (height - y - 1) * widthDepth, data.getPosition(), widthDepth);
                    data.incrementPosition(widthDepth);
                }

                surface.displayImage(Bitmap.convertImage(pixel, Bpp), width, height, destLeft, destTop, cx, cy);
            } else {
                // 8 bytes are saved by not sending the header each time
                if ((flags & NO_BITMAP_COMPRESSION_HDR) != 0) {
                    logger.debug("flags & 0x400 != 0");
                    size = bitmapLength;
                } else {
                    logger.debug("flags & 0x400 == 0");
                    data.incrementPosition(2); // pad
                    size = data.getLittleEndian16();

                    data.incrementPosition(4); // line size, final size
                }
                if (Bpp == 1) {
                    pixel = Bitmap.decompress(width, height, size, data);
                    if (pixel != null)
                        surface.displayImage(Bitmap.convertImage(pixel, Bpp), width, height, destLeft, destTop, cx, cy);
                    else
                        logger.warn("Could not decompress bitmap cause: pixel is NULL");
                } else {
                    if (Options.getBitmapDecompressionStore() == Options.INTEGER_BITMAP_DECOMPRESSION) {
                        int[] pixelInts = Bitmap.decompressInt(width, height, size, data, Bpp);
                        if (pixelInts != null) {
                            surface.displayImage(pixelInts, width, height, destLeft, destTop, cx, cy);
                        } else {
                            logger.warn("Could not decompress bitmap cause: pixelInts is NULL");
                        }
                    } else if (Options.getBitmapDecompressionStore() == Options.BUFFERED_IMAGE_BITMAP_DECOMPRESSION) {
                        Image pixelImage = Bitmap.decompressImg(width, height, size, data, Bpp, null);
                        if (pixelImage != null) {
                            surface.displayImage(pixelImage, destLeft, destTop);
                        } else {
                            logger.warn("Could not decompress bitmap cause: pixelImage is NULL");
                        }
                    } else {
                        // Not a compressed image, display it
                        surface.displayCompressed(destLeft, destTop, width, height, size, data, Bpp);
                    }
                }
            }
        }

        /* ********* Useful test for identifying image boundaries ************ */
        if (drawDebuggingRectangles) {
            Graphics g = surface.getBackingStore().getGraphics();
            Color oldColor = g.getColor();
            g.setColor(Color.YELLOW);
            g.drawRect(minX, minY, maxX - minX + 1, maxY - minY + 1);
            g.setColor(oldColor);
            g.dispose();
        }
        surface.repaint(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
