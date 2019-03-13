package assignment3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.text.DecimalFormat;

import assignment3.exceptions.*;

public class RequestHandler {
    /**
	 * Handles RRQ and WRQ requests 
	 * 
	 * @param socket (socket used to send/receive packets)
	 * @param requestedFile (name of file to read/write)
	 * @param opCode (RRQ=1 or WRQ=2)
	 */

	void handleRQ(TFTPServer server, DatagramSocket socket, String requestedFile, int opCode) throws IOException, UnknownTransferIDException, TransferTimedOutException, AllocationExceededException {
		TransferHandler handler = new TransferHandler();
		final int HEADER_LENGTH = 4;
		InetSocketAddress clientAddress = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
        
        // Read ReQuest
		if(opCode == TFTPServer.OP_RRQ) {
			File newFile = new File(requestedFile);
			byte[] buf = new byte[server.DATA_BLOCK_SIZE];
            
			FileInputStream fis = new FileInputStream(newFile);
			boolean packetAcknowledged = false;
			short block = 1;
			int bytesRead;
			
			ByteBuffer byteBuffer = ByteBuffer.allocate(server.PACKET_SIZE);

            // iterates once per packet
			do {
				byteBuffer.clear();
				byteBuffer.putShort(TFTPServer.OP_DAT);
				byteBuffer.putShort(block);
				
				// read to buffer if file input stream stream contains data 
				if(fis.available() > 0)
					bytesRead = fis.read(buf, 0, buf.length);
				else
					bytesRead = 0;
				
				byteBuffer.put(buf);
				DatagramPacket packet = new DatagramPacket(byteBuffer.array(), bytesRead + HEADER_LENGTH);
				
				// send data and wait for acknowledgement
				packetAcknowledged = handler.sendDataReceiveAck(server, packet, socket, block);
				block ++;
			}
			while(bytesRead == server.DATA_BLOCK_SIZE && packetAcknowledged);
            fis.close();

			if(packetAcknowledged)
                System.out.println("Transferred " + newFile.getName() + " (" + formatFileSize(newFile.length()) + "B) to " + clientAddress.getHostName() + " using port " + clientAddress.getPort());
			else {
                throw new TransferTimedOutException("Transfer error sending " + newFile.getName() + " to " + clientAddress.getHostName());
            }
		}
		// Write ReQuest
		else if (opCode == TFTPServer.OP_WRQ) {
            File newFile = new File(requestedFile);
            
            if(newFile.exists())
                throw new FileAlreadyExistsException("Write request from " + clientAddress.getHostName() + " denied: file exists");
            
            checkDirSize(server, newFile, null);

       		final short HANDSHAKE_BLOCK = 0;
						
			// "Handshake packet"
			// TODO maybe separate "handshake" to its own method
			ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH);
			header.putShort(TFTPServer.OP_ACK);
			header.putShort(HANDSHAKE_BLOCK);
			
			byte[] headerArr = header.array();
			DatagramPacket handShakePacket = new DatagramPacket(headerArr, headerArr.length);
			socket.send(handShakePacket);

			DatagramPacket receivePacket;
			boolean packetAcknowledged;
			byte[] buffer = new byte[server.PACKET_SIZE];

            FileOutputStream fos = new FileOutputStream(newFile);
            short expectedBlock = 1;
            long calc = 0;
            // iterates once per packet
			do {
                packetAcknowledged = false;
				receivePacket = new DatagramPacket(buffer, buffer.length);
				
				packetAcknowledged = handler.receiveDataSendAck(server, receivePacket, socket, expectedBlock ++);

                // checks used space every so often
                calc += receivePacket.getLength() - HEADER_LENGTH;
                if(calc / server.ALLOCATION_CONTROL_INTERVAL > 1) {
                    calc /= server.ALLOCATION_CONTROL_INTERVAL;
                    checkDirSize(server, newFile, fos);
                }

                fos.write(receivePacket.getData(), HEADER_LENGTH, receivePacket.getLength() - HEADER_LENGTH);
			}
			while(receivePacket.getLength() == server.PACKET_SIZE && packetAcknowledged);
            fos.close();
            
            if(packetAcknowledged)
				System.out.println("Received " + newFile.getName() + " (" + formatFileSize(newFile.length()) + "B) from " + clientAddress.getHostName() + " using port " + clientAddress.getPort());
			else {
                newFile.delete();
                throw new TransferTimedOutException("Transfer error receiving " + newFile.getName() + " from " + clientAddress.getHostName() + ". Deleting file");
            }
		}
	}
	
	/**
     * Formats a (file) size to B, KB, MB, or GB rounded to one decimal
     */
    private String formatFileSize(long size) {
        String[] sizePrefix = { "", "K", "M", "G" };
        DecimalFormat df = new DecimalFormat("#.#"); // one decimal
        
        long temp = size;
        int divisions = 0;
        // calculates how many divisions can be made on the size without going below 1
        while(divisions < sizePrefix.length && (temp /= 1024) > 1)
            divisions ++;

        double calc = size / Math.pow(1024, divisions);
        return df.format(calc).replaceAll(",", ".")  + " " + sizePrefix[divisions];
    }

    private void checkDirSize(TFTPServer server, File file, FileOutputStream fos) throws AllocationExceededException, IOException {
        File uploadDirectory = new File(server.WRITE_DIR);
		int dirSize = 0;

		File[] files = uploadDirectory.listFiles();
		for(File f : files)
			dirSize += f.length();
        
        if(dirSize > server.UPLOAD_MAXIMUM) {
            if(fos != null)
                fos.close();
            file.delete();
            throw new AllocationExceededException();
        }
    }
}