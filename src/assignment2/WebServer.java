package assignment2;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

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
//				System.out.print("Buffer size is " + serverBufferSize + " byte(s). ");
				System.out.println();

				while (true) {
					Socket clientSocket = serverSocket.accept();

					ClientThread clientThread = new ClientThread(clientSocket);
					clientThread.start();
				}
			}
		}
		catch(NumberFormatException e) {
			System.out.println("Invalid argument format");
		}
		catch(IllegalArgumentException | IOException e) {
			System.out.println(e.getMessage());
		}
	}
}

class ClientThread extends Thread {

	private BufferedReader in;
//	private DataInputStream in;
	private DataOutputStream out;
	private Socket socket;
	private final String DIR_PATH = "src/assignment2/content/";

	public ClientThread(Socket newSocket) {
		socket = newSocket;

		try {
			socket.setSoTimeout(10000);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));			
//			in = new DataInputStream(socket.getInputStream());		
			out = new DataOutputStream(socket.getOutputStream());
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

	public void run() {
		try {
			int bytesRead;
			String requestString = new String();
			byte[] buf = new byte[1024];
			
			
			String line;
			while(true) {
				line = in.readLine();
				requestString += line + "\n";
				if (line.isEmpty() || line.equals("\r\n"))
					break;
			}
			
//			while((bytesRead = in.read(buf)) != -1) { // loops while input stream contains data. consumes input stream and stores it to buf array
//				requestString += new String(buf, 0, bytesRead);
//				
//			}
			
			HTTPRequest request = HTTPRequest.parseRequest(requestString);
			System.out.println(request .TYPE + " request from " + socket.getInetAddress().toString().substring(1));

			File file = new File(DIR_PATH + request.PATH);

			HTTPHeader header = new HTTPHeader(request, file);
//			String header = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n"; //TODO DEBUG ------------------------------------------------
			out.write(header.toString().getBytes());
			
			// out.write(createHeader(file).getBytes());
			writeFile(file);
			
			System.out.println("Sent " + file.length() + " byte(s) to " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());

			socket.close();
		}
//		catch(SocketException e) {
//			System.err.println(e.getMessage());
//		}
		catch(HTTPException e) {
			System.out.println(e.getMessage());
		}
		catch(IOException e) { //catch(IOException e) {
			System.err.println("This is an exception");
			System.err.println(e.getMessage());
			try {
				socket.close();
			}
			catch(IOException e2) {
			}
		}
//		catch(Exception e) {
//			System.err.println("An exception was thrown!");
//			System.err.println(e.getMessage());
//		}
	}
	
	private void writeFile(File file) {
		byte[] buf = new byte[1024];
		
		try(FileInputStream fIn = new FileInputStream(file)) {
			int bytesRead;
			while((bytesRead = fIn.read(buf)) > 0) {
				out.write(buf, 0, bytesRead);
			}
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
}