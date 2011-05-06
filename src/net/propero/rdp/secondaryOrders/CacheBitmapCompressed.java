package net.propero.rdp.secondaryOrders;

import net.propero.rdp.Bitmap;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.Rdp;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.cached.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 2/8/11
 * Time: 9:58 AM
 */
public class CacheBitmapCompressed {

    static Logger logger = Logger.getLogger(CacheBitmapCompressed.class);

    static {
        logger.setLevel(Level.WARN);
    }

    /**
     * Process a compressed bitmap and store in the bitmap cacheManager
     *
     * @param data Packet containing compressed bitmap
     * @throws net.propero.rdp.RdesktopException
     *          Problem processing the bitmap cacheManager
     */
    public void process(RdpPacket data)
            throws RdesktopException {

        int cacheId = data.get8();
        int pad1 = data.get8();
        int width = data.get8();
        int height = data.get8();
        int bpp = data.get8();
        int Bpp = (bpp + 7) / 8;
        int bufferSize = data.getLittleEndian16();
        int cacheIdx = data.getLittleEndian16();
        int pad2 = data.getLittleEndian16();
        int size = data.getLittleEndian16();
        int rowSize = data.getLittleEndian16();
        int finalSize = data.getLittleEndian16();

        logger.info("BMPCACHE(cx=" + width + ",cy=" + height + ",id=" +
                cacheId + ",idx=" + cacheIdx + ",bpp=" + bpp + ",size=" + size +
                ",pad1=" + pad1 + ",bufferSize=" + bufferSize + ",pad2=" + pad2 + ",rs=" +
                rowSize + ",fs=" + finalSize + ")");

        if (Bpp == 1) {
            byte[] pixel = Bitmap.decompress(width, height, size, data);
            if (pixel != null) {
                CacheManager.getInstance().putBitmap(cacheId, cacheIdx,
                        new Bitmap(Bitmap.convertImage(pixel, Bpp), width, height, 0, 0), 0);
            } else {
                logger.warn("Failed to decompress bitmap");
            }
        } else {
            int[] pixel = Bitmap.decompressInt(width, height, size, data, Bpp);
            if (pixel != null) {
                CacheManager.getInstance().putBitmap(cacheId, cacheIdx,
                        new Bitmap(pixel, width, height, 0, 0), 0);
            } else {
                logger.warn("Failed to decompress bitmap");
            }
        }
    }
}
