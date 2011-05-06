package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.6 Input Capability Set (TS_INPUT_CAPABILITYSET)
 * <p/>
 * The TS_INPUT_CAPABILITYSET structure is used to advertise support for input formats and devices. This capability is sent by both client and server.
 *
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240563(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.1.6 Input Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class InputCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int RDP_CAPLEN_INPUT = 88;

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return RDP_CAPLEN_INPUT;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_INPUT;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_INPUT);
        data.setLittleEndian16(RDP_CAPLEN_INPUT);

        data.setLittleEndian16(0x0001);             // inputFlags = INPUT_FLAG_SCANCODES
        data.setLittleEndian16(0x0000);             // pad2oct
        data.setLittleEndian32(0x00000409);         // Keyboard layout (active input locale identifier).
        data.setLittleEndian32(0x00000004);         // keyboardType = IBM enhanced (101- or 102-key) keyboard
        data.setLittleEndian32(0x00000000);         // Keyboard Sub Type
        data.setLittleEndian32(0x0000000F);         // FunctionKeys = 15

        data.setLittleEndian32(0x00000000);         // imeFileName - 64 bytes of zeros
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // ...
        data.setLittleEndian32(0x00000000);         // End of imeFileName
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
