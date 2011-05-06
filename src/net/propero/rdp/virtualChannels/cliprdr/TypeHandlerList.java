/* TypeHandlerList.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.4 $
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
package net.propero.rdp.virtualChannels.cliprdr;

import net.propero.rdp.RdpPacket;

import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.Iterator;

public class TypeHandlerList {

    private ArrayList<TypeHandler> handlers;

    public TypeHandlerList() {
        handlers = new ArrayList<TypeHandler>();
    }

    public void add(TypeHandler typeHandler) throws ClipboardException {
        if (typeHandler != null) {
            handlers.add(typeHandler);
        } else {
            throw new ClipboardException("Can't add null typeHandler");
        }
    }

    public TypeHandler getHandlerForFormat(int format) {
        TypeHandler result = null;

        for (TypeHandler handler : handlers) {
            if (handler.formatValid(format)) {
                result = handler;
                break;
            }
        }

        return result;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public TypeHandlerList getHandlersForMimeType(String mimeType) throws ClipboardException {
        TypeHandlerList result = new TypeHandlerList();

        for (TypeHandler handler : handlers) {
            if (handler.mimeTypeValid(mimeType)) {
                result.add(handler);
            }
        }

        return result;
    }

    public TypeHandlerList getHandlersForClipboard(DataFlavor[] dataTypes) throws ClipboardException {
        TypeHandlerList result = new TypeHandlerList();

        for (TypeHandler handler : handlers) {
            if (handler.clipboardValid(dataTypes)) {
                result.add(handler);
            }
        }

        return result;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void writeTypeDefinitions(RdpPacket data) {

        for (TypeHandler handler : handlers) {
            data.setLittleEndian32(handler.preferredFormat());
            data.incrementPosition(32);
        }
    }

    public int getCount() {
        return handlers.size();
    }

    public TypeHandler getFirst() {

        TypeHandler result = null;

        if (handlers.size() > 0) {
            result = handlers.get(0);
        }

        return result;
    }

    public Iterator getIterator() {
        return handlers.iterator();
    }
}
