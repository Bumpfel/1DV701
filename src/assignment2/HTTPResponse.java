package assignment2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import assignment2.HTTPRequest.RequestMethod;

public class HTTPResponse {
	private String response;
	private ArrayList<String> headers = new ArrayList<>();
	private File file;
	private Map<String, String> contentTypes = Map.of(
		"html", "text/html; charset=UTF-8",
		"htm", "text/html; charset=UTF-8",
		"png", "image/png; charset=UTF-8",
		"gif", "image/gif",
		"jpg", "image/jpg",
		"jpeg", "image/jpg",
		"css", "text/css"
	);

	public HTTPResponse(HTTPRequest request, String dirPath, String respPath) throws HTTPException {

		response = "HTTP/1.1 200 OK\r\n";

		if(request.METHOD == RequestMethod.GET) {
			file = new File(dirPath + request.PATH);

			// change to index.htm if no index.html is found
			if(!file.exists() && file.getName().equals("index.html"))
				file = new File(dirPath + request.PATH.substring(0, request.PATH.length() - 1));

			// 404 file not found
			if(!file.exists()) {
				response = "HTTP/1.1 404 Not Found\r\n";
				response += "Content-Type: text/html\r\n";
				file = new File(respPath + "/404.html");
				// response += makeHTMLResponse("404", "File Not Found", "The page you're looking for does not exist");
			}
			// 403 forbidden
			else if(request.PATH.startsWith("/forbidden")) {
				response = "HTTP/1.1 403 Forbidden\r\n";
				response += "Content-Type: text/html\r\n";
				file = new File(respPath + "/403.html");
			}
			// 302 redirect
			else if(request.PATH.equals("/start")) {
				response = "HTTP/1.1 302 Found\r\n";
				response += "Location: /";
			}
			else {
				try {
					makeResponse200();
				}
				catch(HTTPException e) {
					response = "HTTP/1.1 500 Internal Server Error\r\n";
					response += "Content-Type: text/html\r\n";
					file = new File(respPath + "/500.html");
				}
			}
			
		}
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
			// System.out.print("---is done");
			// System.out.print(request.ORG_STRING); //TODO debug
		}
		else
			throw new HTTPException("Invalid or unimplemented request");
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
	
	private void makeResponse200() throws HTTPException {
		int dotPos = file.getName().lastIndexOf(".");

		String fileEnding = file.getName().substring(dotPos + 1);

		//TODO remove unnecessary headers
		headers.add("Date: " + new Date().toString());
		if(contentTypes.get(fileEnding) == null)
			throw new HTTPException();
		headers.add("Content-Type: " + contentTypes.get(fileEnding));
		headers.add("Content-Length: " + file.length());
		// headers.add("Connection: keep-alive"); //TODO check if this is needed to close the thread
		// headers.add("Server: Bumpfel WebServer 1.0");
	}

	private String makeHTMLResponse(String code, String title, String extraInfo) {
		String response = "<html><head><link rel='stylesheet' href='/style.css'><title>" + code + " " + title + "</title></head>";
		response += "<body class='" + code + "'><div class='error'>";
		response += "<h1>" + code + "</h1><h2>" + title + "</h2>" + extraInfo + "<br><br><a href='/'>Go to index</a>";
		response += "</div></body></html>";
		return response;
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
