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

public class ServerThread extends Thread {

    private DatagramSocket sendSocket;
    private InetSocketAddress clientAddress;
    private StringBuffer requestedFile;
    private int requestType;
    private final long MAX_FILE_SIZE = (long) (100 * Math.pow(1024, 2));

    public ServerThread(InetSocketAddress clAddress, int reqType, StringBuffer reqFile) {
        try {
            sendSocket = new DatagramSocket(0);
            sendSocket.setSoTimeout(TFTPServer.TRANSFER_TIMEOUT);
            sendSocket.connect(clAddress);
            
            clientAddress = clAddress;
            requestedFile = reqFile;
            requestType = reqType;
        }
        catch(SocketException e) {
            System.err.println(e.getMessage());
        }
    }

    public void run() {
        try {
            try {
                System.out.print((requestType == TFTPServer.OP_RRQ) ? "Read " : "Write ");
                System.out.println("request from " + clientAddress.getHostName() + " using port " + clientAddress.getPort());

                // Read request
                if (requestType == TFTPServer.OP_RRQ) {
                    requestedFile.insert(0, TFTPServer.READ_DIR);
                    handleRQ(sendSocket, requestedFile.toString(), TFTPServer.OP_RRQ);
                }
                // Write request OR invalid request
                else {
                    requestedFile.insert(0, TFTPServer.WRITE_DIR);
                    handleRQ(sendSocket, requestedFile.toString(), TFTPServer.OP_WRQ);
                }
            }
            catch(SocketException e) {
                System.err.println();
                e.printStackTrace(); //TODO printstacktrace
            }
            catch(FileAlreadyExistsException e) {
                new TransferHandler().sendError(sendSocket, TFTPServer.ERROR_ACCESS, "A file with that name already exists on the server");
                System.err.print((requestType == TFTPServer.OP_RRQ) ? "Read " : "Write ");
                System.err.println("request from " + clientAddress.getHostName() + " denied: file exists"); // server print
            }
            catch(FileNotFoundException e) { // can also be caused by no read access (in that case, file existence is kept hidden from client)
                new TransferHandler().sendError(sendSocket, TFTPServer.ERROR_FILENOTFOUND, "Requested file not found"); 
                System.err.print((requestType == TFTPServer.OP_RRQ) ? "Read " : "Write ");
                System.err.println("request from " +  clientAddress.getHostName() + " denied: file not found or file read access denied"); // server print
            }
        }
        catch(IOException e) {
            System.err.println(e.getMessage());
            try {
               new TransferHandler().sendError(sendSocket, TFTPServer.ERROR_UNDEFINED, e.getMessage());
            }
            catch(IOException e2) {
            }
        }

    }

    /**
	 * Handles RRQ and WRQ requests 
	 * 
	 * @param sendSocket (socket used to send/receive packets)
	 * @param requestedFile (name of file to read/write)
	 * @param opCode (RRQ=1 or WRQ=2)
	 */

    //TODO refactor. this method is huge
	private void handleRQ(DatagramSocket sendSocket, String requestedFile, int opCode) throws IOException {
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
				packetAcknowledged = handler.sendDataReceiveAck(packet, sendSocket, block);
				block ++;
			}
			while(bytesRead == TFTPServer.BLOCK_SIZE && packetAcknowledged);
            fis.close();

			if(packetAcknowledged)
				System.out.println("Successfully transferred " + newFile.getName() + " (" + formatFileSize(newFile.length()) + "B) to " + clientAddress.getHostName());
			else {
                handler.sendError(sendSocket, TFTPServer.ERROR_UNDEFINED, "Transfer timed out");
                System.err.println("Transfer error sending " + newFile.getName() + " to " + clientAddress.getHostName());
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
			sendSocket.send(handShakePacket);

			DatagramPacket receivePacket;
			boolean packetAcknowledged;
			byte[] buf = new byte[TFTPServer.BUF_SIZE];

            FileOutputStream fos = new FileOutputStream(newFile);
            // iterates once per packet
            short expectedBlock = 1;
            long bytesWritten = 0;
			do {
                packetAcknowledged = false;
				receivePacket = new DatagramPacket(buf, buf.length);
				
				packetAcknowledged = handler.receiveDataSendAck(receivePacket, sendSocket, expectedBlock ++);
                
                //TODO testing error 3 allocation exceeded
                bytesWritten += TFTPServer.BLOCK_SIZE;
                if(bytesWritten > MAX_FILE_SIZE) {
                    fos.close();
                    newFile.delete();
                    handler.sendError(sendSocket, TFTPServer.ERROR_DISKFULL, "Maximum file size exceeded");
                    break;
                }
                
                fos.write(receivePacket.getData(), HEADER_SIZE, receivePacket.getLength() - HEADER_SIZE);
			}
			while(receivePacket.getLength() == TFTPServer.BUF_SIZE && packetAcknowledged);
            fos.close();
            
            if(packetAcknowledged)
				System.out.println("Successfully transferred " + newFile.getName() + " (" + formatFileSize(newFile.length()) + "B) from " + clientAddress.getHostName());
			else {
                handler.sendError(sendSocket, TFTPServer.ERROR_UNDEFINED, "Transfer timed out");
                System.err.println("Transfer error receiving " + newFile.getName() + " from " + clientAddress.getHostName() + ". Deleting file");
                newFile.delete();
            }
		}
		else {
			System.err.println("Invalid request. Sending an error packet.");
			handler.sendError(sendSocket, TFTPServer.ERROR_ILLEGAL, "Illegal TFTP operation"); //TODO (small) use a map for error msgs?
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