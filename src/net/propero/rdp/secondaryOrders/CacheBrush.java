package net.propero.rdp.secondaryOrders;

import net.propero.rdp.RdpPacket;

/**
 * [MS-RDPEGDI] Section 2.2.2.2.1.2.7 Cache Brush (CACHE_BRUSH_ORDER)
 *
 * The Cache Brush Secondary Drawing Order is used by the server to instruct the
 * client to store a brush in a particular Brush Cache entry. Support for brush
 * caching is specified in the Brush Cache Capability Set (see [MS-RDPBCGR] section 2.2.7.1.7).

 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc241623(v=PROT.10).aspx">[MS-RDPEGDI] Section 2.2.2.2.1.2.7 Cache Brush</a>
 * @since ProperJavaRDP 3.0
 */
public class CacheBrush {

    public void process(RdpPacket data) {

        int cacheEntry = data.get8();       // An 8-bit, unsigned integer. The entry in a specified
                                            //      Brush Cache where the brush data MUST be stored. This
                                            //      value MUST be in the range 0 to 63 (inclusive).

        int iBitmapFormat = data.get8();    // iBitmapFormat (1 byte): An 8-bit, unsigned integer.
                                            //      The color depth of the brush bitmap data. This field MUST
                                            //      be one of the following values.
                                            //
                                            // BMF_1BPP     0x01    1 bit per pixel
                                            // BMF_8BPP     0x03    8 bits per pixel
                                            // BMF_16BPP    0x04    15 or 16 bits per pixel
                                            // BMF_24BPP    0x05    24 bits per pixel
                                            // BMF_32BPP    0x06    32 bits per pixel
                                            //

        int cx = data.get8();
        int cy = data.get8();
        int style = data.get8();
        int iBytes = data.get8();
    }
}
