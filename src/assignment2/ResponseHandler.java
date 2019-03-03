package assignment2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import assignment2.HTTPRequest.RequestMethod;

public class ResponseHandler {

	public HTTPResponse getResponse(HTTPRequest request, Socket socket) throws RequestException, ServerException {
		try {
			// GET Request
			if(request.METHOD == RequestMethod.GET)
				return createGetResponse(request);
			// POST/PUT Request
			else if(request.METHOD == RequestMethod.POST || request.METHOD == RequestMethod.PUT)
				return createPostPutResponse(request, socket);
			// Method not allowed
			else
				return new HTTPResponse(405, null, null, null);
		}
		catch(RequestException e) {
			// extract response code from exception msg
			String[] requestCode = e.getMessage().split(":");
			int code = Integer.valueOf(requestCode[0]);

			return new HTTPResponse(code, null, null, null);
		}
		catch(IOException | NullPointerException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
			return new HTTPResponse(500, null, null, null);
		}
	}

	private HTTPResponse createGetResponse(HTTPRequest request) throws RequestException {
		String URI = request.URI;
		if(URI.endsWith("/")) // set default file if path ends with a slash
			URI += "index.html";
		File requestedFile = new File(WebServer.CONTENT_PATH + URI);

		// change to index.htm if no index.html is found
		if(!requestedFile.isFile() && requestedFile.getName().equals("index.html"))
			requestedFile = new File(WebServer.CONTENT_PATH + URI.substring(0, URI.length() - 1));
		// 302 found (redirect)
		if(request.URI.equals("/home/"))
			return new HTTPResponse(302, null, "/", null);
		// 403 forbidden
		else if(request.URI.startsWith("/forbidden/"))
			return new HTTPResponse(403, null, null, null);
		// 404 file not found
		if(!requestedFile.isFile())
			return new HTTPResponse(404, null, null, null);
		// 200 ok
		else
			return new HTTPResponse(200, requestedFile, null, null);
	}

	private HTTPResponse createPostPutResponse(HTTPRequest request, Socket socket) throws IOException, NullPointerException, RequestException {
		File uploadFile;
		byte [] data = request.DATA;

		// get unique name if it's a POST request; use original name otherwise (if PUT)
		if(request.METHOD == RequestMethod.POST)
			uploadFile = new File(WebServer.UPLOAD_PATH + findUniqueUploadFileName(request));
		else
			uploadFile = new File(WebServer.UPLOAD_PATH + getUploadFileName(request));
		
		// checks if the request expects a HTTP 100 response before sending the data
		if(request.getHeader("Expect: 100-continue") != null) {
			//... sends one if that is the case
			new HTTPResponse(100, null, null, null).sendResponse(socket.getOutputStream());
			//... and receives the data
			data = new RequestHandler(socket.getInputStream()).readData();
		}
		
		// sets requested file if file was sent via html form
		File requestedFile = null;
		if(request.getHeader("Content-Type: multipart/form-data") != null)
			requestedFile = new File(WebServer.CONTENT_PATH + request.URI);
		
		// Write data to file on server
		if(data.length > 0) {
			FileOutputStream fos = new FileOutputStream(uploadFile);
			fos.write(data);
			fos.close();
		}
		else {
			return new HTTPResponse(204, null, null, null); // happens if client submits the html form with no file selected
		}
		return new HTTPResponse(201, requestedFile, null, uploadFile);
	}

	/**
	 * For POST requests
	 */
	private String findUniqueUploadFileName(HTTPRequest request) throws NullPointerException, SecurityException {
		String fileName = getUploadFileName(request);
		
		// ensures file name is unique
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

	/**
	 * For PUT requests
	 */
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
		String[] tmp = request.URI.split("/");
		fileName = tmp[tmp.length - 1];
		return fileName;
	}

}