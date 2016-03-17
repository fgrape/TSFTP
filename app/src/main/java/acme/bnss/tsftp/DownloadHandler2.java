package acme.bnss.tsftp;

import android.os.Environment;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
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
import javax.crypto.spec.IvParameterSpec;
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
            doDownload(secretKey, hash, fileName);
            return new DownloadResult(fileName);
        } catch (Exception e) {
            Log.d("EXCEPTION", e.getMessage());
            return DownloadResult.failure("Failed to download file");
        }
    }

    private void doDownload(SecretKey secretKey, String hash, String fileName) throws Exception {
        String file = "tsftp.php?action=file&hash=" + hash + "&filename=" + fileName;
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        InputStream in = new BufferedInputStream(connection.getInputStream());
        InputStream hex = new HexInputStream(in);
        File fileOnDisk = new File(Environment.getExternalStoragePublicDirectory("TSFTP/Downloads"), fileName);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(fileOnDisk));
        decrypt(secretKey, hex, out);
        out.flush();
        out.close();
    }

    private void decrypt(SecretKey secretKey, InputStream in, OutputStream out) throws Exception {
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec("bnss1337bnss1337".getBytes("ISO8859-1"));
        aes.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        CipherInputStream cipherIn = new CipherInputStream(in, aes);
        byte[] buff = new byte[512];
        for (int i; (i = cipherIn.read(buff)) != -1;) {
            out.write(buff, 0, i);
        }
        cipherIn.close();
    }

    private SecretKey getSecretKey(String hash) throws Exception {
        String file = "tsftp.php?action=key&hash=" + hash;
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        connection.connect();
        InputStream in = new BufferedInputStream(connection.getInputStream());
        InputStream hex = new HexInputStream(in);
        SecretKey secretKey = unwrapSecretKey(hex);
        connection.disconnect();
        return secretKey;
    }

    private SecretKey unwrapSecretKey(InputStream in) throws Exception {
        PrivateKey privateKey = getClientPrivateKey();
        Cipher rsa = Cipher.getInstance("RSA");
        rsa.init(Cipher.UNWRAP_MODE, privateKey);
        byte[] bytes = new byte[512];
        in.read(bytes);
        SecretKey secretKey = (SecretKey) rsa.unwrap(bytes, "AES", Cipher.SECRET_KEY);
        return secretKey;
    }

    private PrivateKey getClientPrivateKey() throws Exception {
        File file = new File(Environment.getExternalStorageDirectory(), "client-phone2.key");
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        byte[] keyBytes = PemReader.getBytesFromPem(in);
        Log.d("KEY" , "Private key length:" + keyBytes.length);
        in.close();
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    private boolean deleteFile(TSFTPFileDescriptor descriptor) {
        String file = "tsftp.php?action=delete&hash=" + descriptor.getHash();
        HttpsURLConnection connection = null;
        try {
            connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
            connection.setRequestMethod("POST");
            connection.setDoInput(false);
            connection.connect();
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
