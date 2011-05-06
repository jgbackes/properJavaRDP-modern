/* Brush.java
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

public class Brush {

    private int xOrigin = 0;
    private int yOrigin = 0;
    private int style = 0;
    private byte[] pattern = new byte[8];

    public Brush() {
    }

    public int getXOrigin() {
        return this.xOrigin;
    }

    public int getYOrigin() {
        return this.yOrigin;
    }

    public int getStyle() {
        return this.style;
    }

    public byte[] getPattern() {
        return this.pattern;
    }

    public void setXOrigin(int xOrigin) {
        this.xOrigin = xOrigin;
    }

    public void setYOrigin(int yOrigin) {
        this.yOrigin = yOrigin;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public void setPattern(byte[] pattern) {
        this.pattern = pattern;
    }

    public void reset() {
        xOrigin = 0;
        yOrigin = 0;
        style = 0;
        pattern = new byte[8];
    }
}
