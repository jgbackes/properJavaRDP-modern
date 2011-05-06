/* Orders.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Encapsulates an RDP order
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
package net.propero.rdp;

import net.propero.rdp.orders.BoundsOrder;
import net.propero.rdp.orders.DestBltOrder;
import net.propero.rdp.orders.GlyphIndexOrder;
import net.propero.rdp.orders.LineToOrder;
import net.propero.rdp.orders.Mem3BltOrder;
import net.propero.rdp.orders.MemBltOrder;
import net.propero.rdp.orders.PatBltOrder;
import net.propero.rdp.orders.PolylineOrder;
import net.propero.rdp.orders.RectangleOrder;
import net.propero.rdp.orders.SaveBitmapOrder;
import net.propero.rdp.orders.ScreenBltOrder;
import net.propero.rdp.secondaryOrders.SecondaryOrdersProcessor;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

@SuppressWarnings({"UnusedDeclaration"})
public class OrdersProcessor {
    static Logger logger = Logger.getLogger(OrdersProcessor.class);

    static {
        logger.setLevel(Level.WARN);
    }

    private OrderState orderState = null;

    private RdesktopCanvas surface = null;

    private final static boolean isDelta = true;
    private final static boolean isAbsolute = false;

    /* Control Flags [MS-RDPEGDI] 2.2.2.2.1.1.2 */
    private static final int TS_STANDARD = 0x01;
    private static final int RDP_ORDER_SECONDARY = 0x02;
    private static final int TS_BOUNDS = 0x04;
    private static final int TS_TYPE_CHANGE = 0x08;
    private static final int TS_DELTA_COORDINATES = 0x10;
    private static final int TS_ZERO_BOUNDS_DELTAS = 0x20;
    private static final int TS_ZERO_FIELD_BYTE_BIT0 = 0x40;
    private static final int TS_ZERO_FIELD_BYTE_BIT1 = 0x80;

    /* Primary Drawing Order [MS-RDPEGDI] 2.2.2.2.1.1 */
    private static final int TS_ENC_DSTBLT_ORDER = 0x00;
    private static final int TS_ENC_PATBLT_ORDER = 0x01;
    private static final int TS_ENC_SCRBLT_ORDER = 0x02;
    private static final int TS_ENC_MEMBLTR1_ORDER = 0x03;
    private static final int TS_ENC_MEM3BLTR1_ORDER = 0x04;
    private static final int TS_ENC_ATEXTOUT_ORDER = 0x05;
    private static final int TS_ENC_AEXTTEXTOUT_ORDER = 0x06;

    private static final int TS_ENC_DRAWNINEGRID_ORDER = 0x07;
    private static final int TS_ENC_MULTI_DRAWNINEGRID_ORDER = 0x08;

    private static final int TS_ENC_LINETO_ORDER = 0x09;
    private static final int TS_ENC_OPAQUERECT_ORDER = 0x0a;
    private static final int TS_ENC_SAVEBITMAP_ORDER = 0x0b;
    // unused 0x0c
    private static final int TS_ENC_MEMBLT_R2_ORDER = 0x0d;
    private static final int TS_ENC_MEM3BLT_R2_ORDER = 0x0e;
    private static final int TS_ENC_MULTIDSTBLT_ORDER = 0x0f;
    private static final int TS_ENC_MULTIPATBLT_ORDER = 0x10;
    private static final int TS_ENC_MULTISCRBLT_ORDER = 0x11;
    private static final int TS_ENC_MULTIOPAQUERECT_ORDER = 0x12;
    private static final int TS_ENC_FAST_INDEX_ORDER = 0x13;
    private static final int TS_ENC_POLYGON_SC_ORDER = 0x14;
    private static final int TS_ENC_POLYGON_CB_ORDER = 0x15;
    private static final int TS_ENC_POLYLINE_ORDER = 0x16;
    // unused 0x17
    private static final int TS_ENC_FAST_GLYPH_ORDER = 0x18;
    private static final int TS_ENC_ELLIPSE_SC_ORDER = 0x19;
    private static final int TS_ENC_ELLIPSE_CB_ORDER = 0x1a;
    private static final int TS_ENC_INDEX_ORDER = 0x1b;
    private static final int TS_ENC_WTEXTOUT_ORDER = 0x1c;
    private static final int TS_ENC_WEXTTEXTOUT_ORDER = 0x1d;
    private static final int TS_ENC_LONG_WTEXTOUT_ORDER = 0x1e;
    private static final int TS_ENC_LONG_WEXTTEXTOUT_ORDER = 0x1f;

    private static OrdersProcessor ordersProcessor;

    private OrdersProcessor() {
        orderState = new OrderState();
    }

    // Implement the singleton pattern
    public static synchronized OrdersProcessor getInstance() {
        if (null == ordersProcessor) {
            ordersProcessor = new OrdersProcessor();
        }
        return ordersProcessor;
    }

    public void resetOrderState() {
        this.orderState.reset();
        orderState.setOrderType(TS_ENC_PATBLT_ORDER);
    }

    private int inPresent(RdpPacket data, int flags, int size) {
        int present = 0;
        int bits;
        int i;

        if ((flags & TS_ZERO_FIELD_BYTE_BIT0) != 0) {
            size--;
        }

        if ((flags & TS_ZERO_FIELD_BYTE_BIT1) != 0) {

            if (size < 2) {
                size = 0;
            } else {
                size -= 2;
            }
        }

        for (i = 0; i < size; i++) {
            bits = data.get8();
            present |= (bits << (i * 8));
        }
        return present;
    }

    /**
     * Process a set of orders sent by the server
     *
     * @param data        Packet packet containing orders
     * @param nextPacket Offset of end of this packet (start of next)
     * @param nOrders    Number of orders sent in this packet
     * @throws OrderException    Unable to process an order
     * @throws RdesktopException Unable to process the Orders
     */
    public void processOrders(RdpPacket data, int nextPacket, int nOrders)
            throws OrderException, RdesktopException {

        int present;
        int orderFlags;
        int orderType = 0;
        int size;
        boolean delta;

        while (nOrders-- > 0) {

            orderFlags = data.get8();

            if ((orderFlags & TS_STANDARD) == 0) {
                throw new OrderException("Order parsing failed! orderFlags = " + orderFlags);
            }

            if ((orderFlags & RDP_ORDER_SECONDARY) != 0) {
                SecondaryOrdersProcessor.processSecondaryOrders(data);
            } else {
                if ((orderFlags & TS_TYPE_CHANGE) != 0) {
                    orderState.setOrderType(data.get8());
                }

                switch (orderState.getOrderType()) {
                    case TS_ENC_MEM3BLT_R2_ORDER:
                    case TS_ENC_INDEX_ORDER:
                        size = 3;
                        break;

                    case TS_ENC_PATBLT_ORDER:
                    case TS_ENC_MEMBLT_R2_ORDER:
                    case TS_ENC_LINETO_ORDER:
                        size = 2;
                        break;

                    default:
                        size = 1;
                }

                present = this.inPresent(data, orderFlags, size);

                // Check to see if there is a clipping rectangle in the flags
                if ((orderFlags & TS_BOUNDS) != 0) {

                    if ((orderFlags & TS_ZERO_BOUNDS_DELTAS) == 0) {
                        this.parseBounds(data, orderState.getBoundsOrder());
                    }

                    surface.setClip(orderState.getBoundsOrder());
                }


                // Delta is either isDelta (true) or isAbsolute (false)
                delta = ((orderFlags & TS_DELTA_COORDINATES) != 0);

                switch (orderState.getOrderType()) {

                    // 2.2.2.2.1.1.2.1 DstBlt (DSTBLT_ORDER)
                    case TS_ENC_DSTBLT_ORDER:
                        logger.debug("DstBlt Order");

                        DestBltOrder destBltOrder = orderState.getDestBltOrder();
                        destBltOrder.processOrder(surface, data, present, delta);
                        break;

                    // 2.2.2.2.1.1.2.3 PatBlt (PATBLT_ORDER)
                    case TS_ENC_PATBLT_ORDER:
                        logger.debug("PatBlt Order");

                        PatBltOrder patBltOrder = orderState.getPatBltOrder();
                        patBltOrder.processOrder(surface, data, present, delta);

                        break;

                    // 2.2.2.2.1.1.2.5 OpaqueRect (OPAQUERECT_ORDER)
                    case TS_ENC_OPAQUERECT_ORDER:
                        logger.debug("OpaqueRect Order");

                        RectangleOrder rectangleOrder = orderState.getRectangleOrder();
                        rectangleOrder.processOrder(surface, data, present, delta);
                        break;

                    // 2.2.2.2.1.1.2.7 ScrBlt (SCRBLT_ORDER)
                    case TS_ENC_SCRBLT_ORDER:
                        logger.debug("ScrBlt Order");

                        ScreenBltOrder screenBltOrder = orderState.getScreenBltOrder();
                        screenBltOrder.processOrder(surface, data, present, delta);
                        break;

                    // 2.2.2.2.1.1.2.9 MemBlt (MEMBLT_ORDER)
                    case TS_ENC_MEMBLT_R2_ORDER:
                        logger.debug("MemBlt Order");

                        MemBltOrder memBltOrder = orderState.getMemBltOrder();
                        memBltOrder.processOrder(surface, data, present, delta);
                        break;

                    // 2.2.2.2.1.1.2.10 Mem3Blt (MEM3BLT_ORDER)
                    case TS_ENC_MEM3BLT_R2_ORDER:
                        logger.debug("Mem3Blt Order");

                        Mem3BltOrder mem3BltOrder = orderState.getMem3BltOrder();
                        mem3BltOrder.processOrder(surface, data, present, delta);
                        break;

                    // 2.2.2.2.1.1.2.11 LineTo (LINETO_ORDER)
                    case TS_ENC_LINETO_ORDER:
                        logger.debug("LineTo Order");

                        LineToOrder lineToOrder = orderState.getLineToOrder();
                        lineToOrder.processOrder(surface, data, present, delta);
                        break;

                    // 2.2.2.2.1.1.2.12 SaveBitmap (SAVEBITMAP_ORDER)
                    case TS_ENC_SAVEBITMAP_ORDER:
                        logger.debug("SaveBitmap Order");

                        SaveBitmapOrder saveBitmapOrder = orderState.getSaveBitmapOrder();
                        saveBitmapOrder.processOrder(surface, data, present, delta);
                        break;

                    // 2.2.2.2.1.1.2.13 GlyphIndex (GLYPHINDEX_ORDER)
                    case TS_ENC_INDEX_ORDER:
                        logger.debug("GlyphIndex Order");

                        GlyphIndexOrder glyphIndexOrder = orderState.getGlyphIndexOrder();
                        glyphIndexOrder.processOrder(surface, data, present, delta);
                        break;

                    // 2.2.2.2.1.1.2.18 Polyline (POLYLINE_ORDER)
                    case TS_ENC_POLYLINE_ORDER:
                        logger.debug("Polyline Order");

                        PolylineOrder polylineOrder = orderState.getPolyLineOrder();
                        polylineOrder.processOrder(surface, data, present, delta);
                        break;

                    default:
                        logger.warn("Unimplemented Order type " + orderType);
                        return;
                }

                if ((orderFlags & TS_BOUNDS) != 0) {
                    surface.resetClip();
                    logger.debug("Reset clip");
                }
            }
        }
        if (data.getPosition() != nextPacket) {
            throw new OrderException("End not reached!");
        }
    }

    /**
     * Register an RdesktopCanvas with this OrdersProcessor object.
     * This surface is where all drawing orders will be carried
     * out.
     *
     * @param surface Surface to register
     */
    public void registerDrawingSurface(RdesktopCanvas surface) {
        this.surface = surface;
    }

    /* Process a bitmap cacheManager v2 order */


    /**
     * Parse a description for a bounding box
     *
     * @param data   Packet containing order defining bounding box
     * @param bounds BoundsOrder object in which to store description of bounds
     * @throws OrderException Problem parsing the Order
     */
    private void parseBounds(RdpPacket data, BoundsOrder bounds)
            throws OrderException {
        int present;

        present = data.get8();

        if ((present & 0x01) != 0) {
            bounds.setLeft(Utilities.setCoordinate(data, bounds.getLeft(), isAbsolute));
        } else if ((present & 0x10) != 0) {
            bounds.setLeft(Utilities.setCoordinate(data, bounds.getLeft(), isDelta));
        }

        if ((present & 0x02) != 0) {
            bounds.setTop(Utilities.setCoordinate(data, bounds.getTop(), isAbsolute));
        } else if ((present & 0x20) != 0) {
            bounds.setTop(Utilities.setCoordinate(data, bounds.getTop(), isDelta));
        }

        if ((present & 0x04) != 0) {
            bounds.setRight(Utilities.setCoordinate(data, bounds.getRight(), isAbsolute));
        } else if ((present & 0x40) != 0) {
            bounds.setRight(Utilities.setCoordinate(data, bounds.getRight(), isDelta));
        }

        if ((present & 0x08) != 0) {
            bounds.setBottom(Utilities.setCoordinate(data, bounds.getBottom(), isAbsolute));
        } else if ((present & 0x80) != 0) {
            bounds.setBottom(Utilities.setCoordinate(data, bounds.getBottom(), isDelta));
        }

        if (data.getPosition() > data.getEnd()) {
            throw new OrderException("Too far!");
        }
    }
}
