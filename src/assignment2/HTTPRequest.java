package assignment2;

public class HTTPRequest {
	public final String[] HEADERS;
	public final String URI;
	public final RequestMethod METHOD;
	public final byte[] DATA;
	
	public enum RequestMethod { GET, POST };
	
	HTTPRequest(String[] newHeaders, String uri, RequestMethod method, byte[] data) {
		HEADERS = newHeaders;
		URI = uri;
		METHOD = method;
		DATA = data;
	}

}
