package acme.bnss.tsftp;

public class DownloadResult {

    private boolean successful;
    private String message;
    private String fileName;

    private DownloadResult() {

    }

    public DownloadResult(String fileName) {
        this.fileName = fileName;
        successful = true;
    }

    public boolean wasSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public String getFileName() {
        return fileName;
    }

    public static DownloadResult failure(String message) {
        DownloadResult result = new DownloadResult();
        result.successful = false;
        result.message = message;
        return result;
    }


}
