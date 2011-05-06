/* Licence.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.7 $
 * Author: $Author: telliott $
 * Date: $Date: 2005/09/27 14:15:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handles request, receipt and processing of
 *          licences
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
// Created on 02-Jul-2003

package net.propero.rdp;

import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.crypto.RC4;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.prefs.Preferences;

/**
 * [MS-RDPELE] Section 2.2.2 Licensing PDU (TS_LICENSING_PDU)
 * <p/>
 * The Licensing PDU packet encapsulates licensing messages that are
 * exchanged between a client and a terminal server.
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc241913(v=prot.10).aspx">
 *      [MS-RDPELE] Section 2.2.2 Licensing PDU</a>
 */
public class Licence {
    static Logger logger = Logger.getLogger(Rdp.class);

    private Secure secure = null;

    Licence(Secure s) {
        secure = s;
        licenceKey = new byte[16];
        licenceSignatureKey = new byte[16];
    }

    private byte[] licenceKey = null;
    private byte[] licenceSignatureKey = null;
    private byte[] inputToken = null;

    /* constants for the licence negotiation */
    private static final int LICENCE_TOKEN_SIZE = 10;
    private static final int LICENCE_HWID_SIZE = 20;
    private static final int LICENCE_SIGNATURE_SIZE = 16;

    private static final int LICENSE_REQUEST = 0x01; // The Licensing PDU is a License Request PDU, and the LicensingMessage contains a Server License Request.
    private static final int PLATFORM_CHALLENGE = 0x02; // The Licensing PDU is a Platform Challenge PDU, and the LicensingMessage contains a Server Platform Challenge.
    private static final int NEW_LICENSE = 0x03; // The Licensing PDU is a New License PDU, and the LicensingMessage contains a Server New License structure
    private static final int UPGRADE_LICENSE = 0x04; // The Licensing PDU is an Upgrade License PDU, and the LicensingMessage contains a Server Upgrade License structure.
    private static final int LICENSE_INFO = 0x12;// The Licensing PDU is a License Info PDU, and the LicensingMessage contains a Client License Information structure.
    private static final int NEW_LICENSE_REQUEST = 0x13; // The Licensing PDU is a New License Request PDU, and the LicensingMessage contains a Client New License Request structure.
    private static final int PLATFORM_CHALLENGE_RESPONSE = 0x15; // The Licensing PDU is a Platform Challenge Response PDU, and the LicensingMessage contains a Client Platform Challenge Response structure.

    private static final int ERROR_ALERT = 0xff; // The Licensing PDU is a Licensing Error Message PDU, and the LicensingMessage contains a license error message structure.

    private static final int LICENCE_TAG_USER = 0x000f;
    private static final int LICENCE_TAG_HOST = 0x0010;


    public byte[] generateHashWidth()
            throws UnsupportedEncodingException {

        byte[] hashWidth = new byte[LICENCE_HWID_SIZE];
        Utilities.setLittleEndian32(hashWidth, 2);
        byte[] name = Options.getHostname().getBytes("US-ASCII");

        if (name.length > LICENCE_HWID_SIZE - 4) {
            System.arraycopy(name, 0, hashWidth, 4, LICENCE_HWID_SIZE - 4);
        } else {
            System.arraycopy(name, 0, hashWidth, 4, name.length);
        }
        return hashWidth;
    }

    /**
     * Process and handle licence data from a packet
     *
     * @param data Packet containing licence data
     * @throws RdesktopException Protocol error
     * @throws IOException       Network I/O error
     * @throws CryptoException   Cryptographic error
     */
    public void process(RdpPacket data) throws RdesktopException, IOException, CryptoException {
        int bMsgType;
        
        bMsgType = data.get8();
        data.incrementPosition(3); // version, length

        switch (bMsgType) {

            case (LICENSE_REQUEST):
                this.processDemand(data);
                break;

            case (PLATFORM_CHALLENGE):
                this.processAuthRequest(data);
                break;

            case (NEW_LICENSE):
                this.processIssue(data);
                break;

            case (UPGRADE_LICENSE):
                logger.debug("Presented licence was accepted!");
                break;

            case (ERROR_ALERT):
                logger.debug("ERROR_ALERT");
                break;

            default:
                logger.warn("got licence bMsgType: " + bMsgType);
        }
    }

