package Assignment1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPEchoServer {
	public static final int MYPORT = 4951;
	private static final boolean VERBOSE_MODE = true; // prints information about every packet (otherwise it will only print when a client has finished sending everything

	public static void main(String[] args) throws IOException {
		
		try {
			if(args.length != 1)
				throw new IllegalArgumentException("Incorrect launch commands. Usage: buffer_size");
			int serverBufferSize = Integer.parseInt(args[0]);
		
			try(ServerSocket serverSocket = new ServerSocket(MYPORT)) {
				System.out.print("Server started on port " + serverSocket.getLocalPort() + ". ");
				System.out.print("Buffer size is " + serverBufferSize + " bytes. ");
				System.out.println("Verbose mode is " + (VERBOSE_MODE ? "on" : "off" + ". "));

				while (true) {
					Socket clientSocket = serverSocket.accept();
	
					ClientThread clientThread = new ClientThread(clientSocket, serverBufferSize);
					clientThread.start();
				}
			}
		}
		catch(NumberFormatException e) {
			System.out.println("Invalid argument format");
		}
		catch(IllegalArgumentException | BindException e) {
			System.out.println(e.getMessage());
		}
	}
}

class ClientThread extends Thread {

	private InputStream in;
	private OutputStream out;
	private Socket socket;
	public int bufferSize;
	private static final boolean VERBOSE_MODE = true; // prints information about every packet (otherwise it will only print when a client has finished sending everything
	
	public ClientThread(Socket newSocket, int serverBufferSize) {
		socket = newSocket;
		bufferSize = serverBufferSize;
		
		try {
			in = new DataInputStream(socket.getInputStream());			
			out = new DataOutputStream(socket.getOutputStream());
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			int bytesReceived;
			String receivedString = new String();
			byte[] buf = new byte[bufferSize];

			int offset = 0;
			while((bytesReceived = in.read(buf)) > 0) { // loops while input stream contains data. consumes input stream and stores it to buf array 
				receivedString += new String(buf, 0, bytesReceived); // piece together the message
				out.write(receivedString.getBytes(), offset, bytesReceived); // send back (echo)
				offset += bytesReceived;
				if(VERBOSE_MODE) {					
					System.out.print("TCP echo request from " + socket.getInetAddress().toString().substring(1) + ". ");
					System.out.println("Sent and received " + bytesReceived + " bytes using port " + socket.getPort());
				}
			}
			if(!VERBOSE_MODE) {
				System.out.print("TCP echo request from " + socket.getInetAddress().toString().substring(1) + ". ");
				System.out.println("Sent and received " + receivedString.getBytes().length + " bytes using port " + socket.getPort());
			}
			socket.close();
		}
		catch(IOException e) {
			try {
				socket.close();
			}
			catch(IOException e2) {
			}
		}
	}
}