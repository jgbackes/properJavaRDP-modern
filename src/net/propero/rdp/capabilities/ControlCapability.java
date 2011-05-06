package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.2.2 Control Capability Set (TS_CONTROL_CAPABILITYSET)
 *
 * <p/>
 *
 * The TS_CONTROL_CAPABILITYSET structure is used by the client to
 * advertise control capabilities and is fully described in [T128]
 * section 8.2.10. This capability is only sent from client to server
 * and the server ignores its contents.
 * 
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240568(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.2.2 Control Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class ControlCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int RDP_CAPLEN_CONTROL = 12;


    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return RDP_CAPLEN_CONTROL;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_CONTROL;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_CONTROL);
        data.setLittleEndian16(RDP_CAPLEN_CONTROL);

        data.setLittleEndian16(0); /* Control capabilities */
        data.setLittleEndian16(0); /* Remote detach */
        data.setLittleEndian16(2); /* Control interest */
        data.setLittleEndian16(2); /* Detach interest */
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
