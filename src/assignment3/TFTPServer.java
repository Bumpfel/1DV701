package assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

//TODO blir inget felmeddelande om man försöker getta till en icke existerande mapp
//TODO find out the dealio with Mode octet. also check max filename size, or rather that the 0 byte is found at the end
public class TFTPServer {

	//TODO (small) could make non static
	static final int TFTP_PORT = 4970;
	static final int BUF_SIZE = 516;
	static final int BLOCK_SIZE = 512;

	static final int MAX_RETRANSMIT_ATTEMPTS = 5; // TODO (small) rename? transfer, not transmit?
	static final int TRANSFER_TIMEOUT = 3000;

	static final String READ_DIR = "src/assignment3/server-files/"; //custom address at your computer
	static final String WRITE_DIR = "src/assignment3/uploaded-files/"; //custom address at your computer
	
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
		if (args.length > 0) {
			System.err.printf("usage: java %s\n", TFTPServer.class.getCanonicalName());
			System.exit(1);
		}

		try {
			TFTPServer server = new TFTPServer();
			server.start();
		}
		catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	private void start() throws SocketException {
		byte[] buf = new byte[BUF_SIZE];
		
		DatagramSocket socket = new DatagramSocket(null);
		SocketAddress localBindPoint = new InetSocketAddress(TFTP_PORT);
		socket.bind(localBindPoint);

		System.out.printf("Listening at port %d for new requests\n", TFTP_PORT);

		// Loop to handle client requests 
		while (true) {
			try {
				final InetSocketAddress clientAddress = receiveFrom(socket, buf);
				
				// If clientAddress is null, an error occurred in receiveFrom()
				if (clientAddress == null)
					continue;

				final StringBuffer requestedFile = new StringBuffer();
				final int reqType = parseRQ(buf, requestedFile);

				new ServerThread(clientAddress, reqType, requestedFile).start();
			}
			catch(IOException e) {
				System.err.println(e.getMessage());
			}
			catch(ArrayIndexOutOfBoundsException e) { //TODO (small) not sure this is needed
				System.err.println("Invalid client request");
			}
		}
	}
	
	/**
	 * Reads the first block of data, i.e., the request for an action (read or write).
	 * @param socket (socket to read from)
	 * @param buf (where to store the read data)
	 * @return socketAddress (the socket address of the client)
	 */
	private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buf) throws IOException {
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);

		return new InetSocketAddress(packet.getAddress(), packet.getPort());
	}

	/**
	 * Parses the request in buf to retrieve the type of request and requestedFile
	 * 
	 * @param buf (received request)
	 * @param requestedFile (name of file to read/write)
	 * @return opcode (request type: RRQ or WRQ)
	 */
	private int parseRQ(byte[] buf, StringBuffer requestedFile) throws IndexOutOfBoundsException { //TODO (small) not sure the throw is necessary
		int opCode = buf[0] + buf[1];
	
		boolean foundFinalByte = false;
		for(int i = 2; i < buf.length; i ++) {
			// 0-byte
			if(buf[i] == 0) {
				foundFinalByte = true;
				break;
			}
			requestedFile.append((char) buf[i]);
		}
		if(!foundFinalByte)
			return -1;
		return opCode;
	}
	
}



