import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPEchoServer {
	public static final int BUFSIZE= 1024;
	public static final int MYPORT= 4950;

	public static void main(String[] args) throws IOException {
//		byte[] buf= new byte[BUFSIZE];

		System.out.println("Server started");
		/* Create server socket */
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
			byte[] buf = new byte[BUFSIZE];

			in.read(buf);
			out.write(buf);
			System.out.println("TCP echo request from " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());

			//			while(in.read() > 0) {
			//				System.out.println("Something happened");
			//				out.write("WWAAOO".getBytes());//in.read());
			//			}
			//			socket.close();
		}
		catch(IOException e) {
			//			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
}