package assignment2;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import assignment2.HTTPRequest.RequestMethod;

public class ServerThread extends Thread {

	private Socket socket;
	
	public ServerThread(Socket socket) {
		this.socket = socket;
	}

	public void run() {
        try {
			RequestHandler reqHandler = new RequestHandler(socket);
			HTTPRequest request = reqHandler.readRequest();

		    // System.out.println(request.METHOD + " REQUEST " + request.URI + " from " + socket.getInetAddress().toString().substring(1)); // TODO make a toString()
			System.out.println(request.toString());
			if(request != null) {
				HTTPResponse response = new ResponseHandler().createResponse(request);
				response.writeResponse(socket.getOutputStream());
				
				// print info about received file if it was a post request
				if(request.METHOD == RequestMethod.POST && response.UPLOADED_FILE != null) {
					System.out.println("Received " + response.UPLOADED_FILE.getName() + " (" + response.UPLOADED_FILE.length() + " B)");
				}
				
				//a file was sent
				if(response.getFile() != null)
				System.out.println("Sent " + response.getFile().getName() + " (" +  response.getFile().length() + " B) to " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());	
            }
			socket.close();
		}
        catch(SocketTimeoutException e) {
			System.err.println("Request timed out");
		}
		catch(Exception e) {
			e.printStackTrace(); //TODO debug
            System.err.println(e.getMessage());
			try {
				socket.close();
			}
			catch(IOException e2) {
			}
		}
	}	
}