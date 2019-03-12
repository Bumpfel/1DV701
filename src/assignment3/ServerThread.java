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

import assignment3.exceptions.IllegalTFTPOperationException;
import assignment3.exceptions.TransferTimedOutException;
import assignment3.exceptions.UnknownTransferIDException;

public class ServerThread extends Thread {

    private DatagramSocket socket;
    private InetSocketAddress clientAddress;
    private TransferHandler handler = new TransferHandler();
    private int requestType;
    private StringBuffer requestedFile;
    private String mode; //TODO dno if I'll use it or immediately reject non octet mode packets
    private final long MAX_FILE_SIZE = (long) (100 * Math.pow(1024, 2)); // 100 MB
    // private final double AVAILABLE_HDD_SPACE = 100 * Math.pow(1024, 2); //100 MB

    //TODO deal with mode (netascii/octet)?
    public ServerThread(int newRequestType, InetSocketAddress newClientAddress, StringBuffer newRequestedFile, String newMode) {
        try {
            requestType = newRequestType;
            clientAddress = newClientAddress;
            requestedFile = newRequestedFile;
            mode = newMode;

            socket = new DatagramSocket(0);
            socket.setSoTimeout(TFTPServer.TRANSFER_TIMEOUT);
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
                requestedFile.insert(0, TFTPServer.READ_DIR);
                handleRQ(socket, requestedFile.toString(), TFTPServer.OP_RRQ);
            }
            // Write request
            else if(requestType == TFTPServer.OP_WRQ) {
                requestedFile.insert(0, TFTPServer.WRITE_DIR);
                handleRQ(socket, requestedFile.toString(), TFTPServer.OP_WRQ);
            }
            else {
                throw new IllegalTFTPOperationException();
            }
            System.out.print((requestType == TFTPServer.OP_RRQ) ? "Read " : "Write ");
            System.out.println("request for " + requestedFile + " from " + clientAddress.getHostName() + " using port " + clientAddress.getPort());
        }
        catch(SocketException e) {
            System.err.println();
            e.printStackTrace(); //TODO printstacktrace
        }
        catch(FileAlreadyExistsException e) {
            handler.sendError(socket, TFTPServer.ERROR_ACCESS, "A file with that name already exists on the server");
            System.err.print((requestType == TFTPServer.OP_RRQ) ? "Read " : "Write ");
            System.err.println("request from " + clientAddress.getHostName() + " denied: file exists"); // server print
        }
        catch(FileNotFoundException e) { // can also be caused by no read access (in that case, file existence is kept hidden from client)
            handler.sendError(socket, TFTPServer.ERROR_FILENOTFOUND, "Requested file not found"); 
            System.err.print((requestType == TFTPServer.OP_RRQ) ? "Read " : "Write ");
            System.err.println("request from " +  clientAddress.getHostName() + " denied: file not found or file read access denied"); // server print
        }
        catch(IllegalTFTPOperationException e) {
            System.err.println("Invalid request from " + clientAddress.getHostName());
            handler.sendError(socket, TFTPServer.ERROR_ILLEGAL, "Illegal TFTP operation");
        }
        catch(TransferTimedOutException e) {
            handler.sendError(socket, TFTPServer.ERROR_UNDEFINED, "Transfer timed out");
            System.err.println(e.getMessage());
        }
        catch(UnknownTransferIDException e) {
            System.err.println("Received packet from " + clientAddress.getHostName() + " does not contain the expected TID");
            new TransferHandler().sendError(socket, TFTPServer.ERROR_TRANSFERID, "Unknown transfer ID");
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

    //TODO refactor. this method is huge
	private void handleRQ(DatagramSocket socket, String requestedFile, int opCode) throws IOException, UnknownTransferIDException, TransferTimedOutException {
		TransferHandler handler = new TransferHandler();

		// Read ReQuest
		if(opCode == TFTPServer.OP_RRQ) {
			File newFile = new File(requestedFile);
			byte[] buf = new byte[TFTPServer.BLOCK_SIZE];
            
			FileInputStream fis = new FileInputStream(newFile);
			boolean packetAcknowledged = false;
			short block = 1;
			int bytesRead;
			final int HEADER_LENGTH = 4;

			ByteBuffer bb = ByteBuffer.allocate(TFTPServer.BUF_SIZE);

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
				packetAcknowledged = handler.sendDataReceiveAck(packet, socket, block);
				block ++;
			}
			while(bytesRead == TFTPServer.BLOCK_SIZE && packetAcknowledged);
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
                throw new FileAlreadyExistsException("");

       		final short HANDSHAKE_BLOCK = 0;
			final int HEADER_SIZE = 4;
			
			// "Handshake packet"
			// TODO maybe separate "handshake" to its own method
			ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE);
			bb.putShort(TFTPServer.OP_ACK);
			bb.putShort(HANDSHAKE_BLOCK);
			
			byte[] arr = bb.array();
			DatagramPacket handShakePacket = new DatagramPacket(arr, arr.length);
			socket.send(handShakePacket);

			DatagramPacket receivePacket;
			boolean packetAcknowledged;
			byte[] buf = new byte[TFTPServer.BUF_SIZE];

            FileOutputStream fos = new FileOutputStream(newFile);
            short expectedBlock = 1;
            long bytesWritten = 0;
            // iterates once per packet
			do {
                packetAcknowledged = false;
				receivePacket = new DatagramPacket(buf, buf.length);
				
				packetAcknowledged = handler.receiveDataSendAck(receivePacket, socket, expectedBlock ++);
                
                //TODO testing error 3 allocation exceeded
                bytesWritten += TFTPServer.BLOCK_SIZE;
                if(bytesWritten >= MAX_FILE_SIZE) {
                    packetAcknowledged = false;
                    fos.close();
                    newFile.delete();
                    handler.sendError(socket, TFTPServer.ERROR_DISKFULL, "Maximum file size exceeded");
                    break;
                }
                
                fos.write(receivePacket.getData(), HEADER_SIZE, receivePacket.getLength() - HEADER_SIZE);
			}
			while(receivePacket.getLength() == TFTPServer.BUF_SIZE && packetAcknowledged);
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

}