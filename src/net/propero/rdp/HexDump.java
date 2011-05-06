/* HexDump.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Manages debug information for all data
 *          sent and received, outputting in hex format
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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class HexDump {
    static Logger logger = Logger.getLogger(HexDump.class);

    private final static String TRAILING_SPACES = "                                                ";
    private final static String SPACE = " ";
    private final static String LEADING_ZEROS = "0000000";
    private final static String LEADING_ZERO = "0";
    private final static String INDEX_SEPARATOR = ": ";
    private final static String ASCII_UNPRINTABLE = ".";

    static {
        logger.setLevel(Level.DEBUG);
    }
    
    /**
     * Construct a HexDump object, sets logging level to Debug
     */
    public HexDump() {
    }

    /**
     * Encode data as hex and output as debug messages along with supplied custom message
     *
     * @param data Array of byte data to be encoded
     * @param msg  Message to include with outputted hex debug messages
     */
    public void encode(byte[] data, String msg) {
        int count = 0;
        StringBuilder index = new StringBuilder(64);
        StringBuilder number = new StringBuilder(16);
        StringBuilder ascii = new StringBuilder(32);

        logger.debug(msg);

        while (count < data.length) {
            index.setLength(0);
            ascii.setLength(0);

            index.append(LEADING_ZEROS).append(Integer.toHexString(count));
            index.delete(0, index.length() - 8);
            index.append(INDEX_SEPARATOR);

            for (int i = 0; i < 16 && count < data.length; i++, count++) {
                number.setLength(0);
                number.append(LEADING_ZERO).append(Integer.toHexString((data[count] & 0x000000ff)));
                number.delete(0, number.length() - 2);

                index.append(number).append(SPACE);

                if (data[count] >= 32 && data[count] <= 127) {
                    ascii.append((char) data[count]);
                } else {
                    ascii.append(ASCII_UNPRINTABLE);
                }
            }

            index.append(TRAILING_SPACES);
            index.delete(59, index.length());
            logger.debug(index.toString() + SPACE + ascii.toString());
        }
    }
}
