/* VChannel.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.4 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:40 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Abstract class for RDP5 channels
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

import net.propero.rdp.Common;
import net.propero.rdp.Constants;
import net.propero.rdp.Options;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.Rdp;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.Secure;
import net.propero.rdp.crypto.CryptoException;
import org.apache.log4j.Logger;

import java.io.IOException;
/**
 * [MS-RDPBCGR] Section 1.3.3 Static Virtual Channels
 * 
 * Static Virtual Channels allow lossless communication between
 * client and server components over the main RDP data connection.
 * Virtual channel data is application-specific and opaque to RDP.
 * A maximum of 31 static virtual channels can be created at connection time.
 *
 * This is the base class.  All virtual channels should extend this class
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240461(v=prot.10).aspx">
 *      [MS-RDPBCGR] Section 1.3.3 Static Virtual Channels</a>
 */
@SuppressWarnings({"UnusedDeclaration"})
public abstract class VChannel {

    protected static Logger logger = Logger.getLogger(Rdp.class);

    private int mcs_id = 0;

    /**
     * Provide the name of this channel
     *
     * @return Channel name as string
     */
    public abstract String name();

    /**
     * Provide the set of flags specifying working options for this channel
     *
     * @return Option flags
     */
    public abstract int flags();

    /**
     * Process a packet sent on this channel
     *
     * @param data Packet sent to this channel
     * @throws RdesktopException Protocol error
     * @throws IOException       Network IO error
     * @throws CryptoException   Cryptographic error
     */
    public abstract void process(RdpPacket data) throws RdesktopException, IOException, CryptoException;

    public int mcs_id() {
        return mcs_id;
    }

    /**
     * Set the MCS ID for this channel
     *
     * @param mcs_id New MCS ID
     */
    public void set_mcs_id(int mcs_id) {
        this.mcs_id = mcs_id;
    }

    /**
     * Initialise a packet for transmission over this virtual channel
     *
     * @param length Desired length of packet
     * @return Packet prepared for this channel
     * @throws RdesktopException Protocol error
     */
    public RdpPacket init(int length) throws RdesktopException {
        RdpPacket s;

        s = Common.secure.init(Options.isEncryption() ? Secure.SEC_ENCRYPT : 0, length + 8);
        s.setHeader(RdpPacket.CHANNEL_HEADER);
        s.incrementPosition(8);

        return s;
    }

    /**
     * Send a packet over this virtual channel
     *
     * @param data Packet to be sent
     * @throws RdesktopException Protocol error
     * @throws IOException       Network IO error
     * @throws CryptoException   Cryptographic error
     */
    public void send_packet(RdpPacket data) throws RdesktopException, IOException, CryptoException {
        if (Common.secure == null)
            return;
        int length = data.size();

        int dataOffset = 0;

        int numPackets = (length / VChannels.CHANNEL_CHUNK_LENGTH);
        numPackets += length - (VChannels.CHANNEL_CHUNK_LENGTH) * numPackets;

        while (dataOffset < length) {

            int thisLength = Math.min(VChannels.CHANNEL_CHUNK_LENGTH, length - dataOffset);

            RdpPacket s = Common.secure.init(Constants.encryption ? Secure.SEC_ENCRYPT : 0, 8 + thisLength);
            s.setLittleEndian32(length);

            int flags = ((dataOffset == 0) ? VChannels.CHANNEL_FLAG_FIRST : 0);
            if (dataOffset + thisLength >= length)
                flags |= VChannels.CHANNEL_FLAG_LAST;

            if ((this.flags() & VChannels.CHANNEL_OPTION_SHOW_PROTOCOL) != 0)
                flags |= VChannels.CHANNEL_FLAG_SHOW_PROTOCOL;

            s.setLittleEndian32(flags);
            s.copyFromPacket(data, dataOffset, s.getPosition(), thisLength);
            s.incrementPosition(thisLength);
            s.markEnd();

            dataOffset += thisLength;

            if (Common.secure != null) {
                Common.secure.sendToChannel(s, Constants.encryption ? Secure.SEC_ENCRYPT : 0, this.mcs_id());
            }
        }
    }
}
