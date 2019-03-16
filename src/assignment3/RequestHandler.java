package assignment3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import assignment3.exceptions.*;

class RequestHandler {

	final int HEADER_LENGTH = 4;

	/**
	 * Parses the request in buf to retrieve the type of request, requestedFile and mode, and controls request validity
	 * 
	 * @param buf (received request)
	 * @param requestedFile - object to store parsed file name in
	 * @param mode object to store request mode in
	 * @return opcode (request type: RRQ or WRQ)
	 * @throws ArrayIndexOutOfBoundsException if request does not contain an op code
	 * @throws IllegalTFTPOperationException if request is faulty
	 * @throws UnsupportedModeException if mode is not octet
	 */
	int parseRQ(byte[] buf, StringBuffer requestedFile, StringBuffer mode) throws ArrayIndexOutOfBoundsException, IllegalTFTPOperationException, UnsupportedModeException {
		int opCode = buf[0] + buf[1];

		if(opCode != TFTPServer.OP_RRQ && opCode != TFTPServer.OP_WRQ)
			throw new IllegalTFTPOperationException("Invalid request");
			
		int foundZeroByte = 0;
		for(int i = 2; i < buf.length; i ++) {

			if(buf[i] == 0) { // found 0-byte separator
				foundZeroByte ++;
				if(foundZeroByte == 2) { // checks if two 0-bytes has been found (end of request reached)
					if(!mode.toString().equals("octet")) // ...and if so that mode is octet
						throw new UnsupportedModeException();
					return opCode; // request parsed ok
				}
			}

			else if(foundZeroByte == 0) // current byte is part of file name
				requestedFile.append((char) buf[i]);
			else if(foundZeroByte == 1) // current byte is part of mode
				mode.append((char) buf[i]);
		}
		throw new IllegalTFTPOperationException("Invalid request"); // did not find two 0-bytes
	}


	/**
	 * Handles RRQ requests 
	 * 
	 * @param socket (socket used to send/receive packets)
	 * @param requestedFile (name of file to read/write)
	 * @throws IOException
	 * @throws PortUnreachableException if socket port is unreachable
	 * @throws UnknownTransferIDException if received packet source id (TID) is not what expected 
	 * @throws TransferTimedOutException if server failed to receive data or ack packet within the timeout value a certain amount of times, as specified in server settings
	 */
	void handleRRQ(DatagramSocket socket, String requestedFile) throws IOException, PortUnreachableException, UnknownTransferIDException, TransferTimedOutException, IllegalTFTPOperationException, AccessViolationException {
		File newFile = new File(requestedFile);
		if(!newFile.canRead())
			throw new AccessViolationException();
		
		TransferHandler transferHandler = new TransferHandler();
		InetSocketAddress clientAddress = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
  
		byte[] buf = new byte[TFTPServer.PAYLOAD_SIZE];

		ByteBuffer byteBuffer = ByteBuffer.allocate(TFTPServer.PACKET_SIZE);
		
		boolean packetAcknowledged = false;
		short block = 1;
		int bytesRead;

		try (FileInputStream fis = new FileInputStream(newFile)) {
			// iterates once per packet
			DatagramPacket dataPacket = new DatagramPacket(buf, buf.length);
			do {
				// read to buffer if file input stream stream contains data 
				if(fis.available() > 0)
					bytesRead = fis.read(buf, 0, buf.length);
				else
					bytesRead = 0;

				// build packet
				byteBuffer.clear();
				byteBuffer.putShort(TFTPServer.OP_DAT);
				byteBuffer.putShort(block);
				byteBuffer.put(buf);
				dataPacket.setData(byteBuffer.array(), 0, bytesRead + HEADER_LENGTH);
				
				// send data and wait for acknowledgement
				packetAcknowledged = transferHandler.sendDataReceiveAck(dataPacket, socket, block);
				block ++;
			}
			while(!transferComplete(dataPacket.getLength()) && packetAcknowledged);
			fis.close();

			if(packetAcknowledged)
				System.out.println("Transferred " + newFile.getName() + " (" + formatFileSize(newFile.length()) + "B) to " + clientAddress.getHostName() + " using port " + clientAddress.getPort());
			else {
				throw new TransferTimedOutException("Transfer error sending " + newFile.getName() + " to " + clientAddress.getHostName());
			}
		}
		catch(ClientAbortedTransferException e) {
			System.err.println("Client aborted transfer of " + newFile.getName());
		}
	}


