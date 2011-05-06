package net.propero.rdp.virtualChannels.rdpSoundOut;

import net.propero.rdp.RdpPacket;

/**
 * User: jbackes
 * Date: 1/18/11
 * Time: 1:01 PM
 */
public class AudioPacket {
    private RdpPacket packet;
    private int tick;
    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public RdpPacket getPacket() {
        return packet;
    }

    public void setPacket(RdpPacket packet) {
        this.packet = packet;
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
    }
}


