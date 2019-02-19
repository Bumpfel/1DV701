package assignment2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import assignment2.HTTPRequest.RequestType;

public class HTTPResponse {
	private String response;
	private ArrayList<String> headers = new ArrayList<>();
	private File file;
	


	public HTTPResponse(HTTPRequest request, String dirPath) throws HTTPException {
		
		if(request.TYPE == RequestType.GET) {
			response = "HTTP/1.1 200 OK\r\n";

			file = new File(dirPath + request.PATH);
			
			// change to index.htm if no index.html is found
			if(!file.exists() && file.getName().equals("index.html"))
				file = new File(dirPath + request.PATH.substring(0, request.PATH.length() - 1));

			// 403 forbidden
			if(request.PATH.startsWith("/forbidden")) {
				response = "HTTP/1.1 403 Forbidden\r\n";
				response += "Content-Type: text/html\r\n";
				file = new File("src/assignment2/responses/403.html");
			}
			// 404 file not found
			else if(!file.exists()) {
				response = "HTTP/1.1 404 Not Found\r\n";
				response += "Content-Type: text/html\r\n";
				file = new File("src/assignment2/responses/404.html");
			}
			// 302 redirect
			else if(request.PATH.equals("/start")) {
				response = "HTTP/1.1 302 Found\r\n";
				response += "Location: /";
			}
			
		}
		else
			throw new HTTPException("Invalid request");		
	}

	public File getFile() { // TODO encapsulation?
		return file;
	}
	
	@Override
	public String toString() {
		StringBuilder strB = new StringBuilder();
		
		strB.append(response);
		for(String header : headers) {
			strB.append(header + "\r\n");
		}
		strB.append("\r\n");
		
		return new String(strB);
	}
	
	private void makeResponse200() {
		Map<String, String> contentTypes = new HashMap<>();
				
		contentTypes.put("html", "text/html; charset=UTF-8");
		contentTypes.put("htm", "text/html; charset=UTF-8");
		contentTypes.put("png", "image/png; charset=UTF-8");
		contentTypes.put("gif", "image/gif");
		contentTypes.put("jpg", "image/jpg");
		contentTypes.put("jpeg", "image/jpg");

		int dotPos = file.getName().lastIndexOf(".");
		String fileEnding = file.getName().substring(dotPos + 1);

		//TODO remove unnecessary headers
		headers.add("Date: " + new Date().toString());
		headers.add("Content-Type: " + contentTypes.get(fileEnding));
		headers.add("Content-Length: " + file.length());
		// headers.add("Connection: close"); 
		// headers.add("Server: Bumpfel WebServer 1.0");
	}



//	return	  "HTTP/1.1 200 OK\r\n"
//	+ "Transfer-Encoding: chunked\r\n"
//	+ "Date: Sat, 16 Feb 2019 20:03:22 CET\r\n"
//	+ "Server: Bum server\r\n"
//	+ "Connection: close\r\n"
//	+ "X-Powered-By: W3 Total Cache/0.8\r\n"
//	+ "Pragma: Public\r\n"
//	+ "Expires: Sat, 28 Nov 2009 05:36:25 GMTr\n"
//	+ "Etag: \"pub1259380237;gz\"\r\n"
//	+ "Cache-Control: max-age=3600, public\r\n"
//	+ "Content-Type: text/html; charset=UTF-8\r\n"
//	+ "Last-Modified: Sat, 28 Nov 2009 03:50:37 GMT\r\n"
//	+ "X-Pingback: localhost\r\n"
//	+ "Content-Encoding: gzip\r\n"
//	+ "Vary: Accept-Encoding, Cookie, User-Agent\r\n"
//	+ "\r\n";

}
