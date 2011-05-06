/* UnicodeHandler.java
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
import net.propero.rdp.Utilities;
import org.apache.log4j.Logger;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class UnicodeHandler extends TypeHandler {

    protected static Logger logger = Logger.getLogger(ClipChannel.class);

    public boolean formatValid(int format) {
        logger.debug("UnicodeHandler:formatValid");
        return (format == CF_UNICODETEXT);
    }

    public boolean mimeTypeValid(String mimeType) {
        logger.debug("UnicodeHandler:mimeTypeValid");
        return mimeType.equals("text");
    }

    public int preferredFormat() {
        logger.debug("UnicodeHandler:preferredFormat");
        return CF_UNICODETEXT;
    }

    public void handleData(RdpPacket data, int length, ClipInterface c) {
        logger.debug("UnicodeHandler:handleData");
        String thingy = "";
        for (int i = 0; i < length; i += 2) {
            int aByte = data.getLittleEndian16();
            if (aByte != 0)
                thingy += (char) (aByte);
        }
        c.copyToClipboard(new StringSelection(thingy));
        //return(new StringSelection(thingy));
    }

    /**
     * 2.2.1.1 ClipboardFormatName
     * The ClipboardFormatName constants are null-terminated
     * ANSI strings ([X224/IEC-8859-1]) that specify the clipboard format.
     *
     * @return The name of this clipboard data type
     */
    public String name() {
        logger.debug("UnicodeHandler:name");
        return "CF_UNICODETEXT";
    }

    public byte[] fromTransferable(Transferable in) {
        logger.debug("UnicodeHandler:fromTransferable");
        String s;
        if (in != null) {
            try {
                s = (String) (in.getTransferData(DataFlavor.stringFlavor));
            } catch (Exception e) {
                s = e.toString();
            }

            // TODO: think of a better way of fixing this
            s = s.replace('\n', (char) 0x0a);
            //s = s.replaceAll("" + (char) 0x0a, "" + (char) 0x0d + (char) 0x0a);
            s = Utilities.strReplaceAll(s, "" + (char) 0x0a, "" + (char) 0x0d + (char) 0x0a);
            byte[] sBytes = s.getBytes();
            int length = sBytes.length;
            int lengthBy2 = length * 2;
            RdpPacket p = new RdpPacket(lengthBy2);
            for (byte sByte : sBytes) {
                p.setLittleEndian16(sByte);
            }
            sBytes = new byte[length * 2];
            p.copyToByteArray(sBytes, 0, 0, lengthBy2);
            return sBytes;
        }
        return null;
    }

    /* (non-Javadoc)
      * @see net.propero.rdp.virtualChannels.cliprdr.TypeHandler#sendData(java.awt.datatransfer.Transferable)
      */
    public void sendData(Transferable in, ClipInterface c) {
        logger.debug("UnicodeHandler:sendData");
        byte[] data = fromTransferable(in);
        c.sendClipboardData(data, data.length);
    }

}