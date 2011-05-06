/* Rdp.java
 * Component: ProperJavaRDP
 *
 * Revision: $Revision: 1.6 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Rdp layer of communication
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

import net.propero.rdp.capabilities.Capability;
import net.propero.rdp.capabilities.CapabilityManager;
import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.pdus.ServerPointerUpdatePDU;
import net.propero.rdp.pdus.UpdatePDU;
import net.propero.rdp.virtualChannels.VChannels;
import net.propero.rdp.virtualChannels.rdpSoundOut.SoundOutChannel;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;

/*
[MS-RDPBCGR] - 2.2.1.11.1.1.1 Extended Info Packet (TS_EXTENDED_INFO_PACKET)
 performanceFlags (4 bytes): A 32-bit, unsigned integer.
 It specifies a list of server desktop shell features to
 enable or disable in the session (with the goal of
 optimizing bandwidth usage). It is used by RDP 5.1, 5.2, 6.0, 6.1, and 7.0 servers.
*/

@SuppressWarnings({"UnusedDeclaration"})
public class Rdp {

    protected static Logger logger = Logger.getLogger(Rdp.class);

    static {
        logger.setLevel(Level.WARN);
    }

    public static int RDP5_DISABLE_NOTHING = 0x00;
    public static int PERF_DISABLE_WALLPAPER = 0x01;
    public static int PERF_DISABLE_FULLWINDOWDRAG = 0x02;
    public static int PERF_DISABLE_MENUANIMATIONS = 0x04;
    public static int PERF_DISABLE_THEMING = 0x08;
    public static int PERF_DISABLE_CURSOR_SHADOW = 0x20;
    public static int PERF_DISABLE_CURSORSETTINGS = 0x40;   // disables cursor blinking

    /* constants for RDP Layer */
    public static final int RDP_LOGON_NORMAL = 0x33;
    public static final int RDP_LOGON_AUTO = 0x8;
    public static final int RDP_LOGON_BLOB = 0x100;
    public static final int RDP_LOGON_LEAVE_AUDIO = 0x2000;

    // PDU Types
    // [MS-RDPBCGR] Section 2.2.8.1.1.1.1 Share Control Header (TS_SHARECONTROLHEADER)
    private static final int PDUTYPE_DEMANDACTIVEPDU = 0x01;    // Demand Active PDU (section 2.2.1.13.1)
    private static final int PDUTYPE_CONFIRMACTIVEPDU = 0x03;   // Confirm Active PDU (section 2.2.1.13.2)
    private static final int PDUTYPE_DEACTIVATEALLPDU = 0x06;   // Deactivate All PDU (section 2.2.3.1)
    private static final int PDUTYPE_DATAPDU = 0x07;            // Data PDU (actual type is revealed by the pduType2
                                                                // field in the Share Data Header (section 2.2.8.1.1.1.2) structure)
    private static final int PDUTYPE_SERVER_REDIR_PKT = 0x0A;   // Enhanced Security Server Redirection PDU (section 2.2.13.3.1).

    // Data PDU Types
    private static final int PDUTYPE2_UPDATE = 2;
    private static final int PDUTYPE2_CONTROL = 20;
    private static final int PDUTYPE2_POINTER = 27;
    private static final int PDUTYPE2_INPUT = 28;
    private static final int PDUTYPE2_SYNCHRONIZE = 31;
    private static final int PDUTYPE2_REFRESH_RECT = 0x21;
    private static final int PDUTYPE2_PLAY_SOUND = 34;
    private static final int PDUTYPE2_SAVE_SESSION_INFO = 38;
    private static final int PDUTYPE2_FONTLIST = 39;
    private static final int PDUTYPE2_SET_ERROR_INFO_PDU = 47;

    // Control PDU types
    private static final int RDP_CTL_REQUEST_CONTROL = 1;
    private static final int RDP_CTL_GRANT_CONTROL = 2;
    private static final int RDP_CTL_DETACH = 3;
    private static final int RDP_CTL_COOPERATE = 4;

