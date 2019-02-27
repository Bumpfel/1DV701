package assignment2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial") // suppress serializable warnings from the hashmaps

public class HTTPResponse {
	private String responseHeader;
	private String body;
	private ArrayList<String> headers = new ArrayList<>();
	private File file;
	public final File UPLOADED_FILE;
	public final int CODE;
	
	private final Map<String, String> CONTENT_TYPES = new HashMap<>() {{
		put("html", "text/html; charset=UTF-8");
		put("htm", "text/html; charset=UTF-8");
		put("png", "image/png; charset=UTF-8");
		put("gif", "image/gif");
		put("jpg", "image/jpg");
		put("jpeg", "image/jpg");
		put("css", "text/css");
	}};

	private final Map<Integer, String> RESPONSE_TITLES = new HashMap<>() {{
		put(200, "OK");
		put(302, "Found");
		put(403, "Forbidden");
		put(404, "File Not Found");
		put(500, "Internal Server Error");
	}};
	
	private final Map<Integer, String> RESPONSE_INFO = new HashMap<>() {{
		put(403, "Access to this resource is forbidden");
		put(404, "The page you're looking for does not exist");
		put(500, "The server encountered an internal error");
	}};


	public HTTPResponse(int rCode, File newFile, String redirectLocation, File uploadFile) throws HTTPException {
		UPLOADED_FILE = uploadFile;
		
		String fileEnding = new String();
		// check file format
		if(newFile != null) {
			int dotPos = newFile.getName().lastIndexOf(".");
			fileEnding = newFile.getName().substring(dotPos + 1);
			// file format not suported
			if(CONTENT_TYPES.get(fileEnding) == null)
				rCode = 500;
			else
				file = newFile;
		}
		CODE = rCode;

		String title = RESPONSE_TITLES.get(CODE);
		responseHeader = "HTTP/1.1 " + CODE + " " + title + "\r\n";
		
		// decide headers
		if(CODE == 302) {
			headers.add("Location: " + redirectLocation);
		}
		else if(CODE != 200)
			headers.add("Content-Type: text/html\r\n");
		else {
			headers.add("Content-Type: " + CONTENT_TYPES.get(fileEnding));
			headers.add("Content-Length: " + file.length());
			headers.add("Connection: close");
		}

		// make body if response is not 200 or 302
		if(CODE != 200 && CODE != 302)
			createBody(rCode);
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
		String info = RESPONSE_INFO.get(code);
		String title = RESPONSE_TITLES.get(code);
		StringBuilder temp = new StringBuilder();

		temp.append("<html>");
		temp.append("<head><link rel='stylesheet' href='/style.css'><title>" + code + " " + title + "</title></head>");
		temp.append("<body class='r" + code + "'>");
		temp.append("<div class='error'><h1>" + code + "</h1><h2>" + title + "</h2>" + info + "<br><br><a href='/'>Go to index</a></div>");
		temp.append("</body>");
		temp.append("</html>");

		body = temp.toString();
	}

}
