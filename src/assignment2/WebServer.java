package assignment2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {
	private final int MYPORT = 9000;
	final static String CONTENT_PATH = "src/assignment2/content/";
	final static String UPLOAD_PATH = "src/assignment2/uploads/";
	public final static int FILE_BUFFER_SIZE = 8192;

	public static void main(String[] args) {
		WebServer tcpServer = new WebServer();
		tcpServer.startServer();
	}

	public void startServer() {
		try(ServerSocket serverSocket = new ServerSocket(MYPORT)) {
			System.out.print("Server started on port " + serverSocket.getLocalPort() + ". ");
			System.out.println();

			while (true) {
				Socket clientSocket = serverSocket.accept();

				new ServerThread(clientSocket).start();
			}
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}
}