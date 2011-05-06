package net.propero.rdp.pdus;

import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;

/**
 * [MS-RDPBCGR] Section 2.2.9.1.1.4 Server Pointer PointerUpdate PDU (TS_POINTER_PDU)
 *
 * The Pointer PointerUpdate PDU is sent from server to client and is used to
 * convey pointer information, including pointers' bitmap images, use of system
 * or hidden pointers, use of cached cursors and position updates.

 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240614(v=prot.10).aspx">
 *     [MS-RDPBCGR] Section 2.2.9.1.1.4 Server Pointer PointerUpdate PDU</a>
 */
public class ServerPointerUpdatePDU extends IncomingPDU {

    // Pointer PDU Types
    private static final int TS_PTRMSGTYPE_SYSTEM = 0x0001;      // System Pointer PointerUpdate ([MS-RDPBCGR] section 2.2.9.1.1.4.3).
    private static final int TS_PTRMSGTYPE_POSITION = 0x0003;    // Pointer Position PointerUpdate ([MS-RDPBCGR] section 2.2.9.1.1.4.2).
    private static final int TS_PTRMSGTYPE_COLOR = 0x0006;       // Color Pointer PointerUpdate ([MS-RDPBCGR] section 2.2.9.1.1.4.4).
    private static final int TS_PTRMSGTYPE_CACHED = 0x0007;      // Cached Pointer PointerUpdate ([MS-RDPBCGR] section 2.2.9.1.1.4.6).
    private static final int TS_PTRMSGTYPE_POINTER = 0x0008;     // New Pointer PointerUpdate ([MS-RDPBCGR] section 2.2.9.1.1.4.5).


    @Override
    public void process(RdesktopCanvas surface, RdpPacket data)
            throws RdesktopException {
        
        int messageType = data.getLittleEndian16();
        data.incrementPosition(2);  // pad2Octets - Padding. Values in this field MUST be ignored.
        switch (messageType) {

            case TS_PTRMSGTYPE_SYSTEM:
                SystemPointerUpdate.process(surface, data);
                break;

            case TS_PTRMSGTYPE_POSITION:
                PointerPositionUpdate.process(surface, data);
                break;

            case TS_PTRMSGTYPE_COLOR:
                ColorPointerUpdate.process(surface, data);
                break;

            case TS_PTRMSGTYPE_CACHED:
                CachedPointerUpdate.process(surface, data);
                break;

            case TS_PTRMSGTYPE_POINTER:
                logger.warn("Unsupported message type: TS_PTRMSGTYPE_POINTER");
                break;
            
            default:
                break;
        }
    }
}
