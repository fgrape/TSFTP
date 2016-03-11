package acme.bnss.tsftp;

import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
            writeFileToDisk(fileIn, fileName);
            fileIn.close();
            return new DownloadResult(fileName);
        } catch (Exception e) {
            return DownloadResult.failure("Failed to acquire file from server: " + e.getMessage());
        }
    }

    private void writeFileToDisk(InputStream in, String fileName) throws Exception {
        if (!isExternalStorageWritable()) {
            throw new Exception();
        }
        File file = new File(Environment.getExternalStoragePublicDirectory("TSFTP/Downloads"), fileName);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] buff = new byte[1024];
            for (int i; (i = in.read(buff)) != -1;) {
                out.write(buff, 0, i);
            }
        }
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

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public boolean deleteFile(String fileLink) {
        TSFTPFileDescriptor fileDescriptor;
        try {
            fileDescriptor = new TSFTPFileDescriptor(fileLink);
        } catch (Exception e) {
            return false;
        }
        String file = "tsftp.php?action=delete&hash=" + fileDescriptor.getHash();
        HttpsURLConnection connection = null;
        try {
            connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
            connection.setRequestMethod("POST");
            // TODO Implement.
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return true;
    }

}
