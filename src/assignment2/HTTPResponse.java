package assignment2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import assignment2.RequestException;
import assignment2.WebServer;

@SuppressWarnings("serial") // suppress serializable warnings from the hashmaps

public class HTTPResponse {
	private String responseHeader;
	private String body;
	private ArrayList<String> headers = new ArrayList<>();
	final File REQUEST_FILE;
	final File UPLOADED_FILE;
	final int CODE;
	
	private final Map<String, String> CONTENT_TYPES = new HashMap<>() {{
		put("html", "text/html; charset=UTF-8");
		put("htm", "text/html; charset=UTF-8");
		put("png", "image/png");
		put("gif", "image/gif");
		put("jpg", "image/jpg");
		put("jpeg", "image/jpg");
		put("css", "text/css");
		put("txt", "text/plain");
	}};

	private final Map<Integer, String> RESPONSE_TITLES = new HashMap<>() {{
		put(100, "Continue");
		put(200, "OK");
		put(201, "Created");
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

	public HTTPResponse(int rCode, File newFile, String redirectLocation, File uploadFile) throws RequestException {
		UPLOADED_FILE = uploadFile;
		
		//TODO generally messy
		String fileEnding = new String(); // TODO don't do this here. do it in responsehandler. do need fileending here tho 
		// check file format
		if(newFile != null) {
			int dotPos = newFile.getName().lastIndexOf(".");
			fileEnding = newFile.getName().substring(dotPos + 1);
			// file format not suported
			if(CONTENT_TYPES.get(fileEnding) == null)
				throw new RequestException("415: Unsupported Media Type");
		}
		REQUEST_FILE = newFile;
		CODE = rCode;

		String title = RESPONSE_TITLES.get(CODE);
		responseHeader = "HTTP/1.1 " + CODE + " " + title + "\r\n";
		
		// decide headers
		if(CODE == 302)
			headers.add("Location: " + redirectLocation);
		else if(CODE != 200 && CODE != 201) {
			headers.add("Content-Type: text/html");
		}
		else if(CODE != 100) {
			headers.add("Content-Type: " + CONTENT_TYPES.get(fileEnding));
			// if(file != null)
			// 	headers.add("Content-Length: " + file.length());
			headers.add("Connection: close");
		}

		// create an html body instead of reading from a file if its an error response
		if(CODE == 403 || CODE == 404 || CODE == 500) {
			createBody(rCode);
		}
	}

	public String getHeaders() {
		StringBuilder strB = new StringBuilder();
		
		strB.append(responseHeader);
		for(String header : headers) {
			strB.append(header + "\r\n");
		}
		strB.append("\r\n");
		
		return new String(strB);
	}

	public void printStatus(Socket socket) {
		System.out.println("RESPONSE " + CODE + " " + RESPONSE_TITLES.get(CODE) + " " + REQUEST_FILE.getName() + " (" +  REQUEST_FILE.length() + " B) to " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());	
	}

	public void printPostPutStatus() {
		System.out.println("-Received " + UPLOADED_FILE.getName() + " (" + UPLOADED_FILE.length() + " B)");
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

	/**
	 * Writes header, and then either file requested in case of a 200 response, or a defined html body unless it's a redirect response (3xx)
	 * @param out 
	 */
	public void sendResponse(OutputStream out) throws IOException {
		out.write(getHeaders().getBytes());
		if(CODE == 200) {
			writeFile(REQUEST_FILE, out);
			return;
		}
		if(CODE == 201) { // TODO body is null if not sent through html form
			if(REQUEST_FILE == null)
				return;
			insertIntoBody("</form>", "<br><span style='color:#080; font-weight:bold'>" + UPLOADED_FILE.getName() + " uploaded successfully</span>");
		}
		// write html body if it's not a redirect or continue code (3xx or 1xx)
		if(!("" + CODE).startsWith("3") && !("" + CODE).startsWith("1")) {
			out.write(body.getBytes());
		}
	}

	private void writeFile(File file, OutputStream out) throws IOException {
		byte[] buf = new byte[WebServer.FILE_BUFFER_SIZE];
		
		FileInputStream fileIn = new FileInputStream(file);
		int bytesRead;
		while((bytesRead = fileIn.read(buf)) > 0) {
			out.write(buf, 0, bytesRead);
		}
		fileIn.close();
	}
	
	/**
	 * reads an html (text) file to string in order to alter the content. used to print successful upload message
	 */
	private void insertIntoBody(String afterTag, String insertString) throws IOException {
		// read file to String
		byte[] buf = new byte[WebServer.FILE_BUFFER_SIZE];
		StringBuilder builder = new StringBuilder();

		FileInputStream fileIn = new FileInputStream(REQUEST_FILE);
		int bytesRead;
		while((bytesRead = fileIn.read(buf)) > 0) {
			builder.append(new String(buf, 0, bytesRead));
		}
		body = builder.toString();
		fileIn.close();

		// insert string
		String[] rows = body.split(afterTag);
		rows[0] += insertString;

		// piece together body again
		body = "";
		for(String row : rows) {
			body += row;
		}
	}

}
