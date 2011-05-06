/* VChannels.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.4 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:40 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Static store for all registered channels 
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
package net.propero.rdp.virtualChannels;

import net.propero.rdp.MCS;
import net.propero.rdp.Options;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.Rdp;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.crypto.CryptoException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Vector;

/**
 * This class manages all of the virtual channels that have been
 * added to the RDP application.
 *
 * @author jbackes
 */
@SuppressWarnings({"UnusedDeclaration"})
public class VChannels {

    protected static Logger logger = Logger.getLogger(Rdp.class);

    /* Sound format constants */
    public static final int WAVE_FORMAT_PCM = 1;
    public static final int WAVE_FORMAT_ADPCM = 2;
    public static final int WAVE_FORMAT_ALAW = 6;
    public static final int WAVE_FORMAT_MULAW = 7;

    /* Virtual channel options */
    public static final int CHANNEL_OPTION_INITIALIZED = 0x80000000;
    public static final int CHANNEL_OPTION_ENCRYPT_RDP = 0x40000000;
    public static final int CHANNEL_OPTION_COMPRESS_RDP = 0x00800000;
    public static final int CHANNEL_OPTION_SHOW_PROTOCOL = 0x00200000;

    /* NT status codes for RDPDR */
    public static final int STATUS_SUCCESS = 0x00000000;
    public static final int STATUS_INVALID_PARAMETER = 0xc000000d;
    public static final int STATUS_INVALID_DEVICE_REQUEST = 0xc0000010;
    public static final int STATUS_ACCESS_DENIED = 0xc0000022;

    public static final int DEFAULT_CHANNEL_COUNT = 16;

    public static final int CHANNEL_CHUNK_LENGTH = 1600;
    public static final int CHANNEL_FLAG_FIRST = 0x01;
    public static final int CHANNEL_FLAG_LAST = 0x02;
    public static final int CHANNEL_FLAG_SHOW_PROTOCOL = 0x10;

    private Vector<VChannel> channels = new Vector<VChannel>(DEFAULT_CHANNEL_COUNT);

    private byte[] fragment_buffer = null;

    public int getChannelCount() {
        return channels.size();
    }

    /**
     * Obtain the MCS ID for a specific numbered channel
     *
     * @param c Channel number for which to obtain MCS ID
     * @return MCS ID associated with the supplied channel number
     */
    public int getMcsId(int c) {
        return MCS.MCS_GLOBAL_CHANNEL + 1 + c;
    }

    /**
     * Initialise the maximum number of Virtual Channels
     */
    public VChannels() {
        channels = new Vector<VChannel>(DEFAULT_CHANNEL_COUNT);
    }

    /**
     * Retrieve the VChannel object for the numbered channel
     *
     * @param c Channel number
     * @return The requested Virtual Channel
     */
    public VChannel channel(int c) {
        return channels.elementAt(c);
    }

    /**
     * Retrieve the VChannel object for the specified MCS channel ID
     *
     * @param channelNumber MCS ID for the required channel
     * @return Virtual Channel associated with the supplied MCS ID
     */
    public VChannel findChannelByChannelNumber(int channelNumber) {
        if (channelNumber > MCS.MCS_GLOBAL_CHANNEL + channels.size()) {
            logger.warn("Channel " + channelNumber
                    + " not defined. Highest channel defined is "
                    + MCS.MCS_GLOBAL_CHANNEL + channels.size());
            return null;
        } else
            return channels.elementAt(channelNumber - MCS.MCS_GLOBAL_CHANNEL - 1);
    }

    /**
     * Remove all registered virtual channels
     */
    public void clear() {
        channels = new Vector<VChannel>(DEFAULT_CHANNEL_COUNT);
    }

    /**
     * Register a new virtual channel
     *
     * @param v Virtual channel to be registered
     * @return True if successful
     * @throws RdesktopException Protocol error
     */
    public boolean register(VChannel v) throws RdesktopException {
        if (!Options.isRdp5()) {
            return false;
        }

        channels.addElement(v);
        v.set_mcs_id(MCS.MCS_GLOBAL_CHANNEL + channels.size());

        return true;
    }

    /**
     * Process a packet sent on a numbered channel
     *
     * @param data       Packet sent to channel
     * @param mcsChannel Number specified for channel
     * @throws RdesktopException Protocol error
     * @throws IOException       Network or disk error
     * @throws CryptoException   Cryptographic error
     */
    public void channelProcess(RdpPacket data, int mcsChannel)
            throws RdesktopException, IOException, CryptoException {

        int flags;
        VChannel channel = null;

        int i;

        for (i = 0; i < channels.size(); i++) {
            if (getMcsId(i) == mcsChannel) {
                channel = channels.elementAt(i);
                break;
            }
        }

        if (i >= channels.size()) {
            throw new RdesktopException("Channel not found.");
        }

        data.getLittleEndian32();
        flags = data.getLittleEndian32();

        if (((flags & CHANNEL_FLAG_FIRST) != 0)
                && ((flags & CHANNEL_FLAG_LAST) != 0)) {
            // single fragment - pass straight up
            channel.process(data);
        } else {
            // append to the defragmentation buffer
            byte[] content = new byte[data.getEnd() - data.getPosition()];
            data.copyToByteArray(content, 0, data.getPosition(), content.length);
            fragment_buffer = append(fragment_buffer, content);

            if ((flags & CHANNEL_FLAG_LAST) != 0) {
                RdpPacket fullpacket = new RdpPacket(fragment_buffer.length);
                fullpacket.copyFromByteArray(fragment_buffer, 0, 0, fragment_buffer.length);
                // process the entire reconstructed packet
                channel.process(fullpacket);
                fragment_buffer = null;
            }
        }
    }

    /**
     * Increase the size of an array
     *
     * @param a      Array to expand
     * @param amount Number of elements to add to the array
     * @return Expanded array
     */
    static Object arrayExpand(Object a, int amount) {
        Class clazz = a.getClass();
        if (!clazz.isArray())
            return null;

        int length = Array.getLength(a);
        int newLength = length + amount; // 50% more

        Class componentType = a.getClass().getComponentType();
        Object newArray = Array.newInstance(componentType, newLength);
        System.arraycopy(a, 0, newArray, 0, length);
        return newArray;
    }

    /**
     * Concatenate two byte arrays
     *
     * @param target Contains initial bytes in output
     * @param source Appended to target array
     * @return Concatenation of arrays, target+source
     */
    static byte[] append(byte[] target, byte[] source) {
        if (target == null || target.length <= 0) {
            return source;
        } else if (source == null || source.length <= 0) {
            return target;
        } else {
            byte[] out = (byte[]) arrayExpand(target, source.length);
            System.arraycopy(source, 0, out, target.length, source.length);
            return out;
        }
    }
}
