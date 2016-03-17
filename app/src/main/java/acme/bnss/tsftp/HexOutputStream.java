package acme.bnss.tsftp;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Erik Borgstrom on 2016-03-17.
 */
public class HexOutputStream extends FilterOutputStream {

    private byte[] hex;

    public HexOutputStream(OutputStream out) throws Exception {
        super(out);
        hex = "0123456789ABCDEF".getBytes("ISO8859-1");
    }

    @Override
    public void write(int b) throws IOException {
        out.write(hex[(b & 0xF0) >>> 4]);
        out.write(hex[b & 0x0F]);
    }

    @Override
    public void write(byte[] buff, int offset, int len) throws IOException {
        int end = offset + len;
        for (int i = offset; i < buff.length && i < end; i++) {
            write(buff[i]);
        }
    }

}
