package net.propero.rdp.persistentCache;

/**
 * Created by IntelliJ IDEA.
 * User: jbackes
 * Date: 1/20/11
 * Time: 1:23 PM
 * To change this template use File | Settings | File Templates.
 */

/* Header for an entry in the persistent bitmap cacheManager file */
class CellHeader {
    byte[] bitmap_id = new byte[8];
    int width;
    int height;
    int length;
    int stamp;

    static int size() {
        return 8 * 8 + 8 * 2 + 16 + 32;
    }

    public CellHeader() {
    }

    public CellHeader(byte[] data) {
        for (int i = 0; i < bitmap_id.length; i++)
            bitmap_id[i] = data[i];

        width = data[bitmap_id.length];
        height = data[bitmap_id.length + 1];
        length = (data[bitmap_id.length + 2] >> 8) +
                data[bitmap_id.length + 3];
        stamp = (data[bitmap_id.length + 6] >> 24) +
                (data[bitmap_id.length + 6] >> 16) +
                (data[bitmap_id.length + 6] >> 8) +
                data[bitmap_id.length + 7];
    }

    public byte[] toBytes() {
        return null;
    }
}