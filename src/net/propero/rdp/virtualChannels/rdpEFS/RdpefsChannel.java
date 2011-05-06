package net.propero.rdp.virtualChannels.rdpEFS;

import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.virtualChannels.VChannel;
import net.propero.rdp.virtualChannels.VChannels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Vector;


/**
 * Created by IntelliJ IDEA.
 * User: johnh
 * Date: 1/21/11
 * Time: 1:05 PM
 */
public class RdpefsChannel extends VChannel {

    // [MS-RDPEFS] Remote Desktop Protocol:
    //      File System Virtual Channel Extension
    //      2.2.1.1 Shared Header (RDPDR_HEADER)
    //          Component Field 16 Bits
    public static final int RDPDR_CTYP_CORE = 0x4472;      // Device re-director core component
    public static final int RDPDR_CTYP_PRN = 0x5052;       // Printing component

    //          PacketId field 16 Bits
    public static final int PAKID_CORE_SERVER_ANNOUNCE = 0x496E;    // Server Announce Request, section 2.2.2.2.
    public static final int PAKID_CORE_CLIENTID_CONFIRM = 0x4343;   // Client Announce Reply and Server Client
    // ID Confirm, sections 2.2.2.3 and 2.2.2.6.
    public static final int PAKID_CORE_CLIENT_NAME = 0x434E;        // Client Name Request, section 2.2.2.4.
    public static final int PAKID_CORE_DEVICELIST_ANNOUNCE = 0x4441; // Client Device List Announce Request
    public static final int PAKID_CORE_DEVICE_REPLY = 0x6472;       // Server Device Announce Response section 2.2.2.9.
    public static final int PAKID_CORE_DEVICE_IOREQUEST = 0x4952;   // Device I/O Request
    public static final int PAKID_CORE_DEVICE_IOCOMPLETION = 0x4943;// Device I/O Response
    public static final int PAKID_CORE_SERVER_CAPABILITY = 0x5350;  // Server Core Capability request
    public static final int PAKID_CORE_CLIENT_CAPABILITY = 0x4350;  // Client Core Capability Respomse
    public static final int PAKID_CORE_DEVICELIST_REMOVE = 0x444D;  // Client Drive list remove
    public static final int PAKID_PRN_CACHE_DATA = 0x5043;          // Add Printer CacheData
    public static final int PAKID_CORE_USER_LOGGEDON = 0x554C;      // Server User Logged On
    public static final int PAKID_PRN_USING_XPS = 0x5543;           // Server Printer Set XPS Mode

    public static final int CAP_GENERAL_TYPE = 0x0001;              // General capability set
    public static final int CAP_PRINTER_TYPE = 0x0002;              // Printer capability set
    public static final int CAP_PORT_TYPE = 0x0003;                 // Port (serial or parallel) capability set
    public static final int CAP_DRIVE_TYPE = 0x0004;                //Drive Capability set
    public static final int CAP_SMARTCARD_TYPE = 0x0005;            // Smart Card capability set


    private Vector<RdpefsServerDeviceCapability> RdpefsServerCapabilitySet = new Vector<RdpefsServerDeviceCapability>();

    private RdpefsGeneralCapability RdpefsGeneralCapsSet = new RdpefsGeneralCapability();
    private RdpefsClientDevices RdpefsClientDeviceList = new RdpefsClientDevices();


    private boolean awaitingDataPacket = false;
    private boolean deviceOpen = false;
    private int versionMajor;
    private int versionMinor;
    private int clientID;
    private int numServerCapabilities;
    private String localHostName = "";


    public RdpefsChannel() {
        super();
        awaitingDataPacket = false;
        deviceOpen = false;

        RdpefsClientDeviceList.initDeviceList();

    }

    public String name() {
        return "RDPDR";
    }

    public int flags() {
        return VChannels.CHANNEL_OPTION_INITIALIZED
                | VChannels.CHANNEL_OPTION_ENCRYPT_RDP;
    }

    //    RDPEFS 2.2.1.1 Shared header
    // initCorePacket create the shared header for the Core component, size if the size of the packet minus the header

    private RdpPacket initCorePacket(int packetId, int size) {
        RdpPacket s = new RdpPacket(size + 4);
        s.setLittleEndian16(RDPDR_CTYP_CORE);          // Component
        s.setLittleEndian16(packetId);                 // Packet Function
        return s;
    }