    // System Pointer Types
    private static final int RDP_NULL_POINTER = 0;
    private static final int RDP_DEFAULT_POINTER = 0x7F00;

    // Input Devices
    private static final int RDP_INPUT_SYNCHRONIZE = 0;
    private static final int RDP_INPUT_CODEPOINT = 1;
    private static final int RDP_INPUT_VIRTKEY = 2;
    private static final int RDP_INPUT_SCANCODE = 4;
    private static final int RDP_INPUT_MOUSE = 0x8001;

    private static final int ORDER_CAP_NEGOTIATE = 2;
    private static final int ORDER_CAP_NOSUPPORT = 4;


    /* RDP capabilities */

    private static final int OS_MAJOR_TYPE_UNIX = 1;
    private static final int OS_MINOR_TYPE_XSERVER = 3;

    private static final int RDP5_FLAG = 0x0030;

    private static final byte[] RDP_SOURCE = {(byte) 0x4D, (byte) 0x53,
            (byte) 0x54, (byte) 0x53, (byte) 0x43, (byte) 0x00}; // string MSTSC encoded as 7 byte US-Ascii

    protected Secure SecureLayer = null;
    private RdesktopFrame frame = null;
    protected RdesktopCanvas surface = null;

    private int nextPacket = 0;

    private int rdp_shareid = 0;

    private boolean connected = false;

    private RdpPacket stream = null;

    private SoundOutChannel soundOutChannel = null;


    /**
     * Process a disconnect PDU
     *
     * @param data Packet containing disconnect PDU at current read position
     * @return Code specifying the reason for disconnection
     */
    protected int processDisconnectPdu(RdpPacket data) {
        logger.debug("Received disconnect PDU");
        return data.getLittleEndian32();
    }

    /**
     * Initialise RDP comms layer, and register virtual channels
     *
     * @param channels Virtual channels to be used in connection
     */
    public Rdp(VChannels channels) {
        this.SecureLayer = new Secure(channels);
        Common.secure = SecureLayer;

        // Retrieve the sound channel
        for (int i = 0; i < channels.getChannelCount(); i++) {
            if (channels.channel(i) instanceof SoundOutChannel) {
                soundOutChannel = (SoundOutChannel) channels.channel(i);
            }
        }
    }

    /**
     * Initialise a packet for sending data on the RDP layer
     *
     * @param size Size of RDP data
     * @return Packet initialised for RDP
     * @throws RdesktopException Problem initializing data
     */
    private RdpPacket initData(int size) throws RdesktopException {
        RdpPacket buffer;

        buffer = SecureLayer.init(Constants.encryption ? Secure.SEC_ENCRYPT : 0, size + 18);
        buffer.pushLayer(RdpPacket.RDP_HEADER, 18);
        // buffer.setHeader(RdpPacket.RDP_HEADER);
        // buffer.incrementPosition(18);
        // buffer.setStart(buffer.getPosition());
        return buffer;
    }

    /**
     * Send a packet on the RDP layer
     *
     * @param data          Packet to send
     * @param data_pdu_type Type of data
     * @throws RdesktopException Problem sending data
     * @throws IOException       IO error when sending data
     * @throws CryptoException   Invalid or unsupported Crypto
     */
    private void sendData(RdpPacket data, int data_pdu_type)
            throws RdesktopException, IOException, CryptoException {

        CommunicationMonitor.lock(this);

        try {
            int length;

            data.setPosition(data.getHeader(RdpPacket.RDP_HEADER));
            length = data.getEnd() - data.getPosition();

            data.setLittleEndian16(length);
            data.setLittleEndian16(PDUTYPE_DATAPDU | 0x10);
            data.setLittleEndian16(SecureLayer.getUserID() + 1001);

            data.setLittleEndian32(this.rdp_shareid);
            data.set8(0); // pad
            data.set8(1); // stream id
            data.setLittleEndian16(length - 14);
            data.set8(data_pdu_type);
            data.set8(0); // compression type
            data.setLittleEndian16(0); // compression length

            SecureLayer.send(data, Constants.encryption ? Secure.SEC_ENCRYPT : 0);
        } finally {
            CommunicationMonitor.unlock(this);
        }

    }

