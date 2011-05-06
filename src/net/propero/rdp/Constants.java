/* Constants.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Stores common constant values
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

@SuppressWarnings({"UnusedDeclaration"})
public class Constants {

    //   public static final int keylayout = 0x809; // UK... was US, 0x409

    //public static final boolean SystemExit = true;
    public static boolean encryption = true;
    public static boolean licence = true;

    public static final int TS_OSMAJORTYPE_UNSPECIFIED = 0x0000;   // Unspecified platform
    public static final int TS_OSMAJORTYPE_WINDOWS = 0x0001;       // Windows platform
    public static final int TS_OSMAJORTYPE_OS2 = 0x0002;           // OS/2 platform
    public static final int TS_OSMAJORTYPE_MACINTOSH = 0x0003;     // Macintosh platform
    public static final int TS_OSMAJORTYPE_UNIX = 0x0004;          // UNIX platform

    public static final int TS_OSMINORTYPE_UNSPECIFIED = 0x0000;    // Unspecified version
    public static final int TS_OSMINORTYPE_WINDOWS_31X = 0x0001;    // Windows 3.1x
    public static final int TS_OSMINORTYPE_WINDOWS_95 = 0x0002;     // Windows 95
    public static final int TS_OSMINORTYPE_WINDOWS_NT = 0x0003;     // Windows NT
    public static final int TS_OSMINORTYPE_OS2_V21 = 0x0004;        // OS/2 2.1
    public static final int TS_OSMINORTYPE_POWER_PC = 0x0005;       // PowerPC
    public static final int TS_OSMINORTYPE_MACINTOSH = 0x0006;      // Macintosh
    public static final int TS_OSMINORTYPE_NATIVE_XSERVER = 0x0007; // Native X Server
    public static final int TS_OSMINORTYPE_PSEUDO_XSERVER = 0x0008; // Pseudo X Server
    public static final int TS_OSMINORTYPE_WINDOWS_2000 = 0x0009;   // Windows 2000
    public static final int TS_OSMINORTYPE_WINDOWS_98 = 0x0000B;    // Windows 98
    public static final int TS_OSMINORTYPE_WINDOWS_Vista = 0x0000D; // Windows Vista
    public static final int TS_OSMINORTYPE_WINDOWS_XP = 0x0000E;    // Windows XP
    public static final int TS_OSMINORTYPE_WINDOWS_7 = 0x0000F;     // Windows 7

    public static int OS = TS_OSMAJORTYPE_UNSPECIFIED;
    public static int OS_MINOR = TS_OSMINORTYPE_UNSPECIFIED;


    /**
     * Order Negotiation constants.
     * <p/>
     * These numbers are indices to TS_ORDER_CAPABILITYSET.orderSupport, used
     * to advertise a node's capability to receive each type of encoded order.
     * Range is 0..TS_MAX_ORDERS-1.
     */
    public static final int TS_NEG_DSTBLT_INDEX = 0x0000;
    public static final int TS_NEG_PATBLT_INDEX = 0x0001;
    public static final int TS_NEG_SCRBLT_INDEX = 0x0002;
    public static final int TS_NEG_MEMBLT_INDEX = 0x0003;
    public static final int TS_NEG_MEM3BLT_INDEX = 0x0004;
    public static final int TS_NEG_ATEXTOUT_INDEX = 0x0005;
    public static final int TS_NEG_AEXTTEXTOUT_INDEX = 0x0006;

    public static final int TS_NEG_DRAWNINEGRID_INDEX = 0x0007;
    public static final int TS_NEG_LINETO_INDEX = 0x0008;
    public static final int TS_NEG_MULTI_DRAWNINEGRID_INDEX = 0x0009;

    public static final int TS_NEG_OPAQUERECT_INDEX = 0x000A;
    public static final int TS_NEG_SAVEBITMAP_INDEX = 0x000B;
    public static final int TS_NEG_WTEXTOUT_INDEX = 0x000C;
    public static final int TS_NEG_MEMBLT_R2_INDEX = 0x000D;
    public static final int TS_NEG_MEM3BLT_R2_INDEX = 0x000E;
    public static final int TS_NEG_MULTIDSTBLT_INDEX = 0x000F;
    public static final int TS_NEG_MULTIPATBLT_INDEX = 0x0010;
    public static final int TS_NEG_MULTISCRBLT_INDEX = 0x0011;
    public static final int TS_NEG_MULTIOPAQUERECT_INDEX = 0x0012;
    public static final int TS_NEG_FAST_INDEX_INDEX = 0x0013;
    public static final int TS_NEG_POLYGON_SC_INDEX = 0x0014;
    public static final int TS_NEG_POLYGON_CB_INDEX = 0x0015;
    public static final int TS_NEG_POLYLINE_INDEX = 0x0016;
    // unused 0x17
    public static final int TS_NEG_FAST_GLYPH_INDEX = 0x0018;
    public static final int TS_NEG_ELLIPSE_SC_INDEX = 0x0019;
    public static final int TS_NEG_ELLIPSE_CB_INDEX = 0x001A;
    public static final int TS_NEG_INDEX_INDEX = 0x001B;
    public static final int TS_NEG_WEXTTEXTOUT_INDEX = 0x001C;
    public static final int TS_NEG_WLONGTEXTOUT_INDEX = 0x001D;
    public static final int TS_NEG_WLONGEXTTEXTOUT_INDEX = 0x001E;

}
