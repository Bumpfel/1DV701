package assignment2;

import java.net.Socket;
import java.util.ArrayList;

public class HTTPRequest {
	public final ArrayList<String> HEADERS;
	public final String URI;
	public final RequestMethod METHOD;
	public final byte[] DATA;
	
	public enum RequestMethod { GET, POST };
	
	HTTPRequest(ArrayList<String> newHeaders, String uri, RequestMethod method, byte[] data) {
		HEADERS = newHeaders;
		URI = uri;
		METHOD = method;
		DATA = data;
	}

	public void printStatus(Socket socket) {
		System.out.println(METHOD + " REQUEST " + URI + " from " +  socket.getInetAddress().toString().substring(1));
	}

}