    /**
     * Receive a packet from the RDP layer
     *
     * @param type Type of PDU received, stored in type[0]
     * @return Packet received from RDP layer
     * @throws IOException       Error during receive
     * @throws RdesktopException Error during receive
     * @throws CryptoException   Error during receive
     * @throws OrderException    Problem processing order
     */
    private RdpPacket receive(int[] type) throws IOException,
            RdesktopException, CryptoException, OrderException {
        int length;

        if ((this.stream == null) || (this.nextPacket >= this.stream.getEnd())) {
            this.stream = SecureLayer.receive();
            if (stream == null)
                return null;
            this.nextPacket = this.stream.getPosition();
        } else {
            this.stream.setPosition(this.nextPacket);
        }
        length = this.stream.getLittleEndian16();

        /* 32k packets are really 8, keepalive fix - rdesktop 1.2.0 */
        if (length == 0x8000) {
            logger.warn("32k packet keepalive fix");
            nextPacket += 8;
            type[0] = 0;
            return stream;
        }
        type[0] = this.stream.getLittleEndian16() & 0xf;
        if (stream.getPosition() != stream.getEnd()) {
            stream.incrementPosition(2);
        }

        this.nextPacket += length;
        return stream;
    }

    /**
     * Connect to a server
     *
     * @param username  Username for log on
     * @param server    Server to connect to
     * @param flags     Flags defining logon type
     * @param domain    Domain for log on
     * @param password  Password for log on
     * @param command   Alternative shell for session
     * @param directory Initial working directory for connection
     * @throws ConnectionException Unable to connect
     */
    public void connect(String username, InetAddress server, int flags,
                        String domain, String password, String command, String directory)
            throws ConnectionException {
        try {
            SecureLayer.connect(server);
            this.connected = true;
            this.sendLogonInfo(flags, domain, username, password, command, directory);
        }
        // Handle an unresolvable hostname
        catch (UnknownHostException e) {
            throw new ConnectionException(
                    "Could not resolve host name: " + server);
        }
        // Handle a refused connection
        catch (ConnectException e) {
            throw new ConnectionException(
                    "Connection refused when trying to connect to " + server + " on port " + Options.getPort());
        }
        // Handle a timeout on connecting
        catch (NoRouteToHostException e) {
            throw new ConnectionException(
                    "Connection timed out when attempting to connect to " + server);
        } catch (IOException e) {
            throw new ConnectionException("Connection Failed");
        } catch (RdesktopException e) {
            throw new ConnectionException(e.getMessage());
        } catch (OrderException e) {
            throw new ConnectionException(e.getMessage());
        } catch (CryptoException e) {
            throw new ConnectionException(e.getMessage());
        }

    }

    /**
     * Disconnect from an RDP session
     */
    public void disconnect() {
        this.connected = false;
        SecureLayer.disconnect();
    }

    /**
     * Retrieve status of connection
     *
     * @return True if connection to RDP session
     */
    public boolean isConnected() {
        return this.connected;
    }

    boolean deactivated;

    int ext_disc_reason;

