package assignment2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import assignment2.HTTPRequest.RequestMethod;

/**
 * This class contains methods to read and parse a request
 */
public class ResponseHandler {
    
	public HTTPResponse createResponse(HTTPRequest request, String contentPath) throws HTTPException {
	
		File file = new File(contentPath + request.PATH);
		if(request.METHOD == RequestMethod.GET) {
			// change to index.htm if no index.html is found
			if(!file.exists() && file.getName().equals("index.html"))
				file = new File(contentPath + request.PATH.substring(0, request.PATH.length() - 1));
			// 302 redirect
			if(request.PATH.equals("/home"))
				return new HTTPResponse(302, null);
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
			// System.out.println(request.ORG_STRING); // TODO debug
			for(String header : request.HEADERS) {
				if(header.startsWith("Content-Type: multipart/form-data")) {
					String[] temp = header.split("=");
					String boundary = temp[1];
					String[] content = request.ORG_STRING.split(boundary);

					String[] temp2 = content[2].split("\n");
					
					String binaryContent = new String();
					for(int i = 3; i < temp2.length - 1; i ++) {
						binaryContent += temp2[i];
					}
					
					// Write file
					try {
						// System.out.println(binaryContent);

						
						// StringBuilder smt = new StringBuilder();
						// for (int i = 0; i < binaryContent.length(); i++) {
						// 	char ch = binaryContent.charAt(i);
				
						// 	if (ch == '%') {
						// 		System.out.println(
							// 		ch = (char) Integer.parseInt("" + binaryContent.charAt(i + 1) + binaryContent.charAt(i + 2), 16);
							// 		i += 2;
							// 	}
							
							// 	smt.append(ch);
							// }
							
						// byte[] encodedBinaryContent = Base64.getMimeDecoder().decode(binaryContent.toString());
						// fos.write(encodedBinaryContent);
						
						FileOutputStream fos = new FileOutputStream(new File("src/assignment2/upload/somefile.png"));
						fos.write(binaryContent.getBytes());
						// fos.flush();
						fos.close();


						// return new HTTPResponse(200, new File("src/assignment2/content/upload.html"));
						// break;
						//TODO should return a response
						
					}
					catch(IOException e) {
						System.err.println("fehl fihl"); // TODO temp msg
						System.err.println(e.getMessage());
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}
		else
			throw new HTTPException("Invalid or unimplemented request"); //TODO ought to be a 405?
    }
}