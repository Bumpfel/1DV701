package assignment2;

import java.util.Arrays;

public class HTTPRequest {
	public final String[] HEADERS;
	public final String PATH;
	public final RequestMethod METHOD;
	
	public enum RequestMethod { GET, POST };
	
	public HTTPRequest(String[] newHeaders, String newPath, RequestMethod newMethod) {
		HEADERS = newHeaders;
		PATH = newPath;
		METHOD = newMethod;
	}
	
	public static HTTPRequest parseRequest(String requestStr) throws HTTPException {
		String[] lines = requestStr.split("\n");
		
		System.out.print(requestStr); // TODO debug

		if(lines.length > 0) {
			String[] firstLine = lines[0].split(" ");
			if(firstLine.length != 3)
				throw new HTTPException("Malformed header");
			
			String[] headers = Arrays.copyOfRange(lines, 1, lines.length);
			
			String path = firstLine[1];
			if(path.endsWith("/")) // set default file if path ends with a slash
				path += "index.html";

			return new HTTPRequest(headers, path, RequestMethod.valueOf(firstLine[0]));
		}
		throw new HTTPException("Empty request");
	}

}
