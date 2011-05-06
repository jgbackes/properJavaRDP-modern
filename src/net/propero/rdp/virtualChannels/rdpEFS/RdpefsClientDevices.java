package net.propero.rdp.virtualChannels.rdpEFS;

import com.sun.org.apache.bcel.internal.generic.IfInstruction;
import net.propero.rdp.Input;
import net.propero.rdp.RdpPacket;

import java.io.File;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * Created by IntelliJ IDEA.
 * User: johnh
 * Date: 1/31/11
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class RdpefsClientDevices {

    protected static Logger logger = Logger.getLogger(Input.class);
    //      2.2.1.3 Device Announce Header (DEVICE_ANNOUNCE)
    //          DeviceType 32 bits

    public static final int RDPDR_DTYP_SERIAL = 0x00000001;
    public static final int RDPDR_DTYP_PARALLEL = 0x00000002;
    public static final int RDPDR_DTYP_PRINT = 0x00000004;
    public static final int RDPDR_DTYP_FILESYSTEM = 0x00000008;
    public static final int RDPDR_DTYP_SMARTCARD = 0x00000020;


    public RdpefsClientDevices() {

        logger.setLevel(Level.ALL);
    }

    private Vector<RdpefsClientDeviceDetails> RdpefsClientDeviceList = new Vector<RdpefsClientDeviceDetails>();

    private File[] rootFiles; // Drive devices

    private int numClientDevices = 0;


    private void doListRoots() {

        rootFiles = File.listRoots();

        for (int numRootFiles = 0; numRootFiles < rootFiles.length; numRootFiles++) {
            logger.info("Root[" + numRootFiles + "] = " + rootFiles[numRootFiles]);
        }
    }
// initDeviceList needs to discover and add drives, markrdpepc printers and ports
    // currently this is hard coded for disk drives

    public void initDeviceList() {
        Byte[] values = new Byte[64];
        RdpefsClientDeviceDetails newDevice = new RdpefsClientDeviceDetails();

        doListRoots();        // get the number of root files i.e. drives

        for (int numRootFiles = 0; numRootFiles < rootFiles.length; numRootFiles++) {
            newDevice.setDeviceType((RDPDR_DTYP_FILESYSTEM));
            newDevice.setPreferredDosName(rootFiles[numRootFiles].toString()); //("C:\0");
            newDevice.setDeviceDataLength(0);
            for (int i = 0; i < 64; i++) {
                values[i] = 0x00;
            }
            newDevice.setDeviceData(values);
            addDevice(newDevice);
        }
        // markrdpepc add the printer device RDPDR_DTYP_PRINT here

    }

    public boolean processDeviceRequest(RdpPacket data) {
        RdpefsClientDeviceDetails currentDevice = new RdpefsClientDeviceDetails();
        boolean retCode = false;

        int deviceId;
        int index;
        logger.debug("RdpefsClientDevices:processDeviceRequest");

//        get the device ID
        deviceId = data.getLittleEndian32();
        currentDevice = getDeviceDetailsById(deviceId);
        if (currentDevice != null) {
            retCode = true;

            switch (currentDevice.getDeviceType()) {
                case RDPDR_DTYP_FILESYSTEM:
                    logger.debug("RDPDR_DTYP_FILESYSTEM Request");

                    RdpefsDriveDevice driveDevice = currentDevice.getDriveDevice();
                    driveDevice.processRequest(data, currentDevice);
                    break;

                case RDPDR_DTYP_PRINT:                         // markrdpepc
                    logger.debug("RDPDR_DTYP_PRINT Request");

                    RdpefsPrinterDevice printDevice = currentDevice.getPrinterDevice();
                    printDevice.processRequest(data, currentDevice);
                    break;

/**********************
 case RDPDR_DTYP_SERIAL:
 logger.debug("RDPDR_DTYP_SERIAL Request");

 RdpefsDriveDevice driveDevice = currentDevice.getDriveDevice();
 driveDevice.processRequest(data, currentDevice);
 break;

 case RDPDR_DTYP_PARALLEL:
 logger.debug("RDPDR_DTYP_PARALLEL Request");

 RdpefsDriveDevice driveDevice = currentDevice.getDriveDevice();
 driveDevice.processRequest(data, currentDevice);
 break;

 case RDPDR_DTYP_SMARTCARD:
 logger.debug("RDPDR_DTYP_SMARTCARD Request");

 RdpefsDriveDevice driveDevice = currentDevice.getDriveDevice();
 driveDevice.processRequest(data, currentDevice);
 break;
 ********************/
                default:
                    logger.warn("Unimplemented Device type " + currentDevice.getDeviceType());
                    break;

            }

        }
        if (retCode == false)
            logger.warn("Unknown Device Id" + deviceId);

        return retCode;
    }

    public boolean addDevice(RdpefsClientDeviceDetails newDevice) {

        boolean retCode = false;

        RdpefsClientDeviceDetails currentDevice; // = new RdpefsClientDeviceDetails();
        RdpefsClientDeviceList.add(new RdpefsClientDeviceDetails());
        currentDevice = RdpefsClientDeviceList.elementAt(numClientDevices++);
        currentDevice.setDeviceType(newDevice.getDeviceType());
        currentDevice.setDeviceId(numClientDevices);
        currentDevice.setPreferredDosName(newDevice.getPreferredDosName());
        currentDevice.setDeviceDataLength(newDevice.getDeviceDataLength());
        int n = newDevice.getDeviceDataLength();
        if (n != 0) {

            currentDevice.setDeviceData(newDevice.getDeviceData());

        }

        retCode = true;

        switch (currentDevice.getDeviceType()) {
            case RDPDR_DTYP_FILESYSTEM:
                currentDevice.setDriveDevice(new RdpefsDriveDevice());
                break;

            default:
                break;

        }
        return retCode;
    }

    public boolean removeDevice(int id) {

        RdpefsClientDeviceDetails currentDevice;
        boolean retCode = false;

        if (!RdpefsClientDeviceList.isEmpty()) {
            for (int i = 0; i < numClientDevices; i++) {
                currentDevice = RdpefsClientDeviceList.elementAt(i);
                if (currentDevice.getDeviceId() == id) {

                    // Kill any outstanding I/O IOCTL requests
                    RdpefsClientDeviceList.removeElementAt(i);

                    // decrement the numClientDevicess if the remove succeeds

                    numClientDevices--;
                    retCode = true;
                }
            }
        }
        if (retCode == false)
            logger.warn("Device entry not found " + id);
        return retCode;
    }


    public int getDeviceType
            (
                    int index) {
        RdpefsClientDeviceDetails currentDevice = RdpefsClientDeviceList.elementAt(index);
        return currentDevice.getDeviceType();
    }

    public int getNumClientDevices() {
        return numClientDevices;
    }

    public int getDeviceId(int index) {
        RdpefsClientDeviceDetails currentDevice = RdpefsClientDeviceList.elementAt(index);
        return currentDevice.getDeviceId();
    }

    public String getPreferredDosName
            (
                    int index) {
        RdpefsClientDeviceDetails currentDevice = RdpefsClientDeviceList.elementAt(index);
        return currentDevice.getPreferredDosName();
    }

    public int getDeviceDataLength
            (
                    int index) {
        RdpefsClientDeviceDetails currentDevice = RdpefsClientDeviceList.elementAt(index);
        return currentDevice.getDeviceDataLength();
    }

    RdpefsClientDeviceDetails getDeviceDetailsById(int id) {

        RdpefsClientDeviceDetails currentDevice;

        if (!RdpefsClientDeviceList.isEmpty())
            for (int i = 0; i < numClientDevices; i++) {
                if (getDeviceId(i) == id) {
                    currentDevice = RdpefsClientDeviceList.elementAt(i);
                }
            }
        return null;
    }

}
