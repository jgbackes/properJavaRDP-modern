package net.propero.rdp.persistentCache;

import net.propero.rdp.Bitmap;
import net.propero.rdp.Options;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.Utilities;
import net.propero.rdp.cached.CacheManager;
import net.propero.rdp.capabilities.BitmapCache2Capability;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/6/11
 * Time: 3:07 AM
 */

public class PersistentCache {

    protected static Logger logger = Logger.getLogger(PersistentCache.class);

    static int stamp;
    static File[] file = new File[8];
    static int bpp;
    static boolean enumerated = false;

    public static final int MAX_CELL_SIZE = 0x1000;  /* pixels */

    public static boolean isPersistent(int id) {
        return (id < 8 && file[id] != null);
    }

    public static int getStamp() {
        return stamp;
    }

    public static void setStamp(int stamp) {
        PersistentCache.stamp = stamp;
    }

    /* PointerUpdate usage info for a bitmap */
    public static void touchBitmap(int cacheId, int cacheIdx, int stamp) {
        logger.info("PersistentCache:touchBitmap");
        FileOutputStream fd;

        if (!isPersistent(cacheId) || cacheIdx >= BitmapCache2Capability.BMPCACHE2_NUM_PSTCELLS)
            return;

        try {
            fd = new FileOutputStream(file[cacheId]);

            fd.write(Utilities.toBigEndian32(stamp), 12 + cacheIdx * (bpp * MAX_CELL_SIZE + CellHeader.size()), 4);

        } catch (IOException e) {
            logger.warn("PersistentCache:touchBitmap IOException discarded");
        }
    }

    /* Load a bitmap from the persistent cacheManager */
    public static boolean getBitmap(int cacheId, int cacheIdx) throws IOException, RdesktopException {
        boolean result = false;

        logger.info("PersistentCache:getBitmap");
        byte[] celldata;
        FileInputStream fd;

        Bitmap bitmap;
        byte[] cellHead = null;

        if (Options.isPersistentBitmapCaching()) {

            if (isPersistent(cacheId) && cacheIdx < BitmapCache2Capability.BMPCACHE2_NUM_PSTCELLS) {

                fd = new FileInputStream(file[cacheId]);
                int offset = cacheIdx * (bpp * MAX_CELL_SIZE + CellHeader.size());
                fd.read(cellHead, offset, CellHeader.size());
                CellHeader c = new CellHeader(cellHead);

                celldata = new byte[c.length];
                fd.read(celldata);
                logger.debug("PersistentCache:getBitmap Loading bitmap from disk ("
                        + cacheId + ":" + cacheIdx + ")\n");

                bitmap = new Bitmap(celldata, c.width, c.height, 0, 0, Options.getBpp());

                CacheManager.getInstance().putBitmap(cacheId, cacheIdx, bitmap, c.stamp);
                result = true;
            } else {
                logger.warn("PersistentCache:getBitmap CacheManager index is not in range");
            }
        } else {
            logger.warn("PersistentCache:getBitmap caching is not enabled");
        }

        return result;
    }

    /* Store a bitmap in the persistent cacheManager */
    public static boolean putBitmap(int cacheId, int cacheIdx,
                                    byte[] bitmap_id,
                                    int width, int height,
                                    int length, byte[] data) throws IOException {
        logger.info("PersistentCache.putBitmap");
        FileOutputStream fd;
        CellHeader cellhdr = new CellHeader();

        if (!isPersistent(cacheId) || cacheIdx >= BitmapCache2Capability.BMPCACHE2_NUM_PSTCELLS)
            return false;

        cellhdr.bitmap_id = bitmap_id;

        cellhdr.width = width;
        cellhdr.height = height;
        cellhdr.length = length;
        cellhdr.stamp = 0;

        fd = new FileOutputStream(file[cacheId]);
        int offset = cacheIdx * (Options.getBpp() * MAX_CELL_SIZE + CellHeader.size());
        fd.write(cellhdr.toBytes(), offset, CellHeader.size());
        fd.write(data);

        return true;
    }

    /* list the bitmaps from the persistent cacheManager file */
    static int enumerate(int cacheId, int[] idlist) throws IOException, RdesktopException {
        logger.info("PersistentCache.enumerate");
        FileInputStream fd;
        int n, c = 0;
        CellHeader cellhdr = null;

        if (!(Options.isBitmapCaching() && Options.isPersistentBitmapCaching() && isPersistent(cacheId)))
            return 0;

        /* The server disconnects if the bitmap cacheManager content is sent more than once */
        if (enumerated)
            return 0;

        logger.debug("PersisentCache:enumeration... ");
        for (n = 0; n < BitmapCache2Capability.BMPCACHE2_NUM_PSTCELLS; n++) {
            fd = new FileInputStream(file[cacheId]);

            byte[] cellhead_data = new byte[CellHeader.size()];
            if (fd.read(cellhead_data, n * (bpp * MAX_CELL_SIZE + CellHeader.size()), CellHeader.size()) <= 0)
                break;

            cellhdr = new CellHeader(cellhead_data);

            int result = 0;
            for (int i = 0; i < cellhdr.bitmap_id.length; i++) {
                result += cellhdr.bitmap_id[i];
            }

            if (result != 0) {
                for (int i = 0; i < 8; i++) {
                    idlist[(n * 8) + i] = cellhdr.bitmap_id[i];
                }


                if (cellhdr.stamp != 0) {
                    /* Pre-caching is not possible with 8bpp because a colorMap is needed to load them */
                    if (Options.isPreCacheBitmaps() && (Options.getServerBpp() > 8)) {
                        if (getBitmap(cacheId, n))
                            c++;
                    }

                    stamp = Math.max(stamp, cellhdr.stamp);
                }
            } else {
                break;
            }
        }

        logger.info(n + " bitmaps in persistent cacheManager, " + c + " bitmaps loaded in memory\n");
        enumerated = true;
        return n;
    }

    /* initialise the persistent bitmap cacheManager */
    public static boolean initialize(int cacheId) {

        // Turn up the logging level
        logger.setLevel(Level.ALL);

        String filename;

        if (enumerated)
            return true;

        file[cacheId] = null;

        if (!(Options.isBitmapCaching() && Options.isPersistentBitmapCaching()))
            return false;

        bpp = Options.getBpp();

        filename = Utilities.getUserHomeDir().getAbsolutePath() + cacheId + "_" + bpp;
        logger.info("persistent bitmap cacheManager file: " + filename);

        File cacheDir = new File(Utilities.getUserHomeDir().getAbsolutePath() + "cacheManager");
        if (!cacheDir.exists() && !cacheDir.mkdir()) {
            logger.warn("failed to get/make cacheManager directory");
            return false;
        }

        File f = new File(filename);

        try {
            if (!f.exists() && !f.createNewFile()) {
                logger.warn("Could not create cacheManager file");
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        file[cacheId] = f;
        return true;
    }
}
