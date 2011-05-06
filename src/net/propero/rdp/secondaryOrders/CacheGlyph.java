package net.propero.rdp.secondaryOrders;

import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.cached.CacheManager;
import net.propero.rdp.cached.Glyph;

/**
 * [MS-RDPEGDI] 2.2.2.2.1.2.5 Cache Glyph - Revision 1 (CACHE_GLYPH_ORDER)
 * <p/>
 * <p/>
 * The Cache Glyph - Revision 1 Secondary Drawing Order is used by the server to instruct the client to
 * store a glyph in a particular glyph cache entry (section 3.1.1.1.2). Support for glyph caching is
 * specified in the Glyph Cache Capability Set ([MS-RDPBCGR] section 2.2.7.1.8).
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc241619(v=PROT.10).aspx">[MS-RDPEGDI] 2.2.2.2.1.2.5 Cache Glyph</a>
 * @since ProperJavaRDP 3.0
 */
public class CacheGlyph {

    /**
     * Process a font caching order, and store font in the cacheManager
     *
     * @param data Packet containing font cacheManager order, with data for a series of glyphs representing a font
     * @throws net.propero.rdp.RdesktopException
     *          Problem processing font cacheManager
     */
    public void process(RdpPacket data)
            throws RdesktopException {

        Glyph glyph;

        int cacheId;
        int cGlyphs;
        int cacheIndex;                 // The index of the target entry in the destination glyph cache where
        //      the glyph data MUST be stored. This value MUST be greater than or equal
        //      to 0, and less than the maximum number of entries allowed in the
        //      destination glyph cache. The maximum number of entries allowed
        //      in each of the ten glyph caches is specified in the GlyphCache
        //      field of the Glyph Cache Capability Set ([MS-RDPBCGR] section 2.2.7.1.8).
        int x;                          // The X component of the coordinate that defines the origin of the character
        //      within the glyph bitmap. The top-left corner of the bitmap is (0, 0).
        int y;                          // The Y component of the coordinate that defines the origin of the
        //      character within the glyph bitmap. The top-left corner of the
        //      bitmap is (0, 0).
        int cx;                         // The width of the glyph bitmap in pixels.
        int cy;                         // The height of the glyph bitmap in pixels.
        byte[] aj;                      // A variable-sized byte array containing a 1-bit-per-pixel bitmap of
        //      the glyph. The individual scan lines are encoded in top-down order,
        //      and each scan line MUST be byte-aligned. Once the array has been
        //      populated with bitmap data, it MUST be padded to a double-word
        //      boundary (the size of the structure in bytes MUST be a multiple of 4).
        //      For examples of 1-bit-per-pixel encoded glyph bitmaps,
        //      see sections 4.6.1 and 4.6.2.
        int dataSize;

        cacheId = data.get8();          // The ID of the glyph cache in which the glyph data MUST be stored.
        //      This value MUST be in the range 0 to 9 (inclusive).
        cGlyphs = data.get8();          // The number of glyph entries in the glyphData field.

        for (int i = 0; i < cGlyphs; i++) {
            cacheIndex = data.getLittleEndian16();
            x = data.getLittleEndian16();
            y = data.getLittleEndian16();
            cx = data.getLittleEndian16();
            cy = data.getLittleEndian16();
            dataSize = (cy * ((cx + 7) / 8) + 3) & ~3;
            aj = new byte[dataSize];

            data.copyToByteArray(aj, 0, data.getPosition(), dataSize);
            data.incrementPosition(dataSize);
            glyph = new Glyph(cacheId, cacheIndex, x, y, cx, cy, aj);
            CacheManager.getInstance().putFont(glyph);
        }
    }
}
