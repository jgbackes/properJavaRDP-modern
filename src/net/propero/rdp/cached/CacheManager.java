/* Cache.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handle caching of bitmaps, cursors, color maps,
 *          text and fonts
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

import net.propero.rdp.Bitmap;
import net.propero.rdp.Options;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.Rdp;
import net.propero.rdp.capabilities.BitmapCache2Capability;
import net.propero.rdp.persistentCache.PersistentCache;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.IOException;


public class CacheManager {

    protected static Logger logger = Logger.getLogger(Rdp.class);

    static {
        logger.setLevel(Level.WARN);
    }

    private static CacheManager cacheManager = null;

    private static final int RDPCACHE_COLORMAPSIZE = 0x06; // unified patch

    private Bitmap[][] bitmapCache = new Bitmap[3][600];
    private Cursor[] cursorCache = new Cursor[32];
    private Glyph[][] fontCache = new Glyph[12][256];
    private DataBlob[] textCache = new DataBlob[256];
    private int[] highDeskCache = new int[921600];      // 900 * 1024
    private int numberOfBitmapsInMemory[] = new int[3];
    private IndexColorModel[] colorCache = new IndexColorModel[RDPCACHE_COLORMAPSIZE];

    private CacheManager() {
    }

    public static synchronized CacheManager getInstance() {
        if (cacheManager == null) {
            cacheManager = new CacheManager();
        }

        return cacheManager;
    }

    void TOUCH(int id, int idx) {
        int stamp = PersistentCache.getStamp() + 1;
        PersistentCache.setStamp(stamp);

        bitmapCache[id][idx].usage = stamp;
    }

    /**
     * Remove the least-recently-used bitmap from the specified cacheManager
     *
     * @param cacheId Number of cacheManager from which to remove bitmap
     */
    void removeLRUBitmap(int cacheId) {
        int i;
        int cacheIdx = 0;
        int m = 0xffffffff;
        logger.debug("removeLRUBitmap: Removing cacheId = " + cacheId);

        for (i = 0; i < bitmapCache[cacheId].length; i++) {
            if ((bitmapCache[cacheId][i] != null) &&
                    (bitmapCache[cacheId][i].getBitmapData() != null) &&
                    (bitmapCache[cacheId][i].usage < m)) {
                cacheIdx = i;
                m = bitmapCache[cacheId][i].usage;
            }
        }

        bitmapCache[cacheId][cacheIdx] = null;
        --numberOfBitmapsInMemory[cacheId];
    }

    /**
     * Retrieve the indexed color model from the specified cacheManager
     *
     * @param cacheId ID of cacheManager from which to retrieve color model
     * @return Indexed color model for specified cacheManager
     * @throws RdesktopException Protocol error
     */
    public IndexColorModel getColorMap(int cacheId) throws RdesktopException {
        IndexColorModel map = null;
        if (cacheId < colorCache.length) {
            map = colorCache[cacheId];
            if (map != null) {
                return map;
            }
        }
        throw new RdesktopException("Could not get colorMap with cacheId=" + cacheId);
    }

    /**
     * Assign a color model to a specified cacheManager
     *
     * @param cache_id ID of cacheManager to which the color map should be added
     * @param map      Indexed color model to assign to the cacheManager
     * @throws RdesktopException Protocol error
     */
    public void putColorMap(int cache_id, IndexColorModel map)
            throws RdesktopException {

        if (cache_id >= colorCache.length) {
            throw new RdesktopException("Could not put colorMap with cache_id=" + cache_id);
        }

        colorCache[cache_id] = map;
    }

    /**
     * Retrieve a bitmap from the cacheManager
     *
     * @param cacheId  ID of cacheManager from which to retrieve bitmap
     * @param cacheIdx ID of bitmap to return
     * @return Bitmap stored in specified location within the cacheManager
     * @throws RdesktopException Protocol error
     */
    public Bitmap getBitmap(int cacheId, int cacheIdx) throws RdesktopException {

        logger.debug("get: cacheId = " + cacheId + ", cacheIdx = " + cacheIdx);
        Bitmap bitmap;

        if ((cacheId >= bitmapCache.length) || (cacheIdx >= bitmapCache[0].length)) {
            throw new RdesktopException("Could not get Bitmap!");
        } else {
            bitmap = bitmapCache[cacheId][cacheIdx];
        }

        return bitmap;
    }

    /**
     * Add a bitmap to the cacheManager
     *
     * @param cacheId   ID of cacheManager to which the Bitmap should be added
     * @param cacheIdx ID of location in specified cacheManager in which to store the Bitmap
     * @param bitmap    Bitmap object to store in cacheManager
     * @param stamp     Timestamp for storage of bitmap
     * @throws RdesktopException Protocol error
     */
    public void putBitmap(int cacheId, int cacheIdx, Bitmap bitmap, int stamp)
            throws RdesktopException {

        logger.debug("putBitmap: cacheId = " + cacheId + ", cacheIdx = " + cacheIdx);

        if ((cacheId < bitmapCache.length) && (cacheIdx < bitmapCache[0].length)) {
            bitmapCache[cacheId][cacheIdx] = bitmap;
            if (Options.isRdp5()) {
                if (++numberOfBitmapsInMemory[cacheId] > BitmapCache2Capability.BMPCACHE2_C2_CELLS) {
                    removeLRUBitmap(cacheId);
                }
                bitmapCache[cacheId][cacheIdx].usage = stamp;
            }

            bitmapCache[cacheId][cacheIdx] = bitmap;
        } else {
            throw new RdesktopException("Could not put Bitmap! cacheId=" + cacheId + ", cacheIdx=" + cacheIdx);
        }
    }

    /**
     * Retrieve a Cursor object from the cacheManager
     *
     * @param cacheIdx ID of cacheManager in which the Cursor is stored
     * @return Cursor stored in specified cacheManager
     * @throws RdesktopException Protocol error
     */
    public Cursor getCursor(int cacheIdx) throws RdesktopException {
        Cursor cursor;

        if (cacheIdx < cursorCache.length) {
            cursor = cursorCache[cacheIdx];
            if (cursor != null) {
                return cursor;
            }
        }
        throw new RdesktopException("Cursor not found");
    }

    /**
     * Assign a Cursor object to a specific cacheManager
     *
     * @param cache_idx ID of cacheManager to store Cursor in
     * @param cursor    Cursor object to assign to cacheManager
     * @throws RdesktopException Protocol error
     */
    public void putCursor(int cache_idx, Cursor cursor)
            throws RdesktopException {

        if (cache_idx < cursorCache.length) {
            cursorCache[cache_idx] = cursor;
        } else {
            throw new RdesktopException("Could not put Cursor!");
        }
    }

    /**
     * PointerUpdate the persistent bitmap cacheManager MRU information on exit
     */
    public void saveState() {
        int id, idx;

        for (id = 0; id < bitmapCache.length; id++) {
            if (PersistentCache.isPersistent(id)) {
                for (idx = 0; idx < bitmapCache[id].length; idx++) {
                    PersistentCache.touchBitmap(id, idx, bitmapCache[id][idx].usage);
                }
            }
        }
    }

    /**
     * Retrieve a Glyph for a specified character in a specified font
     *
     * @param font      ID of desired font for Glyph
     * @param character ID of desired character
     * @return Glyph representing character in font
     * @throws RdesktopException Protocol error
     */
    public Glyph getFont(int font, int character) throws RdesktopException {

        Glyph result = null;

        if ((font < fontCache.length) && (character < fontCache[0].length)) {
            result = fontCache[font][character];
        }

        if (result == null) {
            throw new RdesktopException("Could not get Font:" + font + ", " + character);
        }

        return result;
    }

    /**
     * Add a font to the cacheManager
     *
     * @param glyph Glyph containing references to relevant font
     * @throws RdesktopException Protocol error
     */
    public void putFont(Glyph glyph) throws RdesktopException {

        if ((glyph.getFont() < fontCache.length) && (glyph.getCharacter() < fontCache[0].length)) {
            fontCache[glyph.getFont()][glyph.getCharacter()] = glyph;
        } else {
            throw new RdesktopException("Could not put font");
        }
    }

    /**
     * Retrieve text stored in the cacheManager
     *
     * @param cache_id ID of cacheManager containing text
     * @return Text stored in specified cacheManager, represented as a DataBlob
     * @throws RdesktopException Protocol error
     */
    public DataBlob getText(int cache_id) throws RdesktopException {
        DataBlob result = null;
        if (cache_id < textCache.length) {
            result = textCache[cache_id];
        }

        if (null == result || result.getData() == null) {
            throw new RdesktopException("Could not get Text:" + cache_id);
        }

        return result;
    }

    /**
     * Store text in the cacheManager
     *
     * @param cache_id ID of cacheManager in which to store the text
     * @param entry    DataBlob representing the text to be stored
     * @throws RdesktopException Protocol error
     */
    public void putText(int cache_id, DataBlob entry) throws RdesktopException {
        if (cache_id < textCache.length) {
            textCache[cache_id] = entry;
        } else {
            throw new RdesktopException("Could not put Text");
        }
    }

    /**
     * Retrieve an image from the desktop cacheManager
     *
     * @param offset Offset of image data within desktop cacheManager
     * @param cx     Width of image
     * @param cy     Height of image
     * @return Integer pixel data for image requested
     * @throws RdesktopException Protocol error
     */
    public int[] getDesktopInt(int offset, int cx, int cy)
            throws RdesktopException {
        int length = cx * cy;
        int cacheData = 0;
        int[] data = new int[length];

        if (offset > highDeskCache.length)
            offset = 0;

        if (offset + length <= highDeskCache.length) {
            for (int i = 0; i < cy; i++) {
                System.arraycopy(highDeskCache, offset, data, cacheData, cx);
                offset += cx;
                cacheData += cx;
            }
            return data;
        }
        throw new RdesktopException("Could not get Bitmap");
    }

    /**
     * Store an image in the desktop cacheManager
     *
     * @param offset Location in desktop cacheManager to begin storage of supplied data
     * @param cx     Width of image to store
     * @param cy     Height of image to store
     * @param data   Array of integer pixel values representing image to be stored
     * @throws RdesktopException Protocol error
     */
    public void putDesktop(int offset, int cx, int cy, int[] data) throws RdesktopException {

        int length = cx * cy;
        int pdata = 0;

        if (offset > highDeskCache.length)
            offset = 0;

        if (offset + length <= highDeskCache.length) {
            for (int i = 0; i < cy; i++) {
                System.arraycopy(data, pdata, highDeskCache, offset, cx);
                offset += cx;
                pdata += cx;
            }
        } else {
            throw new RdesktopException("Could not put Desktop");
        }
    }
}
