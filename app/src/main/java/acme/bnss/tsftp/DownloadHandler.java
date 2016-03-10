package acme.bnss.tsftp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.security.cert.X509Certificate;

/**
 * Created by Erik Borgstrom on 2016-03-08.
 */
public class DownloadHandler {

    public DownloadHandler() {

    }

    public DownloadResult downloadFile(String fileLink) {
        TSFTPFileDescriptor fileDescriptor;
        try {
            fileDescriptor = new TSFTPFileDescriptor(fileLink);
        } catch (Exception e) {
            return DownloadResult.failure("Invalid file link");
        }
        try {
            String hash = fileDescriptor.getHash();
            String fileName = fileDescriptor.getFileName();
            Key symmetricKey = getSymmetricKey(hash);
            InputStream fileIn = getFileInputStream(symmetricKey, hash, fileName);
            writeFileToDisk(fileIn);
            fileIn.close();
            return new DownloadResult(fileName);
        } catch (Exception e) {
            return DownloadResult.failure("");
        }
    }

    private void writeFileToDisk(InputStream in) {

    }

    private InputStream getFileInputStream(Key key, String hash, String fileName) throws Exception {
        String file = "tsftp.php?action=file&hash=" + hash + "&filename=" + fileName;
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        connection.connect();
        Cipher rsa = Cipher.getInstance("AES");
        rsa.init(Cipher.DECRYPT_MODE, key);
        InputStream in = new BufferedInputStream(connection.getInputStream());
        InputStream in2 = new CipherInputStream(in, rsa);
        return in2;
    }

    private Key getSymmetricKey(String hash) throws Exception {
        String file = "tsftp.php?action=key&hash=" + hash;
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        try {
            connection.connect();
            Key privateKey = getClientPrivateKey();
            Cipher rsa = Cipher.getInstance("RSA");
            rsa.init(Cipher.DECRYPT_MODE, privateKey);
            InputStream in = new BufferedInputStream(connection.getInputStream());
            InputStream in2 = new CipherInputStream(in, rsa);
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            for (int b; (b = in2.read()) != -1;) {
                buff.write(b);
            }
            SecretKey symmetricKey = new SecretKeySpec(buff.toByteArray(), "AES");
            return symmetricKey;
        } finally {
            connection.disconnect();
        }
    }

    private Key getClientPrivateKey() throws Exception {
        KeyStore androidKeyStore = KeyStore.getInstance("AndroidKeyStore");
        androidKeyStore.load(null);
        char[] password = new char[] {'w','h','a','t','e','v','e','r'};
        Key key = androidKeyStore.getKey("client", password);
        return key;
    }

    public void deleteFile(String fileLink) {

    }

}
