package assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import assignment3.exceptions.ClientAbortedTransferException;
import assignment3.exceptions.UnknownTransferIDException;

class TransferHandler {

    // Client GET
	boolean sendDataReceiveAck(TFTPServer server, DatagramPacket dataPacket, DatagramSocket socket, short expectedBlock) throws IOException, PortUnreachableException, ClientAbortedTransferException, UnknownTransferIDException {
		int transferAttempt = 0;
		short receivedBlock = -1;
		
		byte[] buf = new byte[server.PACKET_SIZE];
		DatagramPacket ackPacket = new DatagramPacket(buf, buf.length);
		ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
		do {
            transferAttempt ++;
            if(transferAttempt > server.MAX_TRANSFER_ATTEMPTS)
				return false;
			try {
				doControls(dataPacket.getPort(), socket.getPort(), byteBuffer.getShort(0));

				socket.send(dataPacket);
				socket.receive(ackPacket);

                receivedBlock = byteBuffer.getShort(2);
			}
			catch(SocketTimeoutException e) {
				System.err.println("Attempt " + transferAttempt + ": ack receive timed out");
			}
		}
		while(receivedBlock != expectedBlock);
		
		return true;
	}
	
	// Client PUT
	boolean receiveDataSendAck(TFTPServer server, DatagramPacket dataPacket, DatagramSocket socket, short expectedBlock) throws IOException, ClientAbortedTransferException, UnknownTransferIDException {
		
		int transferAttempt = 0;
		ByteBuffer dataBB = ByteBuffer.wrap(dataPacket.getData());
		short receivedBlock = -1, receivedOpCode = -1;
		
		while(true) {
			transferAttempt ++;
			if(transferAttempt > server.MAX_TRANSFER_ATTEMPTS)
				return false;
			try {
				//TODO (hard) times out if ack packet wasn't received by client and the client is waiting for an ack while server is waiting for data at the same time. test by disabling socket.send()
				socket.receive(dataPacket);

				receivedOpCode = dataBB.getShort(0);
				receivedBlock = dataBB.getShort(2);
				
				doControls(dataPacket.getPort(), socket.getPort(), receivedOpCode);

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
	
	private void sendAck(DatagramSocket socket, short block) throws IOException {
		final int HEADER_LENGTH = 4;
		ByteBuffer ackBB = ByteBuffer.allocate(HEADER_LENGTH);

		ackBB.putShort(0, TFTPServer.OP_ACK);
		ackBB.putShort(2, block);
		
		DatagramPacket ackPacket = new DatagramPacket(ackBB.array(), ackBB.array().length);
				
		socket.send(ackPacket);
	}

	void sendHandShake(DatagramSocket socket) throws IOException {
		final short HANDSHAKE_BLOCK = 0;
		sendAck(socket, HANDSHAKE_BLOCK);
	}

	private void doControls(int sourceTID, int destinationTID, short opCode) throws UnknownTransferIDException, ClientAbortedTransferException {
		if(sourceTID != destinationTID)
			throw new UnknownTransferIDException();

		if(opCode == TFTPServer.OP_ERR)
			throw new ClientAbortedTransferException();
	}
	
}