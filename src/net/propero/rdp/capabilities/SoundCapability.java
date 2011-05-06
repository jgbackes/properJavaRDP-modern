package net.propero.rdp.capabilities;

import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * [MS-RDPBCGR] Section 2.2.7.1.11 Sound Capability Set (TS_SOUND_CAPABILITYSET)
 *
 * <p/>
 *
 * The TS_SOUND_CAPABILITYSET structure advertises the ability to play a "beep" sound.
 * This capability is sent only from client to server.
 * <p/>
 * <p/>
 *
 * @author jbackes
 * @see <a href="http://msdn.microsoft.com/en-us/library/cc240552(v=PROT.10).aspx">[MS-RDPBCGR] Section 2.2.7.1.11 Sound Capability Set</a>
 * @since ProperJavaRDP 3.0
 */
public class SoundCapability implements Capability {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    private static final int CAPLEN_SOUND = 8;
    private static final int SOUND_BEEPS_FLAG = 0x0100;

    /**
     * Size of the capability in bytes
     *
     * @return Size of capability
     */
    @Override
    public int getSize() {
        return CAPLEN_SOUND;
    }

    /**
     * Unique ID of this capability
     *
     * @return Unique capability ID
     */
    @Override
    public int getID() {
        return CAPSTYPE_SOUND;
    }

    /**
     * Set the bytes that make up the capability record that will be
     * sent over the wire
     *
     * @param data Packet that will contain the capability information
     */
    @Override
    public void setBytes(RdpPacket data) {
        data.setLittleEndian16(CAPSTYPE_SOUND);
        data.setLittleEndian16(CAPLEN_SOUND);

        data.setLittleEndian16(SOUND_BEEPS_FLAG);       // soundFlags
        data.setLittleEndian16(0);                      // pad2octetsA
    }

    @Override
    public void processServerCapabilities(RdpPacket data) {
        logger.debug("Not yet implemented");
    }
}
