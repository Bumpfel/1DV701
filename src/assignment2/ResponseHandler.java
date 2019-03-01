package assignment2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import assignment2.HTTPRequest.RequestMethod;
import assignment2.responses.*;

public class ResponseHandler {

	public HTTPResponse createResponse(HTTPRequest request) throws RequestException, ServerException {
		// GET Request
		if(request.METHOD == RequestMethod.GET) {
			String URI = request.URI;
			if(URI.endsWith("/")) // set default file if path ends with a slash
				URI += "index.html";
			File file = new File(WebServer.CONTENT_PATH + URI);

			// change to index.htm if no index.html is found
			if(!file.isFile() && file.getName().equals("index.html"))
				file = new File(WebServer.CONTENT_PATH + URI.substring(0, URI.length() - 1));
			// 302 found (redirect)
			if(request.URI.equals("/home/"))
				return new Response302("/");
				// return new HTTPResponse(302, null, "/", null);
			// 403 forbidden
			else if(request.URI.startsWith("/forbidden/"))
				return new Response403();
			// 404 file not found
			if(!file.isFile())
				return new Response404();
			// 200 ok
			else
				return new Response200(file);
		}
		// POST Request
		else if(request.METHOD == RequestMethod.POST || request.METHOD == RequestMethod.PUT) {
			try {
				File uploadFile;

				// get unique name if it's a POST request; use original name otherwise (if PUT)
				if(request.METHOD == RequestMethod.POST)
					uploadFile = new File(WebServer.UPLOAD_PATH + findUniqueUploadFileName(request));
				else
					uploadFile = new File(WebServer.UPLOAD_PATH + getUploadFileName(request));
				
				System.out.println(request.HEADERS.toString()); //TODO debug print
				
				//TODO if not submitted from form. parse that info so I don't have to check over n over

				if(request.extractValue("Expect:").equals("100-continue")) {
					String len = request.extractValue("Content-Length:");
					String type = request.extractValue("Content-Type:");
					System.out.println(type);
					return new Response100(uploadFile, len, type);
				}

				//TODO debug printing
				System.out.println(request.HEADERS.toString());

				// Write file
				FileOutputStream fos = new FileOutputStream(uploadFile); //TODO could maybe do this in HTTPResponse.writeResponse? pass uploadFile in file argument instead and skip the uploadFile parameter
				fos.write(request.DATA);
				fos.close();

				// return new HTTPResponse(201, new File(WebServer.CONTENT_PATH + request.URI), null, uploadFile);
				return new Response201(uploadFile);
			}
			catch(FileNotFoundException e) {
				System.out.println(e.getMessage());
				// throw new RequestException("400: Bad Request");//TODO correct?
				return new Response500(); // might also be a client error
			}
			catch(IOException e) { //TODO not sure this is the correct response
				return new Response302("/upload.html"); // can happen if client clicks upload with empty file and expects a response
			}
			catch(NullPointerException e) {
				return new Response500(); // will only happen if file string is null
			}
		}
		else
			throw new RequestException("405: Method Not Allowed");
	}
	
	private String findUniqueUploadFileName(HTTPRequest request) throws NullPointerException, SecurityException {
		String fileName = getUploadFileName(request);
		
		// ensure file name is unique by renaming file to "fileName (n).end"
		File file =  new File(WebServer.UPLOAD_PATH + fileName);
		int n = 1;
		while(file.isFile()) {
			String[] f = fileName.split("\\.");
			String fName = f[0], fEnd = "";
			if(f.length > 1) {
				fEnd = f[1];
			}
			file = new File(WebServer.UPLOAD_PATH + fName + " (" + n + ")." + fEnd);
			n ++;
		}
		return file.getName();
	}

	private String getUploadFileName(HTTPRequest request) {
		String fileName = new String();
		// get name from html form
		for(String header : request.HEADERS) {
			if(header.startsWith("Content-Disposition")) {
				String[] tmp = header.split("filename=");
				fileName = tmp[1].replaceAll("\"", "").trim();
				break;
			}
		}
		// elsewhere
		if(fileName.isEmpty()) {
			String[] tmp = request.URI.split("/");
			fileName = tmp[tmp.length - 1];
		}
		return fileName;

	}

}