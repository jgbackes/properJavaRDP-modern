/* DeskSaveOrder.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:40 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: 
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
package net.propero.rdp.orders;

import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.Utilities;
import net.propero.rdp.cached.CacheManager;

public class SaveBitmapOrder extends BoundsOrder {

    private int offset = 0;
    private int action = 0;

    public SaveBitmapOrder() {
        super();
    }

    public int getOffset() {
        return this.offset;
    }

    public int getAction() {
        return this.action;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public void reset() {
        super.reset();
        offset = 0;
        action = 0;
    }

    /**
     * Parse data describing a desktop save order, either saving the desktop to cacheManager, or drawing a section to screen
     *
     * @param data    Packet containing desktop save order
     * @param present Flags defining information available within the packet
     * @param delta   True if destination coordinates are described as relative to the source
     * @throws net.propero.rdp.RdesktopException
     *          Problem saving bitmap
     */
    public void processOrder(RdesktopCanvas surface, RdpPacket data,
                             int present, boolean delta) throws RdesktopException {

        int width;
        int height;

        if ((present & 0x01) != 0) {
            setOffset(data.getLittleEndian32());
        }

        if ((present & 0x02) != 0) {
            setLeft(Utilities.setCoordinate(data, getLeft(), delta));
        }

        if ((present & 0x04) != 0) {
            setTop(Utilities.setCoordinate(data, getTop(), delta));
        }

        if ((present & 0x08) != 0) {
            setRight(Utilities.setCoordinate(data, getRight(), delta));
        }

        if ((present & 0x10) != 0) {
            setBottom(Utilities.setCoordinate(data, getBottom(), delta));
        }

        if ((present & 0x20) != 0) {
            setAction(data.get8());
        }

        width = getRight() - getLeft() + 1;
        height = getBottom() - getTop() + 1;

        if (getAction() == 0) {
            int[] pixel = surface.getImage(getLeft(), getTop(), width, height);
            CacheManager.getInstance().putDesktop(getOffset(), width, height, pixel);
        } else {
            int[] pixel = CacheManager.getInstance().getDesktopInt(getOffset(), width, height);
            surface.putImage(getLeft(), getTop(), width, height, pixel);
        }
    }
}
