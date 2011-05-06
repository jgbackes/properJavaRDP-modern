/* Secure.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Secure layer of communication
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

import net.propero.rdp.crypto.BlockMessageDigest;
import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.crypto.MD5;
import net.propero.rdp.crypto.RC4;
import net.propero.rdp.crypto.SHA1;
import net.propero.rdp.virtualChannels.VChannels;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import static net.propero.rdp.Options.*;

@SuppressWarnings({"UnusedDeclaration"})
public class Secure {
    static Logger logger = Logger.getLogger(Rdp.class);

    boolean readCert = false;
    private Licence licence = new Licence(this);
    /* constants for the secure layer */
    public static final int SEC_ENCRYPT = 0x0008;
    public static final int SEC_LOGON_INFO = 0x0040;

    static final int SEC_RANDOM_SIZE = 32;
    static final int SEC_MODULUS_SIZE = 64;
    static final int SEC_PADDING_SIZE = 8;
    private static final int SEC_EXPONENT_SIZE = 4;

    private static final int SEC_CLIENT_RANDOM = 0x0001;
    static final int SEC_LICENCE_NEG = 0x0080;

    // [MS-RDPBCGR] Section 2.2.1.3.1 User Data Header (TS_UD_HEADER)
    private static final int CS_CORE = 0xc001; // The data block that follows contains Client Core Data (section 2.2.1.3.2).
    private static final int CS_SECURITY = 0xc002; // The data block that follows contains Client Security Data (section 2.2.1.3.3).
    private static final int CS_NET = 0xc003; // The data block that follows contains Client Network Data (section 2.2.1.3.4).
    private static final int CS_CLUSTER = 0xc004; // The data block that follows contains Client Cluster Data (section 2.2.1.3.5).
    private static final int CS_MONITOR = 0xC005; // The data block that follows contains Client Monitor Data (section 2.2.1.3.6).

    private static final int SC_CORE = 0x0c01; //The data block that follows contains Server Core Data (section 2.2.1.4.2).
    private static final int SC_SECURITY = 0x0c02; // The data block that follows contains Server Security Data (section 2.2.1.4.3).
    private static final int SC_NET = 0x0c03; // The data block that follows contains Server Network Data (section 2.2.1.4.4).

    private static final int SEC_TAG_PUBKEY = 0x0006;
    private static final int SEC_TAG_KEYSIG = 0x0008;

    private static final int SEC_RSA_MAGIC = 0x31415352; /* RSA1 */

    private MCS McsLayer = null;

    boolean licenceIssued = false;
    private RC4 rc4Encrypt = null;
    private RC4 rc4Decrypt = null;
    private RC4 rc4Update = null;
    private BlockMessageDigest sha1 = null;
    private BlockMessageDigest md5 = null;
    private int keylength = 0;
    private int encryptionCount = 0;
    private int descriptionCount = 0;

    private byte[] secureSigningKey = null;

    private byte[] secureEncryptionKey = null;

    private byte[] sec_encrypt_key = null;

    private byte[] sec_decrypt_update_key = null;

    private byte[] sec_encrypt_update_key = null;
    private byte[] sec_crypted_random = null;

    private byte[] pubExp = null;
    private byte[] modulus = null;

    private byte[] serverRandom = null;
    private byte[] clientRandom = new byte[SEC_RANDOM_SIZE];

    private static final byte[] pad_54 = {
            54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
            54, 54, 54,
            54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
            54, 54, 54
    };

    private static final byte[] pad_92 = {
            92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92,
            92, 92, 92, 92, 92, 92, 92,
            92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92,
            92, 92, 92, 92, 92, 92, 92
    };

    private VChannels channels;

    /**
     * Initialise Secure layer of communications
     *
     * @param channels Virtual channels for this connection
     */
    public Secure(VChannels channels) {
        this.channels = channels;
        McsLayer = new MCS(channels);
        Common.mcs = McsLayer;
        rc4Decrypt = new RC4();
        rc4Encrypt = new RC4();
        rc4Update = new RC4();
        sha1 = new SHA1();
        md5 = new MD5();
        secureSigningKey = new byte[16]; // changed from 8 - rdesktop 1.2.0
        secureEncryptionKey = new byte[16];
        sec_encrypt_key = new byte[16];
        sec_decrypt_update_key = new byte[16]; // changed from 8 - rdesktop 1.2.0
        sec_encrypt_update_key = new byte[16]; // changed from 8 - rdesktop 1.2.0
        sec_crypted_random = new byte[64];

    }

    /**
     * Connect to server
     *
     * @param host Address of server to connect to
     * @param port Port to connect to
     * @throws UnknownHostException Unable to resolve host via DNS
     * @throws IOException          Error occured reading or writing
     * @throws RdesktopException    Protocol error
     * @throws SocketException      Socket error
     * @throws CryptoException      Cryptographic error
     * @throws OrderException       Un expected order number
     */
    public void connect(InetAddress host, int port) throws IOException, RdesktopException, CryptoException, OrderException {
        if (getHostname().equals("")) {
            InetAddress localhost = InetAddress.getLocalHost();
            String name = localhost.getHostName();
            StringTokenizer tok = new StringTokenizer(name, ".");
            setHostname(tok.nextToken());
            setHostname(getHostname().trim());
        }

        RdpPacket mcsData = this.sendMcsData();
        McsLayer.connect(host, port, mcsData);

        this.processMcsData(mcsData);

        if (Constants.encryption) {
            this.establishKey();
        }
    }

    /**
     * Connect to server on default port
     *
     * @param host Server to connect to
     * @throws IOException       Network I/O error
     * @throws RdesktopException Protocol error
     * @throws OrderException    Unable to process this order
     * @throws CryptoException   Cryptographic error
     */
    public void connect(InetAddress host) throws IOException, RdesktopException, OrderException, CryptoException {
        this.connect(host, getPort());
    }

    /**
     * Close connection
     */
    public void disconnect() {
        McsLayer.disconnect();
    }

    /**
     * Construct MCS data, including channel, encryption and display options
     * [MS-RDPBCGR] Section 3.2.5.3.3 Sending MCS Connect Initial PDU with GCC
     * Conference Create Request
     *
     * @return Packet populated with MCS data
     */
    public RdpPacket sendMcsData() {
        logger.debug("Secure:sendMcsData");

        RdpPacket buffer = new RdpPacket(512);

        int hostlen = 2 * (getHostname() == null ? 0 : getHostname().length());

        if (hostlen > 30) {
            hostlen = 30;
        }

        int length = 158;
        if (isRdp5()) {
            length += 76 + 12 + 4;
        }

        if (isRdp5() && (channels.getChannelCount() > 0)) {
            length += channels.getChannelCount() * 12 + 8;
        }

        buffer.setBigEndian16(5);    /* TODO: unknown */
        buffer.setBigEndian16(0x14);
        buffer.set8(0x7c);
        buffer.setBigEndian16(1);

        buffer.setBigEndian16(length | 0x8000);    // remaining length

        buffer.setBigEndian16(8);    // length?
        buffer.setBigEndian16(16);                      // conductedPrivileges - Optional field, not used
        buffer.set8(0);                                 // nonConductedPrivileges - Optional field, not used
        buffer.setLittleEndian16(0xc001);               // conferenceDescription - Optional field, not used
        buffer.set8(0);                                 // callerIdentifier - Optional field, not used

        buffer.setLittleEndian32(0x61637544);           // User Data "Duca"
        buffer.setBigEndian16(length - 14 | 0x8000);    // remaining length

        // Client information
        buffer.setLittleEndian16(CS_CORE);
        buffer.setLittleEndian16(isRdp5() ? 212 : 136);    // length
        buffer.setLittleEndian16(isRdp5() ? 4 : 1);
        buffer.setLittleEndian16(8);
        buffer.setLittleEndian16(getWidth());
        buffer.setLittleEndian16(getHeight());
        buffer.setLittleEndian16(0xca01);
        buffer.setLittleEndian16(0xaa03);
        buffer.setLittleEndian32(getKeyLayout());
        buffer.setLittleEndian32(isRdp5() ? 2600 : 419); // or 0ece	// client build? we are 2600 compatible :-)

        /* Unicode name of client, padded to 32 bytes */
        buffer.outUnicodeString(getHostname().toUpperCase(), hostlen);
        buffer.incrementPosition(30 - hostlen);

        buffer.setLittleEndian32(4);
        buffer.setLittleEndian32(0);
        buffer.setLittleEndian32(12);
        buffer.incrementPosition(64);    /* reserved? 4 + 12 doublewords */

        buffer.setLittleEndian16(0xca01); // out_uint16_le(s, 0xca01);
        buffer.setLittleEndian16(isRdp5() ? 1 : 0);

        if (isRdp5()) {
            buffer.setLittleEndian32(0); // out_uint32(s, 0);
            buffer.set8(getServerBpp()); // out_uint8(s, g_server_bpp);
            buffer.setLittleEndian16(0x0700); // out_uint16_le(s, 0x0700);
            buffer.set8(0); // out_uint8(s, 0);
            buffer.setLittleEndian32(1); // out_uint32_le(s, 1);

            buffer.incrementPosition(64);

            buffer.setLittleEndian16(CS_CLUSTER); // out_uint16_le(s, CS_CLUSTER);
            buffer.setLittleEndian16(12); // out_uint16_le(s, 12);
            buffer.setLittleEndian32(isConsoleSession() ? 0xb : 0xd); // out_uint32_le(s, g_console_session ? 0xb : 9);
            buffer.setLittleEndian32(0); // out_uint32(s, 0);
        }

        // Client encryption settings //
        buffer.setLittleEndian16(CS_SECURITY);
        buffer.setLittleEndian16(isRdp5() ? 12 : 8);    // length

        //if(Options.isRdp5()) buffer.setLittleEndian32(Options.encryption ? 0x1b : 0);	// 128-bit encryption supported
        //else
        buffer.setLittleEndian32(isEncryption() ? (isConsoleSession() ? 0xb : 0x3) : 0);

        if (isRdp5())
            buffer.setLittleEndian32(0); // TODO: unknown

        if (isRdp5() && (channels.getChannelCount() > 0)) {
            logger.debug(("getChannelCount is " + channels.getChannelCount()));
            buffer.setLittleEndian16(CS_NET); // out_uint16_le(s, CS_NET);
            buffer.setLittleEndian16(channels.getChannelCount() * 12 + 8); // out_uint16_le(s, g_num_channels * 12 + 8);	// length
            buffer.setLittleEndian32(channels.getChannelCount()); // out_uint32_le(s, g_num_channels);	// number of virtual channels
            for (int i = 0; i < channels.getChannelCount(); i++) {
                logger.debug(("Requesting channel " + channels.channel(i).name()));
                buffer.outUint8p(channels.channel(i).name(), 8); // out_uint8a(s, g_channels[i].name, 8);
                buffer.setBigEndian32(channels.channel(i).flags()); // out_uint32_be(s, g_channels[i].flags);
            }
        }

        buffer.markEnd();
        return buffer;
    }

    /**
     * Handle MCS info from server (server info, encryption info and channel information)
     *
     * @param mcs_data Data received from server
     * @throws RdesktopException Protocol error
     * @throws CryptoException   Cryptographic error
     */
    public void processMcsData(RdpPacket mcs_data) throws RdesktopException, CryptoException {
        logger.debug("Secure.processMcsData");
        int tag;
        int len;
        int length;
        int nexttag;

        mcs_data.incrementPosition(21); // header (T.124 stuff, probably)
        len = mcs_data.get8();

        if ((len & 0x00000080) != 0) {
            len = mcs_data.get8();
            logger.debug("len = " + len);
        }

        while (mcs_data.getPosition() < mcs_data.getEnd()) {
            tag = mcs_data.getLittleEndian16();
            length = mcs_data.getLittleEndian16();

            if (length <= 4)
                return;

            nexttag = mcs_data.getPosition() + length - 4;

            switch (tag) {
                case (Secure.SC_CORE):
                    processSrvInfo(mcs_data);
                    break;
                case (Secure.SC_SECURITY):
                    this.processCryptInfo(mcs_data);
                    break;
                case (Secure.SC_NET):
                    /* TODO: We should parse this information and
                    use it to map RDP5 channels to MCS
                    channels */
                    break;

                default:
                    throw new RdesktopException("Not implemented! Tag:" + tag + "not recognized!");
            }

            mcs_data.setPosition(nexttag);
        }
    }

    /**
     * Read server info from packet, specifically the RDP version of the server
     *
     * @param mcs_data Packet to read
     */
    private void processSrvInfo(RdpPacket mcs_data) {
        setServerRDPVersion(mcs_data.getLittleEndian16()); // in_uint16_le(s, g_server_rdp_version);
        logger.debug(("Server RDP version is " + getServerRDPVersion()));

        setUseRdp5(1 != getServerRDPVersion());
    }

    public void establishKey() throws RdesktopException, IOException, CryptoException {
        int length = SEC_MODULUS_SIZE + SEC_PADDING_SIZE;
        int flags = SEC_CLIENT_RANDOM;
        RdpPacket buffer = this.init(flags, 76);

        buffer.setLittleEndian32(length);

        buffer.copyFromByteArray(this.sec_crypted_random, 0, buffer.getPosition(), SEC_MODULUS_SIZE);
        buffer.incrementPosition(SEC_MODULUS_SIZE);
        buffer.incrementPosition(SEC_PADDING_SIZE);
        buffer.markEnd();
        this.send(buffer, flags);

    }

    public void processCryptInfo(RdpPacket data) throws RdesktopException, CryptoException {
        int rc4_key_size;

        rc4_key_size = this.parseCryptInfo(data);
        if (rc4_key_size == 0) {
            return;
        }

        //this.clientRandom = this.generateRandom(SEC_RANDOM_SIZE);
        logger.debug("readCert = " + readCert);
        if (readCert) {            /* Which means we should use
				   RDP5-style encryption */


            // *** reverse the client random
            //this.reverse(this.clientRandom);

            // *** load the server public key into the stored data for encryption
            /* this.pubExp = this.server_public_key.getPublicExponent().toByteArray();
           this.modulus = this.server_public_key.getModulus().toByteArray();

           System.out.println("Exponent: " + server_public_key.getPublicExponent());
           System.out.println("Modulus: " + server_public_key.getModulus());
           */

            // *** perform encryption
            //this.sec_crypted_random = RSA_public_encrypt(this.clientRandom, this.server_public_key);
            //this.RSAEncrypt(SEC_RANDOM_SIZE);

            //this.RSAEncrypt(SEC_RANDOM_SIZE);

            // *** reverse the random data back
            //this.reverse(this.sec_crypted_random);

        } else {
            this.generateRandom();
            this.RSAEncrypt(SEC_RANDOM_SIZE);
        }
        this.generateKeys(rc4_key_size);
    }

    /**
     * Intialise a packet at the Secure layer
     *
     * @param flags  Encryption flags
     * @param length Length of packet
     * @return Intialised packet
     * @throws RdesktopException Protocol error
     */
    public RdpPacket init(int flags, int length) throws RdesktopException {
        int headerlength;
        RdpPacket buffer;

        if (!this.licenceIssued)
            headerlength = ((flags & SEC_ENCRYPT) != 0) ? 12 : 4;
        else
            headerlength = ((flags & SEC_ENCRYPT) != 0) ? 12 : 0;

        buffer = McsLayer.init(length + headerlength);
        buffer.pushLayer(RdpPacket.SECURE_HEADER, headerlength);
        //buffer.setHeader(RdpPacket.SECURE_HEADER);
        //buffer.incrementPosition(headerlength);
        //buffer.setStart(buffer.getPosition());
        return buffer;
    }

    /**
     * Send secure data on the global channel
     *
     * @param sec_data Data to send
     * @param flags    Encryption flags
     * @throws RdesktopException Protocol error
     * @throws IOException       Network I/O error
     * @throws CryptoException   Cryptographic error
     */
    public void send(RdpPacket sec_data, int flags) throws RdesktopException, IOException, CryptoException {
        sendToChannel(sec_data, flags, MCS.MCS_GLOBAL_CHANNEL);
    }

    /**
     * Prepare data as a Secure PDU and pass down to the MCS layer
     *
     * @param sec_data Data to send
     * @param flags    Encryption flags
     * @param channel  Channel over which to send data
     * @throws RdesktopException Protocol error
     * @throws IOException       Network I/O error
     * @throws CryptoException   Cryptographic error
     */
    public void sendToChannel(RdpPacket sec_data, int flags, int channel) throws RdesktopException, IOException, CryptoException {
        int dataLength;
        byte[] signature;
        byte[] data;
        byte[] buffer;

        sec_data.setPosition(sec_data.getHeader(RdpPacket.SECURE_HEADER));

        if (!this.licenceIssued || (flags & SEC_ENCRYPT) != 0) {
            sec_data.setLittleEndian32(flags);
        }
        if ((flags & SEC_ENCRYPT) != 0) {
            flags &= ~SEC_ENCRYPT;
            dataLength = sec_data.getEnd() - sec_data.getPosition() - 8;
            data = new byte[dataLength];

            sec_data.copyToByteArray(data, 0, sec_data.getPosition() + 8, dataLength);
            signature = this.sign(this.secureSigningKey, 8, this.keylength, data, dataLength);

            buffer = this.encrypt(data, dataLength);

            sec_data.copyFromByteArray(signature, 0, sec_data.getPosition(), 8);
            sec_data.copyFromByteArray(buffer, 0, sec_data.getPosition() + 8, dataLength);

        }
        //McsLayer.send(sec_data);
        McsLayer.sendToChannel(sec_data, channel);
    }

    /**
     * Generate MD5 signature
     *
     * @param session_key Key with which to sign data
     * @param length      Length of signature
     * @param keylen      Length of key
     * @param data        Data to sign
     * @param datalength  Length of data to sign
     * @return Signature for data
     * @throws CryptoException Cryptographic error
     */
    public byte[] sign(byte[] session_key, int length, int keylen, byte[] data, int datalength) throws CryptoException {
        byte[] shaSignature;
        byte[] md5Signature;
        byte[] headerLength = new byte[4];
        byte[] signature = new byte[length];

        Utilities.setLittleEndian32(headerLength, datalength);

        sha1.engineReset();
        sha1.engineUpdate(session_key, 0, keylen/*length*/);
        sha1.engineUpdate(pad_54, 0, 40);
        sha1.engineUpdate(headerLength, 0, 4);
        sha1.engineUpdate(data, 0, datalength);
        shaSignature = sha1.engineDigest();
        sha1.engineReset();

        md5.engineReset();
        md5.engineUpdate(session_key, 0, keylen/*length*/);
        md5.engineUpdate(pad_92, 0, 48);
        md5.engineUpdate(shaSignature, 0, 20);
        md5Signature = md5.engineDigest();
        md5.engineReset();

        System.arraycopy(md5Signature, 0, signature, 0, length);
        return signature;
    }

    /**
     * Encrypt specified number of bytes from provided data using RC4 algorithm
     *
     * @param data   Data to encrypt
     * @param length Number of bytes to encrypt (from start of array)
     * @return Encrypted data
     * @throws CryptoException Cryptographic error
     */
    public byte[] encrypt(byte[] data, int length) throws CryptoException {
        byte[] buffer;
        if (this.encryptionCount == 4096) {
            sec_encrypt_key = this.update(this.sec_encrypt_key, this.sec_encrypt_update_key);
            byte[] key = new byte[this.keylength];
            System.arraycopy(this.sec_encrypt_key, 0, key, 0, this.keylength);
            this.rc4Encrypt.engineInitEncrypt(key);
//		logger.debug("Packet encryptionCount="+encryptionCount);
            this.encryptionCount = 0;
        }
        //this.rc4.engineInitEncrypt(this.rc4_encrypt_key);
        buffer = this.rc4Encrypt.crypt(data, 0, length);
        this.encryptionCount++;
        return buffer;
    }

    /**
     * Encrypt provided data using the RC4 algorithm
     *
     * @param data Data to encrypt
     * @return Encrypted data
     * @throws CryptoException Cryptographic error
     */
    public byte[] encrypt(byte[] data) throws CryptoException {
        byte[] buffer;
        if (this.encryptionCount == 4096) {
            sec_encrypt_key = this.update(this.sec_encrypt_key, this.sec_encrypt_update_key);
            byte[] key = new byte[this.keylength];
            System.arraycopy(this.sec_encrypt_key, 0, key, 0, this.keylength);
            this.rc4Encrypt.engineInitEncrypt(key);
            //	logger.debug("Packet encryptionCount="+encryptionCount);
            this.encryptionCount = 0;
        }
//	this.rc4.engineInitEncrypt(this.rc4_encrypt_key);

        buffer = this.rc4Encrypt.crypt(data);
        this.encryptionCount++;
        return buffer;
    }

    /**
     * Decrypt specified number of bytes from provided data using RC4 algorithm
     *
     * @param data   Data to decrypt
     * @param length Number of bytes to decrypt (from start of array)
     * @return Decrypted data
     * @throws CryptoException Cryptographic error
     */
    public byte[] decrypt(byte[] data, int length) throws CryptoException {
        byte[] buffer;
        if (this.descriptionCount == 4096) {
            secureEncryptionKey = this.update(this.secureEncryptionKey, this.sec_decrypt_update_key);
            byte[] key = new byte[this.keylength];
            System.arraycopy(this.secureEncryptionKey, 0, key, 0, this.keylength);
            this.rc4Decrypt.engineInitDecrypt(key);
//		logger.debug("Packet descriptionCount="+descriptionCount);
            this.descriptionCount = 0;
        }
        //this.rc4.engineInitDecrypt(this.rc4_decrypt_key);
        buffer = this.rc4Decrypt.crypt(data, 0, length);
        this.descriptionCount++;
        return buffer;
    }

    /**
     * Decrypt provided data using RC4 algorithm
     *
     * @param data Data to decrypt
     * @return Decrypted data
     * @throws CryptoException Cryptographic error
     */
    public byte[] decrypt(byte[] data) throws CryptoException {
        byte[] buffer;
        if (this.descriptionCount == 4096) {
            secureEncryptionKey = this.update(this.secureEncryptionKey, this.sec_decrypt_update_key);
            byte[] key = new byte[this.keylength];
            System.arraycopy(this.secureEncryptionKey, 0, key, 0, this.keylength);
            this.rc4Decrypt.engineInitDecrypt(key);
//		logger.debug("Packet descriptionCount="+descriptionCount);
            this.descriptionCount = 0;
        }
        //this.rc4.engineInitDecrypt(this.rc4_decrypt_key);

        buffer = this.rc4Decrypt.crypt(data);
        this.descriptionCount++;
        return buffer;
    }

    /**
     * Read encryption information from a Secure layer PDU, obtaining and storing
     * level of encryption and any keys received
     *
     * @param data Packet to read encryption information from
     * @return Size of RC4 key
     * @throws RdesktopException Protocol error
     */
    public int parseCryptInfo(RdpPacket data) throws RdesktopException {
        logger.debug("Secure.parseCryptInfo");
        int encryption_level;
        int random_length;
        int RSA_info_length;
        int tag;
        int length;
        int next_tag;
        int end;
        int rc4_key_size;

        rc4_key_size = data.getLittleEndian32(); // 1 = 40-Bit 2 = 128 Bit
        encryption_level = data.getLittleEndian32(); // 1 = low, 2 = medium, 3 = high
        if (encryption_level == 0) { // no encryption
            return 0;
        }
        random_length = data.getLittleEndian32();
        RSA_info_length = data.getLittleEndian32();

        if (random_length != SEC_RANDOM_SIZE) {
            throw new RdesktopException("Wrong Size of Random! Got" + random_length + "expected" + SEC_RANDOM_SIZE);
        }
        this.serverRandom = new byte[random_length];
        data.copyToByteArray(this.serverRandom, 0, data.getPosition(), random_length);
        data.incrementPosition(random_length);

        end = data.getPosition() + RSA_info_length;

        if (end > data.getEnd()) {
            logger.debug("Reached end of crypt info prematurely ");
            return 0;
        }

        //data.incrementPosition(12); // TODO: unknown bytes
        int flags = data.getLittleEndian32(); // in_uint32_le(s, flags);	// 1 = RDP4-style, 0x80000002 = X.509
        logger.debug("Flags = 0x" + Integer.toHexString(flags));
        if ((flags & 1) != 0) {
            logger.debug(("We're going for the RDP4-style encryption"));
            data.incrementPosition(8); //in_uint8s(s, 8);	// TODO: unknown

            while (data.getPosition() < data.getEnd()) {
                tag = data.getLittleEndian16();
                length = data.getLittleEndian16();

                next_tag = data.getPosition() + length;

                switch (tag) {

                    case (Secure.SEC_TAG_PUBKEY):

                        if (!parsePublicKey(data)) {
                            return 0;
                        }

                        break;
                    case (Secure.SEC_TAG_KEYSIG):
                        // Microsoft issued a key but we don't care
                        break;

                    default:
                        throw new RdesktopException("Unimplemented decrypt tag " + tag);
                }
                data.setPosition(next_tag);
            }

            if (data.getPosition() == data.getEnd()) {
                return rc4_key_size;
            } else {
                logger.warn("End not reached!");
                return 0;
            }

        } else {
            //data.incrementPosition(4); // number of certificates
            int num_certs = data.getLittleEndian32();

            int cacert_len = data.getLittleEndian32();
            data.incrementPosition(cacert_len);
            int cert_len = data.getLittleEndian32();
            data.incrementPosition(cert_len);

            readCert = true;

            return rc4_key_size;
        }


    }

    /*
        public X509Certificate readCert(int length, RdpPacket data){
            byte[] buf = new byte[length];

            data.copyToByteArray(buf,0,data.getPosition(),buf.length);
            data.incrementPosition(length);

            for(int i = 0; i < buf.length; i++){
                buf[i] = (byte) (buf[i] & 0xFF);
            }

            ByteArrayInputStream bIn = new ByteArrayInputStream(buf);
            X509Certificate cert = null;
            CertificateFactory cf = null;
            try {
                cf = CertificateFactory.getInstance("X.509");
                cert = (X509Certificate)cf.generateCertificate(bIn);
            } catch (CertificateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            bIn.reset();
            return cert;
        }
    */
    public void generateRandom() {
        /*try{
       SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
       random.nextBytes(this.clientRandom);
   } catch(NoSuchAlgorithmException e){logger.warn("No Such Random Algorithm");}*/
    }

    public void RSAEncrypt(int length) throws RdesktopException {
        byte[] inr = new byte[length];
        //int outlength = 0;
        BigInteger mod;
        BigInteger exp;
        BigInteger x;

        this.reverse(this.pubExp);
        this.reverse(this.modulus);
        System.arraycopy(this.clientRandom, 0, inr, 0, length);
        this.reverse(inr);

        if ((this.modulus[0] & 0x80) != 0) {
            byte[] temp = new byte[this.modulus.length + 1];
            System.arraycopy(this.modulus, 0, temp, 1, this.modulus.length);
            temp[0] = 0;
            mod = new BigInteger(temp);
        } else {
            mod = new BigInteger(this.modulus);
        }
        if ((this.pubExp[0] & 0x80) != 0) {
            byte[] temp = new byte[this.pubExp.length + 1];
            System.arraycopy(this.pubExp, 0, temp, 1, this.pubExp.length);
            temp[0] = 0;
            exp = new BigInteger(temp);
        } else {
            exp = new BigInteger(this.pubExp);
        }
        if ((inr[0] & 0x80) != 0) {
            byte[] temp = new byte[inr.length + 1];
            System.arraycopy(inr, 0, temp, 1, inr.length);
            temp[0] = 0;
            x = new BigInteger(temp);
        } else {
            x = new BigInteger(inr);
        }

        BigInteger y = x.modPow(exp, mod);
        this.sec_crypted_random = y.toByteArray();

        if ((this.sec_crypted_random[0] & 0x80) != 0) {
            throw new RdesktopException("Wrong Sign! Expected positive Integer!");
        }

        if (this.sec_crypted_random.length > SEC_MODULUS_SIZE) {
            logger.warn("sec_crypted_random too big!"); /* FIXME */
        }
        this.reverse(this.sec_crypted_random);

        byte[] temp = new byte[SEC_MODULUS_SIZE];

        if (this.sec_crypted_random.length < SEC_MODULUS_SIZE) {
            System.arraycopy(this.sec_crypted_random, 0, temp, 0, this.sec_crypted_random.length);
            for (int i = this.sec_crypted_random.length; i < temp.length; i++) {
                temp[i] = 0;
            }
            this.sec_crypted_random = temp;

        }

    }


    /**
     * Read in a public key from a provided Secure layer PDU, and store
     * in this.pubExp and this.modulus
     * <p/>
     * [MS-RDPBCGR] Section 2.2.1.4.3.1.1.1 RSA Public Key (RSA_PUBLIC_KEY)
     *
     * @param data Secure layer PDU containing key data
     * @return True if key successfully read
     * @throws RdesktopException Protocol Error
     */
    public boolean parsePublicKey(RdpPacket data) throws RdesktopException {
        int magic;      // A 32-bit, unsigned integer. The sentinel value. This field MUST be set to 0x31415352.
        int keylen;     // A 32-bit, unsigned integer. The size in bytes of the modulus field. This value is directly related to the bitlen field and MUST be ((bitlen / 8) + 8) bytes.
        int bitlen;     // A 32-bit, unsigned integer. The number of bits in the public key modulus.
        int datalen;    // A 32-bit, unsigned integer. The maximum number of bytes that can be encoded using the public key.

        magic = data.getLittleEndian32();

        if (magic != SEC_RSA_MAGIC) {
            throw new RdesktopException("Wrong magic! Expected (" + SEC_RSA_MAGIC + ") got:" + magic);
        }

        keylen = data.getLittleEndian32();
        bitlen = data.getLittleEndian32();
        if (keylen != ((bitlen / 8) + 8)) {
            throw new RdesktopException("Wrong modulus size! Expected (" + ((bitlen / 8) + 8) + ") got:" + keylen);
        }

        datalen = data.getLittleEndian32();
        if (datalen == 0) {
            throw new RdesktopException("NULL datalen!");
        }

        this.pubExp = new byte[SEC_EXPONENT_SIZE];
        data.copyToByteArray(this.pubExp, 0, data.getPosition(), SEC_EXPONENT_SIZE);
        data.incrementPosition(SEC_EXPONENT_SIZE);
        this.modulus = new byte[SEC_MODULUS_SIZE];
        data.copyToByteArray(this.modulus, 0, data.getPosition(), SEC_MODULUS_SIZE);
        data.incrementPosition(SEC_MODULUS_SIZE);
        data.incrementPosition(SEC_PADDING_SIZE);

        return data.getPosition() <= data.getEnd();
    }

    /**
     * Reverse the values in the provided array
     *
     * @param data Array as passed reversed on return
     */
    public void reverse(byte[] data) {
        int i;
        int j;
        byte temp;

        for (i = 0, j = data.length - 1; i < j; i++, j--) {
            temp = data[i];
            data[i] = data[j];
            data[j] = temp;
        }
    }

    public void reverse(byte[] data, int length) {
        int i;
        int j;
        byte temp;

        for (i = 0, j = length - 1; i < j; i++, j--) {
            temp = data[i];
            data[i] = data[j];
            data[j] = temp;
        }
    }

    public byte[] hash48(byte[] in, byte[] salt1, byte[] salt2, int salt) throws CryptoException {
        byte[] shaSignature;
        byte[] pad = new byte[4];
        byte[] out = new byte[48];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j <= i; j++) {
                pad[j] = (byte) (salt + i);
            }
            sha1.engineUpdate(pad, 0, i + 1);
            sha1.engineUpdate(in, 0, 48);
            sha1.engineUpdate(salt1, 0, 32);
            sha1.engineUpdate(salt2, 0, 32);
            shaSignature = sha1.engineDigest();
            sha1.engineReset();

            md5.engineUpdate(in, 0, 48);
            md5.engineUpdate(shaSignature, 0, 20);
            System.arraycopy(md5.engineDigest(), 0, out, i * 16, 16);
        }

        return out;
    }

    public byte[] hash16(byte[] in, byte[] salt1, byte[] salt2, int in_position) throws CryptoException {

        md5.engineUpdate(in, in_position, 16);
        md5.engineUpdate(salt1, 0, 32);
        md5.engineUpdate(salt2, 0, 32);
        return md5.engineDigest();
    }

    /**
     * Generate a 40-bit key and store in the parameter key.
     *
     * @param key Result goes in here
     */
    public void make40bit(byte[] key) {
        key[0] = (byte) 0xd1;
        key[1] = (byte) 0x26;
        key[2] = (byte) 0x9e;
    }

    /**
     * @param key       key value
     * @param updateKey update key
     * @return updated key
     * @throws CryptoException Cryptographic error
     */
    public byte[] update(byte[] key, byte[] updateKey) throws CryptoException {
        byte[] theKey;
        byte[] shaSignature;
        byte[] update = new byte[this.keylength]; // changed from 8 - rdesktop 1.2.0

        sha1.engineReset();
        sha1.engineUpdate(updateKey, 0, keylength);
        sha1.engineUpdate(pad_54, 0, 40);
        sha1.engineUpdate(key, 0, keylength); // changed from 8 - rdesktop 1.2.0
        shaSignature = sha1.engineDigest();
        sha1.engineReset();

        md5.engineReset();
        md5.engineUpdate(updateKey, 0, keylength); // changed from 8 - rdesktop 1.2.0
        md5.engineUpdate(pad_92, 0, 48);
        md5.engineUpdate(shaSignature, 0, 20);
        theKey = md5.engineDigest();
        md5.engineReset();

        System.arraycopy(theKey, 0, update, 0, this.keylength);
        rc4Update.engineInitDecrypt(update);
        // added
        theKey = rc4Update.crypt(theKey, 0, this.keylength);

        if (this.keylength == 8) {
            this.make40bit(theKey);
        }

        return theKey;
    }

    /**
     * Receive a Secure layer PDU from the MCS layer
     *
     * @return Packet representing received Secure PDU
     * @throws RdesktopException Protocol error
     * @throws IOException       Network I/O error
     * @throws CryptoException   Cryptographic error
     * @throws OrderException    Error processing drawing order
     */
    public RdpPacket receive() throws RdesktopException, IOException, CryptoException, OrderException {
        int sec_flags;
        RdpPacket buffer;
        while (true) {
            int[] channel = new int[1];
            buffer = McsLayer.receive(channel);
            if (buffer == null)
                return null;
            buffer.setHeader(RdpPacket.SECURE_HEADER);
            if (Constants.encryption || (!this.licenceIssued)) {

                sec_flags = buffer.getLittleEndian32();

                if ((sec_flags & SEC_LICENCE_NEG) != 0) {
                    licence.process(buffer);
                    continue;
                }
                if ((sec_flags & SEC_ENCRYPT) != 0) {
                    buffer.incrementPosition(8); //signature
                    byte[] data = new byte[buffer.size() - buffer.getPosition()];
                    buffer.copyToByteArray(data, 0, buffer.getPosition(), data.length);
                    byte[] packet = this.decrypt(data);

                    buffer.copyFromByteArray(packet, 0, buffer.getPosition(), packet.length);

                    //buffer.setStart(buffer.getPosition());
                    //return buffer;
                }
            }

            if (channel[0] != MCS.MCS_GLOBAL_CHANNEL) {
                channels.channelProcess(buffer, channel[0]);
                continue;
            }

            buffer.setStart(buffer.getPosition());
            return buffer;
        }
    }

    /**
     * Generate encryption keys of applicable size for connection
     *
     * @param rc4KeySize Size of keys to generate (1 if 40-bit encryption, otherwise 128-bit)
     * @throws CryptoException Cryptographic error
     */
    public void generateKeys(int rc4KeySize) throws CryptoException {
        byte[] sessionKey;
        byte[] tempHash;
        byte[] input = new byte[48];

        System.arraycopy(this.clientRandom, 0, input, 0, 24);
        System.arraycopy(this.serverRandom, 0, input, 24, 24);

        tempHash = this.hash48(input, this.clientRandom, this.serverRandom, 65);
        sessionKey = this.hash48(tempHash, this.clientRandom, this.serverRandom, 88);

        System.arraycopy(sessionKey, 0, this.secureSigningKey, 0, 16); // changed from 8 - rdesktop 1.2.0

        this.secureEncryptionKey = this.hash16(sessionKey, this.clientRandom, this.serverRandom, 16);
        this.sec_encrypt_key = this.hash16(sessionKey, this.clientRandom, this.serverRandom, 32);

        if (rc4KeySize == 1) {
            logger.info("40 Bit Encryption enabled");
            this.make40bit(this.secureSigningKey);
            this.make40bit(this.secureEncryptionKey);
            this.make40bit(this.sec_encrypt_key);
            this.keylength = 8;
        } else {
            logger.info("128 Bit Encryption enabled");
            this.keylength = 16;
        }

        System.arraycopy(this.secureEncryptionKey, 0, this.sec_decrypt_update_key, 0, 16); // changed from 8 - rdesktop 1.2.0
        System.arraycopy(this.sec_encrypt_key, 0, this.sec_encrypt_update_key, 0, 16); // changed from 8 - rdesktop 1.2.0


        byte[] key = new byte[this.keylength];
        System.arraycopy(this.sec_encrypt_key, 0, key, 0, this.keylength);
        rc4Encrypt.engineInitEncrypt(key);
        System.arraycopy(this.secureEncryptionKey, 0, key, 0, this.keylength);
        rc4Decrypt.engineInitDecrypt(key);
    }

    /**
     * @return MCS user ID
     */
    public int getUserID() {
        return McsLayer.getUserID();
    }
}
