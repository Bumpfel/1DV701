package assignment3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.text.DecimalFormat;

import assignment3.exceptions.AllocationExceededException;
import assignment3.exceptions.IllegalTFTPOperationException;
import assignment3.exceptions.TransferTimedOutException;
import assignment3.exceptions.UnknownTransferIDException;

public class ServerThread extends Thread {

    private TFTPServer server;
    private DatagramSocket socket;
    private InetSocketAddress clientAddress;
    private TransferHandler handler = new TransferHandler();
    private int requestType;
    private StringBuffer requestedFile;
    private String mode; //TODO dno if I'll use it or immediately reject non octet mode packets

    //TODO deal with mode (netascii/octet)?
    public ServerThread(TFTPServer tftpServer, int newRequestType, InetSocketAddress newClientAddress, StringBuffer newRequestedFile, String newMode) {
        try {
            server = tftpServer;
            requestType = newRequestType;
            clientAddress = newClientAddress;
            requestedFile = newRequestedFile;
            mode = newMode;

            socket = new DatagramSocket(0);
            socket.setSoTimeout(server.TRANSFER_TIMEOUT);
            socket.connect(clientAddress);
        }
        catch(SocketException e) {
            System.err.println(e.getMessage());
        }
    }

    public void run() {
        try {
            // Read request
            if (requestType == TFTPServer.OP_RRQ) {
                System.out.println("Read request for " + requestedFile + " from " + clientAddress.getHostName() + " using port " + clientAddress.getPort());
                requestedFile.insert(0, server.READ_DIR);
                handleRQ(socket, requestedFile.toString(), TFTPServer.OP_RRQ);
            }
            // Write request
            else if(requestType == TFTPServer.OP_WRQ) {
                System.out.println("Write request for " + requestedFile + " from " + clientAddress.getHostName() + " using port " + clientAddress.getPort());
                requestedFile.insert(0, server.WRITE_DIR);
                handleRQ(socket, requestedFile.toString(), TFTPServer.OP_WRQ);
            }
            else {
                throw new IllegalTFTPOperationException();
            }
        }
        catch(SocketException e) {
            System.err.println();
            e.printStackTrace(); //TODO printstacktrace
        }
        catch(FileAlreadyExistsException e) {
            System.err.println(e.getMessage()); // server print
            handler.sendError(socket, TFTPServer.ERROR_ACCESS, "A file with that name already exists on the server");
        }
        catch(FileNotFoundException e) { // can also be caused by no read access (in that case, file existence is kept hidden from client)
            System.err.println("Read request from " +  clientAddress.getHostName() + " denied: file not found or file read access denied"); // server print
            handler.sendError(socket, TFTPServer.ERROR_FILENOTFOUND, "Requested file not found");
        }
        catch(IllegalTFTPOperationException e) {
            System.err.println("Invalid request from " + clientAddress.getHostName());
            handler.sendError(socket, TFTPServer.ERROR_ILLEGAL, "Illegal TFTP operation");
        }
        catch(TransferTimedOutException e) {
            System.err.println(e.getMessage());
            handler.sendError(socket, TFTPServer.ERROR_UNDEFINED, "Transfer timed out");
        }
        catch(UnknownTransferIDException e) {
            System.err.println("Received packet from " + clientAddress.getHostName() + " does not contain the expected TID");
            handler.sendError(socket, TFTPServer.ERROR_TRANSFERID, "Unknown transfer ID");
        }
        catch(AllocationExceededException e) {
            System.err.println(e.getMessage());
            handler.sendError(socket, TFTPServer.ERROR_DISKFULL, "Disk full or allocation exceeded");
        }
        catch(IOException e) {
            System.err.println(e.getMessage());
            handler.sendError(socket, TFTPServer.ERROR_UNDEFINED, e.getMessage());
        }
    }

    /**
	 * Handles RRQ and WRQ requests 
	 * 
	 * @param socket (socket used to send/receive packets)
	 * @param requestedFile (name of file to read/write)
	 * @param opCode (RRQ=1 or WRQ=2)
	 */

	private void handleRQ(DatagramSocket socket, String requestedFile, int opCode) throws IOException, UnknownTransferIDException, TransferTimedOutException, AllocationExceededException {
		TransferHandler handler = new TransferHandler();
        final int HEADER_LENGTH = 4;
        
        // Read ReQuest
		if(opCode == TFTPServer.OP_RRQ) {
			File newFile = new File(requestedFile);
			byte[] buf = new byte[server.BLOCK_SIZE];
            
			FileInputStream fis = new FileInputStream(newFile);
			boolean packetAcknowledged = false;
			short block = 1;
			int bytesRead;
			
			ByteBuffer bb = ByteBuffer.allocate(server.BUF_SIZE);

            // iterates once per packet
			do {
				bb.clear();
				bb.putShort(TFTPServer.OP_DAT);
				bb.putShort(block);
				
				// read to buffer if file input stream stream contains data 
				if(fis.available() > 0)
					bytesRead = fis.read(buf, 0, buf.length);
				else
					bytesRead = 0;
				
				bb.put(buf);
				DatagramPacket packet = new DatagramPacket(bb.array(), bytesRead + HEADER_LENGTH);
				
				// send data and wait for acknowledgement
				packetAcknowledged = handler.sendDataReceiveAck(server, packet, socket, block);
				block ++;
			}
			while(bytesRead == server.BLOCK_SIZE && packetAcknowledged);
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
            
            checkDirSize(newFile, null);

       		final short HANDSHAKE_BLOCK = 0;
						
			// "Handshake packet"
			// TODO maybe separate "handshake" to its own method
			ByteBuffer bb = ByteBuffer.allocate(HEADER_LENGTH);
			bb.putShort(TFTPServer.OP_ACK);
			bb.putShort(HANDSHAKE_BLOCK);
			
			byte[] arr = bb.array();
			DatagramPacket handShakePacket = new DatagramPacket(arr, arr.length);
			socket.send(handShakePacket);

			DatagramPacket receivePacket;
			boolean packetAcknowledged;
			byte[] buf = new byte[server.BUF_SIZE];

            FileOutputStream fos = new FileOutputStream(newFile);
            short expectedBlock = 1;
            long calc = 0;
            // iterates once per packet
			do {
                packetAcknowledged = false;
				receivePacket = new DatagramPacket(buf, buf.length);
				
				packetAcknowledged = handler.receiveDataSendAck(server, receivePacket, socket, expectedBlock ++);

                // checks used space every so often
                calc += receivePacket.getLength() - HEADER_LENGTH;
                if(calc / server.ALLOCATION_CONTROL_INTERVAL > 1) {
                    calc /= server.ALLOCATION_CONTROL_INTERVAL;
                    checkDirSize(newFile, fos);
                }

                fos.write(receivePacket.getData(), HEADER_LENGTH, receivePacket.getLength() - HEADER_LENGTH);
			}
			while(receivePacket.getLength() == server.BUF_SIZE && packetAcknowledged);
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

    private void checkDirSize(File file, FileOutputStream fos) throws AllocationExceededException, IOException {
        File uploadDirectory = new File(server.WRITE_DIR);
		int dirSize = 0;

		File[] files = uploadDirectory.listFiles();
		for(File f : files)
			dirSize += f.length();
        
        if(dirSize > server.UPLOAD_MAXIMUM) {
            if(fos != null)
                fos.close();
            file.delete();
            throw new AllocationExceededException("Allocated space for upload directory exceeded");
        }
    }

}