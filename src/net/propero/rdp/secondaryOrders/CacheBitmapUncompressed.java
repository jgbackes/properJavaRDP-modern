package net.propero.rdp.secondaryOrders;

import net.propero.rdp.Bitmap;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.cached.CacheManager;

/**
 * [MS-RDPEGDI] Section 2.2.2.2.1.2.2 Cache Bitmap - Revision 1 (CACHE_BITMAP_ORDER)
 * <p/>
 * The Cache Bitmap - Revision 1 Secondary Drawing Order is used by the server to
 * instruct the client to store a bitmap in a particular Bitmap Cache entry.
 * This order only supports memory-based bitmap caching. Support for the
 * Revision 1 bitmap caches (section 3.1.1.1.1) is specified in the Revision 1
 * Bitmap Cache Capability Set ([MS-RDPBCGR] section 2.2.7.1.4.1).
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc241615(v=PROT.10).aspx">[MS-RDPEGDI] Section 2.2.2.2.1.2.2 Cache Bitmap - Revision 1</a>
 * @since ProperJavaRDP 3.0
 */
public class CacheBitmapUncompressed {

    /**
     * @param data Packet containing raw bitmap data
     * @throws RdesktopException Process raw bitmap cacheManager
     */
    public void process(RdpPacket data)
            throws RdesktopException {
        int cacheId = data.get8();                      // The ID of the bitmap cache in which the bitmap data MUST be stored.
        //      This value MUST be in the range 0 to 2 (inclusive).
        data.incrementPosition(1);                      // Padding. Values in this field are arbitrary and MUST be ignored.
        int bitmapWidth = data.get8();                  // The width of the bitmap in pixels.
        int bitmapHeight = data.get8();                 // The height of the bitmap in pixels.
        int bitmapBitsPerPel = data.get8();             // The color depth of the bitmap data in bits per pixel.
        //      This field MUST be one of the following values.
        //      0x08 8-bit color depth.
        //      0x10 16-bit color depth.
        //      0x18 24-bit color depth.
        //      0x20 32-bit color depth.
        int bitmapLength = data.getLittleEndian16();    // The size in bytes of the data in the bitmapComprHdr
        //      and bitmapDataStream fields.
        int cacheIndex = data.getLittleEndian16();      // The index of the target entry in the destination bitmap cache
        //      (specified by the cacheId field) where the bitmap data MUST
        //      be stored. This value MUST be greater than or equal to 0
        //      and less than the maximum number of entries allowed in
        //      the destination bitmap cache. The maximum number of entries
        //      allowed in each individual bitmap cache is specified in
        //      the Revision 1 Bitmap Cache Capability Set ([MS-RDPBCGR]
        //      section 2.2.7.1.4.1) by the Cache0Entries, Cache1Entries,
        //      and Cache2Entries fields.
        int pdata = data.getPosition();

        data.incrementPosition(bitmapLength);

        int Bpp = (bitmapBitsPerPel + 7) / 8;
        byte[] inverted = new byte[bitmapWidth * bitmapHeight * Bpp];
        int pinverted = (bitmapHeight - 1) * (bitmapWidth * Bpp);
        for (int y = 0; y < bitmapHeight; y++) {
            data.copyToByteArray(inverted, pinverted, pdata, bitmapWidth * Bpp);
            pinverted -= bitmapWidth * Bpp;
            pdata += bitmapWidth * Bpp;
        }

        CacheManager.getInstance().putBitmap(cacheId, cacheIndex,
                new Bitmap(Bitmap.convertImage(inverted, Bpp), bitmapWidth, bitmapHeight, 0, 0), 0);
    }

}