    public void process(RdpPacket data) throws RdesktopException, IOException, CryptoException {
        int component;
        int packetId;

        component = data.getLittleEndian16();

        if (component == RDPDR_CTYP_CORE) {
            logger.debug("Received RDPDR_CTYPE_CORE");
            packetId = data.getLittleEndian16();
            switch (packetId) {
                case PAKID_CORE_SERVER_ANNOUNCE:
                    logger.debug("Received Core Server Announce");
                    this.processServerAnnounceRequest(data);
                    // Follow this up with sending the client name  2.2.2.4 Client Name Request
                    this.sendClientNameReq();
                    break;

                case PAKID_CORE_CLIENTID_CONFIRM:
                    logger.debug("Received clientIDConfirm");
                    this.processServerclientIDConfirm(data);
                    this.sendClientDeviceListAnnounce();
                    break;

                case PAKID_CORE_SERVER_CAPABILITY:
                    logger.debug("Received ServerCoreCapabilityRequest");
                    this.processServerCoreCapabilityRequest(data);
                    break;

                case PAKID_CORE_DEVICE_IOREQUEST: {
                    logger.debug("Received PAKID_CORE_DEVICE_IOREQUEST");
                    this.RdpefsClientDeviceList.processDeviceRequest(data);
                    break;
                }


                default:
                    logger.warn("Unimplemented packetId type " + packetId);
                    break;
            }
        } else if (component == RDPDR_CTYP_PRN) {
            logger.debug("Received RDPDR_CTYPE_PRN");
// the printer device may need to process the printerId
            packetId = data.getLittleEndian16();
            switch (packetId) {
//                case PAKID_CORE_SERVER_ANNOUNCE:
//                    logger.debug("Received Core Server Announce for the printers");
//                    this.processServerAnnounceRequest(data);
//                    // Follow this up with sending the client name  2.2.2.4 Client Name Request
//                    this.sendClientNameReq();

                case PAKID_PRN_CACHE_DATA:          // Add Printer CacheData
                    logger.debug("Received Printer PAKID_PRN_CACHE_DATA");
                    this.RdpefsClientDeviceList.processDeviceRequest(data);
                    break;

                case PAKID_PRN_USING_XPS:           // Server Printer Set XPS Mode
                    logger.debug("Received Printer PAKID_PRN_USING_XPS");
                    this.RdpefsClientDeviceList.processDeviceRequest(data);
                    break;
                default:
                    break;

            }
        } else {
            logger.debug("Received Unknown RDPDR Packet component");

        }
    }

    // Process a RDPEFS 2.2.2.2 Server Announce Request
    // and send the RDPEFS 2.2.2.3 Client Announce Reply

