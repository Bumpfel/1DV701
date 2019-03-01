package assignment2.responses;

import java.io.File;

public class Response100 extends HTTPResponse {

    public Response100(File f, String contentLength, String contentType) {
        super(100);
        headers.add("Content-Type: " + contentType);
        headers.add("Content-Length: " + contentLength);
    }
}