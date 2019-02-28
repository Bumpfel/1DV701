package assignment2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import assignment2.HTTPRequest.RequestMethod;

public class ResponseHandler {
	private final String CONTENT_PATH = "src/assignment2/content/";
	private final String UPLOAD_PATH = "src/assignment2/uploads/";

	public HTTPResponse createResponse(HTTPRequest request) throws HTTPException, ServerException {
		// GET Request
		if(request.METHOD == RequestMethod.GET) {
			String URI = request.URI;
			if(URI.endsWith("/")) { // set default file if path ends with a slash
				URI += "index.html";
			}
			File file = new File(CONTENT_PATH + URI);
			// change to index.htm if no index.html is found
			if(!file.isFile() && file.getName().equals("index.html"))
				file = new File(CONTENT_PATH + URI.substring(0, URI.length() - 1));
			// 302 found (redirect)
			if(request.URI.equals("/home"))
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
		//POST Request
		else if(request.METHOD == RequestMethod.POST) {
			try {
				File uploadFile = findUniqueFileName(request);
				
				// Write file
				FileOutputStream fos = new FileOutputStream(uploadFile);
				fos.write(request.DATA);
				fos.close();

				return new HTTPResponse(201, new File(CONTENT_PATH + request.URI), null, uploadFile);
			}
			catch(IOException e) {
				// return null;
				return new HTTPResponse(302, null, "/upload.html", null); // can happen if client clicks upload with empty file and expects a response
				// throw new ServerException("Could not receive file"); // TODO happens if one clicks the button with no file selected
			}
			catch(NullPointerException | SecurityException e) {
				throw new ServerException("Invalid POST request or internal error");
			}
		}
		else
			throw new HTTPException("Invalid or unsupported request");
	}
	
	private File findUniqueFileName(HTTPRequest request) throws NullPointerException, SecurityException {
		// extract original name from request header
		String fileName = new String();
		for(String header : request.HEADERS) {
			if(header.startsWith("Content-Disposition")) {
				String[] tmp = header.split("filename=");
				fileName = tmp[1].replaceAll("\"", "").trim();
				break;
			}
		}

		// ensure file name is unique by renaming file to "fileName (i).end"
		File file =  new File(UPLOAD_PATH + fileName);
		int i = 1;
		while(file.isFile()) {
			String[] f = fileName.split("\\.");
			String fName = f[0], fEnd = "";
			if(f.length > 1) {
				fEnd = f[1];
			}
			file = new File(UPLOAD_PATH + fName + " (" + i + ")." + fEnd);
			i ++;
		}
		return file;
	}

}