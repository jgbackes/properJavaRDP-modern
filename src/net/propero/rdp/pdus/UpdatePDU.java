package net.propero.rdp.pdus;

import net.propero.rdp.OrderException;
import net.propero.rdp.OrdersProcessor;
import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 2/18/11
 * Time: 1:18 AM
 */

public class UpdatePDU extends IncomingPDU {

    // PointerUpdate PDU Types
    public static final int UPDATETYPE_ORDERS = 0x0000;
    public static final int UPDATETYPE_BITMAP = 0x0001;
    public static final int UPDATETYPE_PALETTE = 0x0002;
    public static final int UPDATETYPE_SYNCHRONIZE = 0x0003;

    private int nextPacket = 0;

    public UpdatePDU(int nextPacket) {
        this.nextPacket = nextPacket;
    }

    @Override
    public void process(RdesktopCanvas surface, RdpPacket data)
            throws RdesktopException {

        int updateType = data.getLittleEndian16();

        switch (updateType) {

             // Orders PointerUpdate (see [MS-RDPEGDI] section 2.2.2.2)
            case (UPDATETYPE_ORDERS):
                data.incrementPosition(2); // size
                int numberOrders = data.getLittleEndian16();
                data.incrementPosition(2); // pad
                try {
                    OrdersProcessor.getInstance().processOrders(data, nextPacket, numberOrders);
                } catch (OrderException e) {
                    throw new RdesktopException(e.getMessage());
                }
                break;
            // Bitmap Graphics PointerUpdate (see [MS-RDPBCGR] section 2.2.9.1.1.3.1.2)
            case (UPDATETYPE_BITMAP):
                BitmapUpdatePDU bitmapUpdatePDU = new BitmapUpdatePDU();
                bitmapUpdatePDU.process(surface, data);
                break;
            // Palette PointerUpdate (see [MS-RDPBCGR] section 2.2.9.1.1.3.1.1)
            case (UPDATETYPE_PALETTE):
                PalettePDU palettePDU = new PalettePDU();
                palettePDU.process(surface, data);
                break;
            // Synchronize PointerUpdate (see [MS-RDPBCGR] section 2.2.9.1.1.3.1.3)
            case (UPDATETYPE_SYNCHRONIZE):
                // The TS_UPDATE_SYNC structure is an artifact of the T.128 protocol
                //    (see [T128] section 8.6.2) and SHOULD be ignored.
                break;
            default:
                logger.warn("Unimplemented PointerUpdate type " + updateType);
        }
    }
}
