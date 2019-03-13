package assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

//TODO blir inget felmeddelande om man försöker getta till en icke existerande mapp
//TODO find out the dealio with Mode octet. also check max filename size, or rather that the 0 byte is found at the end
public class TFTPServer {

	// Server Settings (these should relly be program args if one wanted to make them dynamic)
	final int TFTP_PORT = 4970;
	final int PACKET_SIZE = 516;
	final int DATA_BLOCK_SIZE = PACKET_SIZE - 4;

	final int MAX_TRANSFER_ATTEMPTS = 5;
	final int TRANSFER_TIMEOUT = 3000;

	final String READ_DIR = "src/assignment3/server-files/"; // custom address at your computer
	final String WRITE_DIR = "src/assignment3/uploaded-files/"; //custom address at your computer
	
	final double UPLOAD_MAXIMUM = 200 * Math.pow(1024, 2); // Max size allocated to upload folder (200 MB)
	final double ALLOCATION_CONTROL_INTERVAL = 10 * Math.pow(1024, 2); // controls how often a directory space control is made during a PUT. (every 10 MB)

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

				StringBuffer requestedFile = new StringBuffer();
				StringBuffer mode = new StringBuffer();
			
				// If clientAddress is null, an error occurred in receiveFrom()
				if(clientAddress == null)
					continue;
					
				int reqType = parseRQ(buf, requestedFile, mode);

				new ServerThread(this, reqType, clientAddress, requestedFile, mode.toString()).start();
			}
			catch(IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	

	/**
	 * Parses the request in buf to retrieve the type of request and requestedFile
	 * 
	 * @param buffer (received request)
	 * @param requestedFile - object to store parsed file name in (name of file to read/write from/to)
	 * @param mode object to store request mode in
	 * @return opcode (request type: RRQ or WRQ)
	 */
	private int parseRQ(byte[] buffer, StringBuffer requestedFile, StringBuffer mode) throws ArrayIndexOutOfBoundsException {
		int opCode = buffer[0] + buffer[1];

		// if(opCode != TFTPServer.OP_RRQ && opCode != TFTPServer.OP_WRQ)
		// 	throw new IllegalTFTPOperation();

		int foundZeroByte = 0;
		for(int i = 2; i < buffer.length; i ++) {
			if(buffer[i] == 0) {
				foundZeroByte ++;
				if(foundZeroByte == 2)
					break;
			}
			else if(foundZeroByte == 0)
				requestedFile.append((char) buffer[i]);
			else if(foundZeroByte == 1)
				mode.append((char) buffer[i]);
		}
		// if(foundZeroByte != 2)
		// 	throw new IllegalTFTPOperation(); //TODO don't forget to control check the request somewhere else (don't remember if I did already)

		return opCode;
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
	
}



