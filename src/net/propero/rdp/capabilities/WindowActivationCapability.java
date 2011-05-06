package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.2.3 Window Activation Capability Set (TS_WINDOWACTIVATION_CAPABILITYSET)
 *
 * <p/>
 * 
 * The TS_WINDOWACTIVATION_CAPABILITYSET structure is used by the client to advertise
 * window activation characteristics capabilities and is fully specified in [T128]
 * section 8.2.9. This capability is only sent from client to server and the server
 *  ignores its contents.
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240569(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.2.3 Window Activation Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class WindowActivationCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int LENGTH_ACTIVATE = 12;


    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return LENGTH_ACTIVATE;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_ACTIVATION;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_ACTIVATION);
        data.setLittleEndian16(LENGTH_ACTIVATE);

        data.setLittleEndian16(0); /* Help key */
        data.setLittleEndian16(0); /* Help index key */
        data.setLittleEndian16(0); /* Extended help key */
        data.setLittleEndian16(0); /* Window activate */
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
