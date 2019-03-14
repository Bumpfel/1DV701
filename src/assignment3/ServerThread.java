package assignment3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.FileAlreadyExistsException;

import assignment3.exceptions.*;

class ServerThread extends Thread {
    
    private RequestHandler requestHandler = new RequestHandler();
    private ErrorHandler errorHandler = new ErrorHandler();
    private TFTPServer server;
    private DatagramSocket socket;
    private InetSocketAddress clientAddress;
    private int requestType;
    private StringBuffer requestedFile = new StringBuffer();
    private StringBuffer mode = new StringBuffer();
    private byte[] unparsedRequest;
    
    ServerThread(TFTPServer tftpServer, byte[] rawRequest, InetSocketAddress newClientAddress) {
        server = tftpServer;
        clientAddress = newClientAddress;
        unparsedRequest = rawRequest; //TODO urgh
        try {
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
            requestType = requestHandler.parseRQ(unparsedRequest, requestedFile, mode);
        
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
            errorHandler.sendError(socket, TFTPServer.ERROR_UNDEFINED, "Unknown transfer error");
        }
        // Error code 0
        catch(TransferTimedOutException e) {
            System.err.println(e.getMessage());
            errorHandler.sendError(socket, TFTPServer.ERROR_UNDEFINED, "Transfer timed out");
        }
        // Error code 1
        //TODO not sure if it should send an access violation error if this exception is caused by a read access error
        catch(FileNotFoundException e) { // can also be caused by no read access
            System.err.println("Read request from " +  clientAddress.getHostName() + " denied: file not found or file read access denied");
            errorHandler.sendError(socket, TFTPServer.ERROR_FILENOTFOUND, "Requested file not found");
        }
        // Error code 3
        catch(AllocationExceededException e) {
            System.err.println("Allocated space for upload directory exceeded");
            errorHandler.sendError(socket, TFTPServer.ERROR_DISKFULL, "Disk full or allocation exceeded");
        }
        // Error code 4
        catch(IllegalTFTPOperationException e) {
            System.err.println("Invalid request from " + clientAddress.getHostName());
            errorHandler.sendError(socket, TFTPServer.ERROR_ILLEGAL, "Illegal TFTP operation");
        }
        // Error code 4
        catch(UnsupportedModeException e) {
            System.err.println("Invalid request from " + clientAddress.getHostName() + ", reason: mode not supported");
            errorHandler.sendError(socket, TFTPServer.ERROR_ILLEGAL, "Illegal TFTP operation: mode not supported");
        }
        // Error code 5
        catch(UnknownTransferIDException e) {
            System.err.println("Received packet from " + clientAddress.getHostName() + " does not contain the expected TID");
            errorHandler.sendError(socket, TFTPServer.ERROR_TRANSFERID, "Unknown transfer ID");
        }
        // Error code 6
        catch(FileAlreadyExistsException e) {
            System.err.println(e.getMessage());
            errorHandler.sendError(socket, TFTPServer.ERROR_FILEEXISTS, "A file with that name already exists on the server");
        }
        // Error code 2
        catch(IOException e) {
            System.err.println(e.getMessage());
            errorHandler.sendError(socket, TFTPServer.ERROR_ACCESS, "Access violation");
        }
    }

}