package acme.bnss.tsftp;

import android.app.ProgressDialog;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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
        HttpsURLConnection connection = null;
        try {
            connection = HTTPSConnectionHandler.getConnectionToACMEWebServer("tsftp.php?action=upload");
            connection.setRequestMethod("POST");
        //    String boundary = Long.toHexString(System.currentTimeMillis());
        //    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            connection.setDoOutput(true);
            OutputStream out = new BufferedOutputStream(connection.getOutputStream());
            PrintStream printer = new PrintStream(out);

            Charset ISO8859_1 = Charset.forName("ISO8859-1");
            ByteArrayOutputStream buff = null;
            String temp;
            OutputStream buffOut;

            printer.append("fileData=");
            buff = new ByteArrayOutputStream();
            buffOut = new Base64OutputStream(buff, Base64.DEFAULT);
            encryptFile(symmetricKey, fileIn, buffOut, file.length());
            fileIn.close();
            temp = URLEncoder.encode(buff.toString("ISO8859-1"), "ISO8859-1");
            out.write(temp.getBytes(ISO8859_1));

            printer.append("&fileName=");
            printer.append(file.getName());

            printer.append("&encryptionKey=");
            buff = new ByteArrayOutputStream();
            buffOut = new Base64OutputStream(buff, Base64.DEFAULT);
            encryptKey(cert.getPublicKey(), symmetricKey, buffOut);
            temp = URLEncoder.encode(buff.toString("ISO8859-1"), "ISO8859-1");
            out.write(temp.getBytes(ISO8859_1));

            printer.flush();
            out.flush();

//            printer.append("--" + boundary).append(crlf);
//            printer.append("Content-Disposition: form-data; name=\"encryptedFile\"; fileName=\"" + file.getName() + " \"").append(crlf);
//            printer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append(crlf);
//            printer.append("Content-Transfer-Encoding: binary").append(crlf);
//            printer.append(crlf);
//            printer.flush();
//            encryptFile(symmetricKey, fileIn, out);
//            out.flush();
//            printer.append(crlf);
//
//            printer.append("--" + boundary).append(crlf);
//            printer.append("Content-Disposition: form-data; name=\"encryptionKey\"; fileName=\"encryptionKey").append(crlf);
//            printer.append("Content-Type: binary").append(crlf);
//            printer.append("Content-Transfer-Encoding: binary").append(crlf);
//            printer.append(crlf);
//            printer.flush();
//            encryptKey(cert.getPublicKey(), symmetricKey, out);
//            out.flush();
//            printer.append(crlf);
//
//            printer.append("--" + boundary + "--").append(crlf);
//            printer.flush();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return UploadResult.failure("Failed to communicate with server");
            }
            TSFTPFileDescriptor fileDescriptor = getFileDescriptor(connection.getInputStream());
            return new UploadResult(fileDescriptor);
        } catch (Exception e) {
            return UploadResult.failure("Failed to upload file to server: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private TSFTPFileDescriptor getFileDescriptor(InputStream in) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = reader.readLine();
            return new TSFTPFileDescriptor(line);
        }
    }

    private void encryptFile(Key key, InputStream in, OutputStream out, long length) throws  Exception {
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec("bnss1337bnss1337".getBytes("ISO8859-1"));
        aes.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        Log.d("FILE", "Length ============================================== " + length);
        OutputStream out2 = new CipherOutputStream(out, aes);
        byte[] buff = new byte[1024];
        for (int i; (i = in.read(buff)) != -1;) {
            out2.write(buff);
        }
        out2.flush();
        out2.close();
        out.flush();
        in.close();
    }

    private void encryptKey(PublicKey publicKey, Key symmetricKey, OutputStream out) throws  Exception {
        out.write(symmetricKey.getEncoded());
        Cipher rsa = Cipher.getInstance("RSA");
        rsa.init(Cipher.WRAP_MODE, publicKey);
        byte[] key = symmetricKey.getEncoded();
        // byte[] block = new byte[512];
        // System.arraycopy(key, 0, block, 0, key.length);
        Log.d("NYCKEL", "Ursprungsnyckel är: " + new String(key, "ISO8859-1") + "    Längd = " + key.length);
        byte[] encrypted = rsa.doFinal(key);
        Log.d("BLOCK", "Wrote this many bytes for symmetric key block: " + encrypted.length);
        out.write(encrypted);
        out.flush();
    }

    private SecretKey createSymmetricKey() throws NoSuchAlgorithmException {
        //return  new SecretKeySpec(new byte[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}, 0, 16, "AES");
        SecretKey key = KeyGenerator.getInstance("AES").generateKey();
        Log.d("KEY", "Symmetric key byte length is: " + key.getEncoded().length);
        return key;
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
