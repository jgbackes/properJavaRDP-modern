package net.propero.rdp.secondaryOrders;

import net.propero.rdp.Bitmap;
import net.propero.rdp.Options;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.Rdp;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.cached.CacheManager;
import net.propero.rdp.persistentCache.PersistentCache;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * [MS-RDPEGDI] Section 2.2.2.2.1.2.3 Cache Bitmap - Revision 2 (CACHE_BITMAP_REV2_ORDER)
 * <p/>
 * The Cache Bitmap - Revision 2 Secondary Drawing Order is used by the server to instruct
 * the client to store a bitmap in a particular Bitmap Cache entry. This order supports
 * persistent disk bitmap caching and uses a compact encoding format. Support for the
 * Revision 2 bitmap caches (section 3.1.1.1.1) is specified in the Revision 2 Bitmap
 * Cache Capability Set ([MS-RDPBCGR] section 2.2.7.1.4.2).
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc241616(v=prot.10).aspx">
 *     [MS-RDPEGDI] Section 2.2.2.2.1.2.3 Cache Bitmap - Revision 2</a>
 * @since ProperJavaRDP 3.0
 */
public class CacheBitmapRev2 {

    static Logger logger = Logger.getLogger(Rdp.class);

    /* RDP_BMPCACHE2_ORDER */
    private static final int ID_MASK = 0x0007;
    private static final int MODE_MASK = 0x0038;

    // Implies that the bitmap height is the same as the bitmap width.
    // If this flag is set, the bitmapHeight field MUST NOT be present.
    private static final int CBR2_HEIGHT_SAME_AS_WIDTH = 0x0080;
    private static final int CBR2_PERSISTENT_KEY_PRESENT = 0x0100;
    private static final int FLAG_51_UNKNOWN = 0x0800;  // TODO:
    private static final int MODE_SHIFT = 3;
    private static final int LONG_FORMAT = 0x80;
    private static final int BUFSIZE_MASK = 0x3FFF; /* or 0x1FFF? */

    /**
     * Process a bitmap cacheManager v2 order, storing a bitmap in the main cacheManager, and
     * the persistant cacheManager if so required
     *
     * @param data       Packet containing order and bitmap data
     * @param flags      Set of flags defining mode of order
     * @param compressed True if bitmap data is compressed
     * @throws net.propero.rdp.RdesktopException
     *                             Problem processing the cacheManager
     * @throws java.io.IOException Error writing the cacheManager to disk
     */
    public void process(RdpPacket data, int flags, boolean compressed)
            throws RdesktopException, IOException {
        
        Bitmap bitmap;
        int y;
        int cacheId;
        int cacheIdxLow;
        int width;
        int height;
        int Bpp;
        int cacheIdx;
        int bufferSize;
        byte[] bitmapData;
        byte[] bitmapId;

        bitmapId = new byte[8];
        cacheId = flags & ID_MASK;

        Bpp = Options.getBpp();
        if ((flags & CBR2_PERSISTENT_KEY_PRESENT) != 0) {
            data.copyToByteArray(bitmapId, 0, data.getPosition(), 8);
        }

        if ((flags & CBR2_HEIGHT_SAME_AS_WIDTH) != 0) {
            height = width = data.get8();
        } else {
            width = data.get8();
            height = data.get8();
        }

        bufferSize = data.getBigEndian16();
        bufferSize &= BUFSIZE_MASK;
        cacheIdx = data.get8();

        if ((cacheIdx & LONG_FORMAT) != 0) {
            cacheIdxLow = data.get8();
            cacheIdx = ((cacheIdx ^ LONG_FORMAT) << 8) + cacheIdxLow;
        }

        logger.info("CacheBitmapRev2(compr=" + compressed + ",flags=" + flags
                + ",cx=" + width + ",cy=" + height + ",id=" + cacheId
                + ",idx=" + cacheIdx + ",Bpp=" + Bpp + ",bs=" + bufferSize + ")");

        bitmapData = new byte[width * height * Bpp];
        int[] bmpdataInt;

        if (compressed) {
            if (Bpp == 1) {
                bmpdataInt = Bitmap.convertImage(Bitmap.decompress(width, height, bufferSize, data), Bpp);
            } else {
                bmpdataInt = Bitmap.decompressInt(width, height, bufferSize, data, Bpp);
            }

            if (bmpdataInt == null) {
                logger.debug("Failed to decompress bitmap data");
                return;
            }
            bitmap = new Bitmap(bmpdataInt, width, height, 0, 0);
        } else {
            int widthBpp = width * Bpp;
            
            for (y = 0; y < height; y++) {
                data.copyToByteArray(bitmapData, y * widthBpp, (height - y - 1) * widthBpp, widthBpp);
            }

            bitmap = new Bitmap(Bitmap.convertImage(bitmapData, Bpp), width, height, 0, 0);
        }

        CacheManager.getInstance().putBitmap(cacheId, cacheIdx, bitmap, 0);

        if ((flags & CBR2_PERSISTENT_KEY_PRESENT) != 0) {
            PersistentCache.putBitmap(cacheId, cacheIdx, bitmapId, width, height, width * height * Bpp, bitmapData);
        }
    }
}
