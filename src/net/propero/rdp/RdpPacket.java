/* RdpPacket.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Encapsulates data from a single received packet.
 *          Provides methods for reading from and writing to
 *          an individual packet at all relevant levels.
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
// Created on 03-Sep-2003

package net.propero.rdp;

import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings({"UnusedDeclaration"})
public class RdpPacket {
    static Logger logger = Logger.getLogger(Rdp.class);

    /* constants for Packet */
    public static final int MCS_HEADER = 1;
    public static final int SECURE_HEADER = 2;
    public static final int RDP_HEADER = 3;
    public static final int CHANNEL_HEADER = 4;

    private ByteBuffer byteBuffer = null;
    private int size = 0;

    protected int mcs = -1;
    protected int secure = -1;
    protected int rdp = -1;
    protected int channel = -1;
    protected int start = -1;
    protected int end = -1;

    /**
     * Create a new RdpPacket with a given length
     *
     * @param capacity
     */
    public RdpPacket(int capacity) {
        byteBuffer = ByteBuffer.allocateDirect(capacity);
        size = capacity;
    }

    /**
     * Mark current read/write position as end of packet
     */
    public void markEnd() {
        this.end = getPosition();
    }

    /**
     * Mark specified position as end of packet
     *
     * @param position New end position (as byte offset from start)
     */
    public void markEnd(int position) {
        if (position > capacity()) {
            throw new ArrayIndexOutOfBoundsException("Mark > size!");
        }
        this.end = position;
    }

    /**
     * Retrieve location of packet end
     *
     * @return Position of packet end (as byte offset from start)
     */
    public int getEnd() {
        return this.end;
    }

    /**
     * Reserve space within this packet for writing of headers for a specific communications layer.
     * Move read/write position ready for adding data for a higher communications layer.
     *
     * @param header    ID of header type
     * @param increment Required size to be reserved for header
     * @throws RdesktopException Protocol error
     */
    public void pushLayer(int header, int increment) throws RdesktopException {
        this.setHeader(header);
        this.incrementPosition(increment);
        //this.setStart(this.getPosition());
    }

    /**
     * Get location of the header for a specific communications layer
     *
     * @param header ID of header type
     * @return Location of header, as byte offset from start of packet
     * @throws RdesktopException Protocol error
     */
    public int getHeader(int header) throws RdesktopException {
        switch (header) {
            case RdpPacket.MCS_HEADER:
                return this.mcs;
            case RdpPacket.SECURE_HEADER:
                return this.secure;
            case RdpPacket.RDP_HEADER:
                return this.rdp;
            case RdpPacket.CHANNEL_HEADER:
                return this.channel;
            default:
                throw new RdesktopException("Unknown Header");
        }
    }

    /**
     * Set current read/write position as the start of a layer header
     *
     * @param header ID of header type
     * @throws RdesktopException Protocol error
     */
    public void setHeader(int header) throws RdesktopException {
        switch (header) {
            case RdpPacket.MCS_HEADER:
                this.mcs = this.getPosition();
                break;
            case RdpPacket.SECURE_HEADER:
                this.secure = this.getPosition();
                break;
            case RdpPacket.RDP_HEADER:
                this.rdp = this.getPosition();
                break;
            case RdpPacket.CHANNEL_HEADER:
                this.channel = this.getPosition();
                break;
            default:
                throw new RdesktopException("Unknown Header");
        }
    }

    /**
     * Retrieve start location of this packet
     *
     * @return Start location of packet (as byte offset from location 0)
     */
    public int getStart() {
        return this.start;
    }

    /**
     * Set start position of this packet
     *
     * @param position New start position (as byte offset from location 0)
     */
    public void setStart(int position) {
        this.start = position;
    }

    /**
     * Add a unicode string to this packet at the current read/write position
     *
     * @param str String to write as unicode to packet
     * @param len Desired length of output unicode string
     */
    public void outUnicodeString(String str, int len) {
        int i = 0, j = 0;

        if (str.length() != 0) {
            char[] name = str.toCharArray();
            while (i < len) {
                this.setLittleEndian16((short) name[j++]);
                i += 2;
            }
            this.setLittleEndian16(0); // Terminating Null Character
        } else {
            this.setLittleEndian16(0);
        }
    }

    /**
     * Write an ASCII string to this packet at current read/write position
     *
     * @param str    String to be written
     * @param length Length in bytes to be occupied by string (may be longer than string itself)
     */
    public void outUint8p(String str, int length) {
        byte[] bStr = str.getBytes();
        this.copyFromByteArray(bStr, 0, this.getPosition(), bStr.length);
        this.incrementPosition(length);
    }

    public void reset(int length) {
        logger.debug("RdpPacket.reset(" + length + "), capacity = " + byteBuffer.capacity());
        this.end = 0;
        this.start = 0;
        if (byteBuffer.capacity() < length) {
            byteBuffer = ByteBuffer.allocateDirect(length);
        }
        size = length;
        byteBuffer.clear();
    }

    public void set8(int where, int what) {
        if (where < 0 || where >= byteBuffer.capacity()) {
            throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
        }
        byteBuffer.put(where, (byte) what);
    }

    public void set8(int what) {
        if (byteBuffer.position() >= byteBuffer.capacity()) {
            throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
        }
        byteBuffer.put((byte) what);
    }

    // where is a 8-bit offset
    public int get8(int where) {
        if (where < 0 || where >= byteBuffer.capacity()) {
            throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
        }
        return byteBuffer.get(where) & 0xff; // treat as unsigned byte
    }

    // where is a 8-bit offset
    public int get8() {
        if (byteBuffer.position() >= byteBuffer.capacity()) {
            throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
        }
        return byteBuffer.get() & 0xff; // treat as unsigned byte
    }

    public void copyFromByteArray(byte[] array, int arrayOffset, int memOffset, int len) {
        if ((arrayOffset >= array.length) ||
                (arrayOffset + len > array.length) ||
                (memOffset + len > byteBuffer.capacity())) {
            throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
        }
        // store position
        int oldPosition = getPosition();

        setPosition(memOffset);
        byteBuffer.put(array, arrayOffset, len);

        // restore position
        setPosition(oldPosition);
    }

    public void copyToByteArray(byte[] array, int arrayOffset, int memOffset, int len) {
        if ((arrayOffset >= array.length)) {
            throw new ArrayIndexOutOfBoundsException("Array offset beyond end of array!");
        }
        if (arrayOffset + len > array.length) {
            throw new ArrayIndexOutOfBoundsException("Not enough bytes in array to copy!");
        }
        if (memOffset + len > byteBuffer.capacity()) {
            throw new ArrayIndexOutOfBoundsException("Memory accessed out of Range!");
        }

        int oldpos = getPosition();
        setPosition(memOffset);
        byteBuffer.get(array, arrayOffset, len);
        setPosition(oldpos);
    }

    public void copyToPacket(RdpPacket dst, int srcOffset, int dstOffset, int len) {
        int olddstpos = dst.getPosition();
        int oldpos = getPosition();
        dst.setPosition(dstOffset);
        setPosition(srcOffset);
        for (int i = 0; i < len; i++)
            dst.set8(byteBuffer.get());
        dst.setPosition(olddstpos);
        setPosition(oldpos);
    }

    public void copyFromPacket(RdpPacket src, int srcOffset, int dstOffset, int len) {
        int oldsrcpos = src.getPosition();
        int oldpos = getPosition();
        src.setPosition(srcOffset);
        setPosition(dstOffset);
        for (int i = 0; i < len; i++) {
            byteBuffer.put((byte) src.get8());
        }
        src.setPosition(oldsrcpos);
        setPosition(oldpos);
    }

    public int capacity() {
        return byteBuffer.capacity();
    }

    // return size in bytes
    public int size() {
        return size;
        //return byteBuffer.capacity(); //this.end - this.start;
    }

    public int getPosition() {
        return byteBuffer.position();
    }

    public int getLittleEndian16(int where) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getShort(where);
    }

    public int getLittleEndian16() {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getShort();
    }

    public int getBigEndian16(int where) {
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getShort(where);
    }

    public int getBigEndian16() {
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getShort();
    }

    public void setLittleEndian16(int where, int what) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putShort(where, (short) what);
    }

    public void setLittleEndian16(int what) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putShort((short) what);
    }

    public void setBigEndian16(int where, int what) {
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(where, (short) what);
    }

    public void setBigEndian16(int what) {
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort((short) what);
    }

    public int getLittleEndian32(int where) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getInt(where);
    }

    public int getLittleEndian32() {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getInt();
    }

    public int getBigEndian32(int where) {
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getInt(where);
    }

    public int getBigEndian32() {
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getInt();
    }

    public void setLittleEndian32(int where, int what) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(where, what);
    }

    public void setLittleEndian32(int what) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(what);
    }

    public void setBigEndian32(int where, int what) {
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(where, what);
    }

    public void setBigEndian32(int what) {
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(what);
    }

    public void incrementPosition(int length) {

        if (length > byteBuffer.capacity() || length + byteBuffer.position() > byteBuffer.capacity() || length < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byteBuffer.position(byteBuffer.position() + length);
    }

    public void setPosition(int position) {
        if (position > byteBuffer.capacity() || position < 0) {
            logger.warn("stream position =" + getPosition() + " end =" + getEnd() + " capacity =" + capacity());
            logger.warn("setPosition(" + position + ") failed");
            throw new ArrayIndexOutOfBoundsException();
        }
        byteBuffer.position(position);
    }

    public void debugPacket() {
        HexDump hexDump = new HexDump();

        if (null != byteBuffer) {
            if (byteBuffer.hasArray()) {
                byte[] bytes = byteBuffer.array();
                if (null != bytes) {
                    hexDump.encode(bytes, "RdpPacket Debug: ");
                } else {
                    logger.warn("array is null!");
                }
            } else {
                logger.warn("byteBuffer has no Array!");
            }
        } else {
            logger.warn("byteBuffer is null!");
        }
    }
}
