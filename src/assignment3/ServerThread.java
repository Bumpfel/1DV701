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
    // private InetSocketAddress clientAddress;
    private int requestType;
    private StringBuffer requestedFile;
    private String mode; //TODO dno if I'll use it or immediately reject non octet mode packets

    //TODO deal with mode (netascii/octet)?
    public ServerThread(TFTPServer tftpServer, int newRequestType, InetSocketAddress clientAddress, StringBuffer newRequestedFile, String newMode) {
        try {
            server = tftpServer;
            requestType = newRequestType;
            // clientAddress = newClientAddress;
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
        RequestHandler requestHandler = new RequestHandler();
        TransferHandler tranfserHandler = new TransferHandler();
        InetSocketAddress clientAddress = new InetSocketAddress(socket.getInetAddress(), socket.getPort()); //TODO idiotic?

        try {
            // Read request
            if (requestType == TFTPServer.OP_RRQ) {
                System.out.println("Read request for " + requestedFile + " from " + clientAddress.getHostName() + " using port " + clientAddress.getPort());
                requestedFile.insert(0, server.READ_DIR);
                requestHandler.handleRQ(server, socket, requestedFile.toString(), TFTPServer.OP_RRQ);
            }
            // Write request
            else if(requestType == TFTPServer.OP_WRQ) {
                System.out.println("Write request for " + requestedFile + " from " + clientAddress.getHostName() + " using port " + clientAddress.getPort());
                requestedFile.insert(0, server.WRITE_DIR);
                requestHandler.handleRQ(server, socket, requestedFile.toString(), TFTPServer.OP_WRQ);
            }
            else {
                throw new IllegalTFTPOperationException();
            }
        }
        // Error code 0
        catch(SocketException e) {
            System.err.println(e.getMessage());
            e.printStackTrace(); //TODO printstacktrace
            tranfserHandler.sendError(socket, TFTPServer.ERROR_UNDEFINED, "Unknown transfer error"); //TODO correct to send error?
        }
        // Error code 0
        catch(TransferTimedOutException e) {
            System.err.println(e.getMessage());
            tranfserHandler.sendError(socket, TFTPServer.ERROR_UNDEFINED, "Transfer timed out");
        }
        // Error code 1
        catch(FileNotFoundException e) { // can also be caused by no read access
            System.err.println("Read request from " +  clientAddress.getHostName() + " denied: file not found or file read access denied");
            tranfserHandler.sendError(socket, TFTPServer.ERROR_FILENOTFOUND, "Requested file not found");
        }
        // Error code 3
        catch(AllocationExceededException e) {
            System.err.println("Allocated space for upload directory exceeded (" + server.UPLOAD_MAXIMUM + ")");
            tranfserHandler.sendError(socket, TFTPServer.ERROR_DISKFULL, "Disk full or allocation exceeded");
        }
        // Error code 4
        catch(IllegalTFTPOperationException e) {
            System.err.println("Invalid request from " + clientAddress.getHostName());
            tranfserHandler.sendError(socket, TFTPServer.ERROR_ILLEGAL, "Illegal TFTP operation");
        }
        // Error code 5
        catch(UnknownTransferIDException e) {
            System.err.println("Received packet from " + clientAddress.getHostName() + " does not contain the expected TID");
            tranfserHandler.sendError(socket, TFTPServer.ERROR_TRANSFERID, "Unknown transfer ID");
        }
        // Error code 6
        catch(FileAlreadyExistsException e) {
            System.err.println(e.getMessage());
            tranfserHandler.sendError(socket, TFTPServer.ERROR_FILEEXISTS, "A file with that name already exists on the server");
        }
        // Error code 2
        catch(IOException e) {
            System.err.println(e.getMessage());
            tranfserHandler.sendError(socket, TFTPServer.ERROR_ACCESS, e.getMessage());
        }
    }

}