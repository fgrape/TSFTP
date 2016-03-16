package acme.bnss.tsftp;

import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Erik Borgstrom on 2016-03-16.
 */
public class DownloadHandler2 {

    public DownloadHandler2() {

    }

    public DownloadResult downloadFile(String fileLink) {
        TSFTPFileDescriptor descriptor;
        try {
            descriptor = new TSFTPFileDescriptor(fileLink);
        } catch (Exception e) {
            return DownloadResult.failure("Invalid file link");
        }
        try {
            String hash = descriptor.getHash();
            String fileName = descriptor.getFileName();
            SecretKey secretKey = getSecretKey(hash);
            Log.d("KEY", "Secret key is: " + new String(secretKey.getEncoded(), "ISO8859-1"));
            return new DownloadResult("File download not implemented");
        } catch (Exception e) {
            return DownloadResult.failure("Failed to download file: " + e.getCause() + " : " + e.getMessage());
        }
    }

    private SecretKey unwrapSecretKey(InputStream in) throws Exception {
        PrivateKey privateKey = getClientPrivateKey();
        Cipher rsa = Cipher.getInstance("RSA");
        rsa.init(Cipher.UNWRAP_MODE, privateKey);
        byte[] buff = new byte[4096];
        int len = in.read(buff);
        byte[] keyBytes = Arrays.copyOf(buff, len);
        SecretKey secretKey = (SecretKey) rsa.unwrap(keyBytes, "RSA", Cipher.SECRET_KEY);
        return secretKey;
    }

    private byte[] getBytesFromPem(InputStream in) throws Exception {
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

    private PrivateKey getClientPrivateKey() throws Exception {
        File file = new File(Environment.getExternalStorageDirectory(), "client-phone2.key");
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        byte[] keyBytes = getBytesFromPem(in);
        in.close();
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

}
