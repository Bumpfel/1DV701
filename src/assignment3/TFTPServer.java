package assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class TFTPServer {

	// Server Settings
	static final int TFTP_PORT = 4970;
	static final int PAYLOAD_SIZE = 512;
	static final int PACKET_SIZE = PAYLOAD_SIZE + 4;

	static final int MAX_TRANSFER_ATTEMPTS = 5;
	static final int TRANSFER_TIMEOUT = 3000;

	static final String READ_DIR = "src/assignment3/server-files/"; // custom address at your computer
	static final String WRITE_DIR = "src/assignment3/uploaded-files/"; //custom address at your computer
	
	static final double UPLOAD_MAXIMUM = toMB(200); // Max size allocated to upload folder
	static final double ALLOCATION_CONTROL_INTERVAL = toMB(10); // controls how often a directory space control is made during a PUT

	// OP codes
	public static final short OP_RRQ = 1;
	public static final short OP_WRQ = 2;
	public static final short OP_DAT = 3;
	public static final short OP_ACK = 4;
	public static final short OP_ERR = 5;

	// Error codes
	public static final short ERROR_UNDEFINED = 0;
	public static final short ERROR_FILENOTFOUND = 1;
	public static final short ERROR_ACCESS = 2;
	public static final short ERROR_DISKFULL = 3;
	public static final short ERROR_ILLEGAL = 4;
	public static final short ERROR_TRANSFERID = 5;
	public static final short ERROR_FILEEXISTS = 6;
	public static final short ERROR_NOSUCHUSER = 7;

	public static void main(String[] args) {
		if(args.length > 0) {
			System.err.printf("usage: java %s\n", TFTPServer.class.getCanonicalName());
			System.exit(1);
		}
		try {
			TFTPServer server = new TFTPServer();
			server.startServer();
		}
		catch(SocketException e) {
			e.printStackTrace();
		}
	}
	
	private void startServer() throws SocketException {
		byte[] buf = new byte[PACKET_SIZE];
		
		DatagramSocket socket = new DatagramSocket(TFTP_PORT);
		System.out.printf("Listening at port %d for new requests\n", TFTP_PORT);

		// Loop to handle client requests 
		while(true) {
			try {
				InetSocketAddress clientAddress = receiveFrom(socket, buf);

				// If clientAddress is null, an error occurred in receiveFrom()
				if(clientAddress == null)
					continue;

				new ServerThread(buf, clientAddress).start();
			}
			catch(IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	/**
	 * Reads the first block of data, i.e., the request for an action (read or write).
	 * @param socket (socket to read from)
	 * @param buffer (where to store the read data)
	 * @return socketAddress (the socket address of the client)
	 */
	private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buffer) throws IOException {
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);

		return new InetSocketAddress(packet.getAddress(), packet.getPort());
	}

	// converts to megabyte
	private static double toMB(long bytes) {
		return bytes * Math.pow(1024, 2);
	}
	
}



