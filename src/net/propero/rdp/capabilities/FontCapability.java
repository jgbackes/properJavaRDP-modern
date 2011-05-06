package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.2.5 Font Capability Set (TS_FONT_CAPABILITYSET)
 *
 * <p/>
 * 
 * The TS_FONT_CAPABILITYSET structure is used to advertise font
 * support options. This capability is sent by both client and server.
 *
 * <p/>
 * 
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240571(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.2.5 Font Capability Set</a>
 * @since ProperJavaRDP 3.0

 */
public class FontCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPLEN_FONT = 8;
    private static final int FONTSUPPORT_FONTLIST = 0x0001;

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPLEN_FONT;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_FONT;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_FONT);
        data.setLittleEndian16(CAPLEN_FONT);

        data.setLittleEndian16(FONTSUPPORT_FONTLIST);   // fontSupportFlags
        data.setLittleEndian16(0);                      // pad2octetsA
    }

    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