    /**
     * RDP receive loop
     *
     * @param deactivated     On return, stores true in deactivated[0] if the session disconnected cleanly
     * @param ext_disc_reason On return, stores the reason for disconnection in ext_disc_reason[0]
     * @throws IOException       Disc or Network error
     * @throws RdesktopException Protocol Error
     * @throws OrderException    Unsupported Order
     * @throws CryptoException   Cryptographic error
     */
    public void mainLoop(boolean[] deactivated, int[] ext_disc_reason)
            throws IOException, RdesktopException, OrderException,
            CryptoException {
        int[] type = new int[1];

        boolean disconnect = false; /* True when a disconnect PDU was received */
        boolean connected = true;

        RdpPacket data;

        while (connected) {
            try {
                data = this.receive(type);
                if (data == null)
                    return;
            } catch (EOFException e) {
                return;
            }

            if (soundOutChannel != null)
                soundOutChannel.waveOutPlay();

            switch (type[0]) {
                case (Rdp.PDUTYPE_DEMANDACTIVEPDU):
                    logger.debug("Rdp.PDUTYPE_DEMANDACTIVEPDU");
                    // get this after licence negotiation, just before the 1st
                    // order...
                    NDC.push("processDemandActive");
                    this.processDemandActive(data);
                    // can use this to trigger things that have to be done before
                    // 1st order
                    logger.debug("ready to send (got past licence negotiation)");
                    Rdesktop.readyToSend = true;
                    frame.triggerReadyToSend();
                    NDC.pop();
                    deactivated[0] = false;
                    break;

                case (Rdp.PDUTYPE_DEACTIVATEALLPDU):
                    // get this on log off
                    deactivated[0] = true;
                    this.stream = null; // ty this fix
                    break;

                case (Rdp.PDUTYPE_DATAPDU):
                    logger.debug("Rdp.PDUTYPE_DATAPDU");
                    // all the others should be this
                    NDC.push("processData");

                    disconnect = this.processData(data, ext_disc_reason);
                    NDC.pop();
                    break;

                case 0:
                    break; // 32K keep alive fix, see receive() - rdesktop 1.2.0.

                default:
                    throw new RdesktopException("Unimplemented type in main loop :"
                            + type[0]);
            }

            if (disconnect)
                connected = false;
        }
    }

