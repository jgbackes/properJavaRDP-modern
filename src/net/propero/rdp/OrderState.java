/* OrderState.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Storage of current order state, which may consist of one of each of a number of
 *          order types.
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

class OrderState {
    private int orderType = 0;
    private BoundsOrder boundsOrder = null;
    private DestBltOrder destBltOrder = null;
    private PatBltOrder patBltOrder = null;
    private ScreenBltOrder screenBltOrder = null;
    private LineToOrder lineToOrder = null;
    private RectangleOrder rectangleOrder = null;
    private SaveBitmapOrder saveBitmapOrder = null;
    private MemBltOrder memBltOrder = null;
    private Mem3BltOrder mem3BltOrder = null;
    private PolylineOrder polyLineOrder = null;
    private GlyphIndexOrder glyphIndexOrder = null;

    /**
     * Initialise this OrderState object, initialise one of each type of order
     */
    public OrderState() {
        boundsOrder = new BoundsOrder();
        destBltOrder = new DestBltOrder();
        patBltOrder = new PatBltOrder();
        screenBltOrder = new ScreenBltOrder();
        lineToOrder = new LineToOrder();
        rectangleOrder = new RectangleOrder();
        saveBitmapOrder = new SaveBitmapOrder();
        memBltOrder = new MemBltOrder();
        mem3BltOrder = new Mem3BltOrder();
        polyLineOrder = new PolylineOrder();
        glyphIndexOrder = new GlyphIndexOrder();
    }

    /**
     * Get the id of the current order type
     *
     * @return Order type id
     */
    public int getOrderType() {
        return this.orderType;
    }

    /**
     * Set the id of the current order type
     *
     * @param orderType Type id to set for current order
     */
    public void setOrderType(int orderType) {
        this.orderType = orderType;
    }

    /**
     * Retrieve the boundsOrder order stored within this state
     *
     * @return BoundsOrder from this state
     */
    public BoundsOrder getBoundsOrder() {
        return this.boundsOrder;
    }

    /**
     * Retrieve the dest blt order stored within this state
     *
     * @return DestBltOrder from this state
     */
    public DestBltOrder getDestBltOrder() {
        return this.destBltOrder;
    }

    /**
     * Retrieve the pattern blit order stored within this state
     *
     * @return PatBltOrder from this state
     */
    public PatBltOrder getPatBltOrder() {
        return this.patBltOrder;
    }

    /**
     * Retrieve the screen blit order stored within this state
     *
     * @return ScreenBltOrder from this state
     */
    public ScreenBltOrder getScreenBltOrder() {
        return this.screenBltOrder;
    }

    /**
     * Retrieve the lineToOrder order stored within this state
     *
     * @return LineOrder from this state
     */
    public LineToOrder getLineToOrder() {
        return this.lineToOrder;
    }

    /**
     * Retrieve the rectangle order stored within this state
     *
     * @return RectangleOrder from this state
     */
    public RectangleOrder getRectangleOrder() {
        return this.rectangleOrder;
    }

    /**
     * Retrieve the desktop save order stored within this state
     *
     * @return DeskSaveOrder from this state
     */
    public SaveBitmapOrder getSaveBitmapOrder() {
        return this.saveBitmapOrder;
    }

    /**
     * Retrieve the memory blit order stored within this state
     *
     * @return MemBltOrder from this state
     */
    public MemBltOrder getMemBltOrder() {
        return this.memBltOrder;
    }

    /**
     * Retrieve the tri blit order stored within this state
     *
     * @return TriBltOrder from this state
     */
    public Mem3BltOrder getMem3BltOrder() {
        return this.mem3BltOrder;
    }

    /**
     * Retrieve the multi-point lineToOrder order stored within this state
     *
     * @return PolyLineOrder from this state
     */
    public PolylineOrder getPolyLineOrder() {
        return this.polyLineOrder;
    }

    /**
     * Retrieve the glyphIndexOrder order stored within this state
     *
     * @return Text2Order from this state
     */
    public GlyphIndexOrder getGlyphIndexOrder() {
        return this.glyphIndexOrder;
    }

    /**
     * Reset all orders within this order state
     */
    public void reset() {
        boundsOrder.reset();
        destBltOrder.reset();
        patBltOrder.reset();
        screenBltOrder.reset();
        lineToOrder.reset();
        rectangleOrder.reset();
        saveBitmapOrder.reset();
        memBltOrder.reset();
        mem3BltOrder.reset();
        polyLineOrder.reset();
        glyphIndexOrder.reset();
    }
}
    
