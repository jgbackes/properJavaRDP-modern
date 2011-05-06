package net.propero.rdp.pdus;

import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.pdus.IncomingPDU;
import net.propero.rdp.pdus.PointerUpdate;
import org.apache.log4j.Logger;

/**
 *
 * [MS-RDPBCGR] Section 2.2.9.1.1.4.3 System Pointer PointerUpdate (TS_SYSTEMPOINTERATTRIBUTE)
 *
 * The TS_SYSTEMPOINTERATTRIBUTE structure is used to hide the pointer or to
 * set its shape to that of the operating system default (see [T128] section 8.14.1)
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240617(v=PROT.10).aspx">
 *     [MS-RDPBCGR] Section 2.2.9.1.1.4.3 System Pointer PointerUpdate</a>
 */
public class SystemPointerUpdate implements PointerUpdate {

    protected static Logger logger = Logger.getLogger(IncomingPDU.class);

    // System Pointer Types
    private static final int RDP_NULL_POINTER = 0;

    public static void process(RdesktopCanvas surface, RdpPacket data) throws RdesktopException {
        int system_pointer_type = 0;

        data.getLittleEndian16(system_pointer_type); // in_uint16(s, system_pointer_type);
        switch (system_pointer_type) {
            case RDP_NULL_POINTER:
                logger.debug("RDP_NULL_POINTER");
                surface.setCursor(null);
                break;

            default:
                logger.warn("Unimplemented system pointer message 0x"
                        + Integer.toHexString(system_pointer_type));
        }
    }
}
