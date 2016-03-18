package acme.bnss.tsftp;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class HTTPSConnectionHandler {

    public static HttpsURLConnection getConnectionToACMEWebServer(String file) throws Exception {
        URL url = new URL("https", "acme.com.fabianstrom.se", file);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        return connection;
    }

}