    private void processServerAnnounceRequest(RdpPacket data) {
        versionMajor = data.getLittleEndian16(); // in_uint16_le(s, versionMinor);
        versionMinor = data.getLittleEndian16(); // in_uint16_le(s, versionMinor);
        clientID = data.getLittleEndian32();

        RdpPacket out = initCorePacket(PAKID_CORE_CLIENTID_CONFIRM, 12);      //was 16
        out.setLittleEndian16(1);                   // versionMajor
        out.setLittleEndian16(versionMinor);        // versionMinor
        if (versionMinor < 12) {
            Random generator = new Random(19580427);
            clientID = generator.nextInt();
        }
        out.setLittleEndian32(clientID);            // clientID
        out.markEnd();

        try {
            send_packet(out);
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

    // 2.2.2.6 Server Client ID Confirm
    private void processServerclientIDConfirm(RdpPacket data) {
        int temp;

        temp = data.getLittleEndian16();
        if (versionMajor != temp) // in_uint16_le(s, versionMajor);
            logger.warn("Version Major has changed " + versionMajor + temp);

        temp = data.getLittleEndian16();
        if (versionMinor != temp) // in_uint16_le(s, versionMinor);
            logger.warn("Version Minor has changed " + versionMinor + temp);

        temp = data.getLittleEndian32();
        if (clientID != temp) // in_uint32_le(s, clientID);
            logger.warn("clientID has changed " + clientID + temp);
    }

    // Process a 2.2.2.7 Server Core Capability Request

    private void processServerCoreCapabilityRequest(RdpPacket data) {
        int capabilityType;

        numServerCapabilities = data.getLittleEndian16();
        data.incrementPosition(2);                   // skip 2 bytes of padding

        // always clear the RdpefsServerCapabilitySet vector and repopulate
        // The General type structure will be overwritten
        RdpefsServerCapabilitySet.clear();

        for (int i = 0; i < numServerCapabilities; i++) {
            capabilityType = data.getLittleEndian16();

            switch (capabilityType) {
                case CAP_GENERAL_TYPE:              // 2.2.2.7.1 General Capability Set (GENERAL_CAPS_SET)
                    RdpefsGeneralCapsSet.getGeneralCapability(data);
                    break;
                case CAP_DRIVE_TYPE:
                case CAP_PORT_TYPE:
                case CAP_PRINTER_TYPE:
                case CAP_SMARTCARD_TYPE:
                    RdpefsServerCapabilitySet.add(new RdpefsServerDeviceCapability(data, capabilityType));
                    break;

                default:
                    logger.warn("Unknown Capability Type " + capabilityType);
                    break;

            }
        }
        if (numServerCapabilities != 0)
            sendClientCoreCapabilityResponse();
        // Now gather the devices to be redirected and send the device list announce PDU
    }

    private void sendRdpefsMessage(RdpPacket out) {
        out.markEnd();

        try {
            send_packet(out);

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
    // Send a 2.2.2.8 Client Core Capability Response (DR_CORE_CAPABILITY_RSP)

    private void sendClientCoreCapabilityResponse() {
        RdpefsServerDeviceCapability thisDeviceCaps;
        // jkh the length of the Cient Core Caps response PDU is determined by
        // jkh the # of capabilities + the padding + the length of the General Caps + no of other cap sets * 8
        int length = (RdpefsGeneralCapsSet.getGeneralCapsLength() + 8) + ((numServerCapabilities - 1) * 8);
        RdpPacket out = initCorePacket(PAKID_CORE_CLIENT_CAPABILITY, length);
        out.setLittleEndian16(numServerCapabilities);    // No. of Capabilities
        out.setLittleEndian16(0x0000);                   // Padding
        out.setLittleEndian16(CAP_GENERAL_TYPE);
        out.setLittleEndian16(length);
        out.setLittleEndian32(RdpefsGeneralCapsSet.getGeneralCapsVersion());
        out.setLittleEndian32(RdpefsGeneralCapsSet.getOsType());
        out.setLittleEndian32(RdpefsGeneralCapsSet.getOsVersion());
        out.setLittleEndian16(RdpefsGeneralCapsSet.getRdpMajorVersion());
        out.setLittleEndian16(RdpefsGeneralCapsSet.getRdpMinorVersion());
        out.setLittleEndian32(RdpefsGeneralCapsSet.getIoCode1());
        out.setLittleEndian32(RdpefsGeneralCapsSet.getIoCode2());
        out.setLittleEndian32(RdpefsGeneralCapsSet.getExtraFlags1());
        out.setLittleEndian32(RdpefsGeneralCapsSet.getExtraFlags2());
        out.setBigEndian32(RdpefsGeneralCapsSet.getSpecialTypeDeviceCap());

        // Now set the remaining Capabilities
        for (int i = 0; i < (numServerCapabilities - 1); i++) {            // Exclude the general caps
            thisDeviceCaps = RdpefsServerCapabilitySet.get(i);

            out.setLittleEndian16(thisDeviceCaps.getCapabilityType());
            out.setLittleEndian16(thisDeviceCaps.getCapabilityLength());
            out.setLittleEndian16(thisDeviceCaps.getVersion());
        }

        sendRdpefsMessage(out);
    }

    // Send the 2.2.2.4 Client Name Request (DR_CORE_CLIENT_NAME_REQ) to the server

    private void getLocalName() {
        try {
            InetAddress addr;
            addr = java.net.InetAddress.getLocalHost(); //InetAddress.getByName("127.0.0.1");
            localHostName = addr.getHostName();
            localHostName += '\0';
        } catch (UnknownHostException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    // 2.2.2.4 Client Name Request (DR_CORE_CLIENT_NAME_REQ)

    private void sendClientNameReq() {

        getLocalName();
        int length = localHostName.length();

        RdpPacket out = initCorePacket(PAKID_CORE_CLIENT_NAME, 48);

        out.setLittleEndian32(0x00000001);              // UnicodeFlag = true
        out.setLittleEndian32(0x00000000);              // Codepage   Must be 0
        out.setLittleEndian32(localHostName.length());  // String Length
        out.outUnicodeString(localHostName, localHostName.length());

        sendRdpefsMessage(out);

    }

    // 2.2.2.9 Client Device List Announce Request

    private void sendClientDeviceListAnnounce() {
        int numDevices = RdpefsClientDeviceList.getNumClientDevices();
        // Calculate the size of all the devices including data
        int size = numDevices * 20;

        for (int i = 0; i < numDevices; i++) {
            size = size + RdpefsClientDeviceList.getDeviceDataLength(i);
        }
        RdpPacket out = initCorePacket(PAKID_CORE_DEVICELIST_ANNOUNCE, 4 + size);

        out.setLittleEndian32(RdpefsClientDeviceList.getNumClientDevices());

        for (int i = 0; i < numDevices; i++) {
            out.setLittleEndian32(RdpefsClientDeviceList.getDeviceType(i));
            out.setLittleEndian32(RdpefsClientDeviceList.getDeviceId(i));
            out.outUint8p(RdpefsClientDeviceList.getPreferredDosName(i), 8);
            out.setLittleEndian32(RdpefsClientDeviceList.getDeviceDataLength(i));
        }
        sendRdpefsMessage(out);
    }


}
