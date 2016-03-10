package acme.bnss.tsftp;

/**
 * Created by Erik Borgstrom on 2016-03-09.
 */
public class UploadResult {

    private boolean successful;
    private String message;
    private TSFTPFileDescriptor fileDescriptor;

    private UploadResult() {

    }

    public UploadResult(TSFTPFileDescriptor fileDescriptor) {
        this.fileDescriptor = fileDescriptor;
        successful = true;
    }

    public boolean wasSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public TSFTPFileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    public static UploadResult failure(String message) {
        UploadResult result = new UploadResult();
        result.successful = false;
        result.message = message;
        return result;
    }

}
