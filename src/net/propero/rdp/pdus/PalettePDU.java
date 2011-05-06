package net.propero.rdp.pdus;

import net.propero.rdp.RdesktopCanvas;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdpPacket;

import java.awt.image.IndexColorModel;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 2/22/11
 * Time: 6:07 PM
 */
public class PalettePDU extends IncomingPDU {

    @Override
    public void process(RdesktopCanvas surface, RdpPacket data) throws RdesktopException {
        int n_colors;
        IndexColorModel cm;
        byte[] palette;

        byte[] red;
        byte[] green;
        byte[] blue;
        int j = 0;

        data.incrementPosition(2); // pad
        n_colors = data.getLittleEndian16(); // Number of Colors in Palette
        data.incrementPosition(2); // pad
        palette = new byte[n_colors * 3];
        red = new byte[n_colors];
        green = new byte[n_colors];
        blue = new byte[n_colors];
        data.copyToByteArray(palette, 0, data.getPosition(), palette.length);
        data.incrementPosition(palette.length);
        for (int i = 0; i < n_colors; i++) {
            red[i] = palette[j];
            green[i] = palette[j + 1];
            blue[i] = palette[j + 2];
            j += 3;
        }
        cm = new IndexColorModel(8, n_colors, red, green, blue);
        surface.registerPalette(cm);
    }
}
