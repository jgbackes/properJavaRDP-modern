package net.propero.rdp.secondaryOrders;

import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.cached.CacheManager;

import java.awt.image.IndexColorModel;

/**
 * [MS-RDPEGDI] Section 2.2.2.2.1.2.4 Cache Color Table (CACHE_COLOR_TABLE_ORDER)
 * <p/>
 * The Cache Color Table Secondary Drawing Order is used by the server to instruct
 * the client to store a color table in a particular Color Table Cache entry.
 * Color tables are used in the MemBlt (section 2.2.2.2.1.1.2.9) and
 * Mem3Blt (section 2.2.2.2.1.1.2.10) Primary Drawing Orders.
 * <p/>
 * Support for color table caching is not specified in the Color Table Cache
 * Capability Set (section 2.2.1.1), but is instead implied by support for
 * the MemBlt (section 2.2.2.2.1.1.2.9) and Mem3Blt (section 2.2.2.2.1.1.2.10)
 * Primary Drawing Orders. If support for these orders is advertised in the
 * Order Capability Set (see [MS-RDPBCGR] section 2.2.7.1.3), the existence of
 * a color table cache with entries for six palettes is assumed when palettized
 * color is being used, and the Cache Color Table is used to update these palettes.
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc241617(v=PROT.10).aspx">[MS-RDPEGDI] Section 2.2.2.2.1.2.4 Cache Color Table</a>
 * @since ProperJavaRDP 3.0
 */

public class CacheColorTable {

    /**
     * Process and store details of a color cacheManager
     *
     * @param data Packet containing cacheManager information
     * @throws RdesktopException Problem processing the color cacheManager
     */
    public void process(RdpPacket data)
            throws RdesktopException {
        byte[] palette;

        byte[] red;
        byte[] green;
        byte[] blue;
        int j = 0;

        int cacheIndex = data.get8();                   // cacheIndex - An entry in the Cache Color Table where the color table MUST
        //      be stored. This value MUST be in the range 0 to 5 (inclusive).
        int numberColors = data.getLittleEndian16();    // numberColors - The number of Color Quad (section 2.2.2.2.1.2.4.1)
        //      structures in the colorTable field. This field MUST be set to 256 entries.

        palette = new byte[numberColors * 4];
        red = new byte[numberColors];
        green = new byte[numberColors];
        blue = new byte[numberColors];

        // colorTable (variable) - A Color Table composed of an array of Color Quad
        //      (section 2.2.2.2.1.2.4.1) structures. The number of entries in the array
        //      is given by the numberColors field.
        data.copyToByteArray(palette, 0, data.getPosition(), palette.length);
        data.incrementPosition(palette.length);
        for (int i = 0; i < numberColors; i++) {
            blue[i] = palette[j];
            green[i] = palette[j + 1];
            red[i] = palette[j + 2];

            j += 4; // palette[j+3] is pad
        }
        IndexColorModel indexColorModel = new IndexColorModel(8, numberColors, red, green, blue);
        CacheManager.getInstance().putColorMap(cacheIndex, indexColorModel);
    }
}
