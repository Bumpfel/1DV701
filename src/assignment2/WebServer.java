package assignment2;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import assignment2.HTTPRequest.RequestMethod;

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
	private InputStream in2; //TODO remove one of these. This is used for readInData()
	private DataOutputStream out;
    private Socket socket;
    
    private final String DIR_PATH = "src/assignment2/content/";

	public ClientThread(Socket socket) {
		this.socket = socket;

		try {
			socket.setSoTimeout(10000);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));			
			out = new DataOutputStream(socket.getOutputStream());

			in2 = socket.getInputStream();
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

	public void run() {
		try {
			HTTPRequest request = HTTPRequest.parseRequest(readInput());
			System.out.println(request.METHOD + " request from " + socket.getInetAddress().toString().substring(1) + " for " + request.PATH.substring(1));

			HTTPResponse response = HTTPResponse.createResponse(request, DIR_PATH);
			writeResponse(response);
			
			if(response.getFile() != null)
				System.out.println("Sent " + response.getFile().getName() + " (" +  response.getFile().length() / 1000.000 + " kB) to " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());

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

	private String readInData() throws IOException {
		byte[] buffer = new byte[1500];
		StringBuilder builder = new StringBuilder();		
		do
		{
			int read = in2.read(buffer, 0, buffer.length);
			String str = new String(buffer, "ISO-8859-15").substring(0, read);
			builder.append(str);
		} while (in2.available() > 0 );
		
		return builder.toString().trim();
	}
	
	// private String readInput_() throws IOException {
	// 	String requestString = new String();
	// 	int bytesReceived;
	// 	char[] buf = new char[1500];
	// 	while(true) {
	// 		bytesReceived = in.read(buf);
	// 		requestString += new String(buf) + "\n";
	// 		if (bytesReceived <= 0 | requestString.endsWith("\r\n"))
	// 			break;
	// 		System.out.print(requestString);
	// 	}
	// 	return requestString;
	// }
	
	private String readInput() throws IOException {
		String line, requestString = new String();
		while(true) {
			line = in.readLine();
			requestString += line + "\n";
			if (line.isEmpty() || line.equals("\r\n"))
				break;
		}
		return requestString;
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