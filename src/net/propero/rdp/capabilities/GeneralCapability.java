package net.propero.rdp.capabilities;

import net.propero.rdp.Constants;
import net.propero.rdp.Options;
import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.1 General Capability Set (TS_GENERAL_CAPABILITYSET)
 * <p/>
 * <p/>
 * <p/>
 * The TS_GENERAL_CAPABILITYSET structure is used to advertise general characteristics
 * and is based on the capability set specified in [T128] section 8.2.3.
 * This capability is sent by both client and server.
 * <p/>
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240549(v=prot.10).aspx">[MS-RDPBCGR] Section 2.2.7.1.1 General Capability Set</a>
 * @since ProperJavaRDP 3.0
 */

public class GeneralCapability implements Capability {

    static Logger logger = Logger.getLogger(GeneralCapability.class);

    static {
        logger.setLevel(Level.WARN);
    }

    private static final int CAPSLEN_GENERAL = 24;
    private static final int TS_CAPS_PROTOCOLVERSION = 0x0200;

    // RDP 5.0, 5.1, 5.2, 6.0, 6.1, and 7.0 support the following flags.
    private static final int FASTPATH_OUTPUT_SUPPORTED = 0x0001;    // Advertiser supports fast-path output.
    private static final int NO_BITMAP_COMPRESSION_HDR = 0x0400;    // Advertiser supports excluding the 8-byte Compressed Data Header (section 2.2.9.1.1.3.1.2.3) from the Bitmap Data (section 2.2.9.1.1.3.1.2.2) structure or the Cache Bitmap (Revision 2) Secondary Drawing Order ([MS-RDPEGDI] section 2.2.2.2.1.2.3).

    // RDP 5.1, 5.2, 6.0, 6.1, and 7.0 support the following additional flags. Flag Meaning
    private static final int LONG_CREDENTIALS_SUPPORTED = 0x0004;   // Advertiser supports long-length credentials for the user name, password, or domain name in the Save Session Info PDU (section 2.2.10.1).

    // RDP 5.2, 6.0, 6.1, and 7.0 support the following additional flags. Flag Meaning
    private static final int AUTORECONNECT_SUPPORTED = 0x0008;      // Advertiser supports auto-reconnection (section 5.5).
    private static final int ENC_SALTED_CHECKSUM = 0x0010;          // Advertiser supports salted MAC generation (see section 5.3.6.1.1).

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPSLEN_GENERAL;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_GENERAL;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire.
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_GENERAL);               // capabilitySetType
        data.setLittleEndian16(CAPSLEN_GENERAL);                // lengthCapability

        data.setLittleEndian16(Constants.OS);                   // osMajorType
        data.setLittleEndian16(Constants.OS_MINOR);             // osMinorType
        data.setLittleEndian16(TS_CAPS_PROTOCOLVERSION);        // protocolVersion
        data.setLittleEndian16(Options.isRdp5() ? 0x40d : 0);   // pad2octetsA
        data.setLittleEndian16(0);                              // generalCompressionTypes
        data.setLittleEndian16(0);                              // extraFlags
        data.setLittleEndian16(0);                              // updateCapabilityFlag
        data.setLittleEndian16(0);                              // remoteUnshareFlag
        data.setLittleEndian16(0);                              // generalCompressionLevel
        data.set8(0);                                           // refreshRectSupport
        data.set8(0);                                           // suppressOutputSupport
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        int os = data.getLittleEndian16();
        int osMinor = data.getLittleEndian16();
        int protocolVersion = data.getLittleEndian16();
        int pad2OctetsA = data.getLittleEndian16();
        int generalCompressionTypes = data.getLittleEndian16();
        int extraFlags = data.getLittleEndian16();
        int updateCompatibilityFlag = data.getLittleEndian16();
        int remoteUnshareFlag = data.getLittleEndian16();
        int generalCompressionLevel = data.getLittleEndian16();
        int refreshRectSupport = data.get8();
        int suppressOutputSupport = data.get8();

        logger.debug("os = " + os +
                ", osMinor = " + osMinor +
                ", protocolVersion = " + protocolVersion +
                ", pad2OctetsA = " + pad2OctetsA +
                ", generalCompressionTypes = " + generalCompressionTypes +
                ", extraFlags = " + extraFlags +
                ", updateCompatibilityFlag = " + updateCompatibilityFlag +
                ", remoteUnshareFlag = " + remoteUnshareFlag +
                ", generalCompressionLevel = " + generalCompressionLevel +
                ", refreshRectSupport = " + refreshRectSupport +
                ", suppressOutputSupport = " + suppressOutputSupport);

        // Validate the server capabilities
        if (protocolVersion != TS_CAPS_PROTOCOLVERSION) {
            logger.warn("Illegal protocolVersion = " + protocolVersion);
        }

        if (generalCompressionTypes != 0) {
            logger.warn("Illegal generalCompressionTypes = " + generalCompressionTypes);
        }

        // Display flags as text
        if ((extraFlags & FASTPATH_OUTPUT_SUPPORTED) == FASTPATH_OUTPUT_SUPPORTED) {
            logger.info("extraFlag FASTPATH_OUTPUT_SUPPORTED is set");
        }
        if ((extraFlags & NO_BITMAP_COMPRESSION_HDR) == NO_BITMAP_COMPRESSION_HDR) {
            logger.info("extraFlag NO_BITMAP_COMPRESSION_HDR is set");
        }
        if ((extraFlags & LONG_CREDENTIALS_SUPPORTED) == LONG_CREDENTIALS_SUPPORTED) {
            logger.info("extraFlag LONG_CREDENTIALS_SUPPORTED is set");
        }
        if ((extraFlags & AUTORECONNECT_SUPPORTED) == AUTORECONNECT_SUPPORTED) {
            logger.info("extraFlag AUTORECONNECT_SUPPORTED is set");
        }
        if ((extraFlags & ENC_SALTED_CHECKSUM) == ENC_SALTED_CHECKSUM) {
            logger.info("extraFlag ENC_SALTED_CHECKSUM is set");
        }

        if (updateCompatibilityFlag != 0) {
            logger.warn("Illegal updateCompatibilityFlag = " + updateCompatibilityFlag);
        }

        if (remoteUnshareFlag != 0) {
            logger.warn("Illegal updateCompatibilityFlag = " + remoteUnshareFlag);
        }

        if (generalCompressionLevel != 0) {
            logger.warn("Illegal generalCompressionLevel = " + generalCompressionLevel);
        }
    }
}
