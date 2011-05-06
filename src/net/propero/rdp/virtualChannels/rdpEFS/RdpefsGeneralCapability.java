package net.propero.rdp.virtualChannels.rdpEFS;

import net.propero.rdp.RdpPacket;

/**
 * Created by IntelliJ IDEA.
 * User: johnh
 * Date: 1/28/11
 * Time: 4:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class RdpefsGeneralCapability {

    public static final int CAP_GENERAL_TYPE = 0x0001;              // General capability set
    public static final int CAP_PRINTER_TYPE = 0x0002;              // Printer capability set
    public static final int CAP_PORT_TYPE = 0x0003;                 // Port (serial or parallel) capability set
    public static final int CAP_DRIVE_TYPE = 0x0004;                //Drive Capability set
    public static final int CAP_SMARTCARD_TYPE = 0x0005;            // Smart Card capability set

    //          Version field 32 Bits Capability Specific version

    public static final int GENERAL_CAPABILITY_VERSION_01 = 0x00000001;
    public static final int GENERAL_CAPABILITY_VERSION_02 = 0x00000002;


    // 2.2.2.7.1 General Capability Set (GENERAL_CAPS_SET) - There should be only one of these
    private int GeneralCapsLength;
    private int GeneralCapsVersion;
    private int osType;
    private int osVersion;
    private int rdpMajorVersion;
    private int rdpMinorVersion;
    private int ioCode1;
    private int ioCode2;        //Reserved
    private int extendedPDU;
    private int extraFlags1;
    private int extraFlags2;
    private int specialTypeDeviceCap;

    // 2.2.2.7.1 General Capability Set (GENERAL_CAPS_SET)
    public void getGeneralCapability(RdpPacket data) {

        GeneralCapsLength = data.getLittleEndian16();
        GeneralCapsVersion = data.getLittleEndian32();         // jkh  I had this length incorrect
        osType = data.getLittleEndian32();
        osVersion = data.getLittleEndian32();
        rdpMajorVersion = data.getLittleEndian16();
        rdpMinorVersion = data.getLittleEndian16();
        ioCode1 = data.getLittleEndian32();                 // JKH gets data correctly till here
        ioCode2 = data.getLittleEndian32();        //Reserved
        extendedPDU = data.getLittleEndian32();
        extraFlags1 = data.getLittleEndian32();
        extraFlags2 = data.getLittleEndian32();

        if (GeneralCapsVersion == GENERAL_CAPABILITY_VERSION_02)
            specialTypeDeviceCap = data.getLittleEndian32();
    }
    public int getGeneralCapsLength() {
        return GeneralCapsLength;
    }

    public int getGeneralCapsVersion() {
        return GeneralCapsVersion;
    }

    public int getOsType() {
        return osType;
    }

    public int getOsVersion() {
        return osVersion;
    }

    public int getRdpMajorVersion() {
        return rdpMajorVersion;
    }

    public int getRdpMinorVersion() {
        return rdpMinorVersion;
    }

    public int getIoCode1() {
        return ioCode1;
    }

    public int getIoCode2() {
        return ioCode2;
    }

    public int getExtendedPDU() {
        return extendedPDU;
    }

    public int getExtraFlags1() {
        return extraFlags1;
    }

    public int getExtraFlags2() {
        return extraFlags2;
    }

    public int getSpecialTypeDeviceCap() {
        return specialTypeDeviceCap;
    }

}