    /**
     * Process a demand for a licence. Find a license and transmit to server, or request new licence
     *
     * @param data Packet containing details of licence demand
     * @throws UnsupportedEncodingException Requested encoding is not supported
     * @throws RdesktopException            Protocol error
     * @throws IOException                  Network I/O error
     * @throws CryptoException              Cryptographic error
     */
    public void processDemand(RdpPacket data)
            throws RdesktopException, IOException, CryptoException {
        byte[] nullData = new byte[Secure.SEC_MODULUS_SIZE];
        byte[] serverRandom = new byte[Secure.SEC_RANDOM_SIZE];
        byte[] host = Options.getHostname().getBytes("US-ASCII");
        byte[] user = Options.getUserName().getBytes("US-ASCII");

        /*retrieve the server random */
        data.copyToByteArray(serverRandom, 0, data.getPosition(), serverRandom.length);
        data.incrementPosition(serverRandom.length);

        /* Null client keys are currently used */
        this.generateKeys(nullData, serverRandom, nullData);

        if (!Options.isBuiltInLicence() && Options.isLoadLicence()) {
            byte[] licenceData = loadLicence();
            if ((licenceData != null) && (licenceData.length > 0)) {
                logger.debug("licence_data.length = " + licenceData.length);
                /* Generate a signature for the HWID buffer */
                byte[] hwid = generateHashWidth();
                byte[] signature = secure.sign(this.licenceSignatureKey, 16, 16, hwid, hwid.length);

                /*now crypt the hwid */
                RC4 rc4_licence = new RC4();
                byte[] cryptKey = new byte[this.licenceKey.length];
                byte[] cryptHwid = new byte[LICENCE_HWID_SIZE];
                System.arraycopy(this.licenceKey, 0, cryptKey, 0, this.licenceKey.length);
                rc4_licence.engineInitEncrypt(cryptKey);
                rc4_licence.crypt(hwid, 0, LICENCE_HWID_SIZE, cryptHwid, 0);

                present(nullData, nullData, licenceData, licenceData.length, cryptHwid, signature);
                logger.debug("Presented stored licence to server!");
                return;
            }
        }
        this.sendRequest(nullData, nullData, user, host);
    }

    /**
     * Handle an authorisation request, based on a licence signature (store signatures in this Licence object
     *
     * @param data Packet containing details of request
     * @return True if signature is read successfully
     * @throws RdesktopException Protocol error
     */
    public boolean parseAuthRequest(RdpPacket data) throws RdesktopException {

        int tokenLength;
        byte[] inputSignature;

        data.incrementPosition(6); // TODO: unknown

        tokenLength = data.getLittleEndian16();

        if (tokenLength == LICENCE_TOKEN_SIZE) {
            this.inputToken = new byte[tokenLength];
            data.copyToByteArray(this.inputToken, 0, data.getPosition(), tokenLength);
            data.incrementPosition(tokenLength);
            inputSignature = new byte[LICENCE_SIGNATURE_SIZE];
            data.copyToByteArray(inputSignature, 0, data.getPosition(), LICENCE_SIGNATURE_SIZE);
            data.incrementPosition(LICENCE_SIGNATURE_SIZE);
        } else {
            throw new RdesktopException("Wrong Token length!");
        }
        return data.getPosition() == data.getEnd();
    }

    /**
     * Respond to authorisation request, with token, hwid and signature, send response to server
     *
     * @param token     Token data
     * @param cryptHwid HWID for encryption
     * @param signature Signature data
     *
     * @throws RdesktopException Protocol error
     * @throws IOException       Network I/O error
     * @throws CryptoException   Cryptographic error
     */
    public void sendAuthResponse(byte[] token, byte[] cryptHwid, byte[] signature) throws RdesktopException, IOException, CryptoException {
        int secFlags = Secure.SEC_LICENCE_NEG;
        int length = 58;
        RdpPacket data;

        data = secure.init(secFlags, length + 2);

        data.set8(PLATFORM_CHALLENGE_RESPONSE);
        data.set8(2); // version
        data.setLittleEndian16(length);

        data.setLittleEndian16(1);
        data.setLittleEndian16(LICENCE_TOKEN_SIZE);
        data.copyFromByteArray(token, 0, data.getPosition(), LICENCE_TOKEN_SIZE);
        data.incrementPosition(LICENCE_TOKEN_SIZE);

        data.setLittleEndian16(1);
        data.setLittleEndian16(LICENCE_HWID_SIZE);
        data.copyFromByteArray(cryptHwid, 0, data.getPosition(), LICENCE_HWID_SIZE);
        data.incrementPosition(LICENCE_HWID_SIZE);

        data.copyFromByteArray(signature, 0, data.getPosition(), LICENCE_SIGNATURE_SIZE);
        data.incrementPosition(LICENCE_SIGNATURE_SIZE);
        data.markEnd();
        secure.send(data, secFlags);
    }

