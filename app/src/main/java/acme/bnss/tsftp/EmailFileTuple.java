package acme.bnss.tsftp;

import java.io.File;

public class EmailFileTuple {
    private String email;
    private File file;

    public EmailFileTuple(String email, File file) {
        this.email = email;
        this.file = file;
    }

    public String getEmail() {
        return email;
    }
    public File getFile() {
        return file;
    }
}
