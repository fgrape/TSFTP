package acme.bnss.tsftp;

import android.os.Environment;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Erik Borgstrom on 2016-03-16.
 */
public class UploadHandler2 {

    private String message;

    public UploadHandler2() {

    }

    public UploadResult uploadFile(String email, File file) {
            X509Certificate receiverCert;
            try {
               receiverCert = getCertificateFor(email);
            } catch (Exception e) {
                return UploadResult.failure("Invalid recipient: " + email);
            }
        try {
            verifyRecipientCert(receiverCert);
        } catch (Exception e) {
            return UploadResult.failure("Invalid recipiant: " + email);
        }
        SecretKey secretKey;
        try {
            secretKey = generateSecretKey();
        } catch (Exception e) {
            return UploadResult.failure("Internal error");
        }
        InputStream fileIn;
        try {
            fileIn = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            return UploadResult.failure("Could not read file: " + e.getMessage());
        }
        try {
            TSFTPFileDescriptor fileDescriptor = doUpload(file.getName(), fileIn, receiverCert, secretKey);
            return new UploadResult(fileDescriptor);
        } catch (Exception e) {
            Log.d("EXCEPTION", e.getCause().toString());
            return UploadResult.failure("Failed to upload file: " + e.getMessage());
        }
    }

    private TSFTPFileDescriptor doUpload(String fileName, InputStream fileInputStream, X509Certificate receiverCert, SecretKey secretKey) throws Exception {
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer("tsftp.php?action=upload");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        OutputStream out = new BufferedOutputStream(connection.getOutputStream());
        PrintStream printer = new PrintStream(out);

        printer.append("&fileName=");
        printer.append(fileName);

        printer.append("&encryptionKey=");
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        OutputStream base64Out = new Base64OutputStream(buff, Base64.DEFAULT);
        wrapSecretKey(secretKey, receiverCert, base64Out);
        String urlEncoded = URLEncoder.encode(buff.toString("ISO8859-1"), "ISO8859-1");
        out.write(urlEncoded.getBytes("ISO8859_1"));
        out.flush();

        printer.append("fileData=");
        ByteArrayOutputStream buff2 = new ByteArrayOutputStream();
        OutputStream base64Out2 = new Base64OutputStream(buff2, Base64.DEFAULT);
        encrypt(secretKey, fileInputStream, base64Out2);
        String urlEncoded2 = URLEncoder.encode(buff.toString("ISO8859-1"), "ISO8859-1");
        out.write(urlEncoded2.getBytes("ISO8859_1"));

        out.flush();
        out.close();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to communicate with server");
        }
        TSFTPFileDescriptor fileDescriptor = getFileDescriptor(connection.getInputStream());
        connection.disconnect();
        return fileDescriptor;
    }

    private void encrypt(SecretKey secretKey, InputStream in, OutputStream out) throws Exception {
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec("bnss1337bnss1337".getBytes("ISO8859-1"));
        aes.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        CipherOutputStream cipherOut = new CipherOutputStream(out, aes);
        byte[] buff = new byte[512];
        for (int i; (i = in.read(buff)) != -1;) {
            cipherOut.write(buff, 0, i);
        }
        cipherOut.flush();
        cipherOut.close();
    }

    private void wrapSecretKey(SecretKey secretKey, X509Certificate cert, OutputStream out) throws Exception {
        Cipher rsa = Cipher.getInstance("RSA");
        rsa.init(Cipher.WRAP_MODE, cert);
        byte[] cipherText = rsa.wrap(secretKey);
        out.write(cipherText);
        out.flush();
    }

    private void verifyRecipientCert(X509Certificate cert) throws Exception {

    }

    private TSFTPFileDescriptor getFileDescriptor(InputStream in) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = reader.readLine();
            return new TSFTPFileDescriptor(line);
        }
    }

    private X509Certificate getCertificateFor(String email) throws Exception {
        String file = "tsftp.php?action=cert&user=" + getUser(email);
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            message = "Failed to communicate with server";
            return null;
        }
        InputStream in = new BufferedInputStream(connection.getInputStream());
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        InputStream certIn = new ByteArrayInputStream(getBytesFromPem(in));
        X509Certificate cert = (X509Certificate) factory.generateCertificate(certIn);
        in.close();
        return cert;
    }

    private String getUser(String email) {
        if (email.contains("@")) {
            return email.split("@")[0];
        } else {
            return email;
        }
    }

    private SecretKey generateSecretKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        SecretKey secretKey = generator.generateKey();
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

}
