package net.propero.rdp.capabilities;

import net.propero.rdp.Options;
import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.2 Bitmap Capability Set (TS_BITMAP_CAPABILITYSET)
 * <p/>
 * <p/>
 * <p/>
 * The TS_BITMAP_CAPABILITYSET structure is used to advertise
 * bitmap-orientated characteristics and is based on the capability
 * set specified in [T128] section 8.2.4. This capability is sent by both client and server.
 * <p/>
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240554(v=prot.10).aspx">[MS-RDPBCGR] Section 2.2.7.1.2 Bitmap Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class BitmapCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPSLEN_BITMAP = 28;

    // Indicates support for lossy compression of 32 bpp bitmaps
    // by reducing color-fidelity on a per-pixel basis ([MS-RDPEGDI] section 3.1.9.1.4).
    private static final int DRAW_ALLOW_DYNAMIC_COLOR_FIDELITY = 0x02;

    // Indicates support for chroma subsampling when
    // compressing 32 bpp bitmaps ([MS-RDPEGDI] section
    private static final int DRAW_ALLOW_COLOR_SUBSAMPLING = 0x04;

    // Indicates that the client supports the removal of the alpha-channel
    // when compressing 32 bpp bitmaps. In this case the alpha is
    // assumed to be 0xFF, meaning the bitmap is opaque.
    private static final int DRAW_ALLOW_SKIP_ALPHA = 0x08;
    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPSLEN_BITMAP;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_BITMAP;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_BITMAP);        // capabilitySetType - The type of the capability set. This field MUST be set to CAPSTYPE_BITMAP (2).
        data.setLittleEndian16(CAPSLEN_BITMAP);         // lengthCapability - The length in bytes of the capability data, including the size of the capabilitySetType and lengthCapability

        data.setLittleEndian16(Options.getServerBpp()); // preferredBitsPerPixel - The server MUST set this field to the color depth of the session, while the client SHOULD set this field to the color depth requested in the Client Core Data (section 2.2.1.3.2).
        data.setLittleEndian16(1);                      // receive1BitPerPixel - Indicates whether the client can receive 1 bpp. This field is ignored and SHOULD be set to TRUE (0x0001).
        data.setLittleEndian16(1);                      // receive4BitsPerPixel - Indicates whether the client can receive 4 bpp. This field is ignored and SHOULD be set to TRUE (0x0001).
        data.setLittleEndian16(1);                      // receive8BitsPerPixel - Indicates whether the client can receive 8 bpp. This field is ignored and SHOULD be set to TRUE (0x0001).
        data.setLittleEndian16(Options.getWidth());     // desktopWidth - The width of the desktop in the session.
        data.setLittleEndian16(Options.getHeight());    // desktopHeight - The height of the desktop in the session.
        data.setLittleEndian16(0);                      // pad2octets - Padding. Values in this field MUST be ignored.
        data.setLittleEndian16(1);                      // desktopResizeFlag - Indicates whether resizing the desktop by using a Deactivation-Reactivation Sequence is supported (see section 1.3.1.3 for an overview of the Deactivation-Reactivation Sequence).

        // bitmapCompressionFlag - Indicates whether bitmap compression is supported. This field MUST be set to TRUE (0x0001) because support for compressed bitmaps is required for a connection to proceed.
        data.setLittleEndian16(Options.isBitmapCompression() ? 1 : 0);
        data.set8(0);                                   // highColorFlags - Client support for 16 bpp color modes. This field is ignored and SHOULD be set to 0.
        data.set8(0);                                   // drawingFlags - Flags describing support for 32 bpp bitmaps.
        data.setLittleEndian16(1);                      // multipleRectangleSupport - Indicates whether the use of multiple bitmap rectangles is supported in the Bitmap Update (section 2.2.9.1.1.3.1.2). This field MUST be set to TRUE (0x0001) because multiple rectangle support is required for a connection to proceed.
        data.setLittleEndian16(0);                      // pad2octetsB - Padding. Values in this field MUST be ignored.
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        int desktopWidth;
        int desktopHeight;
        int preferredBitsPerPixel;
        int desktopResizeFlag;
        int bitmapCompressionFlag;
        int highColorFlags;
        int drawingFlags;
        int multipleRectangleSupport;

        preferredBitsPerPixel = data.getLittleEndian16();
        data.incrementPosition(6);  // Skip the receiveBitPerPixel values as they should be ignored anyway
        desktopWidth = data.getLittleEndian16();    // The width of the desktop in the session.
        desktopHeight = data.getLittleEndian16();    // The height of the desktop in the session
        desktopResizeFlag = data.getLittleEndian16(); // Indicates whether resizing the desktop by using a Deactivation-Reactivation Sequence is supported (see section 1.3.1.3 for an overview of the Deactivation-Reactivation Sequence).
        bitmapCompressionFlag = data.getLittleEndian16(); // Indicates whether bitmap compression is supported.
        highColorFlags = data.get8(); // Client support for 16 bpp color modes
        drawingFlags = data.get8(); // Flags describing support for 32 bpp bitmaps
        multipleRectangleSupport = data.getLittleEndian16(); // Indicates whether the use of multiple bitmap rectangles is supported in the Bitmap Update


        // TODO: Force 24 bit for now.. Need to support all depths
        preferredBitsPerPixel = Math.max(preferredBitsPerPixel, 24);

        logger.debug("Setting desktop size and preferredBitsPerPixel to: " + desktopWidth + "x" + desktopHeight + "x" + preferredBitsPerPixel);

        /*
         * The server may limit preferredBitsPerPixel and change the size of the
         * desktop (for example when shadowing another session).
         */
        if (Options.getServerBpp() != preferredBitsPerPixel) {
            logger.warn("Color depth changed from " + Options.getServerBpp() + " to " + preferredBitsPerPixel);
            Options.setServerBpp(preferredBitsPerPixel);
        }

        if (Options.getWidth() != desktopWidth || Options.getHeight() != desktopHeight) {
            logger.warn("Screen size changed from " + Options.getWidth() + "x" + Options.getHeight() +
                    " to " + desktopWidth + "x" + desktopHeight);
            Options.setWidth(desktopWidth);
            Options.setHeight(desktopHeight);
        }

        logger.warn("desktopResizeFlag = " + desktopResizeFlag);

        if (bitmapCompressionFlag != 0x0001) {
            logger.warn("bitmapCompressionFlag MUST be 1. Is = " + bitmapCompressionFlag);
        }

        if (highColorFlags != 0) {
            logger.warn("highColorFlags SHOULD be 0. Is = " + highColorFlags);
        }

        logger.warn("drawingFlags = " + drawingFlags);

        if (multipleRectangleSupport != 0x0001) {
            logger.warn("multipleRectangleSupport MUST be 1. Is " + multipleRectangleSupport);
        }
    }
}
