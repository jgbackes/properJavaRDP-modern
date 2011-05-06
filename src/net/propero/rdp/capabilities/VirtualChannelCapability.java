package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.10 Virtual Channel Capability Set (TS_VIRTUALCHANNEL_CAPABILITYSET)
 * <p/>
 * <p/>
 * <p/>
 * The TS_VIRTUALCHANNEL_CAPABILITYSET structure is used to advertise virtual
 * channel support characteristics. This capability is sent by both client and server.
 * <p/>
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240551(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.1.10 Virtual Channel Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class VirtualChannelCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPSLEN_VIRTUALCHANNEL = 24;

    private static final int VCCAPS_NO_COMPR = 0x00000000;      // Virtual channel compression is not supported.
    private static final int VCCAPS_COMPR_SC = 0x00000001;      // Indicates to the server that virtual channel compression is supported by the client for server-to-client traffic. The highest compression level supported by the client is advertised in the Client Info PDU (section 2.2.1.11).
    private static final int VCCAPS_COMPR_CS_8K = 0x00000002;   // Indicates to the client that virtual channel compression is supported by the server for client-to-server traffic (the compression level is limited to RDP 4.0 bulk compression).


    @Override
    public int getSize() {
        return CAPSLEN_VIRTUALCHANNEL;
    }

    @Override
    public int getID() {
        return CAPSTYPE_VIRTUALCHANNEL;
    }

    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_VIRTUALCHANNEL);       // capabilitySetType
        data.setLittleEndian16(CAPSLEN_VIRTUALCHANNEL);        // lengthCapability

        data.setLittleEndian32(VCCAPS_NO_COMPR |                // flags
                VCCAPS_COMPR_SC |
                VCCAPS_COMPR_CS_8K);
        data.setLittleEndian32(0x0000);                         // VCChunkSize (optional)
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