    /**
     * Present a licence to the server
     *
     * @param clientRandom Client random number
     * @param rsaData      Rsa hash
     * @param licenceData  License data
     * @param licenceSize  Size of the license data
     * @param hashWidth    Width of the hash
     * @param signature    Signature
     *
     * @throws RdesktopException Protocol error
     * @throws IOException       Network I/O error
     * @throws CryptoException   Cryptographic error
     */
    public void present(byte[] clientRandom, byte[] rsaData,
                        byte[] licenceData, int licenceSize,
                        byte[] hashWidth, byte[] signature)
            throws RdesktopException, IOException, CryptoException {
        int securityFlags = Secure.SEC_LICENCE_NEG;
        int length = /* rdesktop is 16 not 20, but this must be wrong?! */
                20 + Secure.SEC_RANDOM_SIZE + Secure.SEC_MODULUS_SIZE + Secure.SEC_PADDING_SIZE +
                        licenceSize + LICENCE_HWID_SIZE + LICENCE_SIGNATURE_SIZE;

        RdpPacket s = secure.init(securityFlags, length + 4);

        s.set8(LICENSE_INFO);
        s.set8(2); // version
        s.setLittleEndian16(length);

        s.setLittleEndian32(1);
        s.setLittleEndian16(0);
        s.setLittleEndian16(0x0201);

        s.copyFromByteArray(clientRandom, 0, s.getPosition(), Secure.SEC_RANDOM_SIZE);
        s.incrementPosition(Secure.SEC_RANDOM_SIZE);
        s.setLittleEndian16(0);
        s.setLittleEndian16((Secure.SEC_MODULUS_SIZE + Secure.SEC_PADDING_SIZE));
        s.copyFromByteArray(rsaData, 0, s.getPosition(), Secure.SEC_MODULUS_SIZE);
        s.incrementPosition(Secure.SEC_MODULUS_SIZE);
        s.incrementPosition(Secure.SEC_PADDING_SIZE);

        s.setLittleEndian16(1);
        s.setLittleEndian16(licenceSize);
        s.copyFromByteArray(licenceData, 0, s.getPosition(), licenceSize);
        s.incrementPosition(licenceSize);

        s.setLittleEndian16(1);
        s.setLittleEndian16(LICENCE_HWID_SIZE);
        s.copyFromByteArray(hashWidth, 0, s.getPosition(), LICENCE_HWID_SIZE);
        s.incrementPosition(LICENCE_HWID_SIZE);
        s.copyFromByteArray(signature, 0, s.getPosition(), LICENCE_SIGNATURE_SIZE);
        s.incrementPosition(LICENCE_SIGNATURE_SIZE);

        s.markEnd();
        secure.send(s, securityFlags);
    }

