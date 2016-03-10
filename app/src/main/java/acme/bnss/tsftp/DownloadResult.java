package acme.bnss.tsftp;

/**
 * Created by Erik Borgstrom on 2016-03-09.
 */
public class DownloadResult {

    private boolean successfull;
    private String message;
    private String fileID;

    public DownloadResult() {

    }

    public DownloadResult(String fileID) {
        this.fileID = fileID;
        successfull = true;
    }

    public boolean wasSuccessfull() {
        return successfull;
    }

    public String getMessage() {
        return message;
    }

    public String getFileID() {
        return fileID;
    }

    public static DownloadResult failure(String message) {
        DownloadResult result = new DownloadResult();
        result.successfull = false;
        result.message = message;
        return result;
    }


}
