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
			// Receive request and print request status on the server
			RequestHandler reqHandler = new RequestHandler(socket.getInputStream());
			HTTPRequest request = reqHandler.readRequest();
			request.printStatus(socket);

			// Create appropriate response and send it to client
			ResponseHandler respHandler = new ResponseHandler();
			HTTPResponse response = respHandler.createResponse(request);
			response.sendResponse(socket.getOutputStream());
			
			boolean containsExpectHeader = false;
			ArrayList<String> newHeaders = new ArrayList<>();
			//Make a new request, removing the expect header
			//TODO maybe have a separate method for this
			for(String header : request.HEADERS) {
				if(header.startsWith("Expect")) {
					containsExpectHeader = true;
					break;
				}
				newHeaders.add(header);
			}
			// Read data following an Expect: 100 Continue request header
			if(containsExpectHeader) {
				byte[] data = reqHandler.readData();

				request = new HTTPRequest(newHeaders, null, request.METHOD, data);

				response = respHandler.createResponse(request);
				response.sendResponse(socket.getOutputStream());
			}

			// print info about received file if it was a post or put request
			if((request.METHOD == RequestMethod.POST || request.METHOD == RequestMethod.PUT) && response.UPLOADED_FILE != null)
				response.printPostPutStatus();

			// print response status if a file was sent to client
			if(response.FILE != null)
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