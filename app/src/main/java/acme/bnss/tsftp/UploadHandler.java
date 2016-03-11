package acme.bnss.tsftp;

import android.app.ProgressDialog;
import android.util.Base64;
import android.util.Base64InputStream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.net.ssl.HttpsURLConnection;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

/**
 * Created by Erik Borgstrom on 2016-03-08.
 */
public class UploadHandler {

    private String message;

    public UploadHandler() {

    }

    public UploadResult uploadFile(String email, File file, ProgressDialog progressDialog) {
        // progressDialog.setMax(100);
        X509Certificate cert;
        try {
             cert = getCertificateFor(email);
        } catch (Exception e) {
            return UploadResult.failure("Failed to acquire recipient certificate: " + e.getMessage());
        }
        // progressDialog.setProgress(10);
        Key symmetricKey;
        try {
            symmetricKey = createSymmetricKey();
        } catch (Exception e) {
            return UploadResult.failure("");
        }
        // progressDialog.setProgress(20);
        InputStream fileIn;
        try {
            fileIn = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            return UploadResult.failure("Could not read file: " + e.getMessage());
        }
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer("tsftp.php?action=upload");
        try {
            connection.setRequestMethod("POST");
            String boundary = Long.toHexString(System.currentTimeMillis());
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            String crlf = "\r\n";

            connection.setDoOutput(true);
            OutputStream out = connection.getOutputStream();
            PrintStream printer = new PrintStream(out);

            printer.append("--" + boundary).append(crlf);
            printer.append("Content-Disposition: form-data; name=\"encryptedFile\"; fileName=\"" + file.getName() + " \"").append(crlf);
            printer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append(crlf);
            printer.append("Content-Transfer-Encoding: binary").append(crlf);
            printer.append(crlf);
            printer.flush();
            encryptFile(symmetricKey, fileIn, out);
            out.flush();
            printer.append(crlf);

            printer.append("--" + boundary).append(crlf);
            printer.append("Content-Disposition: form-data; name=\"encryptionKey\"; fileName=\"encryptionKey").append(crlf);
            printer.append("Content-Type: binary").append(crlf);
            printer.append("Content-Transfer-Encoding: binary").append(crlf);
            printer.append(crlf);
            printer.flush();
            encryptKey(cert.getPublicKey(), symmetricKey, out);
            out.flush();
            printer.append(crlf);

            printer.append("--" + boundary + "--").append(crlf);
            printer.flush();

            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return UploadResult.failure("Failed to communicate with server");
            }
            TSFTPFileDescriptor fileDescriptor = getFileDescriptor(connection.getInputStream());
            return new UploadResult(fileDescriptor);
        } catch (Exception e) {
            return UploadResult.failure("Failed to upload file to server: " + e.getMessage());
        } finally {
            connection.disconnect();
        }
    }

    private TSFTPFileDescriptor getFileDescriptor(InputStream in) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = reader.readLine();
            return new TSFTPFileDescriptor(line);
        }
    }

    private void encryptFile(Key key, InputStream in, OutputStream out) throws  IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        Cipher aes = Cipher.getInstance("AES");
        aes.init(Cipher.ENCRYPT_MODE, key);
        CipherOutputStream out2 = new CipherOutputStream(out, aes);
        byte[] buff = new byte[1024];
        for (int i; (i = in.read(buff)) != -1;) {
            out2.write(buff, 0 ,i);
        }
    }

    private void encryptKey(PublicKey publicKey, Key symmetricKey, OutputStream out) throws  IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        Cipher aes = Cipher.getInstance("RSA");
        aes.init(Cipher.ENCRYPT_MODE, publicKey);
        CipherOutputStream out2 = new CipherOutputStream(out, aes);
        out2.write(symmetricKey.getEncoded());
    }

    private SecretKey createSymmetricKey() throws NoSuchAlgorithmException {
        return KeyGenerator.getInstance("AES").generateKey();
    }

    private X509Certificate getCertificateFor(String email) throws IOException, ProtocolException, CertificateException {
        String file = "tsftp.php?action=cert&user=" + getUser(email);
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            message = "Failed to communicate with server";
            return null;
        }
        InputStream in = connection.getInputStream();
        Charset charset = Charset.forName("ISO8859-1");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in,
                charset));
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
        InputStream in2 = new Base64InputStream(new ByteArrayInputStream(buff.toByteArray()), Base64.DEFAULT);
        X509Certificate cert = X509Certificate.getInstance(in2);
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

}