    /**
     * Send user logon details to the server
     *
     * @param flags     Set of flags defining logon type
     * @param domain    Domain for logon
     * @param username  Username for logon
     * @param password  Password for logon
     * @param command   Alternative shell for session
     * @param directory Starting working directory for session
     * @throws RdesktopException Problem occured sending logon information
     * @throws IOException       Networking problem
     * @throws CryptoException   Cryptographic problem
     */
    @SuppressWarnings({"PointlessArithmeticExpression"})
    private void sendLogonInfo(int flags, String domain, String username,
                               String password, String command, String directory)
            throws RdesktopException, IOException, CryptoException {

        int len_ip = 2 * "127.0.0.1".length();
        int len_dll = 2 * "C:\\WINNT\\System32\\mstscax.dll".length();
        int packetlen;

        int sec_flags = Constants.encryption ? (Secure.SEC_LOGON_INFO | Secure.SEC_ENCRYPT)
                : Secure.SEC_LOGON_INFO;
        int domainlen = 2 * domain.length();
        int userlen = 2 * username.length();
        int passlen = 2 * password.length();
        int commandlen = 2 * command.length();
        int dirlen = 2 * directory.length();

        RdpPacket data;

        if (!Options.isRdp5() || 1 == Options.getServerRDPVersion()) {
            logger.debug("Sending RDP4-style Logon packet");

            data = SecureLayer.init(sec_flags, 18 + domainlen + userlen
                    + passlen + commandlen + dirlen + 10);

            data.setLittleEndian32(0);
            data.setLittleEndian32(flags);
            data.setLittleEndian16(domainlen);
            data.setLittleEndian16(userlen);
            data.setLittleEndian16(passlen);
            data.setLittleEndian16(commandlen);
            data.setLittleEndian16(dirlen);
            data.outUnicodeString(domain, domainlen);
            data.outUnicodeString(username, userlen);
            data.outUnicodeString(password, passlen);
            data.outUnicodeString(command, commandlen);
            data.outUnicodeString(directory, dirlen);

        } else {
            flags |= RDP_LOGON_BLOB;
            logger.debug("Sending RDP5-style Logon packet");
            packetlen = 4
                    + // TODO:  Unknown uint32
                    4
                    + // flags
                    2
                    + // len_domain
                    2
                    + // len_user
                    ((flags & RDP_LOGON_AUTO) != 0 ? 2 : 0)
                    + // len_password
                    ((flags & RDP_LOGON_BLOB) != 0 ? 2 : 0)
                    + // Length of BLOB
                    2
                    + // len_program
                    2
                    + // len_directory
                    (0 < domainlen ? domainlen + 2 : 2)
                    + // domain
                    userlen
                    + ((flags & RDP_LOGON_AUTO) != 0 ? passlen : 0)
                    + 0
                    + // We have no 512 byte BLOB. Perhaps we must?
                    ((flags & RDP_LOGON_BLOB) != 0 && (flags & RDP_LOGON_AUTO) == 0 ? 2 : 0)
                    + (0 < commandlen ? commandlen + 2 : 2)
                    + (0 < dirlen ? dirlen + 2 : 2) + 2 + // TODO: Unknown (2)
                    2 + // Client ip length
                    len_ip + // Client ip
                    2 + // DLL string length
                    len_dll + // DLL string
                    2 + // TODO:  Unknown
                    2 + // TODO:  Unknown
                    64 + // Time zone #0
                    20 + // TODO: Unknown
                    64 + // Time zone #1
                    32 + 6; // TODO: Unknown

            data = SecureLayer.init(sec_flags, packetlen); // s = sec_init(sec_flags, packetlen);
            // logger.debug("Called sec_init with packetlen " + packetlen);

            data.setLittleEndian32(0); // out_uint32(s, 0); // TODO: Unknown
            data.setLittleEndian32(flags); // out_uint32_le(s, flags);
            data.setLittleEndian16(domainlen); // out_uint16_le(s, len_domain);
            data.setLittleEndian16(userlen); // out_uint16_le(s, len_user);
            if ((flags & RDP_LOGON_AUTO) != 0) {
                data.setLittleEndian16(passlen); // out_uint16_le(s, len_password);
            }
            if ((flags & RDP_LOGON_BLOB) != 0 && ((flags & RDP_LOGON_AUTO) == 0)) {
                data.setLittleEndian16(0); // out_uint16_le(s, 0);
            }
            data.setLittleEndian16(commandlen); // out_uint16_le(s, len_program);
            data.setLittleEndian16(dirlen); // out_uint16_le(s, len_directory);

            if (0 < domainlen)
                data.outUnicodeString(domain, domainlen); // rdp_out_unistr(s, domain, len_domain);
            else
                data.setLittleEndian16(0); // out_uint16_le(s, 0);

            data.outUnicodeString(username, userlen); // rdp_out_unistr(s, user, len_user);
            if ((flags & RDP_LOGON_AUTO) != 0) {
                data.outUnicodeString(password, passlen); // rdp_out_unistr(s, password, len_password);
            }
            if ((flags & RDP_LOGON_BLOB) != 0 && (flags & RDP_LOGON_AUTO) == 0) {
                data.setLittleEndian16(0); // out_uint16_le(s, 0);
            }
            if (0 < commandlen) {
                data.outUnicodeString(command, commandlen); // rdp_out_unistr(s, program, len_program);
            } else {
                data.setLittleEndian16(0); // out_uint16_le(s, 0);
            }
            if (0 < dirlen) {
                data.outUnicodeString(directory, dirlen); // rdp_out_unistr(s, directory, len_directory);
            } else {
                data.setLittleEndian16(0); // out_uint16_le(s, 0);
            }
            data.setLittleEndian16(2); // out_uint16_le(s, 2);
            data.setLittleEndian16(len_ip + 2); // out_uint16_le(s, len_ip + 2); Length of client ip
            data.outUnicodeString("127.0.0.1", len_ip); // rdp_out_unistr(s, "127.0.0.1", len_ip);
            data.setLittleEndian16(len_dll + 2); // out_uint16_le(s, len_dll + 2);
            data.outUnicodeString("C:\\WINNT\\System32\\mstscax.dll", len_dll); // rdp_out_unistr(s,"C:\\WINNT\\System32\\mstscax.dll", len_dll);
            data.setLittleEndian16(0xffc4); // out_uint16_le(s, 0xffc4);
            data.setLittleEndian16(0xffff); // out_uint16_le(s, 0xffff);


            data.outUnicodeString("GTB, normaltid", 2 * "GTB, normaltid".length()); // rdp_out_unistr(s, "GTB, normaltid", 2 * strlen("GTB, normaltid"));
            data.incrementPosition(62 - 2 * "GTB, normaltid".length()); // out_uint8s(s, 62 - 2 * strlen("GTB, normaltid"));

            data.setLittleEndian32(0x0a0000); // out_uint32_le(s, 0x0a0000);
            data.setLittleEndian32(0x050000); // out_uint32_le(s, 0x050000);
            data.setLittleEndian32(3); // out_uint32_le(s, 3);
            data.setLittleEndian32(0); // out_uint32_le(s, 0);
            data.setLittleEndian32(0); // out_uint32_le(s, 0);

            data.outUnicodeString("GTB, sommartid", 2 * "GTB, sommartid".length()); // rdp_out_unistr(s, "GTB, sommartid", 2 * strlen("GTB, sommartid"));
            data.incrementPosition(62 - 2 * "GTB, sommartid".length()); // out_uint8s(s, 62 - 2 * strlen("GTB, sommartid"));

            data.setLittleEndian32(0x30000); // out_uint32_le(s, 0x30000);
            data.setLittleEndian32(0x050000); // out_uint32_le(s, 0x050000);
            data.setLittleEndian32(2); // out_uint32_le(s, 2);
            data.setLittleEndian32(0); // out_uint32(s, 0);
            data.setLittleEndian32(0xffffffc4); // out_uint32_le(s, 0xffffffc4);
            data.setLittleEndian32(0xfffffffe); // out_uint32_le(s, 0xfffffffe);
            data.setLittleEndian32(Options.getRdp5PerformanceFlags()); // out_uint32_le(s, 0x0f);
            data.setLittleEndian32(0); // out_uint32(s, 0);
        }

        data.markEnd();
        byte[] buffer = new byte[data.getEnd()];
        data.copyToByteArray(buffer, 0, 0, data.getEnd());
        SecureLayer.send(data, sec_flags);
    }

