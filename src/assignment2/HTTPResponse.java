package assignment2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import assignment2.HTTPRequest.RequestMethod;

public class HTTPResponse {
	private String responseHeader;
	private String body;
	private ArrayList<String> headers = new ArrayList<>();
	private File file;
	private Map<String, String> CONTENT_TYPES = new HashMap<>() {{
		put("html", "text/html; charset=UTF-8");
		put("htm", "text/html; charset=UTF-8");
		put("png", "image/png; charset=UTF-8");
		put("gif", "image/gif");
		put("jpg", "image/jpg");
		put("jpeg", "image/jpg");
		put("css", "text/css");
	}};
	private Map<Integer, String> responseTitles = new HashMap<>() {{
		put(404, "File Not Found");
		put(403, "Forbidden");
		put(500, "Internal Server Error");
	}};
	private Map<Integer, String> responseInfo = new HashMap<>() {{
		put(404, "The page you're looking for does not exist");
		put(403, "Access to this resource is forbidden");
		put(500, "The server encountered an internal error");
	}};

	public HTTPResponse(int rCode, File newFile) throws HTTPException {
		String fileEnding = new String();
		if(newFile != null) {
			int dotPos = newFile.getName().lastIndexOf(".");
			fileEnding = newFile.getName().substring(dotPos + 1);
			// file format not suported
			if(CONTENT_TYPES.get(fileEnding) == null)
				rCode = 500;
			else
				file = newFile;
		}

		String title = responseTitles.get(rCode);
		responseHeader = "HTTP/1.1 " + rCode + " " + title + "\r\n";
		
		// decide content type
		if(rCode != 200)
			headers.add("Content-Type: text/html\r\n");
		else {
			headers.add("Content-Type: " + CONTENT_TYPES.get(fileEnding));
		}

		// make body if response is not 200
		if(rCode != 200 && rCode != 302)
			createBody(rCode);
	}

	public static HTTPResponse createResponse(HTTPRequest request, String dirPath) throws HTTPException {
	
		File file = new File(dirPath + request.PATH);
		if(request.METHOD == RequestMethod.GET) {
			// change to index.htm if no index.html is found
			if(!file.exists() && file.getName().equals("index.html"))
				file = new File(dirPath + request.PATH.substring(0, request.PATH.length() - 1));
			// 404 file not found
			if(!file.exists())
				return new HTTPResponse(404, null);
			// 403 forbidden
			else if(request.PATH.startsWith("/forbidden"))
				return new HTTPResponse(403, null);
			// 302 redirect
			else if(request.PATH.equals("/start"))
				return new HTTPResponse(302, null);
			// 200 ok
			else
				return new HTTPResponse(200, file);			
		}
		//File  upload (not working)
		//TODO remove or fix
		else if(request.METHOD == RequestMethod.POST) {
			// System.out.print(request.ORG_STRING);
			boolean foundStart = false;
			int offset = 0;
			String imageContent = new String();
			String[] fileInfo = new String[4];
			String fileName = new String();
			for(int i = 0; i < request.HEADERS.length; i ++) {
				offset += request.HEADERS[i].length();
				if(request.HEADERS[i].startsWith("Content-Disposition: form-data")) {
					// System.out.print("-- " + request.HEADERS[i + 2] + " --");
					foundStart = true;
					fileInfo = request.HEADERS[i].split(" ");

					fileName = fileInfo[3].substring(10, fileInfo[3].length() - 2);
					continue;
				}
				if(foundStart) {
					// if(request.HEADERS[i].equalsIgnoreCase("Content-Type: image/png")) {
						try {
							// ERIC.png (2 267 byte) 2449?
							imageContent = request.ORG_STRING.substring(offset);
							
							int pos = imageContent.charAt('-');
							imageContent = imageContent.substring(0, 2267);

							System.out.print(imageContent);
							
							System.out.println();
							System.out.println((double) imageContent.length()/1000 + "kB");
							// FileOutputStream fos = new FileOutputStream(new File("somefile.txt"));
							FileOutputStream fos = new FileOutputStream(new File("src/assignment2/upload/" + fileName));
							fos.write(imageContent.getBytes());
							fos.flush();
							fos.close();
							break;
						}
						catch(IOException e ) { // TODO temp
							System.out.print("fel pa filhantering");
							System.out.print(e.getMessage());
						}
					}
					
					// if(request.HEADERS[i].startsWith("------WebKitFormBoundary")) {
						// 	i += 4;
						// 	startSend = true;
						// }
				}
			return null;
			// System.out.print("---is done");
			// System.out.print(request.ORG_STRING); //TODO debug
		}
		else
			throw new HTTPException("Invalid or unimplemented request"); //TODO ought to be a 405?
	}

	public String getHeader() {
		StringBuilder strB = new StringBuilder();
		
		strB.append(responseHeader);
		for(String header : headers) {
			strB.append(header + "\r\n");
		}
		strB.append("\r\n");
		
		return new String(strB);
	}

	public File getFile() {
		return file;
	}
	
	public String getBody() {
		return body;
	}

	private void createBody(int code) {
		String info = responseInfo.get(code);
		String title = responseTitles.get(code);

		body = "<html><head><link rel='stylesheet' href='/style.css'><title>" + code + " " + title + "</title></head>";
		body += "<body class='r" + code + "'><div class='error'>";
		body += "<h1>" + code + "</h1><h2>" + title + "</h2>" + info + "<br><br><a href='/'>Go to index</a>";
		body += "</div></body></html>";
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
