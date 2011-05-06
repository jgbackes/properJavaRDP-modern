/* ClipChannel.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.4 $
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
package net.propero.rdp.virtualChannels.cliprdr;

import net.propero.rdp.Common;
import net.propero.rdp.CommunicationMonitor;
import net.propero.rdp.Options;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.virtualChannels.VChannel;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.Iterator;

import static net.propero.rdp.Constants.encryption;
import static net.propero.rdp.Secure.SEC_ENCRYPT;
import static net.propero.rdp.virtualChannels.VChannels.*;

public class ClipChannel extends VChannel implements ClipInterface, ClipboardOwner, FocusListener {

    protected static Logger logger = Logger.getLogger(ClipChannel.class);

    static {
        logger.setLevel(Level.WARN);
    }

    /**
     * msgType (2 bytes): An unsigned, 16-bit integer that specifies
     * the type of the clipboard PDU that follows the dataLen field.
     */
    public static final int CB_MONITOR_READY = 0x0001;          // Monitor Ready PDU
    public static final int CB_FORMAT_LIST = 0x0002;            // Format List PDU
    public static final int CB_FORMAT_LIST_RESPONSE = 0x0003;   // Format List Response PDU
    public static final int CB_FORMAT_DATA_REQUEST = 0x0004;    // Format Data Request PDU
    public static final int CB_FORMAT_DATA_RESPONSE = 0x0005;   // Format Data Response PDU
    public static final int CB_TEMP_DIRECTORY = 0x0006;         // Temporary Directory PDU
    public static final int CB_CLIP_CAPS = 0x0007;              // Clipboard Capabilities PDU
    public static final int CB_FILECONTENTS_REQUEST = 0x0008;   // File Contents Request PDU
    public static final int CB_FILECONTENTS_RESPONSE = 0x0009;  // File Contents Response PDU
    public static final int CB_LOCK_CLIPDATA = 0x000A;          // Lock Clipboard Data PDU
    public static final int CB_UNLOCK_CLIPDATA = 0x000B;        // Unlock Clipboard Data PDU

    // Message status codes
    public static final int CLIPRDR_REQUEST = 0;

    public static final int CB_RESPONSE_OK = 1;
    public static final int CB_RESPONSE_FAIL = 2;


    Clipboard clipboard;                // TypeHandler for data currently being awaited
    TypeHandler currentHandler = null;  // All type handlers available
    TypeHandlerList allHandlers = null; // byte[] localClipData = null;

    public ClipChannel() {
        this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        // initialise all clipboard format handlers
        allHandlers = new TypeHandlerList();
        try {
            allHandlers.add(new UnicodeHandler());
            allHandlers.add(new TextHandler());
            allHandlers.add(new DIBHandler());
            //allHandlers.add(new MetafilepictHandler());
        } catch (ClipboardException e) {
            logger.warn("Could not create one or more clipboard data handlers");
        }
    }

    /*
      * VChannel inherited abstract methods
      */

    /**
     * 2.1 Transport
     * <p/>
     * The virtual channel name is "CLIPRDR".
     *
     * @return The name of this virtual channel
     */
    public String name() {
        logger.debug("ClipChannel:name");
        return "cliprdr";
    }

    /**
     * Describes the options used during this session
     *
     * @return Options used
     */
    public int flags() {
        logger.debug("ClipChannel:flags");
        return CHANNEL_OPTION_INITIALIZED | CHANNEL_OPTION_ENCRYPT_RDP |
                CHANNEL_OPTION_COMPRESS_RDP | CHANNEL_OPTION_SHOW_PROTOCOL;
    }

    /*
      * Data processing methods
      */

    /**
     * Process the data received on this virtual channel
     *
     * @param data Packet sent to this channel
     * @throws RdesktopException Protocol error
     * @throws IOException       Networking I/O error
     * @throws CryptoException   Cryptographic error
     */
    public void process(RdpPacket data) throws RdesktopException, IOException, CryptoException {
        logger.debug("ClipChannel:process");

        int type, status;
        int length;
        //int format;

        type = data.getLittleEndian16();
        status = data.getLittleEndian16();
        length = data.getLittleEndian32();

        if (status == CB_RESPONSE_FAIL) {
            if (type == CB_FORMAT_LIST_RESPONSE) {
                sendFormatAnnounce();
                return;
            }

            return;
        }

        switch (type) {
            case CB_MONITOR_READY:
                sendFormatAnnounce();
                break;
            case CB_FORMAT_LIST:
                handleClipFormatAnnounce(data, length);
                return;
            case CB_FORMAT_LIST_RESPONSE:
                break;
            case CB_FORMAT_DATA_REQUEST:
                handleDataRequest(data);
                break;
            case CB_FORMAT_DATA_RESPONSE:
                handleDataResponse(data, length);
                break;
            case CB_TEMP_DIRECTORY:
                logger.warn("Unimplemented CB_TEMP_DIRECTORY message");
                break;
            case CB_CLIP_CAPS:
                logger.warn("Unimplemented CB_CLIP_CAPS message");
                break;
            case CB_FILECONTENTS_REQUEST:
                logger.warn("Unimplemented CB_FILECONTENTS_REQUEST message");
                break;
            case CB_FILECONTENTS_RESPONSE:
                logger.warn("Unimplemented CB_FILECONTENTS_RESPONSE message");
                break;
            case CB_LOCK_CLIPDATA:
                logger.warn("Unimplemented CB_LOCK_CLIPDATA message");
                break;
            case CB_UNLOCK_CLIPDATA:
                logger.warn("Unimplemented CB_UNLOCK_CLIPDATA message");
                break;
            default:
                logger.warn("Unimplemented packet type! " + type);
        }


    }

    public void sendNull(int type, int status) {
        logger.debug("ClipChannel:sendNull");

        RdpPacket s;

        s = new RdpPacket(12);
        s.setLittleEndian16(type);
        s.setLittleEndian16(status);
        s.setLittleEndian32(0);
        s.setLittleEndian32(0); // pad
        s.markEnd();

        try {
            this.send_packet(s);
        } catch (RdesktopException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (CryptoException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    void sendFormatAnnounce() throws RdesktopException, IOException, CryptoException {
        logger.debug("ClipChannel:sendFormatAnnounce");

        Transferable clipData = clipboard.getContents(clipboard);
        DataFlavor[] dataTypes = clipData.getTransferDataFlavors();

        TypeHandlerList availableFormats = allHandlers.getHandlersForClipboard(dataTypes);

        RdpPacket s;
        int number_of_formats = availableFormats.getCount();

        s = new RdpPacket(number_of_formats * 36 + 12);
        s.setLittleEndian16(CB_FORMAT_LIST);
        s.setLittleEndian16(CLIPRDR_REQUEST);
        s.setLittleEndian32(number_of_formats * 36);

        TypeHandler handler;
        for (Iterator i = availableFormats.getIterator(); i.hasNext();) {
            handler = (TypeHandler) i.next();
            s.setLittleEndian32(handler.preferredFormat());
            s.incrementPosition(32);
        }

        s.setLittleEndian32(0); // pad
        s.markEnd();
        send_packet(s);
    }

    private void handleClipFormatAnnounce(RdpPacket data, int length)
            throws RdesktopException, IOException, CryptoException {

        TypeHandlerList serverTypeList = new TypeHandlerList();

        logger.debug("ClipChannel:handleClipFormatAnnounce:Available types:");

        // Go backward though the list of types on the clipboard
        for (int c = length; c >= 36; c -= 36) {
            int typeCode = data.getLittleEndian32();
            logger.debug(typeCode + " ");
            data.incrementPosition(32);

            TypeHandler typeHandler = allHandlers.getHandlerForFormat(typeCode);
            if (null != typeHandler) {
                serverTypeList.add(typeHandler);
            } else {
                logger.warn("Unknown typeCode = " + typeCode);
            }
        }

        sendNull(CB_FORMAT_LIST_RESPONSE, CB_RESPONSE_OK);
        currentHandler = serverTypeList.getFirst();

        if (currentHandler != null) {
            requestClipboardData(currentHandler.preferredFormat());
        }
    }

    private void handleDataRequest(RdpPacket data) throws RdesktopException, IOException, CryptoException {
        logger.debug("ClipChannel:handleDataRequest");

        int format = data.getLittleEndian32();
        Transferable clipData = clipboard.getContents(this);
        //byte[] outData = null;

        TypeHandler outputHandler = allHandlers.getHandlerForFormat(format);
        if (outputHandler != null) {
            outputHandler.sendData(clipData, this);
            // outData = outputHandler.fromTransferable(clipData);
            //if(outData != null){
            //	sendData(outData,outData.length);
            //	return;
            //}
            //else System.out.println("Clipboard data to send == null!");
        }

        //this.sendNull(CB_FORMAT_DATA_RESPONSE,CB_RESPONSE_FAIL);
    }

    private void handleDataResponse(RdpPacket data, int length) {
        logger.debug("ClipChannel:handleDataResponse");
        //if(currentHandler != null)clipboard.setContents(currentHandler.handleData(data, length),this);
        //currentHandler = null;
        if (currentHandler != null)
            currentHandler.handleData(data, length, this);
        currentHandler = null;
    }

    private void requestClipboardData(int formatCode) throws RdesktopException, IOException, CryptoException {
        logger.debug("ClipChannel:requestClipboardData");

        RdpPacket s = Common.secure.init(encryption ? SEC_ENCRYPT : 0, 24);
        s.setLittleEndian32(16); // length

        int flags = CHANNEL_FLAG_FIRST | CHANNEL_FLAG_LAST;
        if ((this.flags() & CHANNEL_OPTION_SHOW_PROTOCOL) != 0) {
            flags |= CHANNEL_FLAG_SHOW_PROTOCOL;
        }

        s.setLittleEndian32(flags);
        s.setLittleEndian16(CB_FORMAT_DATA_REQUEST);
        s.setLittleEndian16(CLIPRDR_REQUEST);
        s.setLittleEndian32(4); // Remaining length
        s.setLittleEndian32(formatCode);
        s.setLittleEndian32(0); // TODO: Unknown. Garbage pad?
        s.markEnd();

        Common.secure.sendToChannel(s, encryption ? SEC_ENCRYPT : 0, this.mcs_id());
    }

    public void sendClipboardData(byte[] data, int length) {
        logger.debug("ClipChannel:sendClipboardData");

        CommunicationMonitor.lock(this);

        RdpPacket all = new RdpPacket(12 + length);

        all.setLittleEndian16(CB_FORMAT_DATA_RESPONSE);
        all.setLittleEndian16(CB_RESPONSE_OK);
        all.setLittleEndian32(length + 4);      // TODO: don't know why, but we need to add between 1 and 4 to the length, otherwise the server cliprdr thread hangs
        all.copyFromByteArray(data, 0, all.getPosition(), length);
        all.incrementPosition(length);
        all.setLittleEndian32(0);

        try {
            this.send_packet(all);
        } catch (RdesktopException e) {
            logger.warn(e.getMessage());
            if (!Common.underApplet) {
                System.exit(-1);
            }
        } catch (IOException e) {
            logger.warn(e.getMessage());
            if (!Common.underApplet) {
                System.exit(-1);
            }
        } catch (CryptoException e) {
            logger.warn(e.getMessage());
            if (!Common.underApplet) {
                System.exit(-1);
            }
        } finally {
            CommunicationMonitor.unlock(this);
        }
    }

    /*
      * FocusListener methods
      */
    public void focusGained(FocusEvent arg0) {
        logger.debug("ClipChannel:focusGained");
        // synchronise the clipboard types here, so the server knows what's available
        if (Options.isRdp5()) {
            try {
                sendFormatAnnounce();
            } catch (RdesktopException ignored) {
            } catch (IOException ignored) {
            } catch (CryptoException ignored) {
            }
        }
    }

    public void focusLost(FocusEvent arg0) {
        logger.debug("ClipChannel:focusLost");
    }

    /*
      * Support methods
      */
    @SuppressWarnings({"UnusedDeclaration"})
    private void resetBooleanArray(boolean[] x) {
        logger.debug("ClipChannel:resetBooleanArray");
        for (int i = 0; i < x.length; i++)
            x[i] = false;
    }

    /*
      * ClipboardOwner methods
      */
    public void lostOwnership(Clipboard arg0, Transferable arg1) {
        logger.debug("ClipChannel:lostOwnership");
    }

    public void copyToClipboard(Transferable t) {
        logger.debug("ClipChannel:copyToClipboard");
        clipboard.setContents(t, this);
    }
}
