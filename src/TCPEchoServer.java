import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPEchoServer {
//	public static final int BUFSIZE= 1024;
	public static final int MYPORT= 4951;

	public static void main(String[] args) throws IOException {
//		byte[] buf= new byte[BUFSIZE];

		System.out.println("Server started");

		try(ServerSocket serverSocket = new ServerSocket(MYPORT)) {
			while (true) {
				Socket clientSocket = serverSocket.accept();

				Client client = new Client(clientSocket);
				client.start();
			}
		}
	}
}

class Client extends Thread {

	private InputStream in;
	private OutputStream out;
	private Socket socket;
	public static final int BUFSIZE = 1024;

	public Client(Socket newSocket) {
		socket = newSocket;

		try {
			in = new DataInputStream(socket.getInputStream());			
			out = new DataOutputStream(socket.getOutputStream());
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

	public void run() {
		try {
			String received;
			do {
				byte[] buf = new byte[BUFSIZE];
				in.read(buf); // receive
				received = new String(buf).trim(); // make received byte[] to string
				out.write(buf); // echo back
				System.out.println(received.length() + " bytes received over TCP, sent from " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());
			}
			while(!received.isEmpty());
			System.out.println();
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