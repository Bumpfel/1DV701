package assignment2;

public class HTTPRequest {
	public final String[] HEADERS;
	public final String URI;
	public final RequestMethod METHOD;
	public final byte[] DATA;
	private String source;
	
	public enum RequestMethod { GET, POST };
	
	HTTPRequest(String[] newHeaders, String uri, RequestMethod method, byte[] data, String sourceIP) {
		HEADERS = newHeaders;
		URI = uri;
		METHOD = method;
		DATA = data;
		source = sourceIP;
	}


	@Override
	public String toString() {
		return METHOD + " REQUEST " + URI + " from " + source;
	}

}