    /**
     * Process an authorisation request
     *
     * @param data Packet containing request details
     * 
     * @throws UnsupportedEncodingException Requested encoding is not supported
     * @throws RdesktopException            Protocol error
     * @throws IOException                  Network I/O error
     * @throws CryptoException              Cryptographic error
     */
    public void processAuthRequest(RdpPacket data) throws RdesktopException, IOException, CryptoException {

        byte[] outToken = new byte[LICENCE_TOKEN_SIZE];
        byte[] decryptToken = new byte[LICENCE_TOKEN_SIZE];

        byte[] cryptHashWidthSize = new byte[LICENCE_HWID_SIZE];
        byte[] sealedBuffer = new byte[LICENCE_TOKEN_SIZE + LICENCE_HWID_SIZE];
        byte[] outSig;
        RC4 rc4Licence = new RC4();
        byte[] cryptKey;

        /* parse incoming packet and save encrypted token */
        if (!parseAuthRequest(data)) {
            throw new RdesktopException("Authentication Request was corrupt!");
        }
        System.arraycopy(this.inputToken, 0, outToken, 0, LICENCE_TOKEN_SIZE);

        /* decrypt token. It should read TEST in Unicode */
        cryptKey = new byte[this.licenceKey.length];
        System.arraycopy(this.licenceKey, 0, cryptKey, 0, this.licenceKey.length);
        rc4Licence.engineInitDecrypt(cryptKey);
        rc4Licence.crypt(this.inputToken, 0, LICENCE_TOKEN_SIZE, decryptToken, 0);

        byte[] hashWidth = this.generateHashWidth();

        /* generate signature for a buffer of token and HWId */
        System.arraycopy(decryptToken, 0, sealedBuffer, 0, LICENCE_TOKEN_SIZE);
        System.arraycopy(hashWidth, 0, sealedBuffer, LICENCE_TOKEN_SIZE, LICENCE_HWID_SIZE);

        outSig = secure.sign(this.licenceSignatureKey, 16, 16, sealedBuffer, sealedBuffer.length);

        /* deliberately break signature if licencing disabled */
        if (!Constants.licence) {
            outSig = new byte[LICENCE_SIGNATURE_SIZE]; // set to 0
        }

        /*now crypt the hashWidth */
        System.arraycopy(this.licenceKey, 0, cryptKey, 0, this.licenceKey.length);
        rc4Licence.engineInitEncrypt(cryptKey);
        rc4Licence.crypt(hashWidth, 0, LICENCE_HWID_SIZE, cryptHashWidthSize, 0);

        this.sendAuthResponse(outToken, cryptHashWidthSize, outSig);
    }

    /**
     * Handle a licence issued by the server, save to disk if Options.saveLicence
     *
     * @param data Packet containing issued licence
     * @throws CryptoException Cryptographic error
     */
    public void processIssue(RdpPacket data) throws CryptoException {
        int length;
        int check;
        RC4 rc4Licence = new RC4();
        byte[] key = new byte[this.licenceKey.length];
        System.arraycopy(this.licenceKey, 0, key, 0, this.licenceKey.length);

        data.incrementPosition(2); //TODO: unknown
        length = data.getLittleEndian16();

        if (data.getPosition() + length > data.getEnd()) {
            return;
        }

        rc4Licence.engineInitDecrypt(key);
        byte[] buffer = new byte[length];
        data.copyToByteArray(buffer, 0, data.getPosition(), length);
        rc4Licence.crypt(buffer, 0, length, buffer, 0);
        data.copyFromByteArray(buffer, 0, data.getPosition(), length);

        check = data.getLittleEndian16();
        if (check != 0) {
            //return;
        }
        secure.licenceIssued = true;

        /*
          data.incrementPosition(2); // in_uint8s(s, 2);	// pad

          // advance to fourth string
          length = 0;
          for (int i = 0; i < 4; i++)
          {
              data.incrementPosition(length); // in_uint8s(s, length);
              length = data.getLittleEndian32(length); // in_uint32_le(s, length);
              if (!(data.getPosition() + length <= data.getEnd()))
                  return;
          }*/

        secure.licenceIssued = true;
        logger.debug("Server issued Licence");
        if (Options.isSaveLicence())
            saveLicence(data, length - 2);
    }

