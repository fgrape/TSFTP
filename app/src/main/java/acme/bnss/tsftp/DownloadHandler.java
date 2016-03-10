package acme.bnss.tsftp;

import java.security.Key;

/**
 * Created by Erik Borgstrom on 2016-03-08.
 */
public class DownloadHandler {

    public DownloadHandler() {

    }

    public DownloadResult downloadFile(String fileLink) {
        TSFTPFileDescriptor fileDescriptor;
        try {
            fileDescriptor = new TSFTPFileDescriptor(fileLink);
        } catch (Exception e) {
            return DownloadResult.failure("Invalid file link");
        }

        return null;
    }

    private Key getSymmetricKey(String hash) {

        return null;
    }

    public void deleteFile(String fileLink) {

    }

}
