package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.1.1 Color Table Cache Capability Set (TS_COLORTABLE_CAPABILITYSET)
 *
 * <p/>
 * 
 * The TS_COLORTABLE_CAPABILITYSET structure is an unused capability set that advertises
 *  the size of the color table cache used in conjunction with the
 * Cache Color Table Secondary Drawing Order (see section 2.2.2.2.1.2.4) and is based on the capability set
 * in [T128] section 8.2.8. This capability is sent by both client and server.
 * <p/>
 * Instead of being specified by the Color Table Cache Capability Set,
 * the existence of color table caching is tied to support for the
 * MemBlt (section 2.2.2.2.1.1.2.9) and Mem3Blt (section 2.2.2.2.1.1.2.10)
 * Primary Drawing orders. If support for these orders is advertised in the
 * Order Capability Set (see [MS-RDPBCGR] section 2.2.7.1.3), the existence
 * of a color table cache with entries for six palettes is implied when palettized
 * color is being used.
 * <p/>
 * <p/>
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc241564(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.1.1 Color Table Cache Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class ColorCacheCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPSLEN_COLORCACHE = 8;

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPSLEN_COLORCACHE;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_COLORCACHE;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {

        data.setLittleEndian16(CAPSTYPE_COLORCACHE);
        data.setLittleEndian16(CAPSLEN_COLORCACHE);

        data.setLittleEndian16(6); /* cacheManager size */
        data.setLittleEndian16(0); /* pad */
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
