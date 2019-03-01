package assignment2.responses;

public class Response302 extends HTTPResponse {

    public Response302(String redirectLocation) {
        super(302, "html");
        headers.add("Location: " + redirectLocation);
    }

}