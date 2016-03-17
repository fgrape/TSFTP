package acme.bnss.tsftp;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Erik Borgstrom on 2016-03-17.
 */
public class HexInputStream extends FilterInputStream {

    public HexInputStream(InputStream in) {
        super(in);

    }

    @Override
    public int read() throws IOException {
        int high = in.read();
        int low = in.read();
        if (high == -1) {
            return -1;
        }
        if (low == -1) {
            return -1;
        }
        if (high < 58) {
            high -= 48;
        } else {
            high -= 55;
        }
        if (low < 58) {
            low -= 48;
        } else {
            low -= 55;
        }
        return (high << 4) | low;
    }

    @Override
    public int read(byte[] buff, int offset, int len) throws IOException {
        int len2 = buff.length;
        int i = 0;
        for (; i < len && offset + i < len2; i++) {
            int b = read();
            if (b == -1) {
                return i == 0 ? -1 : i;
            }
            buff[offset + i] = (byte) b;
        }
        return i;
    }

}
