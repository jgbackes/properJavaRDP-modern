package net.propero.rdp.virtualChannels.rdpEFS;

import net.propero.rdp.RdpPacket;

/**
 * Created by IntelliJ IDEA.
 * User: johnh
 * Date: 2/7/11
 * Time: 9:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class RdpefsDriveDevice extends VRdpefsDevice {

//    public deviceName() { }

   private int fileId;
    private int completionId;
    private int majorFunction;
    private int minorFunction;

    // MS-RDPEFS 2.2.1.4 Device I/O Request (DR_DEVICE_IOREQUEST)

    public void processRequest(RdpPacket data, RdpefsClientDeviceDetails deviceDetails) {

        int deviceId = deviceDetails.getDeviceId();

        fileId = data.getLittleEndian32();
        completionId = data.getLittleEndian32();
        majorFunction = data.getLittleEndian32();
        minorFunction = data.getLittleEndian32();

        switch (majorFunction) {
            case IRP_MJ_CREATE:

                break;

            default:
               logger.debug("Unknown Device I/O request");
                break;

        }




    }
}
