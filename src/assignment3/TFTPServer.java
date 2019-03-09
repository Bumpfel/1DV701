package assignment3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

//TODO blir inget felmeddelande om man försöker getta till en icke existerande mapp
public class TFTPServer 
{
	public static final int TFTPPORT = 4970;
	public static final int BUFSIZE = 516;
	public static final int BLOCK_SIZE = 512;

	static final int MAX_RETRANSMIT_ATTEMPTS = 6;
	static final int TRANSMIT_TIMEOUT = 5000;

	public static final String READDIR = "c:/users/eric/documents/github/java/1dv701/src/assignment3/server-files/"; //custom address at your PC
	public static final String WRITEDIR = "c:/users/eric/documents/github/java/1dv701/src/assignment3/uploaded-files/"; //custom address at your PC
	// OP codes
	public static final short OP_RRQ = 1;
	public static final short OP_WRQ = 2;
	public static final short OP_DAT = 3;
	public static final short OP_ACK = 4;
	public static final short OP_ERR = 5;

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
							sendSocket.setSoTimeout(TRANSMIT_TIMEOUT);

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
								HandleRQ(sendSocket, requestedFile.toString(),OP_WRQ);
							}
							sendSocket.close();
						}
						catch(SocketException e) {
								e.printStackTrace();
						}
						catch(FileNotFoundException e) {
							System.err.println("Bad request: file not found");
							// e.printStackTrace();
							//TODO Send ERR
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					}
				}.start();
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
	private int ParseRQ(byte[] buf, StringBuffer requestedFile) throws IndexOutOfBoundsException { //TODO (small) not sure the throw is necessary
		int opCode = buf[0] + buf[1];
	
		for(int i = 2; i < buf.length; i ++) {
			// 0-byte
			if(buf[i] == 0) {
				break;
			}
			requestedFile.append((char) buf[i]);
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
		// Read ReQuest
		if(opcode == OP_RRQ) {
			File file = new File(requestedFile);
			byte[] buf = new byte[BLOCK_SIZE];
			
			FileInputStream fis = new FileInputStream(file);
			boolean packetAcknowledged = false;
			short block = 1;
			int bytesRead;
			final int HEADER_LENGTH = 4;

			ByteBuffer bb = ByteBuffer.allocate(BUFSIZE);

			do {
				bb.clear();
				bb.putShort(OP_DAT);
				bb.putShort(block);
				
				// read to buffer if stream contains data 
				if(fis.available() > 0)
					bytesRead = fis.read(buf, 0, buf.length);
				else
					bytesRead = 0;
				
				bb.put(buf);
				DatagramPacket packet = new DatagramPacket(bb.array(), bytesRead + HEADER_LENGTH);
				
				// send and wait for acknowledgement
				packetAcknowledged = send_DATA_receive_ACK(packet, sendSocket, block);
				block ++;
			}
			while(bytesRead == BLOCK_SIZE && packetAcknowledged && block != 0); // could throw some kind of exception instead from the send method

			//TODO debug and error handling. add sendErrs
			if(block == 0) // short overflowed meaning maximum file transfer size exceeded
				System.err.println("Could't transfer entire file. File too big"); // TODO warning before starting? refuse to start?
			else if(packetAcknowledged)
				System.out.println("Successfully transferred " + file.getName() + " (" + file.length() + " B) to " + sendSocket.getInetAddress());
			else
				System.err.println("Transfer error");
			fis.close();
		}
		// Write ReQuest
		else if (opcode == OP_WRQ) { // TODO needs refactoring
			byte[] buf = new byte[BUFSIZE];
			final short HANDSHAKE_BLOCK = 0;

			// ByteBuffer bb = ByteBuffer.wrap(buf);
			ByteBuffer bb = ByteBuffer.allocate(4); // TODO use constant for 4
			
			bb.putShort(OP_ACK);
			bb.putShort(HANDSHAKE_BLOCK);

			final int OFFSET = 4;

			DatagramPacket handShakePacket = new DatagramPacket(bb.array(), bb.array().length);//buf.length);
			sendSocket.send(handShakePacket); // TODO maybe separate "handshake" to its own method

			FileOutputStream fos = new FileOutputStream(requestedFile);
			DatagramPacket receivePacket;
			// block = 1;
			boolean packetAcknowledged;
			int fileSize = 0;
			do { // loop once per packet
				receivePacket = new DatagramPacket(buf, buf.length);
				
				packetAcknowledged = false;
				// while(!packetAcknowledged)
				packetAcknowledged = receive_DATA_send_ACK(receivePacket, sendSocket);//, block);
				// block ++;

				fos.write(receivePacket.getData(), OFFSET, receivePacket.getLength() - OFFSET);
				fileSize += receivePacket.getLength();
			}
			while(receivePacket.getLength() - OFFSET == BLOCK_SIZE && packetAcknowledged);
			
			//TODO debug prints
			if(packetAcknowledged) {
				String[] fileData = requestedFile.split("/");
				String fileName = fileData[fileData.length - 1];
				System.out.println("Successfully transferred " + fileName + " (" + fileSize + " B) from " + sendSocket.getInetAddress());

			}
			else
				System.err.println("Transfer error");
			fos.close();

		}
		else {
			System.err.println("Invalid request. Sending an error packet.");
			// See "TFTP Formats" in TFTP specification for the ERROR packet contents
			send_ERR();
			return;
		}		
	}
	
	// GET
	private boolean send_DATA_receive_ACK(DatagramPacket dataPacket, DatagramSocket socket, short expectedBlock) throws IOException {
		int receivedBlock = -1, transferAttempt = 1;
		
		byte[] buf = new byte[BUFSIZE];
		DatagramPacket ackPacket = new DatagramPacket(buf, buf.length);
		ByteBuffer bb = ByteBuffer.wrap(buf);
		do {
			if(transferAttempt > 1)
				System.err.println("Re-sending packet");
			try {
				socket.send(dataPacket);
				socket.receive(ackPacket);
				receivedBlock = bb.getShort(2); // byte 2 and 3
			}
			catch(SocketTimeoutException e) {
				System.err.println("Attempt " + transferAttempt + ": ack receive timed out"); //debug
			}
			catch(PortUnreachableException e) { //TODO not sure this is the correct way to do it. unsure when this exception is thrown
				System.err.println("Connection broken");
				return false;
			}
			transferAttempt ++;
			if(transferAttempt >= MAX_RETRANSMIT_ATTEMPTS) {
				//TODO send_ERR to stop the client
				return false;
			}
		}
		while(receivedBlock != expectedBlock);
		
		return true;
	}
	

	boolean test = false;
	// PUT
	private boolean receive_DATA_send_ACK(DatagramPacket dataPacket, DatagramSocket socket) throws IOException { //, short expectedBlock
		System.out.println("waiting on data");// expecting block " + expectedBlock);

		int transferAttempt = 0;
		ByteBuffer bb = ByteBuffer.allocate(4);
		ByteBuffer bb2 = ByteBuffer.wrap(dataPacket.getData()); //rename
		short receivedBlock = 0, receivedOpcode = -1;

		while(true) { //TODO handle stuff when client aborts. sends block 0, but maybe it also sends something else?
			transferAttempt ++;
			if(transferAttempt == MAX_RETRANSMIT_ATTEMPTS)
				return false;
			try {
				//TODO times out if ack packet wasn't received by client and the client is waiting for an ack while server is waiting for data at the same time
				socket.receive(dataPacket);

				receivedOpcode = bb2.getShort(0);
				receivedBlock = bb2.getShort(2);
				
				System.out.println("RECEIVED " + dataPacket.getLength() + " B. BLOCK=" + receivedBlock + ", OP_CODE=" + receivedOpcode);
				
				//interrupted by client
				//TODO make this a separate method
				if(receivedOpcode == 5) //receivedBlock == 0 && 
					return false;
				
				bb.putShort(OP_ACK);
				bb.putShort((short) receivedBlock);
				
				System.out.println("sending ack. block " + receivedBlock); // TODO debug
				
				DatagramPacket ackPacket = new DatagramPacket(bb.array(), bb.array().length);
				
				socket.send(ackPacket);
				System.out.println("----");

				break;
			}
			catch(SocketTimeoutException e) {
				System.err.println("Attempt " + transferAttempt + ": receive timed out"); // debug
			}
			catch(ArrayIndexOutOfBoundsException e) { // not sure if necessary. should only happen if client sends faulty packet or it becomes corrupted
				System.out.println("Server error");
				return false;
			}
		}

		return true;
	}
	
	private void send_ERR()	{
	}
	
}



