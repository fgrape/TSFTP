package acme.bnss.tsftp;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.net.ssl.HttpsURLConnection;

public class DownloadHandler2 {

    private String message;

    public DownloadResult downloadFile(String fileLink) {
        TSFTPFileDescriptor descriptor;
        try {
            descriptor = new TSFTPFileDescriptor(fileLink);
        } catch (Exception e) {
            return DownloadResult.failure("Invalid file link");
        }
        File fileOnDisk = null;
        try {
            String hash = descriptor.getHash();
            String fileName = descriptor.getFileName();
            SecretKey secretKey = getSecretKey(hash);
            fileOnDisk = new File(Environment.getExternalStoragePublicDirectory("TSFTP/Downloads"), fileName);
            doDownload(secretKey, hash, fileName, fileOnDisk);
            boolean auth = verityAuthenticity(hash, fileOnDisk);
            if (!auth) {
                deleteFileOnDisk(fileOnDisk);
                String msg = message != null ? ": " + message : "";
                return DownloadResult.failure("Authentication failure" + msg);
            }
            deleteFile(hash);
            return new DownloadResult(fileName);
        } catch (Exception e) {
            if (fileOnDisk != null) {
                deleteFileOnDisk(fileOnDisk);
            }
            Log.d("EXCEPTION", e.getMessage());
            String msg = message != null ? ": " + message : "";
            return DownloadResult.failure("Failed to download file" + msg);
        }
    }

    private void deleteFileOnDisk(File file) {
        file.delete();
    }

    private boolean verifySender(X509Certificate cert, String senderName) throws Exception {
        String certSenderName = getSenderNameFromCert(cert);
        if (!senderName.equalsIgnoreCase(certSenderName)) {
            // return false;
        }
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        ArrayList<Certificate> certificates = new ArrayList<>();
        certificates.add(cert);
        CertPath certPath = factory.generateCertPath(certificates);
        CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        File caFile = new File(Environment.getExternalStorageDirectory(), "ca.crt");
        InputStream caCertIn = new BufferedInputStream(new FileInputStream(caFile));
        Certificate caCert = factory.generateCertificate(caCertIn);
        caCertIn.close();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        keyStore.setCertificateEntry("ca", caCert);
        PKIXParameters params = new PKIXParameters(keyStore);
        params.setRevocationEnabled(false);
        validator.validate(certPath, params); // Throws exception if not validated.
        return true;
    }

    private boolean verityAuthenticity(String hash, File file) throws Exception {
        String senderName = getSenderName2(hash);
        X509Certificate senderCert = getCertificateFor(senderName);
        if (!verifySender(senderCert, senderName)) {
            message = "Sender not authenticated";
            return false;
        }
        byte[] sig = getSignature(hash);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(senderCert);
        InputStream fileIn = new BufferedInputStream(new FileInputStream(file));
        byte[] buff = new byte[512];
        for (int i; (i = fileIn.read(buff)) != -1; ) {
            signature.update(buff, 0, i);
        }
        fileIn.close();
        boolean sigValid = signature.verify(sig);
        return sigValid;
    }

    private byte[] getSignature(String hash) throws Exception {
        String file = "tsftp.php?action=sig&hash=" + hash;
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to communicate with server");
        }
        InputStream in = new BufferedInputStream(connection.getInputStream());
        InputStream hex = new HexInputStream(in);
        byte[] buff = new byte[4096];
        int len = hex.read(buff);
        connection.disconnect();
        byte[] sig = Arrays.copyOf(buff, len);
        return sig;
    }

    private String getSenderNameFromCert(X509Certificate cert) throws Exception {
        String principal = cert.getSubjectDN().getName();
        String[] split = principal.split(",");
        String temp = split[2];
        String[] split2 = temp.split("=");
        String name = split2[1];
        return name;
    }

    private X509Certificate getCertificateFor(String email) throws Exception {
        String file = "tsftp.php?action=cert&user=" + getUser(email);
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to communicate with server");
        }
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        InputStream in = new BufferedInputStream(connection.getInputStream());
        X509Certificate cert = (X509Certificate) factory.generateCertificate(in);
        in.close();
        return cert;
    }

    private void doDownload(SecretKey secretKey, String hash, String fileName, File fileOnDisk) throws Exception {
        String file = "tsftp.php?action=file&hash=" + hash + "&filename=" + fileName;
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to communicate with server");
        }
        InputStream in = new BufferedInputStream(connection.getInputStream());
        InputStream hex = new HexInputStream(in);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(fileOnDisk));
        decrypt(secretKey, hex, out);
        out.flush();
        out.close();
        connection.disconnect();
    }

    private void decrypt(SecretKey secretKey, InputStream in, OutputStream out) throws Exception {
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec("4b2ff702585d9104".getBytes("ISO8859-1"));
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
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to communicate with server");
        }
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
        File file = new File(Environment.getExternalStorageDirectory(), "client.key");
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        byte[] keyBytes = PKCS8Reader.getBytes(in);
        in.close();
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    private boolean deleteFile(String hash) {
        String file = "tsftp.php?action=delete&hash=" + hash;
        HttpsURLConnection connection;
        try {
            connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
            connection.setRequestMethod("POST");
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private String getSenderName2(String hash) throws Exception {
        String file = "tsftp.php?action=sender&hash=" + hash;
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to communicate with server");
        }
        InputStream in = new BufferedInputStream(connection.getInputStream());
        InputStream hex = new HexInputStream(in);
        byte[] buff = new byte[512];
        int len = hex.read(buff);
        connection.disconnect();
        String sender = new String(buff, 0, len, "ISO8859-1");
        return sender;
    }

    public String getSenderName(String fileLink) {
        TSFTPFileDescriptor descriptor;
        try {
            descriptor = new TSFTPFileDescriptor(fileLink);
        } catch (Exception e) {
            return "Invalid file link";
        }
        try {
            return getSenderName2(descriptor.getHash());
        } catch (Exception e) {
            return "Failed to get sender";
        }
    }

    private String getUser(String email) {
        if (email.contains("@")) {
            return email.split("@")[0];
        } else {
            return email;
        }
    }

}
