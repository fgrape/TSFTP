package acme.bnss.tsftp;

import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
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
            //Key symmetricKey = new SecretKeySpec(new byte[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}, 0, 16, "AES");
            Log.d("ABC", "We get here!!!!");
            InputStream fileIn = getFileInputStream(symmetricKey, hash, fileName);
            Log.d("ABC", "We get there!!!!");
            writeFileToDisk(fileIn, fileName);
            Log.d("ABC", "We get the whole way!!!!");
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

        ByteArrayOutputStream aa = new ByteArrayOutputStream();
        File file = new File(Environment.getExternalStoragePublicDirectory("TSFTP/Downloads"), fileName);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] buff = new byte[1024];
            for (int i; (i = in.read(buff)) != -1;) {
                out.write(buff, 0, i);
                aa.write(buff, 0, i);
            }
            out.flush();
        }
        Log.d("DATA", "krypterad data: " + aa.toString("ISO8859-1").substring(aa.size()-50, aa.size()));
    }

    private InputStream getFileInputStream(Key key, String hash, String fileName) throws Exception {
        String file = "tsftp.php?action=file&hash=" + hash + "&filename=" + fileName;
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        connection.connect();
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec("bnss1337bnss1337".getBytes("ISO8859-1"));
        aes.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        InputStream in = new BufferedInputStream(connection.getInputStream());
        InputStream in2 = new CipherInputStream(in, aes);
        return in2;
    }

    private Key getSymmetricKey(String hash) throws Exception {
        String file = "tsftp.php?action=key&hash=" + hash;
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        try {
            connection.connect();
            Key privateKey = getClientPrivateKey();
            //Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            //rsa.init(Cipher.DECRYPT_MODE, privateKey);
           // InputStream in = new CipherInputStream(new BufferedInputStream(connection.getInputStream()), rsa);
            InputStream in = new BufferedInputStream(connection.getInputStream());
            byte[] block = new byte[1600];
            in.read(block);
            Log.d("NYCKEL", "Nedladdad nyckel: " + new String(block, 0, 16, "ISO8859-1"));
            SecretKey symmetricKey = new SecretKeySpec(block, 0, 16, "AES");

            return symmetricKey;
        } finally {
            connection.disconnect();
        }
    }

    private Key getClientPrivateKey() throws Exception {
//        KeyStore androidKeyStore = KeyStore.getInstance("AndroidKeyStore");
//        androidKeyStore.load(null);
//
//
//        Enumeration<String> keys = androidKeyStore.aliases();
//        while (keys.hasMoreElements()) {
//            String s = keys.nextElement();
//            Log.d("ABC", s);
//        }
//        char[] password = new char[] {'w','h','a','t','e','v','e','r'};
//        Key key = androidKeyStore.getKey("client_phone", password);
//        Log.d("ABC", "Is This key null?: " + key);
//        return key;
        String whatIsThis = "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";
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.decode(whatIsThis.getBytes("ISO8859-1"), Base64.DEFAULT));
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
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
