/* Subversion properties, do not modify!
 * 
 * Sound Channel Process Functions - javax.sound-driver
 * 
 * $Date: 2008-04-17 02:14:13 -0700 (Thu, 17 Apr 2008) $
 * $Revision: 32 $
 * $Author: miha_vitorovic $
 * 
 * Author: Miha Vitorovic
 * 
 * Based on: (rdpsnd_libao.c)
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

import net.propero.rdp.Rdp;
import net.propero.rdp.RdpPacket;
import net.propero.rdp.virtualChannels.VChannels;
import org.apache.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.util.Calendar;

public class SoundOutDriver {

    private static final int MAX_QUEUE = 2048;

    //private static final int BUFFER_SIZE = 65536;
    private static final int BUFFER_SIZE = 1024 * 16;

    protected static Logger logger = Logger.getLogger(Rdp.class);

    private SoundOutChannel soundOutChannel;
    private AudioPacket[] packetQueue;
    private int queueHi;
    private int queueLo;
    private boolean reopened;
    private boolean dspBusy;
    private byte[] buffer;
    private byte[] outBuffer;
    private SourceDataLine outputDataLine;
    private WaveFormatEx format;
    private int volume;
    private FloatControl volumeControl;
    private FloatControl panControl;
    private long startTime;

    public SoundOutDriver(SoundOutChannel sndOutChannel) {
        soundOutChannel = sndOutChannel;
        packetQueue = new AudioPacket[MAX_QUEUE];
        for (int i = 0; i < MAX_QUEUE; i++) {
            packetQueue[i] = new AudioPacket();
        }
        queueHi = 0;
        queueLo = 0;
        reopened = true;
        dspBusy = false;
        buffer = new byte[BUFFER_SIZE];
        outBuffer = new byte[BUFFER_SIZE];
        outputDataLine = null;
        format = null;
        volume = 65535;
        volumeControl = null;
        panControl = null;
    }

    public boolean open() {
        return true;
    }

    public void close() {
        while (queueLo != queueHi) {
            soundOutChannel.sendCompletion(packetQueue[queueLo].getTick(),
                    packetQueue[queueLo].getIndex());
            queueLo = (queueLo + 1) % MAX_QUEUE;
        }
        if (outputDataLine != null) {
            outputDataLine.stop();
            outputDataLine.flush();
            outputDataLine.close();
            outputDataLine = null;
            volumeControl = null;
            panControl = null;
        }
    }

    public boolean setSoundFormat(WaveFormatEx waveFormat) {
        boolean result = false;

        format = waveFormat;

        WaveFormatEx trFormat = SoundDecoder.translateFormatForDevice(waveFormat);
        AudioFormat audioFormat = new AudioFormat(
                trFormat.getnSamplesPerSec(),
                trFormat.getwBitsPerSample(),
                trFormat.getnChannels(),
                true, false);       // Signed, littleEndian

        try {
            // If the ouput data line is open already, drain and close
            if (outputDataLine != null) {
                outputDataLine.drain();
                outputDataLine.close();
            }

            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            outputDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

            outputDataLine.open(audioFormat);
            if (outputDataLine.isControlSupported(FloatControl.Type.VOLUME)) {
                volumeControl = (FloatControl) outputDataLine.getControl(FloatControl.Type.VOLUME);
            } else {
                logger.info("SoundOutDriver:setSoundFormat volumeControl = NULL");
            }

            if (outputDataLine.isControlSupported(FloatControl.Type.VOLUME)) {
                panControl = (FloatControl) outputDataLine.getControl(FloatControl.Type.PAN);
            } else {
                logger.info("SoundOutDriver:setSoundFormat panControl = NULL");
            }

            outputDataLine.start();

            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        reopened = true;

        return result;
    }

    public void write(RdpPacket s, int tick, int packetIndex) {
        AudioPacket packet = packetQueue[queueHi];
        int nextHi = (queueHi + 1) % MAX_QUEUE;

        if (nextHi == queueLo) {
            logger.error("No space to queue audio packet");
            return;
        }

        queueHi = nextHi;

        packet.setPacket(s);
        packet.setTick(tick);
        packet.setIndex(packetIndex);

        packet.getPacket().incrementPosition(4);

        if (!dspBusy) {
            waveOutPlay();
        }
    }

    /**
     * Set the volume on both channels and set the pan value based on the
     * relative difference between the two values
     *
     * @param left  Volume setting for left channel
     * @param right Volume setting for right channel
     */
    public void setOutputVolume(int left, int right) {

        if (volumeControl != null) {
            float volumeValue = volume * volumeControl.getPrecision();
            volumeValue += volumeControl.getMinimum();

            logger.info("SoundOutDriver:setOutputVolume volumeValue = " + volumeValue);

            volumeControl.setValue(volumeValue);
        } else {
            logger.info("SoundOutDriver:setOutputVolume volumeControl = NULL");
        }

        if (panControl != null) {
            float leftPercent = left / 65535;
            float rightPercent = right / 65535;
            float panValue = leftPercent - rightPercent;

            logger.info("SoundOutDriver:setOutputVolume panValue = " + panValue);

            panControl.setValue(panValue);
        } else {
            logger.info("SoundOutDriver:setOutputVolume panControl = NULL");
        }
    }

    public void waveOutPlay() {

        if (reopened) {
            reopened = false;
            startTime = Calendar.getInstance().getTimeInMillis();
        }

        if (queueLo != queueHi) {

            AudioPacket packet = packetQueue[queueLo];
            RdpPacket out = packet.getPacket();

            int writeLength = out.size() - out.getPosition();

            if (writeLength > BUFFER_SIZE) {
                logger.warn("SoundOutDriver:waveOutPlay:writeLength = " +
                        writeLength + ", truncated to " + BUFFER_SIZE);
                writeLength = BUFFER_SIZE;
            }

            out.copyToByteArray(buffer, 0, out.getPosition(), writeLength);
            out.incrementPosition(writeLength);

            int outLen = SoundDecoder.getBufferSize(writeLength, format);
            if (outLen > outBuffer.length) {
                logger.info("SoundOutDriver:waveOutPlay:outLen = " + outLen);
                outBuffer = new byte[outLen];
            }
            outBuffer = SoundDecoder.decode(buffer, outBuffer, writeLength, format);

            outputDataLine.write(outBuffer, 0, outLen);

            long currentTime = Calendar.getInstance().getTimeInMillis();
            long duration = currentTime - startTime;
            int nextTick;
            if (((queueLo + 1) % MAX_QUEUE) != queueHi) {
                nextTick = packetQueue[(queueLo + 1) % MAX_QUEUE].getTick();
            } else {
                nextTick = (packet.getTick() + 65535) % 65536;
            }

            if (packet.getTick() > nextTick) {
                nextTick += 65536;
            }

            if ((out.getPosition() == out.size())
                    || (duration > nextTick - packet.getTick() + 500)) {
                startTime = currentTime;
                soundOutChannel.sendCompletion(
                        ((packet.getTick() + (int) duration) % 65536),
                        packet.getIndex());
                queueLo = (queueLo + 1) % MAX_QUEUE;
            } else {
                logger.warn("out.getPosition() != out.size()");
            }
            dspBusy = true;
        } else {
            logger.info("SoundOutDriver:waveOutPlay Queue is empty");
            dspBusy = false;
        }
    }

    public boolean doesDspHaveData() {
        return dspBusy;
    }

    /**
     * Is the specified format supported
     *
     * @param fmt Check this format
     * @return true if the format is supported
     */
    public boolean isSoundFormatSupported(WaveFormatEx fmt) {
        switch (fmt.getwFormatTag()) {
            case VChannels.WAVE_FORMAT_ALAW:
                return ((fmt.getnChannels() == 1) || (fmt.getnChannels() == 2))
                        && (fmt.getwBitsPerSample() == 8);
            case VChannels.WAVE_FORMAT_PCM:
                return ((fmt.getnChannels() == 1) || (fmt.getnChannels() == 2))
                        && ((fmt.getwBitsPerSample() == 8) || (fmt.getwBitsPerSample() == 16));
            // ADPCM crashes the "RDP Clip monitor" on the server
            //case VChannels.WAVE_FORMAT_ADPCM:
            //	logger.info( "ADPCM" );
            //	return ( ( fmt.nChannels == 1 ) || ( fmt.nChannels == 2 ) ) && ( fmt.wBitsPerSample == 4 );
            default:
                return false;
        }
    }
}
