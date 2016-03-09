package acme.bnss.tsftp;

/**
 * Created by Erik Borgstrom on 2016-03-09.
 */
public class UploadResult {

    private boolean successfull;
    private String message;
    private String fileID;

    public UploadResult() {

    }

    public UploadResult(String fileID) {
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

    public static UploadResult failure(String message) {
        UploadResult result = new UploadResult();
        result.successfull = false;
        result.message = message;
        return result;
    }

}
