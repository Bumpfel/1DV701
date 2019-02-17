package assignment2;

import java.util.Arrays;

public class HTTPRequest {
	public final String[] HEADERS;
	public final String PATH;
	public final RequestType TYPE;
	
	public enum RequestType { GET }
	
	public HTTPRequest(String[] newHeaders, String newPath, RequestType newType) {
		HEADERS = newHeaders;
		PATH = newPath;
		TYPE = newType;
	}
	
	public static HTTPRequest parseRequest(String requestStr) throws HTTPException {
		String[] lines = requestStr.split("\n");
		
		if(lines.length > 0) {
			String[] firstLine = lines[0].split(" ");
			if(firstLine.length != 3)
				throw new HTTPException("Malformed header");
			
			String[] headers = Arrays.copyOfRange(lines, 1, lines.length);
			
			String path = firstLine[1];
			if(path.equals("/"))
				path = "index.html";
			
			return new HTTPRequest(headers, path, RequestType.valueOf(firstLine[0]));
		}
		throw new HTTPException("Empty request");
	}
	
}
