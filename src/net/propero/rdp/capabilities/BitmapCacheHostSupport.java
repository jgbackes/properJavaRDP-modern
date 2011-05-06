package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.2.1 Bitmap Cache Host Support Capability Set (TS_BITMAPCACHE_HOSTSUPPORT_CAPABILITYSET)
 *
 * <p/>
 *
 * The TS_BITMAPCACHE_HOSTSUPPORT_CAPABILITYSET structure is
 * used to advertise support for persistent bitmap caching (see
 * [MS-RDPEGDI] section 3.1.1.1.1). This capability set is only sent
 * from server to client.
 *
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240557(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.2.1 Bitmap Cache Host Support Capability Set</a>
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc241669(v=prot.10).aspx">[MS-RDPEGDI] Remote Desktop Protocol: Graphics Device Interface (GDI) Acceleration Extensions</a>
 * @since ProperJavaRDP 3.0

*/
public class BitmapCacheHostSupport implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPSLEN_BITMAP_CACHE_HOST_SUPPORT = 8;

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPSLEN_BITMAP_CACHE_HOST_SUPPORT;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_BITMAPCACHE_HOSTSUPPORT;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire.
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_GENERAL);                   // capabilitySetType
        data.setLittleEndian16(CAPSLEN_BITMAP_CACHE_HOST_SUPPORT);  // lengthCapability

        data.setLittleEndian16(0x0001);             // cacheVersion - An 8-bit, unsigned integer. Cache version.
                                                    // This field MUST be set to TS_BITMAPCACHE_REV2 (0x01),
                                                    // which indicates support for the Revision 2 bitmap caches
                                                    // (see [MS-RDPEGDI] section 3.1.1.1.1).
        data.set8(0x00);                            // pad1 Padding. Values in this field MUST be ignored.
        data.setLittleEndian16(0x00);               // pad2 Padding. Values in this field MUST be ignored.
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
