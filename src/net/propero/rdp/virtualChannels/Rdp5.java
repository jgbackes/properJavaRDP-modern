/* Rdp5.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:40 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handle RDP5 orders
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 * 
 * (See gpl.txt for details of the GNU General Public License.)
 * 
 */

package net.propero.rdp.virtualChannels;

import net.propero.rdp.OrderException;
import net.propero.rdp.OrdersProcessor;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.Rdp;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.pdus.BitmapUpdatePDU;
import net.propero.rdp.pdus.CachedPointerUpdate;
import net.propero.rdp.pdus.ColorPointerUpdate;
import net.propero.rdp.pdus.NullSystemPointer;
import net.propero.rdp.pdus.PalettePDU;


/**
 * [MS-RDPBCGR] Section 2.2.9.1.2 Server Fast-Path Update PDU (TS_FP_UPDATE_PDU)
 * <p/>
 * The TS_FP_UPDATE structure is used to describe and encapsulate the data for a
 * fast-path update sent from server to client. All fast-path updates conform to
 * this basic structure (see sections 2.2.9.1.2.1.1 to 2.2.9.1.2.1.10).
 * 
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240621(v=prot.10).aspx">
 *      [MS-RDPBCGR] Section 2.2.9.1.2 Server Fast-Path Update PDU</a>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class Rdp5 extends Rdp {

    public final static int FASTPATH_UPDATETYPE_BITMAP = 0x1;       // Fast-Path Bitmap Update (see section 2.2.9.1.2.1.2).
    public final static int FASTPATH_UPDATETYPE_PALETTE = 0x2;      // Fast-Path Palette Update (see section 2.2.9.1.2.1.1).
    public final static int FASTPATH_UPDATETYPE_SYNCHRONIZE = 0x3;  // Fast-Path Synchronize Update (see section 2.2.9.1.2.1.3).
    public final static int FASTPATH_UPDATETYPE_SURFCMDS = 0x4;     // Fast-Path Surface Commands Update (see section 2.2.9.1.2.1.10).
    public final static int FASTPATH_UPDATETYPE_PTR_NULL = 0x5;     // Fast-Path System Pointer Hidden Update (see section 2.2.9.1.2.1.5).
    public final static int FASTPATH_UPDATETYPE_PTR_DEFAULT = 0x6;  // Fast-Path System Pointer Default Update (see section 2.2.9.1.2.1.6).
    public final static int FASTPATH_UPDATETYPE_PTR_POSITION = 0x8; // Fast-Path Pointer Position Update (see section 2.2.9.1.2.1.4).
    public final static int FASTPATH_UPDATETYPE_COLOR = 0x9;        // Fast-Path Color Pointer Update (see section 2.2.9.1.2.1.7).
    public final static int FASTPATH_UPDATETYPE_CACHED = 0xA;       // Fast-Path Cached Pointer Update (see section 2.2.9.1.2.1.9).
    public final static int FASTPATH_UPDATETYPE_POINTER = 0xB;      // Fast-Path New Pointer Update (see section 2.2.9.1.2.1.8).

    private VChannels channels;

    /**
     * Initialise the RDP5 communications layer, with specified virtual channels
     *
     * @param channels Virtual channels for RDP layer
     */
    public Rdp5(VChannels channels) {
        super(channels);
        this.channels = channels;
    }

    /**
     * Process an RDP5 packet only short form
     *
     * @param s Packet to be processed
     * @param e True if packet is encrypted
     * @throws RdesktopException Protocol error
     * @throws OrderException    Drawing error
     * @throws CryptoException   Cryptographic error
     */
    public void rdp5Process(RdpPacket s, boolean e)
            throws RdesktopException, OrderException, CryptoException {
        rdp5Process(s, e, false);
    }

    /**
     * [MS-RDPBCGR] Section 2.2.9.1.2.1 Fast-Path Update (TS_FP_UPDATE)
     * <p/>
     * The TS_FP_UPDATE structure is used to describe and encapsulate the data for a
     * fast-path update sent from server to client. All fast-path updates conform to
     * this basic structure (see sections 2.2.9.1.2.1.1 to 2.2.9.1.2.1.10).
     *
     * @param s          Packet to be processed
     * @param encryption True if packet is encrypted
     * @param shortform  True if packet is of the "short" form
     * @throws RdesktopException Protocol error
     * @throws OrderException    Drawing error
     * @throws CryptoException   Cryptographic error
     */
    public void rdp5Process(RdpPacket s, boolean encryption,
                            boolean shortform) throws RdesktopException, OrderException,
            CryptoException {
        logger.debug("Processing RDP 5 order");

        int length, count;
        int type;
        int next;

        if (encryption) {
            s.incrementPosition(shortform ? 6 : 7 /* XXX HACK */); /* signature */
            byte[] data = new byte[s.size() - s.getPosition()];
            s.copyToByteArray(data, 0, s.getPosition(), data.length);
            byte[] packet = SecureLayer.decrypt(data);
            // TODO: It sure looks like encryption is not yet finished - 2011/02/23 JGB
        } else {
            logger.warn("Packet is not encrypted");
        }

        while (s.getPosition() < s.getEnd()) {
            type = s.get8();
            length = s.getLittleEndian16();

            next = s.getPosition() + length;
            logger.debug("RDP5: type = " + type);
            switch (type) {
                case 0: // [MS-RDPEGDI] Section 2.2.2.1 Orders Update (TS_UPDATE_ORDERS_PDU_DATA)
                    count = s.getLittleEndian16();
                    OrdersProcessor.getInstance().processOrders(s, next, count);
                    break;
                case FASTPATH_UPDATETYPE_BITMAP:
                    // The format of this field (as well as the possible values)
                    // is the same as the size field specified in the
                    // Fast-Path Update structure.
                    s.getLittleEndian16();
                    BitmapUpdatePDU bitmapUpdatePDU = new BitmapUpdatePDU();
                    bitmapUpdatePDU.process(surface, s);
                    break;
                case FASTPATH_UPDATETYPE_PALETTE:
                    // The format of this field (as well as the possible values)
                    // is the same as the size field specified in the
                    // Fast-Path Update structure.
                    s.getLittleEndian16();

                    PalettePDU palettePDU = new PalettePDU();
                    palettePDU.process(surface, s);
                    break;
                case FASTPATH_UPDATETYPE_SYNCHRONIZE:
                    logger.warn("FASTPATH_UPDATETYPE_SYNCHRONIZE not supported");
                    break;
                case FASTPATH_UPDATETYPE_SURFCMDS:
                    logger.warn("FASTPATH_UPDATETYPE_SURFCMDS not supported");
                    break;
                case FASTPATH_UPDATETYPE_PTR_NULL:
                    NullSystemPointer.process(surface, s);
                    break;
                case FASTPATH_UPDATETYPE_PTR_DEFAULT:
                    logger.warn("FASTPATH_UPDATETYPE_PTR_DEFAULT not supported");
                    break;
                case FASTPATH_UPDATETYPE_PTR_POSITION:
                    logger.warn("FASTPATH_UPDATETYPE_PTR_POSITION not supported");
                    break;
                case FASTPATH_UPDATETYPE_COLOR:
                    ColorPointerUpdate.process(surface, s);
                    break;
                case FASTPATH_UPDATETYPE_CACHED:
                    CachedPointerUpdate.process(surface, s);
                    break;
                case FASTPATH_UPDATETYPE_POINTER:
                    logger.warn("FASTPATH_UPDATETYPE_POINTER not supported");
                    break;
                default:
                    logger.warn("Unimplemented RDP5 opcode " + type);
            }

            s.setPosition(next);
        }
    }

    /**
     * Process an RDP5 packet from a virtual channel
     *
     * @param s         Packet to be processed
     * @param channelno Channel on which packet was received
     */
    void rdp5ProcessChannel(RdpPacket s, int channelno) {
        VChannel channel = channels.findChannelByChannelNumber(channelno);
        if (channel != null) {
            try {
                channel.process(s);
            } catch (Exception ignore) {
            }
        }
    }
}