    /**
     * Process an activation demand from the server (received between licence negotiation and 1st order)
     *
     * @param data Packet containing demand at current read position
     * @throws RdesktopException Protocol error
     * @throws IOException       Network or disk IO error
     * @throws CryptoException   Cryptographic error
     * @throws OrderException    Unsupported order
     */
    private void processDemandActive(RdpPacket data)
            throws RdesktopException, IOException, CryptoException,
            OrderException {
        int type[] = new int[1];

        this.rdp_shareid = data.getLittleEndian32();

        data.debugPacket();
        
        int lengthSourceDescriptor = data.getLittleEndian16();
        int lengthCombinedCapabilities = data.getLittleEndian16();
        data.incrementPosition(lengthSourceDescriptor);

        CapabilityManager.getInstance().processServerCaps(data, lengthCombinedCapabilities);

        this.sendConfirmActivePDU();

        this.sendSynchronize();
        this.sendControl(RDP_CTL_COOPERATE);
        this.sendControl(RDP_CTL_REQUEST_CONTROL);

        this.receive(type); // Receive RDP_PDU_SYNCHRONIZE
        this.receive(type); // Receive RDP_CTL_COOPERATE
        this.receive(type); // Receive RDP_CTL_GRANT_CONTROL

        this.sendInput(0, RDP_INPUT_SYNCHRONIZE, 0, 0, 0);
        this.sendFonts(1);
        this.sendFonts(2);

        this.receive(type);

        OrdersProcessor.getInstance().resetOrderState();
    }

