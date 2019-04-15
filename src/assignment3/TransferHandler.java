package assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import assignment3.exceptions.ClientAbortedTransferException;
import assignment3.exceptions.IllegalTFTPOperationException;
import assignment3.exceptions.UnknownTransferIDException;

class TransferHandler {
	// to avoid creating new objects for each packet, these are created on class object creation level instead
	private byte[] ackBuf = new byte[4];
	private ByteBuffer ackBuffer = ByteBuffer.wrap(ackBuf);
	private DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length);
	
	// Client GET
	boolean sendDataReceiveAck(DatagramPacket dataPacket, DatagramSocket socket, short block) throws IOException, PortUnreachableException, ClientAbortedTransferException, IllegalTFTPOperationException, UnknownTransferIDException {
		int transferAttempt = 0;
		short receivedBlock = -1;
		
		// iterates if the transfer attempt failed (up to a number of times specified in server settings)
		do {
            transferAttempt ++;
            if(transferAttempt > TFTPServer.MAX_TRANSFER_ATTEMPTS)
				return false;
			try {
				socket.send(dataPacket);
				socket.receive(ackPacket);

				validatePacket(dataPacket.getPort(), socket.getPort(), ackBuffer.getShort(0), TFTPServer.OP_ACK);
				
                receivedBlock = ackBuffer.getShort(2);
			}
			catch(SocketTimeoutException e) {
				System.err.println("Attempt " + transferAttempt + ": ack receive timed out");
			}
		}
		while(receivedBlock != block);
		
		return true;
	}

	
	// Client PUT
	boolean receiveDataSendAck(DatagramPacket dataPacket, DatagramSocket socket, short expectedBlock) throws IOException, ClientAbortedTransferException, IllegalTFTPOperationException, UnknownTransferIDException {
		
		int transferAttempt = 0;
		ByteBuffer dataBB = ByteBuffer.wrap(dataPacket.getData());
		short receivedBlock = -1, receivedOpCode = -1;
		
		// iterates if receive attempt timed out
		while(true) {
			transferAttempt ++;
			if(transferAttempt > TFTPServer.MAX_TRANSFER_ATTEMPTS)
				return false;
			try {
				socket.receive(dataPacket);

				receivedOpCode = dataBB.getShort(0);
				receivedBlock = dataBB.getShort(2);
				
				validatePacket(dataPacket.getPort(), socket.getPort(), receivedOpCode, TFTPServer.OP_DAT);

				sendAck(socket, receivedBlock);

				// re-receive if receivedBlock was not the expected (happens if client did not receive previously sent ack)
				// prevents writing received data to file if received data was old
				if(receivedBlock != expectedBlock)
					continue;
				
				return true;
			}
			catch(SocketTimeoutException e) {
				System.err.println("Attempt " + transferAttempt + ": data receive timed out");
				System.err.println("Receive attempt timed out");
			}
		}
	}
	
	
	void sendHandShake(DatagramSocket socket) throws IOException {
		final short HANDSHAKE_BLOCK = 0;
		sendAck(socket, HANDSHAKE_BLOCK);
	}
	
	
	private void sendAck(DatagramSocket socket, short block) throws IOException {
		ackBuffer.putShort(0, TFTPServer.OP_ACK);
		ackBuffer.putShort(2, block);
		
		// DatagramPacket ackPacket = new DatagramPacket(ackBuffer.array(), ackBuffer.array().length); // TODO remove if set works
		ackPacket.setData(ackBuffer.array(), 0, ackBuffer.array().length);
				
		socket.send(ackPacket);
	}


	// throws various exceptions if the packet contains faults
	private void validatePacket(int sourceTID, int destinationTID, short opCode, short expectedOpCode) throws IllegalTFTPOperationException, UnknownTransferIDException, ClientAbortedTransferException {
		if(sourceTID != destinationTID)
			throw new UnknownTransferIDException();
		
		if(opCode != expectedOpCode)
			throw new IllegalTFTPOperationException("Received unexpected op code");
		
		if(opCode == TFTPServer.OP_ERR)
			throw new ClientAbortedTransferException();
	}
	
}