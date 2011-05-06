package net.propero.rdp.updates;

import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.cached.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.9.1.1.4.6 Cached Pointer PointerUpdate (TS_CACHEDPOINTERATTRIBUTE)
 *
 * The TS_CACHEDPOINTERATTRIBUTE structure is used to instruct the client to change
 * the current pointer shape to one already present in the pointer cache.
 *
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240620(v=PROT.10).aspx">
 *     [MS-RDPBCGR] 2.2.9.1.1.4.6 Cached Pointer PointerUpdate</a>
 */

public class CachedPointerUpdate implements PointerUpdate {

    protected static Logger logger = Logger.getLogger(CachedPointerUpdate.class);

    static {
        logger.setLevel(Level.WARN);
    }
    
    public static void process(RdesktopCanvas surface, RdpPacket data) throws RdesktopException {
        logger.debug("Rdp.RDP_POINTER_CACHED");
        int cache_idx = data.getLittleEndian16();
        logger.info("Setting cursor " + cache_idx);
        surface.setCursor(CacheManager.getInstance().getCursor(cache_idx));
    }
}
