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
	final File REQUESTED_FILE;
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
		put(204, "No Content");
		put(302, "Found");
		put(403, "Forbidden");
		put(404, "File Not Found");
		put(405, "Method Not Allowed");
		put(415, "Unsupported Media Type");
		put(500, "Internal Server Error");
	}};
	
	private final Map<Integer, String> RESPONSE_INFO = new HashMap<>() {{
		put(403, "Access to this resource is forbidden");
		put(404, "The page you're looking for does not exist");
		put(405, "");
		put(415, "");
		put(500, "The server encountered an internal error");
	}};

	public HTTPResponse(int rCode, File newFile, String redirectLocation, File uploadFile) throws RequestException {
		UPLOADED_FILE = uploadFile;
		
		String fileEnding = new String();
		// check file format
		if(newFile != null) {
			int dotPos = newFile.getName().lastIndexOf(".");
			fileEnding = newFile.getName().substring(dotPos + 1);
			// file format not supported
			if(CONTENT_TYPES.get(fileEnding) == null)
				throw new RequestException("415: Unsupported Media Type");
		}
		REQUESTED_FILE = newFile;
		CODE = rCode;

		String title = RESPONSE_TITLES.get(CODE);
		responseHeader = "HTTP/1.1 " + CODE + " " + title + "\r\n";
		
		// decide headers
		if(CODE == 100)
			return;
		else if(CODE == 302)
			headers.add("Location: " + redirectLocation);
		else if(REQUESTED_FILE == null) {
			headers.add("Content-Type: text/html");
			headers.add("Connection: close");
			createBody(rCode);
		}
		else {
			headers.add("Content-Type: " + CONTENT_TYPES.get(fileEnding));
			headers.add("Content-Length: " + REQUESTED_FILE.length());
			headers.add("Connection: close");
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

	public void printStatus() {
		System.out.print("RESPONSE " + CODE + " " + RESPONSE_TITLES.get(CODE));
	}
	public void printRequestFileStatus(Socket socket) {
		System.out.println(" " + REQUESTED_FILE.getName() + " (" +  REQUESTED_FILE.length() + " B) to " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());
	}
	
	public void printReceiveFileStatus() {
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
	 * Writes header and content
	 * @param out 
	 */
	public void sendResponse(OutputStream out) throws IOException {
		//write headers
		out.write(getHeaders().getBytes());

		// write content
		if(CODE == 200 || CODE == 201)
			writeFile(REQUESTED_FILE, out);
		else if(body != null)
			out.write(body.getBytes());
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

}
