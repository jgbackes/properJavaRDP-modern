package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.4.1 Revision 1 (TS_BITMAPCACHE_CAPABILITYSET)
 * <p/>
 * <p/>
 * <p/>
 * The TS_BITMAPCACHE_CAPABILITYSET structure is used to advertise
 * support for Revision 1 bitmap caches (see [MS-RDPEGDI] section 3.1.1.1.1).
 * This capability is only sent from client to server.
 * <p/>
 * In addition to specifying bitmap caching parameters in the Revision
 * 1 Bitmap Cache Capability Set, a client MUST also support the MemBlt and
 * Mem3Blt Primary Drawing Orders (see [MS-RDPEGDI] sections 2.2.2.2.1.1.2.9
 * and 2.2.2.2.1.1.2.10, respectively) in order to receive the Cache Bitmap (Revision 1)
 * Secondary Drawing Order (see [MS-RDPEGDI] section 2.2.2.2.1.2.2).
 * <p/>
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240559(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.1.4.1 Revision 1 (TS_BITMAPCACHE_CAPABILITYSET)</a>
 * @since ProperJavaRDP 3.0
 */
public class BitmapCacheCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPSLEN_BITMAPCACHE = 40;


    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPSLEN_BITMAPCACHE;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_BITMAPCACHE;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {

        data.setLittleEndian16(CAPSTYPE_BITMAPCACHE);
        data.setLittleEndian16(CAPSLEN_BITMAPCACHE);

        data.setLittleEndian32(0);              // pad1 - Padding. Values in this field MUST be ignored
        data.setLittleEndian32(0);              // pad2 - Padding. Values in this field MUST be ignored
        data.setLittleEndian32(0);              // pad3 - Padding. Values in this field MUST be ignored
        data.setLittleEndian32(0);              // pad4 - Padding. Values in this field MUST be ignored
        data.setLittleEndian32(0);              // pad5 - Padding. Values in this field MUST be ignored
        data.setLittleEndian32(0);              // pad6 - Padding. Values in this field MUST be ignored

        data.setLittleEndian16(200);            // Cache0Entries - The number of entries in Bitmap Cache 0 (maximum allowed value is 200 entries).
        data.setLittleEndian16(256);            // Cache0MaximumCellSize - The maximum cell size in Bitmap Cache 0.
        data.setLittleEndian16(600);            // Cache1Entries - The number of entries in Bitmap Cache 1 (maximum allowed value is 600 entries).
        data.setLittleEndian16(1024);           // Cache1MaximumCellSize - The maximum cell size in Bitmap Cache 1.
        data.setLittleEndian16(16384);          // Cache2Entries - The number of entries in Bitmap Cache 2 (maximum allowed value is 65535 entries).
        data.setLittleEndian16(4096);           // Cache2MaximumCellSize - The maximum cell size in Bitmap Cache 2.
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
