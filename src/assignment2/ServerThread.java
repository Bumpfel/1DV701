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

	public void run() { //TODO better 500
        try {
			RequestHandler reqHandler = new RequestHandler(socket.getInputStream());
			HTTPRequest request = reqHandler.readRequest();

			request.printStatus(socket);

			HTTPResponse response = new ResponseHandler().createResponse(request);
			response.sendResponse(socket.getOutputStream());
			
			// print info about received file if it was a post request
			if(request.METHOD == RequestMethod.POST && response.UPLOADED_FILE != null)
				response.printPOSTStatus();
			
			//a file was sent
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