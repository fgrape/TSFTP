package acme.bnss.tsftp;

import android.util.Log;

import java.net.URL;
import java.security.cert.Certificate;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Erik Borgstrom on 2016-03-04.
 */
public class HTTPSConnectionHandler {

    public static HttpsURLConnection getConnectionToACMEWebServer(String file) throws Exception {
        URL url = new URL("https", "acme.com.fabianstrom.se", file);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
//        Certificate[] certs = connection.getServerCertificates();
//        if (certs.length == 0) {
//            throw new Exception();
//        }
//        for (Certificate cert : certs) {
//            verifyServerCert(cert);
//        }
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        return connection;
    }

    private static void verifyServerCert(Certificate cert) throws Exception {

    }

}
