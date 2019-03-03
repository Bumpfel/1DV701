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
			// Receive request and print request status on the server
			RequestHandler reqHandler = new RequestHandler(socket.getInputStream());
			HTTPRequest request = reqHandler.readRequest();
			request.printStatus(socket);
			
			// Get response from handler and send it to client
			ResponseHandler respHandler = new ResponseHandler();
			HTTPResponse response = respHandler.getResponse(request, socket);
			response.sendResponse(socket.getOutputStream());

			// print info about received file if available
			if((request.METHOD == RequestMethod.POST || request.METHOD == RequestMethod.PUT) && response.UPLOADED_FILE != null)
				response.printReceiveFileStatus();

			// print response status if requested file was sent to client
			response.printStatus();
			if(response.REQUESTED_FILE != null)
				response.printRequestFileStatus(socket);
			else
				System.out.println();
			
			socket.close();
		}
        catch(SocketTimeoutException e) { // happens if client attempts to submit the html form with no selected file
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