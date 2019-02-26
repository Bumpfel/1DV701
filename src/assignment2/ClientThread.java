package assignment2;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientThread extends Thread {

	// private BufferedReader bufIn;
    // private InputStream inStream; //TODO remove so there's only one of these inputstream objects. This is used for readInData()
    // private Scanner scanIn;
	private DataOutputStream out;
    private Socket socket;
    
	private final String CONTENT_PATH = "src/assignment2/content/";
	private final int SOCKET_TIME_OUT = 10000;

	public ClientThread(Socket socket) {
		this.socket = socket;

		try {
			socket.setSoTimeout(SOCKET_TIME_OUT);
			// bufIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));	//TODO clean up		
			out = new DataOutputStream(socket.getOutputStream());

            // inStream = socket.getInputStream();
            // scanIn = new Scanner(socket.getInputStream());
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

	public void run() {
		try {
			RequestHandler reqHandler = new RequestHandler(socket.getInputStream());
            String inData = reqHandler.readChars();// TODO use one
            // String inData = reqHandler.readLines();
            // String inData = reqHandler.readScanner();
            // System.out.print(inData); // TODO remove debug
						
            HTTPRequest request = reqHandler.parseRequest(inData);
            // System.out.println(request.METHOD + " request from " + socket.getInetAddress().toString().substring(1) + " for " + request.PATH.substring(1));
            if(request != null) {

				HTTPResponse response = new ResponseHandler().createResponse(request, CONTENT_PATH);
                if(response != null)
                    writeResponse(response);
                
                // if(response.getFile() != null)
                // System.out.println("Sent " + response.getFile().getName() + " (" +  response.getFile().length() / 1000.000 + " kB) to " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());
                
            }
            socket.close();
        }
        catch(SocketTimeoutException e) {
        }
		catch(Exception e) {
			e.printStackTrace();
            // System.err.println(e.getMessage());
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
	private void writeResponse(HTTPResponse response) {
		try {
			out.write(response.getHeader().getBytes());
			File file = response.getFile();
			if(file != null) {
				writeFile(file);
			}
			else {
				out.write(response.getBody().getBytes());
			}
		}
		catch(IOException e) {
			System.out.print(e.getMessage());
		}

	}
	
	private void writeFile(File file) {
		byte[] buf = new byte[1500];
		
		try(FileInputStream fileIn = new FileInputStream(file)) {
			int bytesRead;
			while((bytesRead = fileIn.read(buf)) > 0) {
				out.write(buf, 0, bytesRead);
			}
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
}