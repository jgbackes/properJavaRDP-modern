package net.propero.rdp.pdus;

import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.cached.CacheManager;

/**
 * [MS-RDPBCGR] Section 2.2.9.1.2.1.5 Fast-Path System Pointer Hidden Update (TS_FP_SYSTEMPOINTERHIDDENATTRIBUTE)
 *
 * The TS_FP_SYSTEMPOINTERHIDDENATTRIBUTE structure is the fast-path variant of
 * the TS_SYSTEMPOINTERATTRIBUTE (section 2.2.9.1.1.4.3) structure which contains
 * the SYSPTR_NULL (0x00000000) flag.
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/dd306368(v=PROT.10).aspx">
 *      [MS-RDPBCGR] Section 2.2.9.1.2.1.5 Fast-Path System Pointer Hidden Update</a>
 *
 */
public class NullSystemPointer implements PointerUpdate {

    public static void process(RdesktopCanvas surface, RdpPacket data) throws RdesktopException {
        // TODO: We should probably set another cursor here, like the X window system base cursor or something.
        surface.setCursor(CacheManager.getInstance().getCursor(0));
    }
}
