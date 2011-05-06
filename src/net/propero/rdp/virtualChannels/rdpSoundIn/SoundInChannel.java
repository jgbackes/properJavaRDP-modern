package net.propero.rdp.virtualChannels.rdpSoundIn;

import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.virtualChannels.VChannel;
import net.propero.rdp.virtualChannels.rdpSoundOut.WaveFormatEx;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 2/14/11
 * Time: 9:44 AM
 */
public class SoundInChannel extends VChannel {

    // [MS-RDPEAI] Section 2.2.1 SNDIN_PDU Header
    public final static int MSG_SNDIN_VERSION = 0x01; // Version PDU
    public final static int MSG_SNDIN_FORMATS = 0x02; // Sound Formats PDU
    public final static int MSG_SNDIN_OPEN = 0x03; // Open PDU
    public final static int MSG_SNDIN_OPEN_REPLY = 0x04; // Open Reply PDU
    public final static int MSG_SNDIN_DATA_INCOMING = 0x05; // Incoming Data PDU
    public final static int MSG_SNDIN_DATA = 0x06; // Data PDU
    public final static int MSG_SNDIN_FORMATCHANGE = 0x07; // Format Change PDU

    public static final int MAX_FORMATS = 10;
    public static final int AUDIO_FORMAT_SIZE = 18;         // 2.2.2.1.1 Audio Format (AUDIO_FORMAT)

    private SoundInDriver soundInDriver;
    private boolean awaitingDataPacket;
    private boolean deviceOpen;
    private int format;
    private int currentFormat;
    private int tick;
    private int packetIndex;
    private WaveFormatEx[] formats;

    public SoundInChannel() {
        soundInDriver = new SoundInDriver(this);

    }

    @Override
    public String name() {
        return "SoundInChannel";
    }

    @Override
    public int flags() {
        return 0;
    }

    @Override
    public void process(RdpPacket data) throws RdesktopException, IOException,
            CryptoException {

        int msgType;
        int bodySize;

        if (awaitingDataPacket) {
            if (format >= MAX_FORMATS) {
                logger.error("SoundOutChannel:process: Invalid format index");
                return;
            }


            if (!deviceOpen || (format != currentFormat)) {
                if (!deviceOpen && !soundInDriver.open()) {
                    sendCompletion(tick, packetIndex);
                    return;
                }
                if (!soundInDriver.setSoundFormat(formats[format])) {
                    sendCompletion(tick, packetIndex);
                    soundInDriver.close();
                    deviceOpen = false;
                    return;
                }
                deviceOpen = true;
                currentFormat = format;
            }
            soundInDriver.write(data, tick, packetIndex);
            awaitingDataPacket = false;
            return;
        }

        // Need to get data from the PDU and setup the sound channel
        msgType = data.get8();
        data.get8();                // skip 8 bits bPad
        data.getLittleEndian16();   // skip 16 bits bodySize

        switch (msgType) {
            case MSG_SNDIN_VERSION:
                logger.debug("SoundInChannel:process MSG_SNDIN_VERSION");
                break;
            case MSG_SNDIN_FORMATS:
                logger.debug("SoundInChannel:process MSG_SNDIN_FORMATS");
                break;
            case MSG_SNDIN_OPEN:
                logger.debug("SoundInChannel:process MSG_SNDIN_OPEN");
                break;
            case MSG_SNDIN_OPEN_REPLY:
                logger.debug("SoundInChannel:process MSG_SNDIN_OPEN_REPLY");
                break;
            case MSG_SNDIN_DATA_INCOMING:
                logger.debug("SoundInChannel:process MSG_SNDIN_DATA_INCOMING");
                break;
            case MSG_SNDIN_DATA:
                logger.debug("SoundInChannel:process MSG_SNDIN_DATA");
                break;
            case MSG_SNDIN_FORMATCHANGE:
                logger.debug("SoundInChannel:process MSG_SNDIN_FORMATCHANGE");
                break;
            default:
                logger.debug("SoundInChannel:RDPSNDIN packet msgType " + msgType);
                break;
        }
    }

    /**
     * [MS-RDPEAI] Section 2.2.2.1 Version PDU (MSG_SNDIN_VERSION)
     * <p/>
     * The Version PDU is sent by the server and the client to negotiate
     * which version of the protocol MUST be used.
     */
    public void sendVersionPdu() {
        RdpPacket out = new RdpPacket(5);
        out.set8(MSG_SNDIN_VERSION);
        out.setLittleEndian32(0x00000001);
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

    /**
     * 2.2.2.2 Sound Formats PDU (MSG_SNDIN_FORMATS)
     * <p/>
     * The Sound Formats PDU is sent by the server and the client to negotiate
     * a common list of supported audio formats.
     */
    public void sendSoundFormatsPdu() {

    }

    public void sendCompletion(int wTimeStampPlus, int cConfirmedBlockNo) {
    }

}
