package acme.bnss.tsftp;

import java.nio.charset.MalformedInputException;

/**
 * Created by Erik Borgstrom on 2016-03-10.
 */
public class TSFTPFileDescriptor {

    private String fileLink;
    private String server;
    private String fileName;
    private String hash;

    public TSFTPFileDescriptor(String fileLink) throws Exception {
        this.fileLink = fileLink;
        String[] split = fileLink.split("/");
        try {
            server = split[2];
            hash = split[3];
            fileName = split[4];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new Exception();
        }
    }

    public TSFTPFileDescriptor(String server, String fileName, String hash) {
        this.server = server;
        this.fileName = fileName;
        this.hash = hash;
    }

    public String getFileLink() {
        return fileLink;
    }

    public String getServer() {
        return server;
    }

    public String getHash() {
        return hash;
    }

    public String getFileName() {
        return fileName;
    }

}