    /**
     * Process a data PDU received from the server
     *
     * @param data            Packet containing data PDU at current read position
     * @param ext_disc_reason If a disconnect PDU is received, stores disconnection reason at ext_disc_reason[0]
     * @return True if disconnect PDU was received
     * @throws RdesktopException Protocol error
     * @throws OrderException    Unsupported order found
     */
    @SuppressWarnings({"UnusedAssignment"})
    private boolean processData(RdpPacket data, int[] ext_disc_reason)
            throws RdesktopException, OrderException {
        int pduType2;
        int ctype;
        int clen;
        int len;
        int roff;
        int rlen;

        data.incrementPosition(6); // skip shareid, pad, streamid
        len = data.getLittleEndian16();
        pduType2 = data.get8();
        ctype = data.get8(); // compression type
        clen = data.getLittleEndian16(); // compression length
        clen -= 18;

        switch (pduType2) {

            case (Rdp.PDUTYPE2_UPDATE):
                logger.debug("Rdp.PDUTYPE2_UPDATE");
                UpdatePDU updatePDU = new UpdatePDU(nextPacket);
                updatePDU.process(surface, data);
                break;

            case PDUTYPE2_CONTROL:
                logger.debug(("Received Control PDU\n"));
                break;

            case PDUTYPE2_SYNCHRONIZE:
                logger.debug(("Received Sync PDU\n"));
                break;

            case (Rdp.PDUTYPE2_POINTER):
                logger.debug("Received pointer PDU");
                ServerPointerUpdatePDU serverPointerUpdatePDU = new ServerPointerUpdatePDU();
                serverPointerUpdatePDU.process(surface, data);
                break;
            case (Rdp.PDUTYPE2_PLAY_SOUND):
                logger.debug("Received bell PDU");
                Toolkit tx = Toolkit.getDefaultToolkit();
                tx.beep();
                break;
            case (Rdp.PDUTYPE2_SAVE_SESSION_INFO):
                logger.debug("User logged on");
                Rdesktop.loggedOn = true;
                break;
            case PDUTYPE2_SET_ERROR_INFO_PDU:
                /*
                * Normally received when user logs out or disconnects from a
                * console session on Windows XP and 2003 Server
                */
                ext_disc_reason[0] = processDisconnectPdu(data);
                logger.debug(("Received disconnect PDU\n"));
                return true;

            default:
                logger.warn("Unimplemented Data PDU type " + pduType2);

        }
        return false;
    }

    /**
     * [MS-RDPBCGR] Section 2.2.1.13.2.1 Confirm Active PDU Data (TS_CONFIRM_ACTIVE_PDU)
     * <p/>
     * <p/>
     * <p/>
     * The TS_CONFIRM_ACTIVE_PDU structure is a standard T.128 Confirm Active PDU (see [T128] section 8.4.1).
     *
     * @throws RdesktopException Protocol error
     * @throws IOException       Network error
     * @throws CryptoException   Cryptographic error
     */
    private void sendConfirmActivePDU() throws RdesktopException, IOException,
            CryptoException {

        CapabilityManager capabilityManager = CapabilityManager.getInstance();
        Capability[] mandatoryCaps = capabilityManager.getMandatoryCaps();
        Capability[] optionalCaps = capabilityManager.getOptionalCaps();

        // Compute the total size of all of the combined capabilities
        int capsLength = 0;
        for (Capability capability : mandatoryCaps) {
            capsLength += capability.getSize();
        }
        for (Capability capability : optionalCaps) {
            capsLength += capability.getSize();
        }

        int sec_flags = Options.isEncryption() ? (RDP5_FLAG | Secure.SEC_ENCRYPT) : RDP5_FLAG;

        RdpPacket data = SecureLayer.init(sec_flags, 6 + 14 + capsLength + RDP_SOURCE.length);


        // Shared control header is 6 bytes
        // Shared ID is 4 bytes
        // Originator is 2 bytes
        // lengthSourceDescriptor 2 bytes
        // lengthCombinedCapabilities 2 bytes
        // sourceDescriptor (variable)
        // numberCapabilities 2 bytes
        // pad2Octets 2 bytes
        // capabilitySets (variable)
        // 20 bytes of fixed length data & two variable sized buffers

        data.setLittleEndian16(20 + capsLength + RDP_SOURCE.length);    // The total length of the packet in bytes (the length includes the size of the Share Control Header).
        data.setLittleEndian16((PDUTYPE_CONFIRMACTIVEPDU | 0x10));        // The PDU type and protocol version information.
        data.setLittleEndian16(Common.mcs.getUserID() + 1001);          // The channel ID which is the transmission source of the PDU.

        data.setLittleEndian32(this.rdp_shareid);   // shareId - The share identifier for the packet (see [T128] section 8.4.2 for more information regarding share IDs).
        data.setLittleEndian16(0x03EA);             // originatorId - The identifier of the packet originator. This field MUST be set to the server channel ID (0x03EA).
        data.setLittleEndian16(RDP_SOURCE.length);  // lengthSourceDescriptor - The size in bytes of the sourceDescriptor field.
        data.setLittleEndian16(2 + 2 + capsLength);     // lengthCombinedCapabilities - The combined size in bytes of the numberCapabilities, pad2Octets and capabilitySets fields.

        data.copyFromByteArray(RDP_SOURCE, 0, data.getPosition(), RDP_SOURCE.length);
        data.incrementPosition(RDP_SOURCE.length);

        data.setLittleEndian16(mandatoryCaps.length + optionalCaps.length); // numberCapabilities
        data.setLittleEndian16(2);                                          // Padding. Values in this field MUST be ignored.

        for (Capability capability : mandatoryCaps) {
            capability.setBytes(data);
        }
        for (Capability capability : optionalCaps) {
            capability.setBytes(data);
        }

        data.markEnd();
        logger.debug("confirm active");

        Common.secure.send(data, sec_flags);
    }

