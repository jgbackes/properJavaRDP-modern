/* Subversion properties, do not modify!
 * 
 * $Date: 2008-02-13 23:40:22 -0800 (Wed, 13 Feb 2008) $
 * $Revision: 29 $
 * $Author: miha_vitorovic $
 * 
 * Author: Miha Vitorovic
 * 
 * Based on: (rdpsnd_libao.c)
 *  rdesktop: A Remote Desktop Protocol client.
 *  Sound Channel Process Functions
 *  Copyright (C) Matthew Chapman 2003
 *  Copyright (C) GuoJunBo guojunbo@ict.ac.cn 2003
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.propero.rdp.virtualChannels.rdpSoundOut;

/**
 * This is a container class used to hold information about
 * sounds that are sent to/from the server and client
 */
public class WaveFormatEx {
    public static final int MAX_CBSIZE = 256;

    private int wFormatTag;         // uint16
    private int nChannels;          // uint16
    private int nSamplesPerSec;     // uint32
    private int nAvgBytesPerSec;    // uint32
    private int nBlockAlign;        // uint16
    private int wBitsPerSample;     // uint16
    private int cbSize;             // uint16
    private byte cb[] = new byte[MAX_CBSIZE]; // uint8

    public int getwFormatTag() {
        return wFormatTag;
    }

    public void setwFormatTag(int wFormatTag) {
        this.wFormatTag = wFormatTag;
    }

    public int getnChannels() {
        return nChannels;
    }

    public void setnChannels(int nChannels) {
        this.nChannels = nChannels;
    }

    public int getnSamplesPerSec() {
        return nSamplesPerSec;
    }

    public void setnSamplesPerSec(int nSamplesPerSec) {
        this.nSamplesPerSec = nSamplesPerSec;
    }

    public int getnAvgBytesPerSec() {
        return nAvgBytesPerSec;
    }

    public void setnAvgBytesPerSec(int nAvgBytesPerSec) {
        this.nAvgBytesPerSec = nAvgBytesPerSec;
    }

    public int getnBlockAlign() {
        return nBlockAlign;
    }

    public void setnBlockAlign(int nBlockAlign) {
        this.nBlockAlign = nBlockAlign;
    }

    public int getwBitsPerSample() {
        return wBitsPerSample;
    }

    public void setwBitsPerSample(int wBitsPerSample) {
        this.wBitsPerSample = wBitsPerSample;
    }

    public int getCbSize() {
        return cbSize;
    }

    public void setCbSize(int cbSize) {
        this.cbSize = cbSize;
    }

    public byte[] getCb() {
        return cb;
    }

    public void setCb(byte[] cb) {
        this.cb = cb;
    }

    public String toString() {
        StringBuffer out = new StringBuffer(256);
        out.append("[wFormatTag: ").append(wFormatTag).append(", nChannels: ")
                .append(nChannels).append(", nSamplesPerSec: ");
        out.append(nSamplesPerSec).append(", nAvgBytesPerSec: ").append(
                nAvgBytesPerSec).append(", nBlockAlign: ");
        out.append(nBlockAlign).append(", wBitsPerSample: ").append(
                wBitsPerSample).append(", cbSize: ").append(cbSize);
        out.append("]");
        return out.toString();
    }

}
