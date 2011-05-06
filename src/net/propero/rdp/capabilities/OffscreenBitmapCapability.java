package net.propero.rdp.capabilities;

import net.propero.rdp.Rdp;
import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.9 Offscreen Bitmap Cache Capability Set (TS_OFFSCREEN_CAPABILITYSET)
 * <p/>
 * The TS_OFFSCREEN_CAPABILITYSET structure is used to advertise support for
 * offscreen bitmap caching (see [MS-RDPEGDI] section 3.1.1.1.5).
 * This capability is only sent from client to server.
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240550(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.1.9 Offscreen Bitmap Cache Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class OffscreenBitmapCapability implements Capability {

    protected static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }
    
    private static final int CAPSLEN_OFFSCREEN_BITMAP = 12;

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPSLEN_OFFSCREEN_BITMAP;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_OFFSCREENCACHE;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire.
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_OFFSCREENCACHE);    // capabilitySetType
        data.setLittleEndian16(CAPSLEN_OFFSCREEN_BITMAP);   // lengthCapability

        data.setLittleEndian32(0x00000001);                 // offscreenSupportLevel - Offscreen bitmap cache is supported.
        data.setLittleEndian16(7680);                       // offscreenCacheSize - The maximum size in kilobytes of the offscreen bitmap cache (largest allowed value is 7680 KB).
        data.setLittleEndian16(500);                        // offscreenCacheEntries - The maximum number of cache entries (largest allowed value is 500 entries).
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