    private void sendSynchronize() throws RdesktopException, IOException,
            CryptoException {
        RdpPacket data = this.initData(4);

        data.setLittleEndian16(1); // type
        data.setLittleEndian16(1002);

        data.markEnd();
        logger.debug("sendSynchronize");
        this.sendData(data, PDUTYPE2_SYNCHRONIZE);
    }

    private void sendControl(int action) throws RdesktopException, IOException,
            CryptoException {

        RdpPacket data = this.initData(8);

        data.setLittleEndian16(action);
        data.setLittleEndian16(0); // userid
        data.setLittleEndian32(0); // control id

        data.markEnd();
        logger.debug("sendControl");
        this.sendData(data, PDUTYPE2_CONTROL);
    }

    public void sendInput(int time, int message_type, int device_flags,
                          int param1, int param2) {
        RdpPacket data = null;
        try {
            data = this.initData(16);
        } catch (RdesktopException e) {
            Rdesktop.error(e, this, frame, false);
        }

        assert data != null;
        data.setLittleEndian16(1); /* number of events */
        data.setLittleEndian16(0); /* pad */

        data.setLittleEndian32(time);
        data.setLittleEndian16(message_type);
        data.setLittleEndian16(device_flags);
        data.setLittleEndian16(param1);
        data.setLittleEndian16(param2);

        data.markEnd();
        // logger.info("input");
        // if(logger.isInfoEnabled()) logger.info(data);

        try {
            this.sendData(data, PDUTYPE2_INPUT);
        } catch (RdesktopException r) {
            if (Common.rdp.isConnected()) {
                Rdesktop.error(r, Common.rdp, Common.frame, true);
            }
            Common.exit();
        } catch (CryptoException c) {
            if (Common.rdp.isConnected()) {
                Rdesktop.error(c, Common.rdp, Common.frame, true);
            }
            Common.exit();
        } catch (IOException i) {
            if (Common.rdp.isConnected()) {
                Rdesktop.error(i, Common.rdp, Common.frame, true);
            }
            Common.exit();
        }
    }

    private void sendFonts(int seq) throws RdesktopException, IOException,
            CryptoException {

        RdpPacket data = this.initData(8);

        data.setLittleEndian16(0);      // numberFonts
        data.setLittleEndian16(0x3e);   // totalNumFonts
        data.setLittleEndian16(seq);    // listFlags
        data.setLittleEndian16(0x32);   // entrySize

        data.markEnd();
        logger.debug("sendFonts");
        this.sendData(data, PDUTYPE2_FONTLIST);
    }

    public void registerDrawingSurface(RdesktopFrame rdesktopFrame) {
        this.frame = rdesktopFrame;
        RdesktopCanvas rdesktopFrameCanvas = rdesktopFrame.getCanvas();
        this.surface = rdesktopFrameCanvas;
        OrdersProcessor.getInstance().registerDrawingSurface(rdesktopFrameCanvas);
    }
}