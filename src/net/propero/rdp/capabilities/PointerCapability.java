package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.5 Pointer Capability Set (TS_POINTER_CAPABILITYSET)
 * <p/>
 * <p/>
 * <p/>
 * The TS_POINTER_CAPABILITYSET structure advertises pointer cache sizes and flags
 * and is based on the capability set specified in [T128] section 8.2.11.
 * This capability is sent by both client and server.
 * <p/>
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240549(v=prot.10).aspx">[MS-RDPBCGR] Section 2.2.7.1.5 Pointer Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class PointerCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int RDP_CAPLEN_POINTER = 0x08;

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return RDP_CAPLEN_POINTER;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_POINTER;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_POINTER);
        data.setLittleEndian16(RDP_CAPLEN_POINTER);

        data.setLittleEndian16(0); /* Color pointer */
        data.setLittleEndian16(20); /* CacheManager size */
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
