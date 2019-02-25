package assignment2;

import java.util.Arrays;

public class HTTPRequest {
	public final String[] HEADERS;
	public final String PATH;
	public final RequestMethod METHOD;
	public final String ORG_STRING;
	
	public enum RequestMethod { GET, POST }; // TODO remove POST if not implemented
	
	HTTPRequest(String requestString, String[] newHeaders, String newPath, RequestMethod newMethod) {
		HEADERS = newHeaders;
		PATH = newPath;
		METHOD = newMethod;
		ORG_STRING = requestString;
	}
	
	public static HTTPRequest parseRequest(String requestStr) throws HTTPException, IllegalArgumentException {
		String[] lines = requestStr.split("\n");
		
		if(requestStr.length() > 0) {
			String[] firstLine = lines[0].split(" ");
			if(firstLine.length != 3)
				throw new HTTPException("Malformed header");
			if(!firstLine[2].contains("HTTP/1.1")) {
				throw new HTTPException("HTTP version not supported");
			}
			
			String[] headers = Arrays.copyOfRange(lines, 1, lines.length);
			
			String path = firstLine[1];
			if(path.endsWith("/")) // set default file if path ends with a slash
				path += "index.html";
			try {
				return new HTTPRequest(requestStr, headers, path, RequestMethod.valueOf(firstLine[0]));
			}
			catch(IllegalArgumentException e) {
				System.err.println("Method not supported");
			}
		}
		throw new HTTPException("Empty request");
	}

}
