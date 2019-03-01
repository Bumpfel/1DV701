package assignment2.responses;

import java.io.File;

public class Response201 extends HTTPResponse {
    private File file;

    public Response201(File f) {
        super(201, "html");
        file = f;
    }
}