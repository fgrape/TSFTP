package acme.bnss.tsftp;

import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
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

    private long length;

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

    private void decryptFile(Key symmetricKey, InputStream in, OutputStream out) {

    }

    private void writeFileToDisk(InputStream in, String fileName) throws Exception {
        if (!isExternalStorageWritable()) {
            throw new Exception("Could not write to external storage");
        }
        File file = new File(Environment.getExternalStoragePublicDirectory("TSFTP/Downloads"), fileName);
        try (OutputStream out = new FileOutputStream(file)) {
            byte[] buff = new byte[1024];
            for (int i; (i = in.read(buff)) != -1;) {
                out.write(buff, 0, i);
            }
            Log.d("FILE", "Length =================================================== " + length);
            out.flush();
            out.close();
            in.close();
        }
    }

    private InputStream getFileInputStream(Key key, String hash, String fileName) throws Exception {
        String file = "tsftp.php?action=file&hash=" + hash + "&filename=" + fileName;
        HttpsURLConnection connection = HTTPSConnectionHandler.getConnectionToACMEWebServer(file);
        connection.setRequestMethod("GET");
        connection.connect();
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec("bnss1337bnss1337".getBytes("ISO8859-1"));
        aes.init(Cipher.DECRYPT_MODE, key, ivSpec);
        InputStream in = connection.getInputStream();
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
            Cipher rsa = Cipher.getInstance("RSA");
            rsa.init(Cipher.DECRYPT_MODE, privateKey);
            InputStream in = connection.getInputStream();
            byte[] block = new byte[512];
            int i = in.read(block);
            byte[] decrypted = rsa.doFinal(block);
            SecretKey symmetricKey = new SecretKeySpec(decrypted, 0, 24, "AES");
            Log.d("NYCKEL", "Läst nyckel längd är: " + decrypted.length);
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
        //String whatIsThis = "MIIJRAIBADANBgkqhkiG9w0BAQEFAASCCS4wggkqAgEAAoICAQDU80lLYnNfCo0xwG2tmsSOL97bq6mNYdSz7/xvxzxDsbf9RtNUgTKoGuEC4hOMFOj+nh9rZp7XZ7LCuLZK38YdW7imPDH2rxmi0/2GYGe64RqtJQYD0hkJ6gZnzsF8kYQd94Dvh4UkvJKsSeTx3ObvwJAkbLm/9d/QAk/P66nm8a2himt0DE7vCc+r65ySDMJZNDb+duyapOIFlm/Ofm1ARpZadN6kAR0eaCLCsRDHtqHpI7oBtuiuTb60b98yaBFStns4Na0QcN6SlSZzsf00PYXmUlmgqDBLVaGrW/FNJWz/1DqFwIZzQwWB2JrZ0/aCFaHeAvfpVmX3SinBedE+ihA4oollQ5Q1roUYTfnbsi+uJfuzXh2ZrmrTovjxPiiqW5wnPfVMgtvV5lVA1ozn4ZMMTusFv9WfqQYntNTYQCTu+uBpurn5VS4+PMBWKT0JAL3tmwA7FciN7xWXqPCZ5iP61JrrT5DKllUWPo8h4P6iEm2zz4EmX+GUk36GqFEvGnnL4Kj0BH2DenccNmtCsPn8cm4VeaImYpjSdFKo/oqi48Q7jmaTGIAcOgr5StE7nSwFh/C/okwK3h8bc6DSizWY9RGhthaONJCMQBc7y48GZrhfLdI6ymsD6C6xHob2RhCtijHcL+/6FWujekul2Z+sKH4GNEgKCsQa6dU/3QIDAQABAoICAQDMDcArA1DIPqUjil/OneJA2XA0kN6swpT+QzViqsh9mXtTsJ1qeU7gNLlueNIARj0pVr2GvdPxVsW9vx+2yYzervhWGpJJ7mmjVH24WQKs2W0lgrery5QAowa2TDGtuwadbIBZhkO3+xaVD03Z0TJdhOjabnDjl6h6euydsRWiXe1iQPBMSEsO1RjxhZkHfkBxGA6mcbCxUtPeRNQoS49Gqx0B2sFI6GRNsb21T9RjewbVD036vJlkBM17u0Nl2sDWwFN8fjqy6WTtkwxAisq1V0rKdBi8UM3OMmTzfevQX899qZDWf7CWHB/X14YyuuCYsD/mLHQAXLDicGdopOExyxI++GoRo8uZE0bLIPxEzUThwAzj2KDwlFQXmvsON4WlVMGzcE26zgcuJYCBOgv3kOaRe50DJrHprmhA36a93MU2xjgTBPu+yRRcvQnZmmDuFjDURdOU3yOC47Ng85pYFgQw9ZT9a68wfSl+y3vOcM1yP+0u8OfMcgbKXsb81Ky8XGqNsWnf5cWi0qbw+uUdVWBHlxYi1QZBR04X3z3D4EGz58w18pjRmE1n6XUuPOLVbC0JtWYv/FqJZfTOLp+nyVQziwl9/VpdlmQ9ZXKyoE3PT1AeL4EHWG21LAlK5FuRmtVF4yZNvnBQ1ywQqrjxhGx8HlLFKyNELSuMmgKrZQKCAQEA9uFlPFJ3GmV7i+k61CVkjEVZGH3Vt9e50UFccWdKMI/INmK+PGYPNH5DR1HtVz8xYYxsvlLWi3IxAS/u6ltLvJXRcBGb4rtu1d0Fz6+R3/uOOnMSQyQANauL+Y/MY02MhWCD+jybbsG4atG/DvJPClA5E5AW860yrZwUNVBKJHp+umS65vR3stmvusGFIYJzZJ3n1ttmd9wmX/LpA1jieKww9i5HSxBfPnzMoxGFs0CQEqCr6VW718w0YxPIBuRgmLJm2TrEWBMQGbI0bTBr73lyJcaxsma05aBCoij43cXkDNQTSgq6anVWOJ34liQe+hi5+9cofUjxkZALqovZWwKCAQEA3NEIljP0SdSKB2k2hyRk6We1fJLZFOsKhPCTAugbKm0/x+rLS2V92rgrCeNRkBHFAkcQIetrVpgTGak6d3ytcy3q0AC/lG96LP3lxPLE7edCvhx492tIn15qOhSAXMQJVjI03h52OiOF6JGg/ba77zGYgGYR6TEXIDz2sKSDdKGIAlwgm+HRr7DUKM+BhpxXhUb/OqOY63h2vG7XfK9Tb2Wb210DwSebffQQXSL+Bown+OCAHLVVztWqWLHg+KNbTndSSJhUxLxh5YYwjVgMcdIIemzNW5gLBrVQcPEgJNg3utQ/2TRGZU1P91iJxk3FhfqLjKMzBR7rZuOGcyvZJwKCAQEA5GsOm41BHW8frAeSoo4mRZI0KPDkOECkb/OlcFc80Ul22LrrgD59evgHj4lBqyOQsMyYIE/MqSByLsMGrlbh8A49xQ7FyDXbCoyAv/OVaEL7CSFGmFBK0V1zGOfvF0G87fqqoXBJ72PVGSAPQej3MHehF3SNh8+LwA1UtS8gKjgb4KEqPF2cEiJO8N/0KVjlIwbmZmiAmg1ahBPfe3VVtIi8lEmlNNJlDQ+y1n1z0rG9mz6FUFI+hh8tqS24OIg8x4rolbxFxad9kgjLcOsIMn2PG1yHNKTKYlqwzBBUkfxyT1w4rJzYdX8y0xd11JR/BK60BUrA74n519AKuzTBWQKCAQEAilF2K4BH2aeE/rdtLKIjhAEuseXajTvdzwBTVlLGEYF1nmj5BzV/7G3Ip9z4zmITIiYZeOXaytdfv4c91HMIw0lT+Q8+8mASC/TO5Nudd9iQ+j0jmPhe+VVO/SqybWlu38t7SleSf0CZNOFkEwlQF7jdajCFr6jMb6lAbc3fekNkyvWih4Kgsoj1nMq+u+9a82VRow2vUHCZD9fuq2+3t+PSCUrueyOwRumHBpBQxxEsSL14AoOkSHeWyRBDd/v7yd78b/TbxJCsUfYIm8E3qmSCYw2cZW+MGJui09x24h1+a01g4VHZdsNDABE53DoDLLWQjHv+DYscscp8AdDJqQKCAQA22yhtmZhAwPd+5665SqiUV//zgsbK8Pg4butErLGnDsg9EiIeJjere2IVeitKQg7IkY0O/oqs+Ci6Hm1FnZwJHXQkaVmioqwHLhNZVp45QT/GyNCLoeBr56foWNpCtNhMNXbpkoOt+fu0c2nuBxQwJnVnBmAhZ1Qfp/uwwJACFnSXnjULPxfozLLeZkFV3sE2HxTYqY1+NiUghltI7idXKsd8rKAp5A/75DTdP4nkHolt07gXNrB4VpQb9ZB1hiaBJjnhrJtiCbPNuW9/0ZI2yUQusDFPHWEKIPPShPZGjA91QVsFEWHjt3l2mGZyq3BqB1dQEt5q+hw0fUPjM9X1";
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
