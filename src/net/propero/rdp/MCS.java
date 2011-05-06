/* MCS.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: MCS Layer of communication
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

import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.virtualChannels.VChannels;
import org.apache.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;

@SuppressWarnings({"UnusedDeclaration"})
public class MCS {
    static Logger logger = Logger.getLogger(Rdp.class);
    private X224 x224Layer = null;
    private int McsUserID;

    /* this for the MCS Layer */
    private static final int CONNECT_INITIAL = 0x7f65;
    private static final int CONNECT_RESPONSE = 0x7f66;

    private static final int BER_TAG_BOOLEAN = 1;
    private static final int BER_TAG_INTEGER = 2;
    private static final int BER_TAG_OCTET_STRING = 4;
    private static final int BER_TAG_RESULT = 10;
    private static final int TAG_DOMAIN_PARAMS = 0x30;

    public static final int MCS_GLOBAL_CHANNEL = 1003;
    public static final int MCS_USERCHANNEL_BASE = 1001;

    private static final int EDRQ = 1;        /* Erect Domain Request */
    private static final int DPUM = 8;        /* Disconnect Provider Ultimatum */
    private static final int AURQ = 10;        /* Attach User Request */
    private static final int AUCF = 11;        /* Attach User Confirm */
    private static final int CJRQ = 14;        /* Channel Join Request */
    private static final int CJCF = 15;        /* Channel Join Confirm */
    private static final int SDRQ = 25;        /* Send Data Request */
    private static final int SDIN = 26;        /* Send Data Indication */

    private VChannels channels;

    /**
     * Initialise the MCS layer (and lower layers) with provided channels
     *
     * @param channels Set of available MCS channels
     */
    public MCS(VChannels channels) {
        this.channels = channels;
        x224Layer = new X224();
    }

    /**
     * Connect to a server
     *
     * @param host Address of server
     * @param port Port to connect to on server
     * @param data Packet to use for sending connection data
     * @throws IOException       I/O Error on disc or network
     * @throws RdesktopException Protocol error
     * @throws OrderException    Unsupported order
     * @throws CryptoException   Cryptographic error
     */
    public void connect(InetAddress host, int port, RdpPacket data)
            throws IOException, RdesktopException, OrderException, CryptoException {
        logger.debug("MCS.connect");
        x224Layer.connect(host, port);

        this.sendConnectInitial(data);
        this.receiveConnectResponse(data);

        logger.debug("connect response received");

        sendErectDomainRequest();
        sendAttachUserRequest();

        this.McsUserID = receiveAttachUserConfirm();
        sendChannelJoinRequest(this.McsUserID + MCS_USERCHANNEL_BASE);
        receiveChannelJoinConfirm();
        sendChannelJoinRequest(MCS_GLOBAL_CHANNEL);
        receiveChannelJoinConfirm();

        for (int i = 0; i < channels.getChannelCount(); i++) {
            sendChannelJoinRequest(channels.getMcsId(i));
            receiveChannelJoinConfirm();
        }

    }


    /**
     * Disconnect from server
     */
    public void disconnect() {
        x224Layer.disconnect();
        //in=null;
        //out=null;
    }

    /**
     * Initialise a packet as an MCS PDU
     *
     * @param length Desired length of PDU
     * @return PDU with the header initialized
     * @throws RdesktopException Protocol error
     */
    public RdpPacket init(int length) throws RdesktopException {
        RdpPacket data = x224Layer.init(length + 8);
        //data.pushLayer(RdpPacket.MCS_HEADER, 8);
        data.setHeader(RdpPacket.MCS_HEADER);
        data.incrementPosition(8);
        data.setStart(data.getPosition());
        return data;
    }

    /**
     * Send a packet to the global channel
     *
     * @param buffer Packet to send
     * @throws RdesktopException Protocol error
     * @throws IOException       I/O Error on disc or network
     */
    public void send(RdpPacket buffer) throws RdesktopException, IOException {
        sendToChannel(buffer, MCS_GLOBAL_CHANNEL);
    }

    /**
     * Send a packet to a specified channel
     *
     * @param buffer  Packet to send to channel
     * @param channel Id of channel on which to send packet
     * @throws RdesktopException Protocol error
     * @throws IOException       I/O Error on disc or network
     */
    public void sendToChannel(RdpPacket buffer, int channel) throws RdesktopException, IOException {
        int length;
        buffer.setPosition(buffer.getHeader(RdpPacket.MCS_HEADER));

        length = buffer.getEnd() - buffer.getHeader(RdpPacket.MCS_HEADER) - 8;
        length |= 0x8000;

        buffer.set8((SDRQ << 2));
        buffer.setBigEndian16(this.McsUserID);
        buffer.setBigEndian16(channel);
        buffer.set8(0x70); //Flags
        buffer.setBigEndian16(length);
        x224Layer.send(buffer);
    }

    /**
     * Receive an MCS PDU from the next channel with available data
     *
     * @param channel ID of channel will be stored in channel[0]
     * @return Received packet
     * @throws IOException       I/O Error on disc or network
     * @throws RdesktopException Protocol error
     * @throws OrderException    Unsupported order
     * @throws CryptoException   Cryptographic error
     */
    public RdpPacket receive(int[] channel) throws IOException, RdesktopException, OrderException, CryptoException {
        logger.debug("Rdp:receive");
        int opcode;
        int appid;
        int length;

        RdpPacket buffer = x224Layer.receive();
        if (buffer == null)
            return null;
        buffer.setHeader(RdpPacket.MCS_HEADER);
        opcode = buffer.get8();

        appid = opcode >> 2;

        if (appid != SDIN) {
            if (appid != DPUM) {
                throw new RdesktopException("Expected data got" + opcode);
            }
            throw new EOFException("End of transmission!");
        }

        buffer.incrementPosition(2); // Skip UserID
        channel[0] = buffer.getBigEndian16(); // Get ChannelID
        logger.debug("Channel ID = " + channel[0]);
        buffer.incrementPosition(1); // Skip Flags

        length = buffer.get8();

        if ((length & 0x80) != 0) {
            buffer.incrementPosition(1);
        }
        buffer.setStart(buffer.getPosition());
        return buffer;
    }


    /**
     * send an Integer encoded according to the X224 ASN.1 Basic Encoding Rules
     *
     * @param buffer Packet in which to store encoded value
     * @param value  Integer value to store
     */
    public void sendBerInteger(RdpPacket buffer, int value) {

        int len = 1;

        if (value > 0xff)
            len = 2;

        sendBerHeader(buffer, BER_TAG_INTEGER, len);

        if (value > 0xff) {
            buffer.setBigEndian16(value);
        } else {
            buffer.set8(value);
        }

    }

    /**
     * Determine the size of a BER header encoded for the specified tag and data length
     *
     * @param tagval Value of tag identifying data type
     * @param length Length of data header will precede
     * @return Size of the BER header
     */
    private int berHeaderSize(int tagval, int length) {
        int total = 0;
        if (tagval > 0xff) {
            total += 2;
        } else {
            total += 1;
        }

        if (length >= 0x80) {
            total += 3;
        } else {
            total += 1;
        }
        return total;
    }

    /**
     * Send a Header encoded according to the X224 ASN.1 Basic Encoding rules
     *
     * @param buffer Packet in which to send the header
     * @param tagval Data type for header
     * @param length Length of data header precedes
     */
    public void sendBerHeader(RdpPacket buffer, int tagval, int length) {
        if (tagval > 0xff) {
            buffer.setBigEndian16(tagval);
        } else {
            buffer.set8(tagval);
        }

        if (length >= 0x80) {
            buffer.set8(0x82);
            buffer.setBigEndian16(length);
        } else {
            buffer.set8(length);
        }
    }

    /**
     * Determine the size of a BER encoded integer with specified value
     *
     * @param value Value of integer
     * @return Number of bytes the encoded data would occupy
     */
    private int BERIntSize(int value) {
        if (value > 0xff)
            return 4;
        else
            return 3;
    }

    /**
     * Determine the size of the domain parameters, encoded according to the X224 ASN.1 Basic Encoding Rules
     *
     * @param max_channels Maximum number of channels
     * @param max_users    Maximum number of users
     * @param max_tokens   Maximum number of tokens
     * @param max_pdusize  Maximum size of an MCS PDU
     * @return Number of bytes the domain parameters would occupy
     */
    private int domainParamSize(int max_channels, int max_users, int max_tokens, int max_pdusize) {
        int endSize = BERIntSize(max_channels) +
                BERIntSize(max_users) +
                BERIntSize(max_tokens) +
                BERIntSize(1) +
                BERIntSize(0) +
                BERIntSize(1) +
                BERIntSize(max_pdusize) +
                BERIntSize(2);
        return berHeaderSize(TAG_DOMAIN_PARAMS, endSize) + endSize;
    }

    /**
     * send a DOMAIN_PARAMS structure encoded according to the X224 ASN.1
     * Basic Encoding rules
     *
     * @param buffer       Packet in which to send the structure
     * @param max_channels Maximum number of channels
     * @param max_users    Maximum number of users
     * @param max_tokens   Maximum number of tokens
     * @param max_pdusize  Maximum size for an MCS PDU
     */
    public void sendDomainParams(RdpPacket buffer, int max_channels, int max_users, int max_tokens, int max_pdusize) {

        int size = BERIntSize(max_channels) +
                BERIntSize(max_users) +
                BERIntSize(max_tokens) +
                BERIntSize(1) +
                BERIntSize(0) +
                BERIntSize(1) +
                BERIntSize(max_pdusize) +
                BERIntSize(2);

        sendBerHeader(buffer, TAG_DOMAIN_PARAMS, size);
        sendBerInteger(buffer, max_channels);
        sendBerInteger(buffer, max_users);
        sendBerInteger(buffer, max_tokens);

        sendBerInteger(buffer, 1); // num_priorities
        sendBerInteger(buffer, 0); // min_throughput
        sendBerInteger(buffer, 1); // max_height

        sendBerInteger(buffer, max_pdusize);
        sendBerInteger(buffer, 2); // ver_protocol
    }

    /**
     * Send an MCS_CONNECT_INITIAL message (encoded as ASN.1 Ber)
     *
     * @param data Packet in which to send the message
     * @throws IOException       I/O Error on disc or network
     * @throws RdesktopException Protocol error
     */
    public void sendConnectInitial(RdpPacket data) throws IOException, RdesktopException {
        logger.debug("MCS:sendConnectInitial");

        int datalen = data.getEnd();
        int length = 9 +
                domainParamSize(34, 2, 0, 0xffff) +
                domainParamSize(1, 1, 1, 0x420) +
                domainParamSize(0xffff, 0xfc17, 0xffff, 0xffff) +
                4 + datalen; // RDP5 Code

        RdpPacket buffer = x224Layer.init(length + 5);

        sendBerHeader(buffer, CONNECT_INITIAL, length);
        sendBerHeader(buffer, BER_TAG_OCTET_STRING, 1); //calling domain
        buffer.set8(1); // RDP5 Code
        sendBerHeader(buffer, BER_TAG_OCTET_STRING, 1); // called domain
        buffer.set8(1);  // RDP5 Code

        sendBerHeader(buffer, BER_TAG_BOOLEAN, 1);
        buffer.set8(0xff); //upward flag

        sendDomainParams(buffer, 34, 2, 0, 0xffff); //target parameters // RDP5 Code
        sendDomainParams(buffer, 1, 1, 1, 0x420); // minimum parameters
        sendDomainParams(buffer, 0xffff, 0xfc17, 0xffff, 0xffff); //maximum parameters

        sendBerHeader(buffer, BER_TAG_OCTET_STRING, datalen);

        data.copyToPacket(buffer, 0, buffer.getPosition(), data.getEnd());
        buffer.incrementPosition(data.getEnd());
        buffer.markEnd();
        x224Layer.send(buffer);
    }

    /**
     * Receive and handle a connect response from the server
     *
     * @param data Packet containing response data
     * @throws IOException       I/O Error on disc or network
     * @throws RdesktopException Protocol error
     * @throws OrderException    Unsupported order
     * @throws CryptoException   Cryptographic error
     */
    @SuppressWarnings({"UnusedParameters", "UnusedAssignment"})
    public void receiveConnectResponse(RdpPacket data) throws IOException, RdesktopException, OrderException, CryptoException {


        logger.debug("MCS.receiveConnectResponse");

        String[] connect_results = {
                "Successful",
                "Domain Merging",
                "Domain not Hierarchical",
                "No Such Channel",
                "No Such Domain",
                "No Such User",
                "Not Admitted",
                "Other User ID",
                "Parameters Unacceptable",
                "Token Not Available",
                "Token Not Possessed",
                "Too Many Channels",
                "Too Many Tokens",
                "Too Many Users",
                "Unspecified Failure",
                "User Rejected"
        };

        int result;
        int length = 0;

        RdpPacket buffer = x224Layer.receive();
        logger.debug("Received buffer");
        length = berParseHeader(buffer, CONNECT_RESPONSE);
        length = berParseHeader(buffer, BER_TAG_RESULT);

        result = buffer.get8();
        if (result != 0) {
            throw new RdesktopException("MCS Connect failed: " + connect_results[result]);
        }
        length = berParseHeader(buffer, BER_TAG_INTEGER);
        length = buffer.get8(); //connect id
        parseDomainParams(buffer);
        length = berParseHeader(buffer, BER_TAG_OCTET_STRING);

        Common.secure.processMcsData(buffer);
    }

    /**
     * Transmit an EDrq message
     *
     * @throws IOException       I/O Error on disc or network
     * @throws RdesktopException Protocol error
     */
    public void sendErectDomainRequest() throws IOException, RdesktopException {
        logger.debug("sendErectDomainRequest");
        RdpPacket buffer = x224Layer.init(5);
        buffer.set8(EDRQ << 2);
        buffer.setBigEndian16(1); //height
        buffer.setBigEndian16(1); //interval
        buffer.markEnd();
        x224Layer.send(buffer);
    }

    /**
     * Transmit a Channel Join Request (CJrq) message
     *
     * @param channelId Id of channel to be identified in request
     * @throws IOException       I/O Error on disc or network
     * @throws RdesktopException Protocol error
     */
    public void sendChannelJoinRequest(int channelId)
            throws IOException, RdesktopException {
        RdpPacket buffer = x224Layer.init(5);
        buffer.set8(CJRQ << 2);
        buffer.setBigEndian16(this.McsUserID); //height
        buffer.setBigEndian16(channelId); //interval
        buffer.markEnd();
        x224Layer.send(buffer);
    }

    /**
     * Transmit an AUcf message
     *
     * @throws IOException       I/O Error on disc or network
     * @throws RdesktopException Protocol error
     */
    public void sendAttachUserConfirm() throws IOException, RdesktopException {
        RdpPacket buffer = x224Layer.init(2);

        buffer.set8(AUCF << 2);
        buffer.set8(0);
        buffer.markEnd();
        x224Layer.send(buffer);
    }

    /**
     * Transmit an AUrq mesage
     *
     * @throws IOException       I/O Error on disc or network
     * @throws RdesktopException Protocol error
     */
    public void sendAttachUserRequest() throws IOException, RdesktopException {
        RdpPacket buffer = x224Layer.init(1);

        buffer.set8(AURQ << 2);
        buffer.markEnd();
        x224Layer.send(buffer);
    }

    /**
     * Receive and handle a CJcf message
     *
     * @throws IOException       I/O Error on disc or network
     * @throws OrderException    Unsupported order
     * @throws RdesktopException Protocol error
     * @throws CryptoException   Cryptographic error
     */
    public void receiveChannelJoinConfirm() throws IOException, RdesktopException, OrderException, CryptoException {
        logger.debug("receiveChannelJoinConfirm");
        int opcode;
        int result;
        RdpPacket buffer = x224Layer.receive();

        opcode = buffer.get8();
        if ((opcode >> 2) != CJCF) {
            throw new RdesktopException("Expected CJCF got" + opcode);
        }

        result = buffer.get8();
        if (result != 0) {
            throw new RdesktopException("Expected CJRQ got " + result);
        }

        buffer.incrementPosition(4); //skip userid, req_channelid

        if ((opcode & 2) != 0) {
            buffer.incrementPosition(2); // skip join_channelid
        }

        if (buffer.getPosition() != buffer.getEnd()) {
            throw new RdesktopException();
        }
    }

    /**
     * Receive an AUcf message
     *
     * @return UserID specified in message
     * @throws IOException       I/O Error on disc or network
     * @throws RdesktopException Protocol error
     * @throws OrderException    Unsupported order
     * @throws CryptoException   Cryptographic error
     */
    public int receiveAttachUserConfirm() throws IOException, RdesktopException, OrderException, CryptoException {
        logger.debug("receiveAttachUserConfirm");
        int opcode;
        int result;
        int UserID = 0;
        RdpPacket buffer = x224Layer.receive();

        opcode = buffer.get8();
        if ((opcode >> 2) != AUCF) {
            throw new RdesktopException("Expected AUCF got " + opcode);
        }

        result = buffer.get8();
        if (result != 0) {
            throw new RdesktopException("Expected AURQ got " + result);
        }

        if ((opcode & 2) != 0) {
            UserID = buffer.getBigEndian16();
        }

        if (buffer.getPosition() != buffer.getEnd()) {
            throw new RdesktopException();
        }
        return UserID;
    }

    /**
     * Parse a BER header and determine data length
     *
     * @param data   Packet containing header at current read position
     * @param tagval Tag ID for data type
     * @return Length of following data
     * @throws RdesktopException Protocol error
     */
    public int berParseHeader(RdpPacket data, int tagval) throws RdesktopException {
        int tag;
        int length;
        int len;

        if (tagval > 0x000000ff) {
            tag = data.getBigEndian16();
        } else {
            tag = data.get8();
        }

        if (tag != tagval) {
            throw new RdesktopException("Unexpected tag got " + tag + " expected " + tagval);
        }

        len = data.get8();

        if ((len & 0x00000080) != 0) {
            len &= ~0x00000080; // subtract 128
            length = 0;
            while (len-- != 0) {
                length = (length << 8) + data.get8();
            }
        } else {
            length = len;
        }

        return length;
    }

    /**
     * Parse domain parameters sent by server
     *
     * @param data Packet containing domain parameters at current read position
     * @throws RdesktopException Protocol error
     */
    public void parseDomainParams(RdpPacket data) throws RdesktopException {
        int length;

        length = this.berParseHeader(data, TAG_DOMAIN_PARAMS);
        data.incrementPosition(length);

        if (data.getPosition() > data.getEnd()) {
            throw new RdesktopException();
        }
    }

    /**
     * Retrieve the user ID stored by this MCS object
     *
     * @return User ID
     */
    public int getUserID() {
        return this.McsUserID;
    }
}
