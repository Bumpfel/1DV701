package assignment2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import assignment2.HTTPRequest.RequestType;

public class HTTPHeader {
	private String response;
	private ArrayList<String> headers = new ArrayList<>();
	
	public HTTPHeader(HTTPRequest request, File file) {
		
		if(request.TYPE == RequestType.GET) {
			response = "HTTP 1.1/200 OK\r\n";
			
			Map<String, String> contentTypes = new HashMap<>();
			
			contentTypes.put("html", "text/html");
			contentTypes.put("htm", "text/html");
			contentTypes.put("png", "image/png");
			contentTypes.put("gif", "image/gif");
			contentTypes.put("jpg", "image/jpg");
			contentTypes.put("jpeg", "image/jpg");
			

			int dotPos = file.getName().lastIndexOf(".");
			String fileEnding = file.getName().substring(dotPos + 1);

			headers.add("Content-Type: " + contentTypes.get(fileEnding));
	
		}
		
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
