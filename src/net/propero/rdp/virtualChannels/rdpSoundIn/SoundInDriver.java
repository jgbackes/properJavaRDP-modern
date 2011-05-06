package net.propero.rdp.virtualChannels.rdpSoundIn;

import net.propero.rdp.RdpPacket;
import net.propero.rdp.virtualChannels.rdpSoundOut.WaveFormatEx;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 2/14/11
 * Time: 9:45 AM
 */
public class SoundInDriver {

    SoundInChannel soundInChannel;

    public SoundInDriver(SoundInChannel soundInChannel) {
        this.soundInChannel = soundInChannel;
    }


    public boolean open() {
        return true;
    }

    public void close() {
    }

    public boolean setSoundFormat(WaveFormatEx waveFormat) {
        boolean result = false;

        return result;
    }

    public void write(RdpPacket s, int tick, int packetIndex) {

    }
}