	/**
	 * Handles WRQ requests 
	 * 
	 * @param socket (socket used to send/receive packets)
	 * @param requestedFile (name of file to read/write)
	 * @throws AllocationExceededException if server upload directory space allocation is exceeded
	 * @throws IOException
	 * @throws PortUnreachableException if socket port is unreachable
	 * @throws UnknownTransferIDException if received packet source id (TID) is not what expected 
	 * @throws TransferTimedOutException if server failed to receive data or ack packet within the timeout value a certain amount of times, as specified in server settings
	 */
	void handleWRQ(DatagramSocket socket, String requestedFile) throws IOException, PortUnreachableException, UnknownTransferIDException, TransferTimedOutException, IllegalTFTPOperationException, AllocationExceededException {
		File newFile = new File(requestedFile);
		TransferHandler transferHandler = new TransferHandler();
		InetSocketAddress clientAddress = new InetSocketAddress(socket.getInetAddress(), socket.getPort());

		if(newFile.exists())
			throw new FileAlreadyExistsException("Write request from " + clientAddress.getHostName() + " denied: file exists");
		
		controlDirSize(newFile, null);

		transferHandler.sendHandShake(socket);

		boolean packetAcknowledged;
		byte[] buf = new byte[TFTPServer.PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

		try(FileOutputStream fos = new FileOutputStream(newFile)) {
			short expectedBlock = 1;
			long calc = 0;
			// iterates once per packet
			do {
				packetAcknowledged = transferHandler.receiveDataSendAck(receivePacket, socket, expectedBlock ++);
				
				// checks used space vs allocated space every so often
				calc += receivePacket.getLength() - HEADER_LENGTH;
				if(calc / TFTPServer.ALLOCATION_CONTROL_INTERVAL > 1) {
					calc /= TFTPServer.ALLOCATION_CONTROL_INTERVAL;
					controlDirSize(newFile, fos);
				}

				fos.write(receivePacket.getData(), HEADER_LENGTH, receivePacket.getLength() - HEADER_LENGTH);
			}
			while(!transferComplete(receivePacket.getLength()) && packetAcknowledged);
			fos.close();
			
			if(packetAcknowledged)
				System.out.println("Received " + newFile.getName() + " (" + formatFileSize(newFile.length()) + "B) from " + clientAddress.getHostName() + " using port " + clientAddress.getPort());
			else {
				newFile.delete();
				throw new TransferTimedOutException("Transfer error receiving " + newFile.getName() + " from " + clientAddress.getHostName() + ". Deleting file");
			}
		}
		catch(ClientAbortedTransferException e) {
			newFile.delete();
			System.err.println("Client aborted transfer of " + newFile.getName() + ". Deleting file");
		}
	}
	

	// Formats a (file) size to B, KB, MB, or GB rounded to one decimal
    private String formatFileSize(long size) {
        String[] sizePrefix = { "", "K", "M", "G" };
        DecimalFormat df = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.ENGLISH)); // one decimal
        
        long temp = size;
        int divisions = 0;
        // calculates how many divisions can be made on the size without going below 1
        while(divisions < sizePrefix.length && (temp /= 1024) > 1)
            divisions ++;

        double calc = size / Math.pow(1024, divisions);
        return df.format(calc) + " " + sizePrefix[divisions];
    }


	// Controls that a directory hasn't exceeded server maximum upload setting
    private void controlDirSize(File file, FileOutputStream fos) throws AllocationExceededException, IOException {
		File uploadDirectory = new File(TFTPServer.WRITE_DIR);
		if(!uploadDirectory.isDirectory())
			throw new IllegalArgumentException();

		int dirSize = 0;

		for(File f : uploadDirectory.listFiles())
			dirSize += f.length();

        if(dirSize > TFTPServer.UPLOAD_DIR_MAX_SIZE) {
            if(fos != null)
                fos.close();
            file.delete();
            throw new AllocationExceededException();
        }
	}
	

	private boolean transferComplete(int packetSize) {
		return packetSize != TFTPServer.PACKET_SIZE;
	}
}