    /**
     * Send a request for a new licence, or to approve a stored licence
     *
     * @param clientRandom User generated random number
     * @param rsaData      Hash
     * @param userName     Name of the user
     * @param hostname     Name of the host we are connecting to
     * @throws RdesktopException Protocol error
     * @throws IOException       Network I/O error
     * @throws CryptoException   Cryptographic error
     */
    public void sendRequest(byte[] clientRandom, byte[] rsaData, byte[] userName, byte[] hostname) throws RdesktopException, IOException, CryptoException {
        int secFlags = Secure.SEC_LICENCE_NEG;
        int userlen = (userName.length == 0 ? 0 : userName.length + 1);
        int hostlen = (hostname.length == 0 ? 0 : hostname.length + 1);
        int length = 128 + userlen + hostlen;

        RdpPacket buffer = secure.init(secFlags, length);

        buffer.set8(NEW_LICENSE_REQUEST);
        buffer.set8(2); // version
        buffer.setLittleEndian16(length);

        buffer.setLittleEndian32(1);

        if (Options.isBuiltInLicence() && (!Options.isLoadLicence()) && (!Options.isSaveLicence())) {
            logger.debug("Using built-in Windows Licence");
            buffer.setLittleEndian32(0x03010000);
        } else {
            logger.debug("Requesting licence");
            buffer.setLittleEndian32(0xff010000);
        }
        buffer.copyFromByteArray(clientRandom, 0, buffer.getPosition(), Secure.SEC_RANDOM_SIZE);
        buffer.incrementPosition(Secure.SEC_RANDOM_SIZE);
        buffer.setLittleEndian16(0);

        buffer.setLittleEndian16(Secure.SEC_MODULUS_SIZE + Secure.SEC_PADDING_SIZE);
        buffer.copyFromByteArray(rsaData, 0, buffer.getPosition(), Secure.SEC_MODULUS_SIZE);
        buffer.incrementPosition(Secure.SEC_MODULUS_SIZE);

        buffer.incrementPosition(Secure.SEC_PADDING_SIZE);

        buffer.setLittleEndian16(LICENCE_TAG_USER);
        buffer.setLittleEndian16(userlen);

        if (userName.length != 0) {
            buffer.copyFromByteArray(userName, 0, buffer.getPosition(), userlen - 1);
        } else {
            buffer.copyFromByteArray(userName, 0, buffer.getPosition(), userlen);
        }

        buffer.incrementPosition(userlen);

        buffer.setLittleEndian16(LICENCE_TAG_HOST);
        buffer.setLittleEndian16(hostlen);

        if (hostname.length != 0) {
            buffer.copyFromByteArray(hostname, 0, buffer.getPosition(), hostlen - 1);
        } else {
            buffer.copyFromByteArray(hostname, 0, buffer.getPosition(), hostlen);
        }
        buffer.incrementPosition(hostlen);
        buffer.markEnd();
        secure.send(buffer, secFlags);
    }

    /**
     * Load a licence from disk
     *
     * @return Raw byte data for stored licence
     */
    byte[] loadLicence() {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        return preferences.getByteArray("licence." + Options.getHostname(), null);
    }

    /**
     * Save a licence to disk
     *
     * @param data   Packet containing licence data
     * @param length Length of licence
     */
    void saveLicence(RdpPacket data, int length) {
        logger.debug("saveLicence");
        int len;
        int startpos = data.getPosition();
        data.incrementPosition(2); // Skip first two bytes
        /* Skip three strings */
        for (int i = 0; i < 3; i++) {
            len = data.getLittleEndian32();
            data.incrementPosition(len);
            /* Make sure that we won't be past the end of data after
            * reading the next length value
            */
            if (data.getPosition() + 4 - startpos > length) {
                logger.warn("Error in parsing licence key.");
                return;
            }
        }
        len = data.getLittleEndian32();
        logger.debug("saveLicence: len=" + len);
        if (data.getPosition() + len - startpos > length) {
            logger.warn("Error in parsing licence key.");
            return;
        }

        byte[] databytes = new byte[len];
        data.copyToByteArray(databytes, 0, data.getPosition(), len);

        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        prefs.putByteArray("licence." + Options.getHostname(), databytes);

    }

    /**
     * Generate a set of encryption keys
     *
     * @param clientKey Array in which to store client key
     * @param serverKey Array in which to store server key
     * @param clientRsa Array in which to store RSA data
     * @throws CryptoException Cryptographic error
     */
    public void generateKeys(byte[] clientKey, byte[] serverKey, byte[] clientRsa) throws CryptoException {
        byte[] sessionKey;
        byte[] tempHash;

        tempHash = secure.hash48(clientRsa, clientKey, serverKey, 65);
        sessionKey = secure.hash48(tempHash, serverKey, clientKey, 65);

        System.arraycopy(sessionKey, 0, this.licenceSignatureKey, 0, 16);

        this.licenceKey = secure.hash16(sessionKey, clientKey, serverKey, 16);
    }
}
