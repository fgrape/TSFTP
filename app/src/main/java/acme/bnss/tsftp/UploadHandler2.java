package acme.bnss.tsftp;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.net.ssl.HttpsURLConnection;

public class UploadHandler2 {

    public UploadResult uploadFile(String email, File file) {
        X509Certificate clientCert;
        try {
            clientCert = getClientCert();
        } catch (Exception e) {
            Log.d("EXCEPTION", e.getMessage());
            return UploadResult.failure("You are not authenticated 1");
        }
        PrivateKey clientPrivateKey;
        try {
            clientPrivateKey = getClientPrivateKey();
        } catch (Exception e) {
            Log.d("EXCEPTION", e.getMessage());
            return UploadResult.failure("You are not authenticated 2");
        }
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
        InputStream fileIn1;
        InputStream fileIn2;
        try {
            fileIn1 = new BufferedInputStream(new FileInputStream(file));
            fileIn2 = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            Log.d("EXCEPTION", e.getMessage());
            return UploadResult.failure("Could not read file: " + e.getMessage());
        }
        try {
            TSFTPFileDescriptor fileDescriptor = doUpload(file.getName(), fileIn1, recipientCert, secretKey, clientCert, clientPrivateKey, fileIn2);
            return new UploadResult(fileDescriptor);
        } catch (Exception e) {
            Log.d("EXCEPTION", e.getMessage());
            return UploadResult.failure("Failed to upload file: " + e.getMessage());
        }
    }

    private TSFTPFileDescriptor doUpload(String fileName, InputStream fileIn1, X509Certificate recipientCert, SecretKey secretKey, X509Certificate clientCert, PrivateKey clientPrivateKey, InputStream fileIn2) throws Exception {
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

        printer.append("&sender=");
        printer.flush();
        OutputStream hex2 = new HexOutputStream(out);
        writeSender(clientCert, hex2);
        hex2.flush();

        printer.append("&signature=");
        printer.flush();
        OutputStream hex3 = new HexOutputStream(out);
        writeSignature(clientPrivateKey, fileIn2, hex3);
        hex3.flush();

        printer.append("&fileData=");
        printer.flush();
        OutputStream hex4 = new HexOutputStream(out);
        encrypt(secretKey, fileIn1, hex4);

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to communicate with server");
        }
        TSFTPFileDescriptor fileDescriptor = getFileDescriptor(connection.getInputStream());
        connection.disconnect();
        return fileDescriptor;
    }

    private X509Certificate getClientCert() throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        File certFile = new File(Environment.getExternalStorageDirectory(), "client.crt");
        InputStream certIn = new BufferedInputStream(new FileInputStream(certFile));
        X509Certificate cert = (X509Certificate) factory.generateCertificate(certIn);
        return cert;
    }

    private String getSenderNameFromCert(X509Certificate cert) throws Exception {
        String principal = cert.getSubjectDN().getName();
        String[] split = principal.split(",");
        String temp = split[2];
        String[] split2 = temp.split("=");
        String name = split2[1];
        return name;
    }

    private void writeSender(X509Certificate cert, OutputStream out) throws Exception {
        String name = getSenderNameFromCert(cert);
        byte[] bytes = name.getBytes("ISO8859-1");
        out.write(bytes);
    }

    private void writeSignature(PrivateKey privateKey, InputStream in, OutputStream out) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        byte[] buff = new byte[512];
        for (int i; (i = in.read(buff)) != -1; ) {
            signature.update(buff, 0, i);
        }
        in.close();
        byte[] sig = signature.sign();
        out.write(sig);
    }

    private void encrypt(SecretKey secretKey, InputStream in, OutputStream out) throws Exception {
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec("4b2ff702585d9104".getBytes("ISO8859-1"));
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

    private void verifyRecipientCert(X509Certificate cert) throws Exception {
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
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(in);
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
