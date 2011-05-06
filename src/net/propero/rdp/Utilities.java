/* Utilities.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.2 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Provide replacements for useful methods that were unavailable prior to
 *          Java 1.4 (Java 1.1 compliant).
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

import net.propero.rdp.orders.Brush;
import net.propero.rdp.orders.Pen;
import net.propero.rdp.preferences.PreferencesFrame;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.io.File;

public class Utilities {

    static Logger logger = Logger.getLogger(Rdp.class);

    /**
     * Write a 32-bit integer value to an array of bytes, length 4
     *
     * @param data  Modified by method to be a 4-byte array representing the parameter value
     * @param value Integer value to return as a little-endian 32-bit value
     */
    public static void setLittleEndian32(byte[] data, int value) {

        data[3] = (byte) ((value >>> 24) & 0xff);
        data[2] = (byte) ((value >>> 16) & 0xff);
        data[1] = (byte) ((value >>> 8) & 0xff);
        data[0] = (byte) (value & 0xff);
    }

    public static DataFlavor imageFlavor = DataFlavor.imageFlavor;


    /**
     * Retrieve a coordinate from a packet and return as an absolute integer coordinate
     *
     * @param data       Packet containing coordinate at current read position
     * @param coordinate Offset coordinate
     * @param delta      True if coordinate being read should be taken as relative to offset coordinate, false if absolute
     * @return Integer value of coordinate stored in packet, in absolute form
     */
    public static int setCoordinate(RdpPacket data, int coordinate, boolean delta) {
        byte change;

        if (delta) {
            change = (byte) data.get8();
            coordinate += (int) change;
            return coordinate;
        } else {
            coordinate = data.getLittleEndian16();
            return coordinate;
        }
    }

    public static int ROP2_S(int rop3) {
        return (rop3 & 0x0f);
    }

    public static int ROP2_P(int rop3) {
        return ((rop3 & 0x3) | ((rop3 & 0x30) >> 2));
    }

    /**
     * Read a color value from a packet
     *
     * @param data Packet containing color value at current read position
     * @return Integer color value read from packet
     */
    public static int setColor(RdpPacket data) {
        int color;
        int i;

        i = data.get8(); // in_uint8(s, i);
        color = i; // *color = i;
        i = data.get8(); // in_uint8(s, i);
        color |= i << 8; // *color |= i << 8;
        i = data.get8(); // in_uint8(s, i);
        color |= i << 16; // *color |= i << 16;

        return color;
    }


    /**
     * Parse data defining a brush and store brush information
     *
     * @param data    Packet containing brush data
     * @param brush   Brush object in which to store the brush description
     * @param present Flags defining the information available within the packet
     */
    public static void parseBrush(RdpPacket data, Brush brush, int present) {
        if ((present & 0x01) != 0) {
            int xOff = data.get8();
            brush.setXOrigin(xOff);
        }

        if ((present & 0x02) != 0) {
            int yOff = data.get8();
            brush.setYOrigin(yOff);
        }

        if ((present & 0x04) != 0) {
            int style = data.get8();
            brush.setStyle(style);
        }

        byte[] pat = brush.getPattern();

        if ((present & 0x08) != 0) {
            pat[0] = (byte) data.get8();
        }

        if ((present & 0x10) != 0) {
            for (int i = 1; i < 8; i++) {
                pat[i] = (byte) data.get8();
            }
        }

        brush.setPattern(pat);
    }

    /**
     * Parse a pen definition
     *
     * @param data    Packet containing pen description at current read position
     * @param pen     Pen object in which to store pen description
     * @param present Flags defining information available within packet
     * @return True if successful
     */
    public static boolean parsePen(RdpPacket data, Pen pen,
                                   int present) {
        if ((present & 0x01) != 0)
            pen.setStyle(data.get8());
        if ((present & 0x02) != 0)
            pen.setWidth(data.get8());
        if ((present & 0x04) != 0)
            pen.setColor(Utilities.setColor(data));

        return true; // return s_check(s);
    }

    /**
     * Interpret an integer as a 16-bit two's complement number, based on its binary representation
     *
     * @param val Integer interpretation of binary number
     * @return 16-bit two's complement value of input
     */
    public static int twosComplement16(int val) {
        return ((val & 0x8000) != 0) ? -((~val & 0xFFFF) + 1) : val;
    }

    public static byte[] toBigEndian32(int value) {
        byte[] out = new byte[4];
        out[0] = (byte) (value & 0xFF);
        out[1] = (byte) (value & 0xFF00);
        out[2] = (byte) (value & 0xFF0000);
        out[3] = (byte) (value & 0xFF000000);
        return out;
    }

    public static File getUserHomeDir() {

        String homeDirectory = System.getProperty("user.home");
        if (!homeDirectory.endsWith(File.separator)) {
            homeDirectory += File.separator;
        }
        homeDirectory += "ProperJavaRDP"; // Make a TaskTimer subdirectory

        File homeDirFile = new File(homeDirectory);
        if (!homeDirFile.exists()) {
            if (!homeDirFile.mkdir()) {
                logger.warn("Error creating home directory");
            }
        }

        return homeDirFile;
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     *
     * @param path Location of the image
     * @return An ImageIcon
     */
    public static ImageIcon getNewImageIcon(String path) {
        java.net.URL imgURL = Rdesktop.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            return null;
        }
    }

    public static JButton getPictureButton(String title,
                                           String toolTip,
                                           String picture,
                                           char mnemonic,
                                           ActionListener actionListener) {
        JButton button = new JButton(title);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setIcon(Utilities.getNewImageIcon(picture));
        button.setMnemonic(mnemonic);
        button.addActionListener(actionListener);
        button.setBorderPainted(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setOpaque(false);
        button.setBackground(Color.white);
        button.setToolTipText(toolTip);

        return button;
    }

    public static void addTab(JTabbedPane pane, JPanel panel, String title, String picture, char mnemonic) {

        if (panel == null) {
            panel = new JPanel(false);
            JLabel filler = new JLabel("This is where the settings for the " + title + " should go.");
            filler.setHorizontalAlignment(JLabel.CENTER);
            panel.setLayout(new GridLayout(1, 1));
            panel.add(filler);
        }

        pane.addTab(title, Utilities.getNewImageIcon(picture), panel, "DO nothing");
        pane.setMnemonicAt(pane.getTabCount() - 1, mnemonic);

        PreferencesFrame.setMaxW(480);
        PreferencesFrame.setMaxH(240);
    }

    public static JButton getButton(String title, char mnemonic, ActionListener actionListener) {
        JButton button = new JButton(title);
        button.setMnemonic(mnemonic);
        button.addActionListener(actionListener);
        button.setMargin(new Insets(2, 2, 2, 2));

        return button;
    }

    public static String strReplaceAll(String in, String find, String replace) {
        return in.replaceAll(find, replace);
    }

    public static String[] split(String in, String splitWith) {
        return in.split(splitWith);
    }
}
