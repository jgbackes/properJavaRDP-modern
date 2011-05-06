package net.propero.rdp.pdus;

import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.cached.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;

/**
 * [MS-RDPBCGR] 2.2.9.1.1.4.4 Color Pointer PointerUpdate (TS_COLORPOINTERATTRIBUTE)
 *
 * The TS_COLORPOINTERATTRIBUTE structure represents a regular T.128 24 bpp color
 * pointer, as specified in [T128] section 8.14.3. This pointer update is used
 * for both monochrome and color pointers in RDP.
 *
 * @author jbackes
 * @see <a href="hhttp://msdn.microsoft.com/en-us/library/cc240618(v=PROT.10).aspx">
 *     [MS-RDPBCGR] Section 2.2.9.1.1.4.4 Color Pointer PointerUpdate (TS_COLORPOINTERATTRIBUTE)</a>
 */

public class ColorPointerUpdate implements PointerUpdate {

    protected static Logger logger = Logger.getLogger(ColorPointerUpdate.class);

    static {
        logger.setLevel(Level.WARN);
    }

    public static void process(RdesktopCanvas surface, RdpPacket data) throws RdesktopException {
        logger.debug("Rdp:processColorPointerPDU");

        // The zero-based cache entry in the pointer cache in which to store the pointer image.
        // The number of cache entries is specified using the Pointer Capability Set (section 2.2.7.1.5)
        int cacheIndex;

        // Point (section 2.2.9.1.1.4.1) structure containing the x-coordinates
        // and y-coordinates of the pointer hotspot.
        int hotSpotX;
        int hotSpotY;

        // The width of the pointer in pixels.
        // The maximum allowed pointer width is 96 pixels if the client
        // indicated support for large pointers by setting the
        // LARGE_POINTER_FLAG (0x00000001) in the Large Pointer
        // Capability Set (section 2.2.7.2.7). If the LARGE_POINTER_FLAG
        // was not set, the maximum allowed pointer width is 32 pixels.
        int width;

        // The height of the pointer in pixels.
        // The maximum allowed pointer height is 96 pixels if the client
        // indicated support for large pointers by setting the
        // LARGE_POINTER_FLAG (0x00000001) in the Large Pointer
        // Capability Set (section 2.2.7.2.7). If the LARGE_POINTER_FLAG
        // was not set, the maximum allowed pointer height is 32 pixels.
        int height;

        // The size in bytes of the andMaskData field.
        int lengthAndMask;

        // The size in bytes of the xorMaskData field.
        int lengthXorMask;

        // Contains the 24-bpp, bottom-up XOR mask scan-line data.
        // The XOR mask is padded to a 2-byte boundary for each encoded
        // scan-line. For example, if a 3x3 pixel cursor is being sent,
        // then each scan-line will consume 10 bytes (3 pixels per scan-line
        // multiplied by 3 bytes per pixel, rounded up to the next even number of bytes).
        byte[] xorMaskData;

        // Contains the 1-bpp, bottom-up AND mask scan-line data. The AND
        // mask is padded to a 2-byte boundary for each encoded scan-line.
        // For example, if a 7x7 pixel cursor is being sent, then each scan-line
        // will consume 2 bytes (7 pixels per scan-line multiplied by 1 bpp, rounded
        // up to the next even number of bytes).
        byte[] andMaskData;

        // Move the information from the packet into local variables
        cacheIndex = data.getLittleEndian16();
        hotSpotX = data.getLittleEndian16();
        hotSpotY = data.getLittleEndian16();
        width = data.getLittleEndian16();
        height = data.getLittleEndian16();
        lengthAndMask = data.getLittleEndian16();
        lengthXorMask = data.getLittleEndian16();

        logger.info("Creating and setting cursor " + cacheIndex);

        // create local arrays to hold the XOR and AND mask data
        xorMaskData = new byte[lengthAndMask];
        andMaskData = new byte[lengthXorMask];

        data.copyToByteArray(andMaskData, 0, data.getPosition(), lengthXorMask);
        data.incrementPosition(lengthXorMask);
        data.copyToByteArray(xorMaskData, 0, data.getPosition(), lengthAndMask);
        data.incrementPosition(lengthAndMask);

        // Create a usable cursor object from the data gleaned from the packet
        Cursor cursor = surface.createCursor(hotSpotX, hotSpotY, width, height, xorMaskData, andMaskData, cacheIndex);

        // Make this new cursor the visible cursor
        surface.setCursor(cursor);

        // Put the cursor in the cache
        CacheManager.getInstance().putCursor(cacheIndex, cursor);
    }
}
