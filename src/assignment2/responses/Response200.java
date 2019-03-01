package assignment2.responses;

import java.io.File;

public class Response200 extends HTTPResponse {
    private File file; 

    public Response200(File f) {
        super(200, "something");

        file = f;
    
		//TODO generally somewhat messy
		String fileEnding = new String(); // TODO don't do this here. do it in responsehandler. do need fileending here tho 
		// check file format
		if(newFile != null) {
			int dotPos = newFile.getName().lastIndexOf(".");
			fileEnding = newFile.getName().substring(dotPos + 1);
			// file format not suported
			if(CONTENT_TYPES.get(fileEnding) == null)
				throw new RequestException("415: Unsupported Media Type");
		}
		file = newFile;
		CODE = rCode;

		String title = RESPONSE_TITLES.get(CODE);
		responseHeader = "HTTP/1.1 " + CODE + " " + title + "\r\n";
		
		if(CODE == 100) { //TODO Test response
			// TODO make method to extract file ending
			System.out.println("upload file name: " + uploadFile.getName()); 
			System.out.println(uploadFile.length() + " B");
			// headers.add("Content-Type: " + uploadFile);
			// headers.add("Content-Length: " + );
		} 
		else {
			headers.add("Content-Type: " + CONTENT_TYPES.get(fileEnding));
			// headers.add("Content-Length: " + file.length());
			headers.add("Connection: close");
		}

		// create an html body instead of reading from a file if its an error response
		if(CODE == 403 || CODE == 404 || CODE == 500)
			createBody(rCode);
    }

    public File getFile() { // TODO bad encapsulation
        return file;
    }
}