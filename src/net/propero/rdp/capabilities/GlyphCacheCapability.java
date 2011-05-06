package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPGCGR] section 2.2.7.1.8 Glyph Cache Capability Set (TS_GLYPHCACHE_CAPABILITYSET)
 * <p/>
 * The TS_GLYPHCACHE_CAPABILITYSET structure advertises the glyph support
 * level and associated cache sizes. This capability is only sent from client to server.
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240565(v=prot.10).aspx">[MS-RDPBCGR] section 2.2.7.1.8 Glyph Cache Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class GlyphCacheCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPLEN_GLYPHCACHE = 0x34;

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPLEN_GLYPHCACHE;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_GLYPHCACHE;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */

    @Override
    public void setBytes(RdpPacket data) {

        data.setLittleEndian16(CAPSTYPE_GLYPHCACHE);
        data.setLittleEndian16(CAPLEN_GLYPHCACHE);

        /*
            An array of 10 TS_CACHE_DEFINITION structures.
            An ordered specification of the layout of each of the
            glyph caches with IDs 0 through to 9 ([MS-RDPEGDI] section 3.1.1.1.2).

            [MS-RDPGCGR] section 2.2.7.1.8.1 Cache Definition (TS_CACHE_DEFINITION)
                The TS_CACHE_DEFINITION structure specifies details about a particular
                cache in the Glyph Capability Set (section 2.2.7.1.8) structure.

                CacheEntries (2 bytes): A 16-bit, unsigned integer. The number of entries
                    in the cache. The maximum number of entries allowed in a cache is 254,
                    and the largest allowed maximum size of an element is 2048 bytes.
                CacheMaximumCellSize (2 bytes): A 16-bit, unsigned integer.
                    The maximum size in bytes of an entry in the cache.
         */
        data.setLittleEndian16(254);
        data.setLittleEndian16(4);

        data.setLittleEndian16(254);
        data.setLittleEndian16(4);

        data.setLittleEndian16(254);
        data.setLittleEndian16(8);

        data.setLittleEndian16(254);
        data.setLittleEndian16(8);

        data.setLittleEndian16(254);
        data.setLittleEndian16(16);

        data.setLittleEndian16(254);
        data.setLittleEndian16(32);

        data.setLittleEndian16(254);
        data.setLittleEndian16(64);

        data.setLittleEndian16(254);
        data.setLittleEndian16(128);

        data.setLittleEndian16(254);
        data.setLittleEndian16(256);

        data.setLittleEndian16(64);
        data.setLittleEndian16(2048);

        data.setLittleEndian32(0x01000100);     // FragCache - Fragment cache data. The maximum number
        // of entries allowed in the cache is 256, and the largest
        // allowed maximum size of an element is 256 bytes.

        data.setLittleEndian16(0x0002);         // GlyphSupportLevel - The level of glyph support.  SEE Table

        data.setLittleEndian16(0);              // pad2octets
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
