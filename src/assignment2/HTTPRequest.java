package assignment2;

import java.net.Socket;
import java.util.ArrayList;

public class HTTPRequest {
	public final ArrayList<String> HEADERS;
	public final String URI;
	public final RequestMethod METHOD;
	public final byte[] DATA;
	// public final boolean CONTAINS_FORM_DATA;

	public enum RequestMethod { GET, POST, PUT };
	
	HTTPRequest(ArrayList<String> newHeaders, String uri, RequestMethod method, byte[] data) {//, boolean containsFormData) {
		HEADERS = newHeaders;
		URI = uri;
		METHOD = method;
		DATA = data;
		// CONTAINS_FORM_DATA = false; //containsFormData; //TODO temp assign
	}

	public void printStatus(Socket socket) {
		System.out.println(METHOD + " REQUEST " + URI + " from " +  socket.getInetAddress().toString().substring(1));
	}

	public String extractValue(String attribute) { //throws ArrayIndexOutOfBoundsException - 500
		for(String header : HEADERS) {
			if(header.startsWith(attribute)) {
				return header.split(attribute)[1].replaceAll("\"", "").trim();
			}
		}
		return null;
	}
}
