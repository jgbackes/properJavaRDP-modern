package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.2.4 Share Capability Set (TS_SHARE_CAPABILITYSET)
 *
 * <p/>
 * 
 * The TS_SHARE_CAPABILITYSET structure is used to advertise the channel ID of
 * the sender and is fully specified in [T128] section 8.2.12. This capability
 * is sent by both client and server.
 *
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240570(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.2.4 Share Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class ShareCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int RDP_CAPLEN_SHARE = 0x08;

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return RDP_CAPLEN_SHARE;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_SHARE;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_SHARE);
        data.setLittleEndian16(RDP_CAPLEN_SHARE);

        data.setLittleEndian16(0); /* userid */
        data.setLittleEndian16(0); /* pad */
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
