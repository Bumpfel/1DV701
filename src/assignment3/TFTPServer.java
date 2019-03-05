package assignment3;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

public class TFTPServer 
{
	public static final int TFTPPORT = 4970;
	public static final int BUFSIZE = 516;
	public static final String READDIR = "c:/users/eric/documents/github/java/1dv701/src/assignment3/downloaded/"; //custom address at your PC
	public static final String WRITEDIR = "c:/users/eric/documents/github/java/1dv701/src/assignment3/uploaded/"; //custom address at your PC
	// OP codes
	public static final int OP_RRQ = 1;
	public static final int OP_WRQ = 2;
	public static final int OP_DAT = 3;
	public static final int OP_ACK = 4;
	public static final int OP_ERR = 5;

	public static void main(String[] args) {
		if (args.length > 0) 
		{
			System.err.printf("usage: java %s\n", TFTPServer.class.getCanonicalName());
			System.exit(1);
		}

		try 
		{
			TFTPServer server = new TFTPServer();
			server.start();
		}
		catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	private void start() throws SocketException 
	{
		byte[] buf= new byte[BUFSIZE];
		
		DatagramSocket socket = new DatagramSocket(null);
		SocketAddress localBindPoint = new InetSocketAddress(TFTPPORT);
		socket.bind(localBindPoint);

		System.out.printf("Listening at port %d for new requests\n", TFTPPORT);

		// Loop to handle client requests 
		while (true) {
			try {
				final InetSocketAddress clientAddress = receiveFrom(socket, buf);
				
				// If clientAddress is null, an error occurred in receiveFrom()
				if (clientAddress == null)
					continue;

				final StringBuffer requestedFile = new StringBuffer();
				final int reqtype = ParseRQ(buf, requestedFile);

				//TODO maybe separate this thread into its own class?
				new Thread() {
					public void run() {
						try {
							DatagramSocket sendSocket = new DatagramSocket(0);

							// Connect to client
							sendSocket.connect(clientAddress);
							
							System.out.println("host: " + clientAddress.getHostName());

							System.out.print((reqtype == OP_RRQ) ? "Read" : "Write");
							System.out.println(" request for " + clientAddress.getHostName() + " using port " + clientAddress.getPort());
							
							// Read request
							if (reqtype == OP_RRQ) {
								requestedFile.insert(0, READDIR);
								HandleRQ(sendSocket, requestedFile.toString(), OP_RRQ);
							}
							// Write request
							else {
								requestedFile.insert(0, WRITEDIR);
								HandleRQ(sendSocket,requestedFile.toString(),OP_WRQ);
							}
							sendSocket.close();
						}
						catch(SocketException e) {
								e.printStackTrace();
						}
						catch(IOException e) {
							System.err.println(e.getMessage());
						}
					}
				}.start();
			}
			catch(IOException e) {
				System.err.println(e.getMessage());
			}
			catch(ArrayIndexOutOfBoundsException e) { //TODO (small) not sure this is needed
				System.err.println("Invalid request");
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
	private int ParseRQ(byte[] buf, StringBuffer requestedFile) throws IndexOutOfBoundsException { //TODO (small) not sure the throw is necessary
		int opCode = buf[0] + buf[1];
		
		//RRQ
		if(opCode == 1) {
			for(int i = 2; i < buf.length; i ++) {
				requestedFile.append((char) buf[i]);
				// 0-byte
				if(buf[i] == 0) {
					break;
				}
			}
		}
		return opCode;
	}

	/**
	 * Handles RRQ and WRQ requests 
	 * 
	 * @param sendSocket (socket used to send/receive packets)
	 * @param requestedFile (name of file to read/write)
	 * @param opcode (RRQ or WRQ)
	 */
	private void HandleRQ(DatagramSocket sendSocket, String requestedFile, int opcode) throws IOException {
		if(opcode == OP_RRQ) {
			File file = new File(requestedFile);
			byte[] buf = new byte[BUFSIZE];

			//opCode
			buf[0] = 0;
			buf[1] = (byte) opcode;
			//block #
			buf[2] = 0;
			buf[3] = 1;
			//data
			int i = 0, offset = 0;
			while(i < requestedFile.length()) {
				buf[i + 4] = (byte) requestedFile.charAt(i);
				i ++;
			}
			System.out.println("i: " + i);

			DatagramPacket packet = new DatagramPacket(buf, i + 4);
			sendSocket.send(packet);
			// See "TFTP Formats" in TFTP specification for the DATA and ACK packet contents
			boolean result = send_DATA_receive_ACK();
		}
		else if (opcode == OP_WRQ) {
			boolean result = receive_DATA_send_ACK();
		}
		else {
			System.err.println("Invalid request. Sending an error packet.");
			// See "TFTP Formats" in TFTP specification for the ERROR packet contents
			send_ERR();
			return;
		}		
	}
	
	/**
	To be implemented
	*/
	private boolean send_DATA_receive_ACK() {
		return true;
	}
	
	private boolean receive_DATA_send_ACK() {
		return true;
	}
	
	private void send_ERR()	{
	}
	
}



