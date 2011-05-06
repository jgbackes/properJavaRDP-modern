package net.propero.rdp.capabilities;

import net.propero.rdp.Options;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.persistentCache.PersistentCache;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.4.2 Revision 2 (TS_BITMAPCACHE_CAPABILITYSET_REV2)
 * <p/>
 * <p/>
 * <p/>
 * The TS_BITMAPCACHE_CAPABILITYSET_REV2 structure is used to advertise
 * support for Revision 2 bitmap caches (see [MS-RDPEGDI] section 3.1.1.1.1).
 * This capability is only sent from client to server.
 * <p/>
 * In addition to specifying bitmap caching parameters in the Revision 2 Bitmap
 * Cache Capability Set, a client MUST also support the MemBlt and Mem3Blt Primary
 * Drawing Orders (see [MS-RDPEGDI] sections 2.2.2.2.1.1.2.9 and 2.2.2.2.1.1.2.10,
 * respectively) in order to receive the Cache Bitmap (Revision 2) Secondary
 * Drawing Order (see [MS-RDPEGDI] section 2.2.2.2.1.2.3).
 * <p/>
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240560(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.1.4.2 Revision 2 (TS_BITMAPCACHE_CAPABILITYSET_REV2)</a>
 * @since ProperJavaRDP 3.0
 */
public class BitmapCache2Capability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPLEN_BITMAPCACHE2 = 40;
    private static final int BMPCACHE2_FLAG_PERSIST = (1 << 31);

    /* RDP bitmap cacheManager (version 2) constants */
    public static final int BMPCACHE2_C0_CELLS = 0x78;
    public static final int BMPCACHE2_C1_CELLS = 0x78;
    public static final int BMPCACHE2_C2_CELLS = 0x150;
    public static final int BMPCACHE2_NUM_PSTCELLS = 0x9f6;

    public static final int PERSISTENT_KEYS_EXPECTED_FLAG = 0x0001;   // Indicates that the client will send a Persistent Key List PDU during the Connection Finalization phase of the RDP Connection Sequence (see section 1.3.1.1 for an overview of the RDP Connection Sequence phases).
    public static final int ALLOW_CACHE_WAITING_LIST_FLAG = 0x0002; // Indicates that the client supports a cache waiting list. If a waiting list is supported, new bitmaps are cached on the second hit rather than the first (that is, a bitmap is sent twice before it is cached).


    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPLEN_BITMAPCACHE2;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_BITMAPCACHE_REV2;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_BITMAPCACHE_REV2);      // The type of the capability set. This field MUST be set to CAPSTYPE_BITMAPCACHE_REV2 (19).
        data.setLittleEndian16(CAPLEN_BITMAPCACHE2);            // The length in bytes of the capability data, including the size of the capabilitySetType and lengthCapability fields.

        data.setLittleEndian16(Options.isPersistentBitmapCaching() ? ALLOW_CACHE_WAITING_LIST_FLAG : 0); /* version */

        data.setBigEndian16(3);                                 // Number of bitmap caches (with a maximum allowed value of 5)

        /* max cell size for cacheManager 0 is 16x16, 1 = 32x32, 2 = 64x64, etc */
        data.setLittleEndian32(BMPCACHE2_C0_CELLS);
        data.setLittleEndian32(BMPCACHE2_C1_CELLS);

        if (PersistentCache.initialize(2)) {
            //logger.info("Persistent cacheManager initialized");
            data.setLittleEndian32(BMPCACHE2_NUM_PSTCELLS | BMPCACHE2_FLAG_PERSIST);
        } else {
            //logger.info("Persistent cacheManager not initialized");
            data.setLittleEndian32(BMPCACHE2_C2_CELLS);
        }
        data.incrementPosition(20);
    }

    /**
     * [MS-RDPBCGR] Section 2.2.7.1.4.2.1 Bitmap Cache Cell Info
     * <p/>
     * The TS_BITMAPCACHE_CELL_CACHE_INFO structure contains information about a bitmap cache on the client.
     */
    private class BitmapCellCacheInfo {
        int numEntries;
        boolean k;

        public BitmapCellCacheInfo(int numEntries, boolean k) {
            this.numEntries = numEntries;
            this.k = k;
        }

        public int getValue() {
            int result = numEntries;

            if (k) {
                result &= 0x80000000;
            }

            return result;
        }
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
