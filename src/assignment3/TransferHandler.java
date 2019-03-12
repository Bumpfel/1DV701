package assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import assignment3.exceptions.UnknownTransferIDException;

public class TransferHandler {

    // Client GET
	public boolean sendDataReceiveAck(TFTPServer server, DatagramPacket dataPacket, DatagramSocket socket, short expectedBlock) throws IOException, UnknownTransferIDException {
		int receivedBlock = 0, transferAttempt = 0;
		
		byte[] buf = new byte[server.BUF_SIZE];
		DatagramPacket ackPacket = new DatagramPacket(buf, buf.length);
		ByteBuffer bb = ByteBuffer.wrap(buf);
		do {
            transferAttempt ++;
            if(transferAttempt > server.MAX_TRANSFER_ATTEMPTS)
				return false;
			if(transferAttempt > 1)
				System.err.println("Re-sending packet");
			try {
				socket.send(dataPacket);
				socket.receive(ackPacket);

				controlTransferID(ackPacket.getSocketAddress(), socket.getRemoteSocketAddress());
				
                receivedBlock = bb.getShort(2); // byte 2 and 3

                // client sent error
                if(isErroneous(bb.getShort(0)))
                    return false;
			}
			catch(SocketTimeoutException e) {
				System.err.println("Attempt " + transferAttempt + ": ack receive timed out"); //TODO debug
			}
			catch(PortUnreachableException e) { //TODO not sure this is the correct way to do it. unsure when this exception is thrown
				System.err.println("Connection broken");
				return false;
			}
		}
		while(receivedBlock != expectedBlock);
		
		return true;
	}
	
	// Client PUT
	public boolean receiveDataSendAck(TFTPServer server, DatagramPacket dataPacket, DatagramSocket socket, short expectedBlock) throws IOException, UnknownTransferIDException {
		
		int transferAttempt = 0;
		ByteBuffer ackBB = ByteBuffer.allocate(4);
		ByteBuffer dataBB = ByteBuffer.wrap(dataPacket.getData()); //rename
		short receivedBlock = 0, receivedOpCode = -1;
		
		while(true) {
			// System.out.println("Waiting on data. Expecting block " + expectedBlock);
			transferAttempt ++;
			if(transferAttempt == server.MAX_TRANSFER_ATTEMPTS)
				return false;
			try {
				//TODO (hard) times out if ack packet wasn't received by client and the client is waiting for an ack while server is waiting for data at the same time. test by disabling socket.receive()
				socket.receive(dataPacket);

				controlTransferID(dataPacket.getSocketAddress(), socket.getRemoteSocketAddress());
		
				receivedOpCode = dataBB.getShort(0);
				receivedBlock = dataBB.getShort(2);
				
				// client sent error, e.g. interrupted by client
                if(isErroneous(receivedOpCode))
					return false;
				
				// receivedBlock = (short) (receivedBlock - new Random().nextInt(2)); //TODO INTRODUCING a fault to trigger re-send
				// System.out.println("RECEIVED " + dataPacket.getLength() + " B. BLOCK=" + receivedBlock);
				
				ackBB.putShort(0, TFTPServer.OP_ACK);
				ackBB.putShort(2, receivedBlock);
				
				// System.out.println("sending ack. block " + receivedBlock); // TODO debug
				
				byte[] arr = ackBB.array();
				DatagramPacket ackPacket = new DatagramPacket(arr, arr.length);
                
				socket.send(ackPacket);
				// System.out.println("----");

				// prevents writing received data to file if received data was old
				if(receivedBlock != expectedBlock)
					continue;
                
				return true;
			}
			catch(SocketTimeoutException e) {
				System.err.println("Attempt " + transferAttempt + ": receive timed out");
			}
			catch(ArrayIndexOutOfBoundsException e) { // TODO not sure if necessary. should only happen if client sends faulty packet or it becomes corrupted
				System.out.println("Server error");
				return false;
			}
		}
    }

    public void sendError(DatagramSocket socket, short errCode, String errMsg) {
		if(errCode < 0 || errCode > 7)
			throw new IllegalArgumentException("Error code is not a valid TFTP code");

        int packetLength = 4 + errMsg.length() + 1;
        ByteBuffer bb = ByteBuffer.allocate(packetLength);

        bb.putShort(TFTPServer.OP_ERR);
        bb.putShort(errCode);
        bb.put(errMsg.getBytes());
        bb.put((byte) 0);

		DatagramPacket errPacket = new DatagramPacket(bb.array(), packetLength);
		try {
			socket.send(errPacket);
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}

	private void controlTransferID(SocketAddress receivedSourceAddress, SocketAddress sentDestinationAddress) throws UnknownTransferIDException {
		if(!receivedSourceAddress.equals(sentDestinationAddress))
			throw new UnknownTransferIDException();
	}

    private boolean isErroneous(short opCode) {
        return opCode == TFTPServer.OP_ERR;
    }
}