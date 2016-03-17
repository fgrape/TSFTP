package acme.bnss.tsftp;

import android.os.Environment;
import android.util.Base64;
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.security.Principal;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.BitSet;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Erik Borgstrom on 2016-03-16.
 */
public class UploadHandler2 {

    public UploadHandler2() {

    }

    public UploadResult uploadFile(String email, File file) {
        X509Certificate recipientCert;
        try {
            recipientCert = getCertificateFor(email);
        } catch (Exception e) {
            Log.d("EXCEPTION", e.getMessage());
            return UploadResult.failure("Invalid recipient: " + email);
        }
        try {
            verifyRecipientCert(recipientCert);
        } catch (Exception e) {
            Log.d("EXCEPTION", e.getMessage());
            return UploadResult.failure("Invalid recipient: " + email);
        }
        SecretKey secretKey;
        try {
            secretKey = generateSecretKey();
        } catch (Exception e) {
            Log.d("EXCEPTION", e.getMessage());
            return UploadResult.failure("Internal error");
        }
        InputStream fileIn;
        try {
            fileIn = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            Log.d("EXCEPTION", e.getMessage());
            return UploadResult.failure("Could not read file: " + e.getMessage());
        }
        try {
            TSFTPFileDescriptor fileDescriptor = doUpload(file.getName(), fileIn, recipientCert, secretKey);
            return new UploadResult(fileDescriptor);
        } catch (Exception e) {
            Log.d("EXCEPTION", e.getMessage());
            return UploadResult.failure("Failed to upload file: " + e.getMessage());
        }
    }

    private TSFTPFileDescriptor doUpload(String fileName, InputStream fileInputStream, X509Certificate recipientCert, SecretKey secretKey) throws Exception {
        String file = "tsftp.php?action=upload";
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.connect();
        OutputStream out = new BufferedOutputStream(connection.getOutputStream());
        PrintStream printer = new PrintStream(out);

        printer.append("fileName=");
        printer.append(fileName);

        printer.append("&encryptionKey=");
        printer.flush();
        OutputStream hex = new HexOutputStream(out);
        wrapSecretKey(secretKey, recipientCert, hex);
        hex.flush();

        printer.append("&fileData=");
        printer.flush();
        OutputStream hex2 = new HexOutputStream(out);
        encrypt(secretKey, fileInputStream, hex2);

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to communicate with server");
        }
        TSFTPFileDescriptor fileDescriptor = getFileDescriptor(connection.getInputStream());
        connection.disconnect();
        return fileDescriptor;
    }

    private X509Certificate getClientCert() throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        File certFile = new File(Environment.getExternalStorageDirectory(), "client-phone2.crt");
        InputStream pemIn = new BufferedInputStream(new FileInputStream(new File("test.crt")));
        InputStream certIn = new ByteArrayInputStream(PemReader.getBytesFromPem(pemIn));
        X509Certificate cert = (X509Certificate) factory.generateCertificate(certIn);
        return cert;
    }

    private String getSenderFromCert(X509Certificate cert) {
        return cert.getSubjectDN().getName();
    }

    private void sender(X509Certificate cert, OutputStream out) {

    }

    private void signature(X509Certificate cert, InputStream in, OutputStream out) {

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
        Log.d("SECRETKEY", "Length of secret key is: " + cipherText.length);
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
            throw new Exception("Failed to communicate with server");
        }
        InputStream in = new BufferedInputStream(connection.getInputStream());
        InputStream certIn = new ByteArrayInputStream(PemReader.getBytesFromPem(in));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
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

}
