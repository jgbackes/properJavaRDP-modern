package net.propero.rdp.capabilities;

import net.propero.rdp.Options;
import net.propero.rdp.RdpPacket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 3/10/11
 * Time: 8:17 PM
 */
public class CapabilityManager {

    static Logger logger = Logger.getLogger(CapabilityManager.class);

    static {
        logger.setLevel(Level.DEBUG);
    }

    // Mandatory Capability Sets
    private GeneralCapability generalCaps = new GeneralCapability();
    private BitmapCapability bitmapCaps = new BitmapCapability();
    private OrderCapability orderCaps = new OrderCapability();
    private BitmapCacheCapability bitmapCacheCaps = new BitmapCacheCapability();
    private BitmapCache2Capability bitmapCache2Caps = new BitmapCache2Capability();
    private PointerCapability pointerCaps = new PointerCapability();
    private InputCapability inputCaps = new InputCapability();
    private BrushCapability brushCaps = new BrushCapability();
    private GlyphCacheCapability glyphCacheCaps = new GlyphCacheCapability();
    private OffscreenBitmapCapability offscreenBitmapCaps = new OffscreenBitmapCapability();
    private VirtualChannelCapability virtualChannelCaps = new VirtualChannelCapability();
    private SoundCapability soundCaps = new SoundCapability();

    // Optional Capability Sets - Listed in order presented in the BCGR documentation
    private ControlCapability controlCaps = new ControlCapability();
    private WindowActivationCapability windowActivateCaps = new WindowActivationCapability();
    private ShareCapability shareCaps = new ShareCapability();
    private FontCapability fontCaps = new FontCapability();
    private ColorCacheCapability colorCacheCaps = new ColorCacheCapability();

    private static CapabilityManager capabilityManager = null;

    private CapabilityManager() {
    }

    public static synchronized CapabilityManager getInstance() {
        if (null == capabilityManager) {
            capabilityManager = new CapabilityManager();
        }

        return capabilityManager;
    }

    /**
     * Process server capabilities
     *
     * @param data   Packet containing capability set data at current read position
     * @param length Size of the serverCaps data
     */

    public void processServerCaps
    (RdpPacket
             data, int length) {

        int next;
        int start;
        int numberCapabilities;
        int capsetType;
        int capsetLength;

        start = data.getPosition();

        numberCapabilities = data.getLittleEndian16();
        data.incrementPosition(2);

        for (int i = 0; i < numberCapabilities; i++) {
            if (data.getPosition() <= start + length) {

                capsetType = data.getLittleEndian16();
                capsetLength = data.getLittleEndian16();

                next = data.getPosition() + capsetLength - 4;

                switch (capsetType) {
                    case Capability.CAPSTYPE_GENERAL:
                        generalCaps.processServerCapabilities(data);
                        break;
                    case Capability.CAPSTYPE_BITMAP:
                        bitmapCaps.processServerCapabilities(data);
                        break;
                    case Capability.CAPSTYPE_SHARE:
                        shareCaps.processServerCapabilities(data);
                        break;
                    case Capability.CAPSTYPE_VIRTUALCHANNEL:
                        virtualChannelCaps.processServerCapabilities(data);
                        break;
                    case Capability.CAPSTYPE_DRAWGDIPLUS:
                        logger.warn("Unhandled capability type CAPSTYPE_DRAWGDIPLUS");
                        break;
                    case Capability.CAPSTYPE_FONT:
                        fontCaps.processServerCapabilities(data);
                        break;
                    case Capability.CAPSTYPE_ORDER:
                        orderCaps.processServerCapabilities(data);
                        break;
                    case Capability.CAPSTYPE_COLORCACHE:
                        colorCacheCaps.processServerCapabilities(data);
                        break;
                    case Capability.CAPSTYPE_BITMAPCACHE_HOSTSUPPORT:
                        logger.warn("Unhandled capability type CAPSTYPE_BITMAPCACHE_HOSTSUPPORT");
                        break;
                    case Capability.CAPSTYPE_POINTER:
                        pointerCaps.processServerCapabilities(data);
                        break;
                    case Capability.CAPSTYPE_INPUT:
                        inputCaps.processServerCapabilities(data);
                        break;
                    case Capability.CAPSTYPE_RAIL:
                        logger.warn("Unhandled capability type CAPSTYPE_RAIL");
                        break;
                    case Capability.CAPSTYPE_WINDOW:
                        logger.warn("Unhandled capability type CAPSTYPE_WINDOW");
                        break;
                    default:
                        logger.warn("Unhandled capability type = " + capsetType);
                }

                data.setPosition(next);
            } else {
                logger.warn("Ran out of capabilities!");
                break;
            }
        }
    }

    public Capability[] getMandatoryCaps() {

        return new Capability[]{
                generalCaps,
                bitmapCaps,
                orderCaps,
                ((Options.isRdp5() && Options.isPersistentBitmapCaching()) ?
                        bitmapCache2Caps : bitmapCacheCaps),
                pointerCaps,
                inputCaps,
                // brushCaps,
                glyphCacheCaps,
                // offscreenBitmapCaps,
                // virtualChannelCaps,
                soundCaps
        };
    }

    public Capability[] getOptionalCaps() {

        return new Capability[]{
                controlCaps,
                fontCaps,
                colorCacheCaps,
                shareCaps,
                windowActivateCaps
        };
    }
}
