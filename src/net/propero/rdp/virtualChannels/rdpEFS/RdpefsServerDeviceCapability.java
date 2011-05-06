package net.propero.rdp.virtualChannels.rdpEFS;

import net.propero.rdp.RdpPacket;


/**
 * Created by IntelliJ IDEA.
 * User: johnh
 * Date: 1/25/11
 * Time: 3:56 PM
 */

// RDPEFS 2.7.1 General Capability Set (GENERAL_CAPS_SET)

public class RdpefsServerDeviceCapability {

    //      2.2.1.2 Capability Header (CAPABILITY_HEADER)
    //          Capability Type Field 16 Bits

    public static final int CAP_GENERAL_TYPE = 0x0001;              // General capability set
    public static final int CAP_PRINTER_TYPE = 0x0002;              // Printer capability set
    public static final int CAP_PORT_TYPE = 0x0003;                 // Port (serial or parallel) capability set
    public static final int CAP_DRIVE_TYPE = 0x0004;                //Drive Capability set
    public static final int CAP_SMARTCARD_TYPE = 0x0005;            // Smart Card capability set

    //          Version field 32 Bits Capability Specific version


    public static final int PRINT_CAPABILITY_VERSION_01 = 0x00000001;
    public static final int PORT_CAPABILITY_VERSION_01 = 0x00000001;
    private static final int DRIVE_CAPABILITY_VERSION_01 = 0x00000001;
    public static final int DRIVE_CAPABILITY_VERSION_02 = 0x00000002;
    public static final int SMARTCARD_CAPABILITY_VERSION_01 = 0x00000001;

    //          Version field 32 Bits Capability Specific version


    private int CapabilityType;
    private int CapabilityLength;
    private int version;


//  RDPEFS 2.2.1.2.1 Capability Message (CAPABILITY_SET)


    public RdpefsServerDeviceCapability(RdpPacket data, int capabilityType) {

        // ms-rdpefs 2.2.1.2 Capability Header (CAPABILITY_HEADER) Starts with the data length
//        this.CapabilityType = data.getLittleEndian16();
        this.CapabilityType = capabilityType;
        this.CapabilityLength = data.getLittleEndian16();
        this.version = data.getLittleEndian32();

//        switch (capabilityType) {
//
//            case CAP_PRINTER_TYPE:        // markrdpepc - process printer capability
//            case CAP_PORT_TYPE:
//            case CAP_SMARTCARD_TYPE:
//                break;
//
//            case CAP_DRIVE_TYPE:
//                if (version == DRIVE_CAPABILITY_VERSION_01)
//                    break;
//                else if (version == DRIVE_CAPABILITY_VERSION_02)
//                    break;
//            default:
////                Logger.warn("Unimplemented Capability type " + version);
//                break;
//        }

    }

    public int getVersion() {
        return version;
    }

//    public void setVersion(int version) {
//        this.version = version;
//    }

    public int getCapabilityLength() {
        return CapabilityLength;
    }
    public int getCapabilityType() {
        return CapabilityType;
    }

    public void setCapabilityType(int capabilityType) {
        CapabilityType = capabilityType;
    }

}

