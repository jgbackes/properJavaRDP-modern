package net.propero.rdp.virtualChannels.rdpEFS;

import net.propero.rdp.Input;
import net.propero.rdp.RdpPacket;
import org.apache.log4j.Logger;

import javax.management.Notification;
import java.io.IOException;

/**
 * User: johnh
 * Date: 2/3/11
 * Time: 2:51 PM
 */

public abstract class VRdpefsDevice {
    protected static Logger logger = Logger.getLogger(Input.class);

//    public abstract String deviceName();

//    public abstract int deviceId();


    //     MS-RDPEFS 2.2.1.4 Device I/O Request (DR_DEVICE_IOREQUEST)
    //          MajorFunction (4 bytes)

    public static final int IRP_MJ_CREATE = 0x00000000;
    public static final int IRP_MJ_CLOSE = 0x00000002;
    public static final int IRP_MJ_READ = 0x00000003;
    public static final int IRP_MJ_WRITE = 0x00000004;
    public static final int IRP_MJ_DEVICE_CONTROL = 0x0000000E;
    public static final int IRP_MJ_QUERY_VOLUME_INFORMATION = 0x0000000A;
    public static final int IRP_MJ_SET_VOLUME_INFORMATION = 0x0000000B;
    public static final int IRP_MJ_QUERY_INFORMATION = 0x00000005;
    public static final int IRP_MJ_SET_INFORMATION = 0x00000006;
    public static final int IRP_MJ_DIRECTORY_CONTROL = 0x0000000C;
    public static final int IRP_MJ_LOCK_CONTROL = 0x00000011;

    //        MS-RDPEFS 2.2.1.4
    //          MinorFunction (4 bytes)

    public static final int IRP_MN_QUERY_DIRECTORY = 0x00000001;
    public static final int IRP_MN_NOTIFY_CHANGE_DIRECTORY = 0x00000002;


    public int majorFunction;
    public int minorFunction;


    public abstract void processRequest(RdpPacket data, RdpefsClientDeviceDetails deviceDetails) throws IOException;


    public void createRequest(RdpPacket data) {
        logger.debug("Received RDPEFS create request");

    }

    public void closeRequest(RdpPacket data) {
        logger.debug("Received RDPEFS close request");
    }

    // Not used for printer devices                 markrdpepc
    public void readRequest(RdpPacket data) {
        logger.debug("Received RDPEFS read request");

    }

    public void writeRequest(RdpPacket data) {
        logger.debug("Received RDPEFS write request");
    }

    public void ioctlRequest(RdpPacket data) {
        logger.debug("Received RDPEFS IOCTL request");

    }

    private void createResponse() {
         logger.debug("Sendin RDPEFS create response");

     }

     private void closeResponse() {
         logger.debug("Sending RDPEFS close response");
     }

     // Not used for printer devices              // Markrdpepc
     private void readResponse() {
         logger.debug("Sending RDPEFS read response");
     }

     private void writeResponse() {
         logger.debug("Sending RDPEFS write response");

     }

     private void ioctlResponse() {
         logger.debug("Sending RDPEFS write response");
     }

}
