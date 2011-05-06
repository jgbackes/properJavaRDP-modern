/* Subversion properties, do not modify!
 * 
 * $Date: 2008-02-13 23:40:22 -0800 (Wed, 13 Feb 2008) $
 * $Revision: 29 $
 * $Author: miha_vitorovic $
 *
 * Author: Miha Vitorovic
 * Author: Jeffrey Backes
 *
 * Based on: (rdpsnd.c)
 * and the document [MS-RDPEA]
 *  rdesktop: A Remote Desktop Protocol client.
 *  Sound Channel Process Functions
 *  Copyright (C) Matthew Chapman 2003
 *  Copyright (C) GuoJunBo guojunbo@ict.ac.cn 2003
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.propero.rdp.virtualChannels.rdpSoundOut;

import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.virtualChannels.VChannel;
import net.propero.rdp.virtualChannels.VChannels;

import java.io.IOException;
import java.util.Calendar;

@SuppressWarnings({"UnusedDeclaration"})
public class SoundOutChannel extends VChannel {

    // [MS-RDPEA] Remote Desktop Protocol:
    //      Audio Output Virtual Channel Extension
    //      Section 2.2.1 RDPSND PDU Header (SNDPROLOG)
    public static final int SNDC_CLOSE = 0x01;      // Close PDU
    public static final int SNDC_WAVE = 0x02;       // WaveInfo PDU
    public static final int SNDC_SETVOLUME = 0x03;  // Volume PDU
    public static final int SNDC_SETPITCH = 0x04;   // Pitch PDU
    public static final int SNDC_WAVECONFIRM = 0x05;    // Wave Confirm PDU
    public static final int SNDC_TRAINING = 0x06;   // Training PDU or Training Confirm PDU
    public static final int SNDC_FORMATS = 0x07;    // Server Audio Formats and Version PDU or Client Audio Formats and Version PDU
    public static final int SNDC_CRYPTKEY = 0x08;     // Crypt Key PDU
    public static final int SNDC_WAVEENCRYPT = 0x09;  // Wave Encrypt PDU
    public static final int SNDC_UDPWAVE = 0x0A;      // UDP Wave PDU
    public static final int SNDC_UDPWAVELAST = 0x0B;  // UDP Wave Last PDU
    public static final int SNDC_QUALITYMODE = 0x0C;  // Quality Mode PDU

    // [MS-RDPEA] Remote Desktop Protocol:
    //  2.2.2.2 Client Audio Formats and Version PDU
    //      (CLIENT_AUDIO_VERSION_AND_FORMATS)
    public static final int TSSNDCAPS_ALIVE = 0x00000001;   // The client is capable of consuming audio data. This flag MUST be set for audio data to be transferred.
    public static final int TSSNDCAPS_VOLUME = 0x00000002;  // The client is capable of applying a volume change to all the audio data that is received.
    public static final int TSSNDCAPS_PITCH = 0x00000004;   // The client is capable of applying a pitch change to all the audio data that is received.

    public static final int MAX_FORMATS = 10;
    public static final int AUDIO_FORMAT_SIZE = 18;         // 2.2.2.1.1 Audio Format (AUDIO_FORMAT)

    private boolean awaitingDataPacket;
    private boolean deviceOpen;
    private int format;
    private int currentFormat;
    private int tick;
    private long startTime;
    private int packetIndex;
    private int formatCount;
    private SoundOutDriver soundOutDriver;
    private WaveFormatEx[] formats;

    public SoundOutChannel() {
        super();
        awaitingDataPacket = false;
        deviceOpen = false;
        format = 0;
        currentFormat = 0;
        tick = 0;
        startTime = 0;
        packetIndex = 0;
        formatCount = 0;
        formats = new WaveFormatEx[MAX_FORMATS];
        for (int i = 0; i < MAX_FORMATS; i++) {
            formats[i] = new WaveFormatEx();
        }
        soundOutDriver = new SoundOutDriver(this);
    }

    public int flags() {
        return VChannels.CHANNEL_OPTION_INITIALIZED
                | VChannels.CHANNEL_OPTION_ENCRYPT_RDP;
    }

    public String name() {
        return "rdpsnd";
    }

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
                if (!deviceOpen && !soundOutDriver.open()) {
                    sendCompletion(tick, packetIndex);
                    return;
                }
                if (!soundOutDriver.setSoundFormat(formats[format])) {
                    sendCompletion(tick, packetIndex);
                    soundOutDriver.close();
                    deviceOpen = false;
                    return;
                }
                deviceOpen = true;
                currentFormat = format;
            }
            soundOutDriver.write(data, tick, packetIndex);
            awaitingDataPacket = false;
            return;
        }

        // Need to get data from the PDU and setup the sound channel
        msgType = data.get8();
        data.get8();                // skip 8 bits bPad
        data.getLittleEndian16();   // skip 16 bits bodySize

        switch (msgType) {
            case SNDC_WAVE:
                logger.debug("SoundOutChannel:process SNDC_WAVE");

                startTime = Calendar.getInstance().getTimeInMillis();
                tick = data.getLittleEndian16() & 0xFFFF;
                format = data.getLittleEndian16() & 0xFFFF;
                packetIndex = data.getLittleEndian16() & 0xFFFF;
                awaitingDataPacket = true;
                break;
            case SNDC_CLOSE:
                logger.debug("SoundOutChannel:process SNDC_CLOSE");

                soundOutDriver.close();
                deviceOpen = false;
                break;
            case SNDC_FORMATS:
                logger.debug("SoundOutChannel:process SNDC_FORMATS");

                processServerAudioFormats(data);
                break;
            case SNDC_TRAINING:
                logger.debug("SoundOutChannel:process SNDC_TRAINING");

                sendTrainingConfirmPDU(data);
                break;
            case SNDC_SETVOLUME:
                logger.debug("SoundOutChannel:process SNDC_SETVOLUME");

                int volume = data.getLittleEndian32();
                if (deviceOpen) {
                    soundOutDriver.setOutputVolume((volume & 0xffff), (volume >> 16) & 0xffff);
                }
                break;
            case SNDC_SETPITCH:
                logger.warn("SoundOutChannel:process Unhandled msgTyp = SNDC_SETPITCH");
                break;
            case SNDC_CRYPTKEY:
                logger.warn("SoundOutChannel:process Unhandled msgTyp = SNDC_CRYPTKEY");
                break;
            case SNDC_WAVEENCRYPT:
                logger.warn("SoundOutChannel:process Unhandled msgTyp = SNDC_WAVEENCRYPT");
                break;
            case SNDC_UDPWAVE:
                logger.warn("SoundOutChannel:process Unhandled msgTyp = SNDC_UDPWAVE");
                break;
            case SNDC_UDPWAVELAST:
                logger.warn("SoundOutChannel:process Unhandled msgTyp = SNDC_UDPWAVELAST");
                break;
            case SNDC_QUALITYMODE:
                logger.warn("SoundOutChannel:process Unhandled msgTyp = SNDC_QUALITYMODE");
                break;


            default:
                logger.debug("SoundOutChannel:RDPSND packet msgType " + msgType);
                break;
        }
    }

    public void waveOutPlay() {
        logger.debug("SoundOutChannel:waveOutPlay");
        if (soundOutDriver.doesDspHaveData()) {
            soundOutDriver.waveOutPlay();
        }
    }

    /**
     * [MS-RDPEA] Section: 2.2.3.8 Wave Confirm PDU (SNDWAV_CONFIRM)
     *
     * @param wTimeStampPlus    MUST be set to the same field of the originating
     *                          WaveInfo PDU, Wave Encrypt PDU, or UDP Wave Last PDU, plus the
     *                          time, in milliseconds, between receiving the packet from the
     *                          network and sending this PDU. This enables the server to calculate
     *                          the amount of time it takes for the client to receive the audio data
     *                          PDU and send the confirmation.
     * @param cConfirmedBlockNo MUST be the same as the cBlockNo field of the UDP
     *                          Wave Last PDU (section 2.2.3.7), the Wave Encrypt PDU (section 2.2.3.5)
     *                          or the WaveInfo PDU (section 2.2.3.3) just received from the server.
     */
    public void sendCompletion(int wTimeStampPlus, int cConfirmedBlockNo) {
        logger.debug("SoundOutChannel:sendCompletion");
        RdpPacket out = initPacket(SNDC_WAVECONFIRM, 4);
        out.setLittleEndian16(wTimeStampPlus);
        out.set8(cConfirmedBlockNo);
        out.set8(0);
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
     * [MS-RDPEA] section 2.2.2.1 Server Audio Formats and Version PDU (SERVER_AUDIO_VERSION_AND_FORMATS)
     * The Server Audio Formats and Version PDU is a PDU
     * used by the server to send version information and a
     * list of supported audio formats to the client.
     * This PDU MUST be sent using static virtual channels.
     *
     * @param data RDP packet
     */
    private void processServerAudioFormats(RdpPacket data) {
        logger.debug("SoundOutChannel:processServerAudioFormats");
        data.incrementPosition(14); // advance 14 bytes - skipping: dwFlags, dwVolume, dwPitch, wDGramPort

        int wNumberOfFormats = data.getLittleEndian16();

        data.incrementPosition(4); // advance 4 bytes - skipping: cLastBlockConfirmed, wVersion, bPad

        formatCount = 0;

        if (checkRemaining(data, AUDIO_FORMAT_SIZE * wNumberOfFormats)) {       // Insure there is enough room in the buffer
            for (int i = 0; i < wNumberOfFormats; i++) {
                WaveFormatEx audioFormat = formats[formatCount];
                audioFormat.setwFormatTag(data.getLittleEndian16());
                audioFormat.setnChannels(data.getLittleEndian16());
                audioFormat.setnSamplesPerSec(data.getLittleEndian32());
                audioFormat.setnAvgBytesPerSec(data.getLittleEndian32());
                audioFormat.setnBlockAlign(data.getLittleEndian16());
                audioFormat.setwBitsPerSample(data.getLittleEndian16());
                audioFormat.setCbSize(data.getLittleEndian16());

                int readCnt = audioFormat.getCbSize();
                int discardCnt = 0;
                if (audioFormat.getCbSize() > WaveFormatEx.MAX_CBSIZE) {
                    logger.error("cbSize too large for buffer: " + audioFormat.getCbSize());
                    readCnt = WaveFormatEx.MAX_CBSIZE;
                    discardCnt = audioFormat.getCbSize() - WaveFormatEx.MAX_CBSIZE;
                }

                data.copyToByteArray(audioFormat.getCb(), 0, data.getPosition(), readCnt);

                // skip unusable sound formats advance packet position
                data.incrementPosition(readCnt + discardCnt);

                if (soundOutDriver.isSoundFormatSupported(audioFormat)) {
                    logger.debug("Audio format SUPPORTED. audioFormat = " + audioFormat.toString());
                    formatCount++;
                    if (formatCount == MAX_FORMATS) {
                        logger.warn("formatCount reached maximum.");
                        break;
                    }
                } else {
                    logger.info("Audio format not supported. audioFormat = " + audioFormat.toString());
                }
            }
        } else {
            logger.warn("Buffer size error.");
        }

        // Setup the response packet
        // [MS-RDPEA] Section 2.2.2.2 Client Audio Formats and Version PDU (CLIENT_AUDIO_VERSION_AND_FORMATS)
        RdpPacket out = initPacket(SNDC_FORMATS | 0x200, 20 + (AUDIO_FORMAT_SIZE * formatCount));
        out.setLittleEndian32(TSSNDCAPS_ALIVE | TSSNDCAPS_VOLUME); // dwFlags
        out.setLittleEndian32(0xffffffff);      // dwVolume left and right full volume
        out.setLittleEndian32(0);               // dwPitch
        out.setLittleEndian16(0);               // wDGramPort
        out.setLittleEndian16(formatCount);     // wNumberOfFormats
        out.set8(0x00);                         // cLastBlockConfirmed (unused)
        out.setLittleEndian16(0x05);            // wVersion [MS-RDPEA] Section 6 Appendix A: Product Behavior
        out.set8(0x00);                         // bPad

        for (int i = 0; i < formatCount; i++) {
            WaveFormatEx format = formats[i];
            out.setLittleEndian16(format.getwFormatTag());
            out.setLittleEndian16(format.getnChannels());
            out.setLittleEndian32(format.getnSamplesPerSec());
            out.setLittleEndian32(format.getnAvgBytesPerSec());
            out.setLittleEndian16(format.getnBlockAlign());
            out.setLittleEndian16(format.getwBitsPerSample());
            out.setLittleEndian16(0); // cbSize
        }

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

    private boolean checkRemaining(RdpPacket p, int required) {
        return p.getPosition() + required <= p.size();
    }

    // 2.2.3.2 Training Confirm PDU (SNDTRAININGCONFIRM)
    private void sendTrainingConfirmPDU(RdpPacket data) {
        int wTimeStamp;
        int wPackSize;

        wTimeStamp = data.getLittleEndian16();
        wPackSize = data.getLittleEndian16();

        RdpPacket out = initPacket(SNDC_TRAINING, 4);
        out.setLittleEndian16(wTimeStamp);
        out.setLittleEndian16(wPackSize);
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

    private RdpPacket initPacket(int type, int size) {
        RdpPacket s = new RdpPacket(size + 4);
        s.setLittleEndian16(type);
        s.setLittleEndian16(size);
        return s;
    }
}
