package net.propero.rdp.virtualChannels.rdpEFS;

/**
 * Created by IntelliJ IDEA.
 * User: johnh
 * Date: 2/1/11
 * Time: 2:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class RdpefsClientDeviceDetails {

    // 2.2.1.3 Device Announce Header

    private int deviceType = 0;
    private int deviceId = 0;
    private String preferredDosName = "\0";
    private int deviceDataLength = 0;
    private Byte deviceData[];                     // markrdpepc
                                      // ms-rdpepc 2.2.2.1 Client Device List Announce Request (DR_PRN_DEVICE_ANNOUNCE)
                                      // the printer fields flags through to the CachedPrinterConfigData are within
                                      // the deviceData field

    private RdpefsDriveDevice driveDevice;

        // add port and printer devices here
    private RdpefsPrinterDevice printerDevice;        //markrdpepc

    public RdpefsDriveDevice getDriveDevice() {
        return driveDevice;
    }

    public RdpefsPrinterDevice getPrinterDevice() {
        return printerDevice;
    }

    public void setDriveDevice(RdpefsDriveDevice driveDevice) {
        this.driveDevice = driveDevice;
    }


    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getPreferredDosName() {
        return preferredDosName;
    }

    public void setPreferredDosName(String preferredDosName) {
        this.preferredDosName = preferredDosName;
    }

    public int getDeviceDataLength() {
        return deviceDataLength;
    }

    public void setDeviceDataLength(int deviceDataLength) {
        this.deviceDataLength = deviceDataLength;
    }

    public Byte[] getDeviceData() {
        return deviceData;
    }

    public void setDeviceData(Byte[] deviceData) {
        this.deviceData = deviceData;
    }

}
