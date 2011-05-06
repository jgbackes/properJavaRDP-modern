package net.propero.rdp.capabilities;

import net.propero.rdp.Options;
import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static net.propero.rdp.Constants.*;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.3 Order Capability Set (TS_ORDER_CAPABILITYSET)
 * <p/>
 * <p/>
 * <p/>
 * The TS_ORDER_CAPABILITYSET structure advertises support for primary
 * drawing order-related capabilities and is based on the capability set
 * specified in [T128] section 8.2.5 (for more information about primary
 * drawing orders, see [MS-RDPEGDI] section 2.2.2.2.1.1).
 * This capability is sent by both client and server.
 * <p/>
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240556(v=PROT.10).aspx">[MS-RDPBCGR] 2.2.7.1.3 Order Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class OrderCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPSLEN_ORDER = 88;

    private static final int NEGOTIATE_ORDER_SUPPORT = 0x0002;
    private static final int ZERO_BOUNDS_DELTAS_SUPPORT = 0x0008;
    private static final int COLOR_INDEX_SUPPORT = 0x0020;
    private static final int SOLIDPATTERNBRUSHONLY = 0x0040;
    private static final int ORDERFLAGS_EXTRA_FLAGS = 0x0080;

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPSLEN_ORDER;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_ORDER;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        byte[] order_caps = new byte[32];
        order_caps[TS_NEG_DSTBLT_INDEX] = 1; /* dest blt */
        order_caps[TS_NEG_PATBLT_INDEX] = 1; /* pat blt */// nb no rectangle orders if this is 0
        order_caps[TS_NEG_SCRBLT_INDEX] = 1; /* screen blt */
        order_caps[TS_NEG_MEMBLT_INDEX] = (byte) (Options.isBitmapCaching() ? 1 : 0); /* memblt */
        order_caps[TS_NEG_MEM3BLT_INDEX] = 0; /* triblt */
        order_caps[TS_NEG_LINETO_INDEX] = 1; /* line */
        order_caps[TS_NEG_MULTI_DRAWNINEGRID_INDEX] = 1; /* line */
        order_caps[TS_NEG_OPAQUERECT_INDEX] = 1; /* rect */
        order_caps[TS_NEG_SAVEBITMAP_INDEX] = 1; /* desksave */
        order_caps[TS_NEG_MEMBLT_R2_INDEX] = 1; /* memblt */
        order_caps[TS_NEG_MEM3BLT_R2_INDEX] = 1; /* triblt */
        order_caps[TS_NEG_POLYGON_SC_INDEX] = (byte) (Options.isPolygonEllipseOrders() ? 1 : 0); /* polygon */
        order_caps[TS_NEG_POLYGON_CB_INDEX] = (byte) (Options.isPolygonEllipseOrders() ? 1 : 0); /* polygon2 */
        order_caps[TS_NEG_POLYLINE_INDEX] = 1; /* polyline */
        order_caps[TS_NEG_ELLIPSE_SC_INDEX] = (byte) (Options.isPolygonEllipseOrders() ? 1 : 0); /* ellipse */
        order_caps[TS_NEG_ELLIPSE_CB_INDEX] = (byte) (Options.isPolygonEllipseOrders() ? 1 : 0); /* ellipse2 */
        order_caps[TS_NEG_INDEX_INDEX] = 1; /* text2 */
        data.setLittleEndian16(CAPSTYPE_ORDER);
        data.setLittleEndian16(CAPSLEN_ORDER);

        data.incrementPosition(20); /* Terminal desc, pad */
        data.setLittleEndian16(1); // desktopSaveXGranularity
        data.setLittleEndian16(20); // desktopSaveYGranularity
        data.setLittleEndian16(0); // pad2octetsA
        data.setLittleEndian16(1); // maximumOrderLevel
        data.setLittleEndian16(0x0); // numberFonts - Per TD
        data.setLittleEndian16(
                NEGOTIATE_ORDER_SUPPORT |
                        ZERO_BOUNDS_DELTAS_SUPPORT |
                        COLOR_INDEX_SUPPORT); // orderFlags

        data.copyFromByteArray(order_caps, 0, data.getPosition(), 32); // OrdersProcessor supported

        data.incrementPosition(32);

        data.setLittleEndian16(0x6a1); /* Text capability flags */
        data.incrementPosition(6); /* Pad */
        data.setLittleEndian32(480 * 480); // The maximum usable size of bitmap space for bitmap packing in the SaveBitmap Primary Drawing Order
        data.setLittleEndian16(0x00); // pad2octetsC
        data.setLittleEndian16(0x00); // pad2octetsD
        data.setLittleEndian16(0x00); // ANSI code page descriptor being used by the client (for a list of code pages, see [MSDN-CP]).
        data.setLittleEndian16(0x00); // pad2octetsE
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
