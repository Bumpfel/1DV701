package assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

//TODO (small) delete TFTPException if not used
//TODO blir inget felmeddelande om man försöker getta till en icke existerande mapp
//TODO find out the dealio with Mode octet. also check max filename size, or rather that the 0 byte is found at the end
public class TFTPServer {

	//TODO (small) could make non static
	static final int TFTP_PORT = 4970;
	static final int BUF_SIZE = 516;
	static final int BLOCK_SIZE = 512;

	static final int MAX_TRANSFER_ATTEMPTS = 5; // TODO (small) rename? transfer, not transmit?
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


	//TODO not sure if I'm gonna use this
	private static final int MAX_USER_QUOTA = 200 * 1024^2;
	private HashMap<InetSocketAddress, Integer> m = new HashMap<>();

	void addToMap(int size, InetSocketAddress client) {
		m.put(client, size);
	}

	boolean exceededQuota(InetSocketAddress client) {
		return m.get(client) > MAX_USER_QUOTA;
	}

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
		byte[] buf = new byte[BUF_SIZE];
		
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

				new ServerThread(reqType, clientAddress, requestedFile, mode.toString()).start();
			}
			catch(IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	

	/**
	 * Parses the request in buf to retrieve the type of request and requestedFile'
	 * Looks for two 0-bytes. The first separating the requested file and mode, and the other to mark the end of the request. Counts operation as illegal if not found
	 * 
	 * @param buf (received request)
	 * @param requestedFile (name of file to read/write)
	 * @return opcode (request type: RRQ or WRQ)
	 */
	private int parseRQ(byte[] buf, StringBuffer requestedFile, StringBuffer mode) {
		int opCode = buf[0] + buf[1];

		// if(opCode != TFTPServer.OP_RRQ && opCode != TFTPServer.OP_WRQ)
		// 	throw new IllegalTFTPOperation();

		int foundZeroByte = 0;
		for(int i = 2; i < buf.length; i ++) {
			if(buf[i] == 0) {
				foundZeroByte ++;
				if(foundZeroByte == 2)
					break;
			}
			else if(foundZeroByte == 0)
				requestedFile.append((char) buf[i]);
			else if(foundZeroByte == 1)
				mode.append((char) buf[i]);
		}
		// if(foundZeroByte != 2)
		// 	throw new IllegalTFTPOperation();

		return opCode;
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
	
}



