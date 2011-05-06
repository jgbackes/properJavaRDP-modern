package net.propero.rdp.secondaryOrders;

import net.propero.rdp.OrderException;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.Rdp;
import net.propero.rdp.RdpPacket;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 2/8/11
 * Time: 9:47 AM
 */
public class SecondaryOrdersProcessor {

    static Logger logger = Logger.getLogger(Rdp.class);

    /* Secondary Drawing OrdersProcessor [MS-RDPEGDI] 2.2.2.2.1.2*/
    private static final int TS_CACHE_BITMAP_UNCOMPRESSED = 0;
    private static final int TS_CACHE_COLOR_TABLE = 1;
    private static final int TS_CACHE_BITMAP_COMPRESSED = 2;
    private static final int TS_CACHE_GLYPH = 3;
    private static final int TS_CACHE_BITMAP_UNCOMPRESSED_REV2 = 4;
    private static final int TS_CACHE_BITMAP_COMPRESSED_REV2 = 5;
    // private static final int TS_CACHE_BRUSH = 7;
    // private static final int TS_CACHE_BITMAP_COMPRESSED_REV3 = 5;

    private static CacheBitmapUncompressed cacheBitmapUncompressed = new CacheBitmapUncompressed();
    private static CacheColorTable cacheColorTable = new CacheColorTable();
    private static CacheBitmapCompressed cacheBitmapCompressed = new CacheBitmapCompressed();
    private static CacheGlyph cacheGlyph = new CacheGlyph();
    private static CacheBitmapRev2 cacheBitmapRev2 = new CacheBitmapRev2();

    /**
     * Handle secondary, or caching, orders
     *
     * @param data Packet containing secondary order
     * @throws net.propero.rdp.OrderException Problem parsing order
     * @throws RdesktopException              Problem processing the secondary order
     */
    public static void processSecondaryOrders(RdpPacket data)
            throws OrderException, RdesktopException {
        int length;
        int type;
        int flags;
        int next_order;

        length = data.getLittleEndian16();
        flags = data.getLittleEndian16();
        type = data.get8();

        next_order = data.getPosition() + length + 7;

        switch (type) {

            case TS_CACHE_BITMAP_UNCOMPRESSED:
                logger.debug("CacheBitmapUncompressed Order");
                cacheBitmapUncompressed.process(data);
                break;

            case TS_CACHE_COLOR_TABLE:
                logger.debug("CacheColorTable Order");
                cacheColorTable.process(data);
                break;

            case TS_CACHE_BITMAP_COMPRESSED:
                logger.debug("CacheBitmapCompressed Order");
                cacheBitmapCompressed.process(data);
                break;

            case TS_CACHE_GLYPH:
                logger.debug("CacheGlyph Order");
                cacheGlyph.process(data);
                break;

            case TS_CACHE_BITMAP_UNCOMPRESSED_REV2:
                logger.debug("BitmapRev2 Uncompressed Order");
                try {
                    cacheBitmapRev2.process(data, flags, false);
                } catch (IOException e) {
                    throw new RdesktopException(e.getMessage());
                } /* uncompressed */
                break;

            case TS_CACHE_BITMAP_COMPRESSED_REV2:
                logger.debug("BitmapRev2 Compressed Order");
                try {
                    cacheBitmapRev2.process(data, flags, true);
                } catch (IOException e) {
                    throw new RdesktopException(e.getMessage());
                } /* compressed */
                break;

            default:
                logger.warn("Unimplemented Secondary Order type " + type);
        }

        data.setPosition(next_order);
    }
}
