package assignment2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import assignment2.HTTPRequest.RequestMethod;

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
				return new HTTPResponse(302, null, "/", null);
			// 403 forbidden
			else if(request.URI.startsWith("/forbidden/"))
				return new HTTPResponse(403, null, null, null);
			// 404 file not found
			if(!file.isFile())
				return new HTTPResponse(404, null, null, null);
			// 200 ok
			else
				return new HTTPResponse(200, file, null, null);
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
				
				//if not uploaded from html form //TODO reorganize
				String expect = request.extractValue("Expect:");
				if(expect != null && expect.trim().equals("100-continue")) {
					return new HTTPResponse(100, null, null, uploadFile);
				}

				// Write file
				if(request.DATA.length > 0) {
					FileOutputStream fos = new FileOutputStream(uploadFile); //TODO could maybe do this in HTTPResponse.writeResponse? pass uploadFile in file argument instead and skip the uploadFile parameter
					fos.write(request.DATA);
					fos.close();
				}
				else {
					return new HTTPResponse(204, null, null, null); // can happen if client clicks upload with empty file and expects a response
				}

				File file = null;
				if(request.extractHeader("Content-Type: multipart/form-data") != null)
					file = new File(WebServer.CONTENT_PATH + request.URI);

				// return new HTTPResponse(201, new File(WebServer.CONTENT_PATH + request.URI), null, uploadFile);
				return new HTTPResponse(201, file, null, uploadFile);
			}
			catch(IOException e) { //FileNotFoundException e) { //TODO remove extra
				e.printStackTrace(); // TODO temp printstacktrace
				// throw new RequestException("400: Bad Request");//TODO correct?
				return new HTTPResponse(500, null, null, null); // might also be a client error
			}
			catch(NullPointerException | ArrayIndexOutOfBoundsException e) {
				e.printStackTrace(); // TODO temp printstacktrace
				return new HTTPResponse(500, null, null, null);
			}
		}
		else
			throw new RequestException("405: Method Not Allowed");
	}
	
	private String findUniqueUploadFileName(HTTPRequest request) throws NullPointerException, SecurityException {
		String fileName = getUploadFileName(request);
		
		// ensure file name is unique
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
				return fileName;
			}
		}
		//  get name from URI if not sent from html form
		// if(fileName.isEmpty()) {
			String[] tmp = request.URI.split("/");
			fileName = tmp[tmp.length - 1];
		// }
		return fileName;

	}

}