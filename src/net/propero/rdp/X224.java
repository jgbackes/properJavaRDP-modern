/* ISO.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: ISO layer of communication
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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class X224 {
    static Logger logger = Logger.getLogger(X224.class);

    static {
        logger.setLevel(Level.WARN);
    }

    private HexDump dump = null;

    protected Socket rdpSocket = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;

    /* this for the X224 Layer */
    private static final int CONNECTION_REQUEST = 0xE0;
    private static final int CONNECTION_CONFIRM = 0xD0;
    private static final int DISCONNECT_REQUEST = 0x80;
    private static final int DATA_TRANSFER = 0xF0;
    //private static final int ERROR = 0x70;
    private static final int PROTOCOL_VERSION = 0x03;
    private static final int EOT = 0x80;

    /**
     * Construct X224 object, initialises hex dump
     */
    public X224() {
        dump = new HexDump();
    }

    /**
     * Initialise an X224 PDU
     *
     * @param length Desired length of PDU
     * @return Packet configured as X224 PDU, ready to write at higher level
     */
    public RdpPacket init(int length) {
        RdpPacket data = new RdpPacket(length + 7);
        data.incrementPosition(7);
        data.setStart(data.getPosition());
        return data;
    }

    /**
     * Connect to a server
     *
     * @param host Address of server
     * @param port Port to connect to on server
     * @throws IOException       Network I/O error
     * @throws RdesktopException Protocol error
     * @throws OrderException    Invalid drawing order
     * @throws CryptoException   Cryptographic Error
     */
    public void connect(InetAddress host, int port) throws IOException, RdesktopException, OrderException, CryptoException {
        int[] code = new int[1];
        doSocketConnect(host, port);
        rdpSocket.setTcpNoDelay(Options.isLowLatency());

        this.in = new DataInputStream(new BufferedInputStream(rdpSocket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(rdpSocket.getOutputStream()));

        sendConnectionRequest();
        receiveMessage(code);

        if (code[0] != CONNECTION_CONFIRM) {
            throw new RdesktopException("Expected CC got:" + Integer.toHexString(code[0]).toUpperCase());
        }

        /*if(Options.use_ssl){
          try {
              rdpSocket = this.negotiateSSL(rdpSocket);
              this.in = new DataInputStream(rdpSocket.getInputStream());
              this.out= new DataOutputStream(rdpSocket.getOutputStream());
          } catch (Exception e) {
              e.printStackTrace();
              throw new RdesktopException("SSL negotiation failed: " + e.getMessage());
          }
      }*/

    }

    private void outAndFlush(byte[] packet) throws IOException {
        if (Options.isDebugHexDump()) {
            dump.encode(packet, "X224:outAndFlush");
        }
        out.write(packet);
        out.flush();
    }

    /**
     * Send a self contained iso-pdus
     *
     * @param type one of the following CONNECT_RESPONSE, DISCONNECT_REQUEST
     * @throws IOException when an I/O Error occurs
     */
    private void sendMessage(int type) throws IOException {
        RdpPacket buffer = new RdpPacket(11);//getMemory(11);
        byte[] packet = new byte[11];

        buffer.set8(PROTOCOL_VERSION); // send Version Info
        buffer.set8(0); // reserved byte
        buffer.setBigEndian16(11); // Length
        buffer.set8(6); // Length of Header

        buffer.set8(type); //where code = CR or DR
        buffer.setBigEndian16(0); // Destination reference ( 0 at CC and DR)

        buffer.setBigEndian16(0); // source reference should be a reasonable address we use 0
        buffer.set8(0); //service class
        buffer.copyToByteArray(packet, 0, 0, packet.length);
        outAndFlush(packet);
    }

    /**
     * Send a packet to the server, wrapped in X224 PDU
     *
     * @param buffer Packet containing data to send to server
     * @throws IOException       Network I/O error
     * @throws RdesktopException Protocol error
     */
    public void send(RdpPacket buffer)
            throws RdesktopException, IOException {
        if (rdpSocket != null && out != null) {
            if (buffer.getEnd() < 0) {
                throw new RdesktopException("No End Mark!");
            } else {
                int length = buffer.getEnd();
                byte[] packet = new byte[length];

                buffer.setPosition(0);
                buffer.set8(PROTOCOL_VERSION);  // Version
                buffer.set8(0);                 // reserved
                buffer.setBigEndian16(length);  //length of packet

                buffer.set8(2);                 //length of header
                buffer.set8(DATA_TRANSFER);
                buffer.set8(EOT);
                buffer.copyToByteArray(packet, 0, 0, buffer.getEnd());
                outAndFlush(packet);
            }
        }
    }

    /**
     * Receive a data transfer message from the server
     *
     * @return Packet containing message (as X224 PDU)
     * @throws IOException       Network I/O error
     * @throws RdesktopException Protocol error
     * @throws OrderException    Invalid drawing order
     * @throws CryptoException   Cryptographic Error
     */
    public RdpPacket receive()
            throws IOException, RdesktopException, OrderException, CryptoException {

        int[] type = new int[1];

        RdpPacket buffer = receiveMessage(type);
        if (buffer != null) {
            if (type[0] != DATA_TRANSFER) {
                throw new RdesktopException("Expected DATA_TRANSFER got:" + type[0]);
            }
        }
        return buffer;
    }

    /**
     * Receive a specified number of bytes from the server, and store in a packet
     *
     * @param p      Packet to append data to, null results in a new packet being created
     * @param length Length of data to read
     * @return Packet containing read data, appended to original data if provided
     * @throws IOException Network I/O error
     */
    private RdpPacket tcpReceive(RdpPacket p, int length) throws IOException {
        logger.debug("X224:tcpReceive");
        RdpPacket buffer;

        byte[] packet = new byte[length];

        in.readFully(packet, 0, length);

        if (Options.isDebugHexDump()) {
            dump.encode(packet, "RECEIVE" /*System.out*/);
        }

        if (p == null) {
            buffer = new RdpPacket(length);
            buffer.copyFromByteArray(packet, 0, 0, packet.length);
            buffer.markEnd(length);
            buffer.setStart(buffer.getPosition());
        } else {
            buffer = new RdpPacket((p.getEnd() - p.getStart()) + length);
            buffer.copyFromPacket(p, p.getStart(), 0, p.getEnd());
            buffer.copyFromByteArray(packet, 0, p.getEnd(), packet.length);
            buffer.markEnd(p.size() + packet.length);
            buffer.setPosition(p.getPosition());
            buffer.setStart(0);
        }

        return buffer;
    }

    /**
     * Receive a message from the server
     *
     * @param type Array containing message type, stored in type[0]
     * @return Packet object containing data of message
     * @throws IOException       Network I/O error
     * @throws RdesktopException Protocol error
     * @throws OrderException    Invalid drawing order
     * @throws CryptoException   Cryptographic Error
     */
    private RdpPacket receiveMessage(int[] type)
            throws IOException, RdesktopException, OrderException, CryptoException {

        logger.debug("X224:receiveMessage");
        RdpPacket rdpPacket = null;
        int length;
        int version;
        boolean done = false;

        while (!done) {
            logger.debug("Next packet");
            rdpPacket = tcpReceive(null, 4);
            done = true;        // assume failure
            if (rdpPacket != null) {

                version = rdpPacket.get8();

                if (version == 3) {
                    rdpPacket.incrementPosition(1); // pad
                    length = rdpPacket.getBigEndian16();
                } else {
                    length = rdpPacket.get8();
                    if ((length & 0x80) != 0) {
                        length &= ~0x80;
                        length = (length << 8) + rdpPacket.get8();
                    }
                }

                rdpPacket = tcpReceive(rdpPacket, length - 4);
                if (rdpPacket != null) {
                    if ((version & 3) == 0) {
                        logger.debug("Processing virtualChannels packet");
                        Common.rdp.rdp5Process(rdpPacket, (version & 0x80) != 0);
                        done = false;       // Process next message
                    }
                }
            }
        }

        if (rdpPacket != null) {
            rdpPacket.get8();
            type[0] = rdpPacket.get8();

            if (type[0] == DATA_TRANSFER) {
                logger.debug("Data Transfer Packet");
                rdpPacket.incrementPosition(1); // eot
            } else {
                rdpPacket.incrementPosition(5); // dst_ref, src_ref, class
            }
        }

        return rdpPacket;
    }

    /**
     * Disconnect from an RDP session, closing all sockets
     */
    public void disconnect() {
        if (rdpSocket != null) {
            try {
                sendMessage(DISCONNECT_REQUEST);
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (rdpSocket != null) {
                    rdpSocket.close();
                }
            } catch (IOException e) {
                logger.warn("X224:disconnect exception = " + e.getMessage());
            } finally {
                in = null;
                out = null;
                rdpSocket = null;
            }
        }
    }


    /**
     * Send the server a connection request, detailing client protocol version
     *
     * @throws IOException Network I/O error
     */
    void sendConnectionRequest() throws IOException {

        String userName = Options.getUserName();
        if (userName.length() > 9) {
            userName = userName.substring(0, 9);
        }

        boolean hasUserName = userName.length() > 0;
        String cookie = "Cookie: mstshash=" + userName + "\r\n";

        int length = 11 + (hasUserName ? cookie.length() : 0) + 8;
        RdpPacket buffer = new RdpPacket(length);

        buffer.set8(PROTOCOL_VERSION);  // send Version Info
        buffer.set8(0);                 // reserved byte
        buffer.setBigEndian16(length);  // Length
        buffer.set8(length - 5);        // Length of Header
        buffer.set8(CONNECTION_REQUEST);
        buffer.setBigEndian16(0);       // Destination reference ( 0 at CC and DR)
        buffer.setBigEndian16(0);       // source reference should be a reasonable address we use 0
        buffer.set8(0);                 // service class
        if (hasUserName) {
            logger.debug("Including username");
            buffer.outUint8p(cookie, cookie.length());
        }

        byte[] packet = new byte[length];
        buffer.copyToByteArray(packet, 0, 0, packet.length);

        if (Options.isDebugHexDump()) {
            dump.encode(packet, "X224:sendConnectionRequest");
        }
        out.write(packet);
        out.flush();
    }

    protected void doSocketConnect(InetAddress host, int port) throws IOException {
        int timeout_ms = 3000; // timeout in milliseconds

        rdpSocket = new Socket();
        rdpSocket.connect(new InetSocketAddress(host, port), timeout_ms);
    }
}
