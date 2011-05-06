/* Subversion properties, do not modify!
 * 
 * $Date: 2008-02-13 23:40:22 -0800 (Wed, 13 Feb 2008) $
 * $Revision: 29 $
 * $Author: miha_vitorovic $
 */

package net.propero.rdp.virtualChannels.rdpSoundOut;

public class ADPCMChannelStatus {
    public int predictor = 0;

    public short step_index = 0;

    public int step = 0;

    /* for encoding */
    public int prev_sample;

    /* MS version */
    public short sample1;

    public short sample2;

    public int coeff1;

    public int coeff2;

    public int idelta;
}