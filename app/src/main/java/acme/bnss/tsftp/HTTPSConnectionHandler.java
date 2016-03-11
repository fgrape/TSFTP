package acme.bnss.tsftp;

import android.util.Log;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Erik Borgstrom on 2016-03-04.
 */
public class HTTPSConnectionHandler {

    public static HttpsURLConnection getConnectionToACMEWebServer(String file) {
        try {
            URL url = new URL("https", "acme.com.fabianstrom.se", file);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            return connection;
        } catch (Exception e) {
            return null;
        }
    }

}
