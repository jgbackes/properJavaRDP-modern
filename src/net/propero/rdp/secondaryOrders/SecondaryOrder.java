package net.propero.rdp.secondaryOrders;

import net.propero.rdp.RdpPacket;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 2/8/11
 * Time: 10:17 AM
 */
public interface SecondaryOrder {

    public void process(RdpPacket data, int flags, boolean compressed);
}
