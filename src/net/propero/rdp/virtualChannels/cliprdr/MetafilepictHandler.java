/* MetafilepictHandler.java
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

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MetafilepictHandler extends TypeHandler {

    /* Mapping Modes */
    public static final int MM_TEXT = 0x00000001;
    public static final int MM_LOMETRIC = 0x00000002;
    public static final int MM_HIMETRIC = 0x00000003;
    public static final int MM_LOENGLISH = 0x00000004;
    public static final int MM_HIENGLISH = 0x00000005;
    public static final int MM_TWIPS = 0x00000006;
    public static final int MM_ISOTROPIC = 0x00000007;
    public static final int MM_ANISOTROPIC = 0x00000008;

    String[] mapping_modes = {
            "undefined",
            "MM_TEXT",
            "MM_LOMETRIC",
            "MM_HIMETRIC",
            "MM_LOENGLISH",
            "MM_HIENGLISH",
            "MM_TWIPS",
            "MM_ISOTROPIC",
            "MM_ANISOTROPIC"
    };

    public boolean formatValid(int format) {
        return (format == CF_METAFILEPICT);
    }

    public boolean mimeTypeValid(String mimeType) {
        return mimeType.equals("image");
    }

    public int preferredFormat() {
        return CF_METAFILEPICT;
    }

    public Transferable handleData(RdpPacket data, int length) {
        String thingy = "";
        OutputStream out = null;

        //System.out.print("Metafile mapping mode = ");
        int mm = data.getLittleEndian32();
        //System.out.print(mapping_modes[mm]);
        int width = data.getLittleEndian32();
        //System.out.print(", width = " + width);
        int height = data.getLittleEndian32();
        //System.out.println(", height = " + height);

        try {
            out = new FileOutputStream("test.wmf");

            for (int i = 0; i < (length - 12); i++) {
                int aByte = data.get8();
                out.write(aByte);
                thingy += Integer.toHexString(aByte & 0xFF) + " ";
            }
            //System.out.println(thingy);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (new StringSelection(thingy));
    }

    public String name() {
        return "CF_METAFILEPICT";
    }


    public byte[] fromTransferable(Transferable in) {
        return null;
    }

    public void handleData(RdpPacket data, int length, ClipInterface c) {
        String thingy = "";
        OutputStream out = null;

        //System.out.print("Metafile mapping mode = ");
        int mm = data.getLittleEndian32();
        //System.out.print(mapping_modes[mm]);
        int width = data.getLittleEndian32();
        //System.out.print(", width = " + width);
        int height = data.getLittleEndian32();
        //System.out.println(", height = " + height);

        try {
            out = new FileOutputStream("test.wmf");

            for (int i = 0; i < (length - 12); i++) {
                int aByte = data.get8();
                out.write(aByte);
                thingy += Integer.toHexString(aByte & 0xFF) + " ";
            }
            //System.out.println(thingy);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
      * @see net.propero.rdp.virtualChannels.cliprdr.TypeHandler#sendData(java.awt.datatransfer.Transferable, net.propero.rdp.virtualChannels.cliprdr.ClipInterface)
      */
    public void sendData(Transferable in, ClipInterface c) {
        c.sendNull(ClipChannel.CB_FORMAT_DATA_RESPONSE, ClipChannel.CB_RESPONSE_FAIL);
    }

}
