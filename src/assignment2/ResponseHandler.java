package assignment2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import assignment2.HTTPRequest.RequestMethod;

/**
 * This class contains methods to read and parse a request
 */
public class ResponseHandler {
    
	public HTTPResponse createResponse(HTTPRequest request, String contentPath) throws HTTPException {
	
		File file = new File(contentPath + request.PATH);
		if(request.METHOD == RequestMethod.GET) {
			if(request.PATH.equals("/home"))
				file = new File(contentPath + "index.html");
			// change to index.htm if no index.html is found
			if(!file.exists() && file.getName().equals("index.html"))
				file = new File(contentPath + request.PATH.substring(0, request.PATH.length() - 1));
			// 302 redirect
			if(request.PATH.equals("/home")) {
				return new HTTPResponse(302, file);
			}
			// 404 file not found
			if(!file.exists())
				return new HTTPResponse(404, null);
			// 403 forbidden
			else if(request.PATH.startsWith("/forbidden"))
				return new HTTPResponse(403, null);
			// 200 ok
			else
				return new HTTPResponse(200, file);	
		}
		//File  upload (not working)
		//TODO remove or fix
		else if(request.METHOD == RequestMethod.POST) {
			for(String header : request.HEADERS) {
				if(header.startsWith("Content-Type: multipart/form-data")) {
					// String[] temp = header.split("=");
					// String boundary = temp[1];
					String[] content = request.ORG_STRING.split("------WebKitFormBoundary");
					
					System.out.println(content[1]);

					// System.out.println(content.length);
					// System.out.println(content[2]);

					String binaryContent = new String();
					// for(int i = 3; i < temp2.length - 1; i ++) {
					// 	binaryContent += temp2[i];
					// }

					// Write file
					try {
						FileOutputStream fos = new FileOutputStream(new File("src/assignment2/upload/somefile.png"));
						fos.write(binaryContent.getBytes());
						fos.flush();
						fos.close();
						break;
					}
					catch(IOException e) {
						System.err.println("fehl fihl");
						System.err.println(e.getMessage());
					}
				}
			}
			return null;
		}
		else
			throw new HTTPException("Invalid or unimplemented request"); //TODO ought to be a 405?
    }
}