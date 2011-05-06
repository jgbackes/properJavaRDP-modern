package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.7 Brush Capability Set (TS_BRUSH_CAPABILITYSET)
 * <p/>
 * <p/>
 * <p/>
 * The TS_BRUSH_CAPABILITYSET advertises client brush support.
 * This capability is only sent from client to server.
 * <p/>
 * <p/>
 *
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240564(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.1.7 Brush Capability Set (TS_BRUSH_CAPABILITYSET)</a>
 */
public class BrushCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPLEN_BRUSH = 8;

    private static final int BRUSH_DEFAULT = 0x00000000; // Support for solid-color and monochrome pattern brushes with no caching. This is an RDP 4.0 implementation.
    private static final int BRUSH_COLOR_8x8 = 0x00000001; // Ability to handle color brushes (4-bit or 8-bit in RDP 5.0; RDP 5.1, 5.2, 6.0, 6.1, and 7.0 also support 16-bit and 24-bit) and caching. Brushes are limited to 8-by-8 pixels.
    private static final int BRUSH_COLOR_FULL = 0x00000002; // Ability to handle color brushes (4-bit or 8-bit in RDP 5.0; RDP 5.1, 5.2, 6.0, 6.1, and 7.0 also support 16-bit and 24-bit) and caching. Brushes can have arbitrary dimensions.

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPLEN_BRUSH;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_BRUSH;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire.
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_BRUSH);              // capabilitySetType
        data.setLittleEndian16(CAPLEN_BRUSH);                // lengthCapability

        data.setLittleEndian32(BRUSH_COLOR_FULL);
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
