package acme.bnss.tsftp;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class PKCS8Reader {

    public static byte[] getBytesFromPem(InputStream in) throws Exception {
        Charset charset = Charset.forName("ISO8859-1");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        reader.readLine();
        String line = reader.readLine();
        while (line != null) {
            String nextLine = reader.readLine();
            if (nextLine != null) {
                buff.write(line.getBytes(charset));
            }
            line = nextLine;
        }
        byte[] bytes = Base64.decode(buff.toByteArray(), Base64.DEFAULT);
        return bytes;
    }

}
