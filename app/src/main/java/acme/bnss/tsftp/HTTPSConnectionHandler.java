package acme.bnss.tsftp;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Erik Borgstrom on 2016-03-04.
 */
public class HTTPSConnectionHandler {

    private static final String IP = "172.31.112.116";

    public static HttpsURLConnection getConnectionToACMEWebServer(String file) {
        URL url;
        try {
            url = new URL("https", IP, file);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            return connection;
        } catch (Exception e) {
            return null;
        }
    }

}
