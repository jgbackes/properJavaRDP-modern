package net.propero.rdp.capabilities;

import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;

/**
 * [MS-RDPBCGR] Section 2.2.7 Capability Sets
 *
 * <p/>
 *
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240545(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7 Capability Sets</a>
 * @since ProperJavaRDP 3.0
 */
public interface Capability {

    public static final int CAPSTYPE_GENERAL =0x0001;                   // General Capability Set (section 2.2.7.1.1)
    public static final int CAPSTYPE_BITMAP =0x0002;                    // Bitmap Capability Set (section 2.2.7.1.2)
    public static final int CAPSTYPE_ORDER =0x0003;                     // Order Capability Set (section 2.2.7.1.3)
    public static final int CAPSTYPE_BITMAPCACHE =0x0004;               // Revision 1 Bitmap Cache Capability Set (section 2.2.7.1.4.1)
    public static final int CAPSTYPE_CONTROL =0x0005;                   // Control Capability Set (section 2.2.7.2.2)
    public static final int CAPSTYPE_ACTIVATION =0x0007;                // Window Activation Capability Set (section 2.2.7.2.3)
    public static final int CAPSTYPE_POINTER =0x0008;                   // Pointer Capability Set (section 2.2.7.1.5)
    public static final int CAPSTYPE_SHARE =0x0009;                     // Share Capability Set (section 2.2.7.2.4)
    public static final int CAPSTYPE_COLORCACHE =0x000A;                // Color Table Cache Capability Set (see [MS-RDPEGDI] section 2.2.1.1)
    public static final int CAPSTYPE_SOUND =0x000C;                     // Sound Capability Set (section 2.2.7.1.11)
    public static final int CAPSTYPE_INPUT =0x000D;                     // Input Capability Set (section 2.2.7.1.6)
    public static final int CAPSTYPE_FONT =0x000E;                      // Font Capability Set (section 2.2.7.2.5)
    public static final int CAPSTYPE_BRUSH =0x000F;                     // Brush Capability Set (section 2.2.7.1.7)
    public static final int CAPSTYPE_GLYPHCACHE =0x0010;                // Glyph Cache Capability Set (section 2.2.7.1.8)
    public static final int CAPSTYPE_OFFSCREENCACHE =0x0011;            // Offscreen Bitmap Cache CapabilitySet (section 2.2.7.1.9)
    public static final int CAPSTYPE_BITMAPCACHE_HOSTSUPPORT =0x0012;   // Bitmap Cache Host Support Capability Set (section 2.2.7.2.1)
    public static final int CAPSTYPE_BITMAPCACHE_REV2 =0x0013;          // Revision 2 Bitmap Cache Capability Set (section 2.2.7.1.4.2)
    public static final int CAPSTYPE_VIRTUALCHANNEL =0x0014;            // Virtual Channel Capability Set (section 2.2.7.1.10)
    public static final int CAPSTYPE_DRAWNINEGRIDCACHE =0x0015;         // DrawNineGrid Cache Capability Set ([MS-RDPEGDI] section 2.2.1.2)
    public static final int CAPSTYPE_DRAWGDIPLUS =0x0016;               // Draw GDI+ Cache Capability Set ([MS-RDPEGDI] section 2.2.1.3)
    public static final int CAPSTYPE_RAIL =0x0017;                      // Remote Programs Capability Set ([MS-RDPERP] section 2.2.1.1.1)
    public static final int CAPSTYPE_WINDOW =0x0018;                    // Window List Capability Set ([MS-RDPERP] section 2.2.1.1.2)
    public static final int CAPSTYPE_COMPDESK =0x0019;                  // Desktop Composition Extension Capability Set (section 2.2.7.2.8)
    public static final int CAPSTYPE_MULTIFRAGMENTUPDATE =0x001A;       // Multifragment PointerUpdate Capability Set (section 2.2.7.2.6)
    public static final int CAPTYPE_LARGE_POINTER =0x001B;              // Large Pointer Capability Set (section 2.2.7.2.7)

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    public int getSize();

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    public int getID();

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    public void setBytes(RdpPacket data);

    /**
     * process the server capabilities
     *
     * @param data Packet that contains the RDP packet data
     */
    public void processServerCapabilities(RdpPacket data) throws RdesktopException;
}
