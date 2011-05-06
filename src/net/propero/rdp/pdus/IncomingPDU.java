package net.propero.rdp.pdus;

import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.9.1.1 Slow-Path (T.128) Format
 *
 * Base class for PDU
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240604(v=PROT.10).aspx">
 *     [MS-RDPBCGR] Section 2.2.9.1.1 Slow-Path (T.128) Format</a>
 */

public abstract class IncomingPDU {

    protected static Logger logger = Logger.getLogger(BitmapUpdatePDU.class);

    static {
        logger.setLevel(Level.WARN);
    }

    public abstract void process(RdesktopCanvas surface, RdpPacket data) throws RdesktopException;
}
