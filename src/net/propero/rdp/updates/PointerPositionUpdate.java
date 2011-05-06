package net.propero.rdp.updates;

import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.9.1.1.4.2 Pointer Position PointerUpdate (TS_POINTERPOSATTRIBUTE)
 *
 * The TS_POINTERPOSATTRIBUTE structure is used to indicate that the client
 * pointer MUST be moved to the specified position relative to the top-left
 * corner of the server's desktop (see [T128] section 8.14.4).
 * 
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240616(v=prot.10).aspx">
 *     [MS-RDPBCGR] Section 2.2.9.1.1.4.2 Pointer Position PointerUpdate</a>
 */
public class PointerPositionUpdate implements PointerUpdate {

    protected static Logger logger = Logger.getLogger(PointerUpdate.class);

    static {
        logger.setLevel(Level.WARN);
    }

    public static void process(RdesktopCanvas surface, RdpPacket data) throws RdesktopException {
        int x;
        int y;

        x = data.getLittleEndian16();
        y = data.getLittleEndian16();

        if (data.getPosition() <= data.getEnd()) {
            surface.movePointer(x, y);
        }
    }
}
