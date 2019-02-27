package assignment2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import assignment2.HTTPRequest.RequestMethod;

public class ServerThread extends Thread {

	private OutputStream out;
	private Socket socket;
	
	public ServerThread(Socket socket) {
		this.socket = socket;

		try {
			out = socket.getOutputStream();
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

	public void run() {
        try {
            RequestHandler reqHandler = new RequestHandler(socket.getInputStream());

			ArrayList<String> headers = reqHandler.readHeaders();
			byte[] binData = reqHandler.readBytes();

			HTTPRequest request = reqHandler.parseRequest(headers, binData);
		    System.out.println(request.METHOD + " REQUEST " + request.URI + " from " + socket.getInetAddress().toString().substring(1));
            if(request != null) {

				HTTPResponse response = new ResponseHandler().createResponse(request);
                if(response != null)
				writeResponse(response);
				
				// print info about uploaded file if it was a post request
				if(request.METHOD == RequestMethod.POST) {
					System.out.println("Received " + response.UPLOADED_FILE.getName() + " (" + response.UPLOADED_FILE.length() + " B)");
				}
				
				//a file was sent
				if(response.getFile() != null)
		            System.out.println("Sent " + response.getFile().getName() + " (" +  response.getFile().length() + " B) to " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());
                
            }
			socket.close();
		}
        catch(SocketTimeoutException e) {
			System.err.println("Request timed out"); //TODO silence?
		}
		catch(Exception e) {
            System.err.println(e.getMessage());
			try {
				socket.close();
			}
			catch(IOException e2) {
			}
		}
	}
    
	/**
	 * Writes header, and then either file if requested and response is 200, or a defined html body
	 * @param response
	 */
	private void writeResponse(HTTPResponse response) throws IOException {
		out.write(response.getHeader().getBytes());
		File file = response.getFile();
		if(file != null) {
			writeFile(file);
		}
		else if(response.CODE != 302) {
			out.write(response.getBody().getBytes());
		}
	}
	
	private void writeFile(File file) throws IOException {
		byte[] buf = new byte[1024];
		
		FileInputStream fileIn = new FileInputStream(file);
		int bytesRead;
		while((bytesRead = fileIn.read(buf)) > 0) {
			out.write(buf, 0, bytesRead);
		}
		fileIn.close();
	}
	
}