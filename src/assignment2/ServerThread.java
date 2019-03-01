package assignment2;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import assignment2.HTTPRequest.RequestMethod;

public class ServerThread extends Thread {

	private Socket socket;
	
	public ServerThread(Socket socket) {
		this.socket = socket;
	}

	public void run() {
        try {
			HTTPRequest request = new RequestHandler(socket.getInputStream()).readRequest();
			request.printStatus(socket);

			HTTPResponse response = new ResponseHandler().createResponse(request);
			response.sendResponse(socket.getOutputStream());
			
			// print info about received file if it was a post or put request
			if((request.METHOD == RequestMethod.POST || request.METHOD == RequestMethod.PUT) && response.UPLOADED_FILE != null)
				response.printPostPutStatus();
			
			// print response status if a file was sent
			if(response.getFile() != null)
				response.printStatus(socket);
			
			socket.close();
		}
        catch(SocketTimeoutException e) {
			System.err.println("408: Request timed out");
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
}