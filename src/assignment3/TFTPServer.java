package assignment3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Random;

//TODO blir inget felmeddelande om man försöker getta till en icke existerande mapp
public class TFTPServer 
{
	public static final int TFTPPORT = 4970;
	public static final int BUFSIZE = 516;
	public static final int BLOCK_SIZE = 512;

	static final int MAX_RETRANSMIT_ATTEMPTS = 5;
	static final int TRANSMIT_TIMEOUT = 2000;

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
							// System.err.println("The requested file does not exist");
							e.printStackTrace();
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
			byte[] buf = new byte[BUFSIZE];
			
			FileInputStream fis = new FileInputStream(file);
			boolean packetAcknowledged;
			int block = 1, offset = 4, bytesRead;

			do {
				//opCode DATA (3)
				buf[0] = 0;
				buf[1] = OP_DAT;
				
				// block number. int to high and low byte, filtered using logical bitwise operations and logical shifts
				buf[2] = (byte) (block >> 8 & 0xFF);
				buf[3] = (byte) (block & 0xFF);
				
				// read to buffer if stream contains data 
				if(fis.available() > 0)
					bytesRead = fis.read(buf, offset, buf.length - offset);
				else
					bytesRead = 0;
				
				System.out.println("prepared to send block " + (buf[2] + buf[3]) + ": " + bytesRead + " B");

				// send and wait for acknowledgement
				DatagramPacket packet = new DatagramPacket(buf, bytesRead + offset);

				packetAcknowledged = send_DATA_receive_ACK(packet, sendSocket, block);
				block ++;
			}
			while(bytesRead == BLOCK_SIZE && packetAcknowledged);
			//TODO debug prints
			if(fis.available() == 0)
				System.out.println("--TRANSFER DONE");
			else
				System.err.println("--TRANSFER FAILURE");
			fis.close();
		}
		// Write ReQuest
		else if (opcode == OP_WRQ) { // TODO needs refactoring
			byte[] buf = new byte[BUFSIZE];
			int block = 0;

			// opCode ACK (04)
			buf[0] = 0;
			buf[1] = OP_ACK;
			
			// Block number 0
			buf[2] = 0;
			buf[3] = (byte) block;
			
			int OFFSET = 4;

			DatagramPacket initialAckPacket = new DatagramPacket(buf, buf.length);
			sendSocket.send(initialAckPacket);
			
			FileOutputStream fos = new FileOutputStream(requestedFile);
			DatagramPacket receivePacket;
			block = 1;
			boolean packetAcknowledged;
			do {
				receivePacket = new DatagramPacket(buf, buf.length);
				
				packetAcknowledged = false;
				while(!packetAcknowledged)
					packetAcknowledged = receive_DATA_send_ACK(receivePacket, sendSocket, block);
				block ++;

				fos.write(receivePacket.getData(), OFFSET, receivePacket.getLength() - OFFSET);
			}
			while(receivePacket.getLength() - OFFSET == BLOCK_SIZE);
			
			//TODO debug prints
			System.out.println("--transfer done");
			fos.close();

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

	private boolean send_DATA_receive_ACK(DatagramPacket packet, DatagramSocket socket, int expectedBlock) throws IOException {
		int receivedBlock = -1, transmitAttempts = 0;
		boolean receivedExpectedAck = false;
		
		byte[] buf = new byte[BUFSIZE];
		while(receivedExpectedAck || receivedBlock != expectedBlock) {
			
			try {
				Thread.sleep(5); // TODO debug. to make sure System.outs and System.errs is printed in the correct order.
				System.out.println("sending packet...");
				socket.send(packet);
				DatagramPacket ackPacket = new DatagramPacket(buf, buf.length);
				System.out.println("packet sent. waiting on ack. expecting block " + expectedBlock); //TODO debug
				
				socket.receive(ackPacket);
				receivedBlock = buf[2] + buf[3];
				receivedExpectedAck = true;

				//TODO debug
				if(receivedBlock != expectedBlock) {
					transmitAttempts ++;
					System.err.print("Attempt " + transmitAttempts + ": Received block is not equal to expected block. Received " + receivedBlock + ", expected " + expectedBlock + ". ");
				}
				else //TODO refactor
					break;
			}
			catch(SocketTimeoutException e) {
				transmitAttempts ++;
				System.err.print("Attempt " + transmitAttempts + ": Ack receive timed out. "); //TODO debug
			}
			catch(InterruptedException e) { //TODO debug
				System.out.println("INTERRUPTED");
			}
			
			if(transmitAttempts >= MAX_RETRANSMIT_ATTEMPTS) {
				System.err.println("Maximum re-transmit attempts reached. Aborting file transfer");
				//TODO send_ERR to stop the client
				return false;
			}
			else
				System.err.println("Re-sending packet");

		}

		System.out.println("ACK RECEIVED. BLOCK=" + receivedBlock);
		System.out.println();
		
		return receivedBlock == expectedBlock;
	}
	
	private boolean receive_DATA_send_ACK(DatagramPacket dataPacket, DatagramSocket socket, int expectedBlock) throws IOException {
		System.out.println("waiting on data. expecting block " + expectedBlock);
		socket.receive(dataPacket);
		
		int receivedBlock = dataPacket.getData()[2] + dataPacket.getData()[3];
		System.out.println("RECEIVED " + dataPacket.getLength() + " B. BLOCK=" + receivedBlock);
		
		byte[] buf = new byte[BUFSIZE];

		DatagramPacket ackPacket = new DatagramPacket(buf, buf.length);
		buf[0] = 0;
		buf[1] = OP_ACK;
		
		buf[2] = (byte) (expectedBlock >> 8 & 0xFF);
		buf[3] = (byte) (expectedBlock & 0xFF);

		System.out.println("sending ack. block " + receivedBlock);
		System.out.println("----");
		socket.send(ackPacket);
		
		return expectedBlock == receivedBlock;
	}
	
	private void send_ERR()	{
	}
	
}



