package assignment2;

import java.util.Arrays;

public class HTTPRequest {
	public final String[] HEADERS;
	public final String PATH;
	public final RequestMethod METHOD;
	public final String ORG_STRING; //TODO debug?
	
	public enum RequestMethod { GET, POST }; // TODO remove POST if not implemented
	
	HTTPRequest(String requestString, String[] newHeaders, String path, RequestMethod method) {
		HEADERS = newHeaders;
		PATH = path;
		METHOD = method;
		ORG_STRING = requestString;

	}

}
