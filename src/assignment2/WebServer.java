package assignment2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

public class WebServer {
	public final int MYPORT = 9000;

	public static void main(String[] args) {
		WebServer tcpServer = new WebServer();
		tcpServer.startServer();
	}

	public void startServer() {
		try {
			try(ServerSocket serverSocket = new ServerSocket(MYPORT)) {
				System.out.print("Server started on port " + serverSocket.getLocalPort() + ". ");
				System.out.println();

				while (true) {
					Socket clientSocket = serverSocket.accept();

					ClientThread clientThread = new ClientThread(clientSocket);
					clientThread.start();
				}
			}
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}
}

class ClientThread extends Thread {

	private BufferedReader in;
	private DataOutputStream out;
    private Socket socket;
    
    private final String DIR_PATH = "src/assignment2/content/";
    private final String RESPONSE_PATH = "src/assignment2/responses";

	public ClientThread(Socket socket) {
		this.socket = socket;

		try {
			// socket.setSoTimeout(10000); // TODO uncomment?
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));			
			out = new DataOutputStream(socket.getOutputStream());
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

	public void run() {
		try {
			HTTPRequest request = HTTPRequest.parseRequest(readRequest());
			System.out.println(request.TYPE + " request from " + socket.getInetAddress().toString().substring(1));

			HTTPResponse response = new HTTPResponse(request, DIR_PATH, RESPONSE_PATH);
			out.write(response.toString().getBytes());
			
			writeFile(response.getFile());
			
			System.out.println("Sent " + response.getFile().length() + " byte(s) to " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());

			socket.close();
		}
		catch(HTTPException | IOException e) {
			System.err.println(e.getMessage());
			try {
				socket.close();
			}
			catch(IOException e2) {
			}
		}
	}
	
	private String readRequest() throws IOException {
		String line, requestString = new String();
		while(true) {
			line = in.readLine();
			requestString += line + "\n";
			if (line.isEmpty() || line.equals("\r\n"))
				break;
		}
		return requestString;
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
			// System.err.println(e.getMessage());
		}
	}
	
}