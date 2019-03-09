package assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class TransferHandler {

    // Client GET
	boolean send_DATA_receive_ACK(DatagramPacket dataPacket, DatagramSocket socket, short expectedBlock) throws IOException {
		int receivedBlock = 0, transferAttempt = 0;
		
		byte[] buf = new byte[TFTPServer.BUFSIZE];
		DatagramPacket ackPacket = new DatagramPacket(buf, buf.length);
		ByteBuffer bb = ByteBuffer.wrap(buf);
		do {
            transferAttempt ++;
            if(transferAttempt >= TFTPServer.MAX_RETRANSMIT_ATTEMPTS) {
				//TODO send_ERR to stop the client
				return false;
			}
			if(transferAttempt > 1)
				System.err.println("Re-sending packet");
			try {
				socket.send(dataPacket);
				socket.receive(ackPacket);
                receivedBlock = bb.getShort(2); // byte 2 and 3
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
	boolean receive_DATA_send_ACK(DatagramPacket dataPacket, DatagramSocket socket) throws IOException {
		System.out.println("waiting on data");

		int transferAttempt = 0;
		ByteBuffer bb = ByteBuffer.allocate(4);
		ByteBuffer bb2 = ByteBuffer.wrap(dataPacket.getData()); //rename
		short receivedBlock = 0, receivedOpCode = -1;

		while(true) { //TODO handle stuff when client aborts. sends block 0, but maybe it also sends something else?
			transferAttempt ++;
			if(transferAttempt == TFTPServer.MAX_RETRANSMIT_ATTEMPTS)
				return false;
			try {
				//TODO times out if ack packet wasn't received by client and the client is waiting for an ack while server is waiting for data at the same time
				socket.receive(dataPacket);

				receivedOpCode = bb2.getShort(0);
				receivedBlock = bb2.getShort(2);
				
				System.out.println("RECEIVED " + dataPacket.getLength() + " B. BLOCK=" + receivedBlock + ", OP_CODE=" + receivedOpCode);
				
				//interrupted by client
                if(isErroneous(receivedOpCode))
                    return false;
				
				bb.putShort(TFTPServer.OP_ACK);
				bb.putShort((short) receivedBlock);
				
				System.out.println("sending ack. block " + receivedBlock); // TODO debug
				
				DatagramPacket ackPacket = new DatagramPacket(bb.array(), bb.array().length);
                
				socket.send(ackPacket);
				System.out.println("----");
                
				break;
			}
			catch(SocketTimeoutException e) {
				System.err.println("Attempt " + transferAttempt + ": receive timed out"); // debug
			}
			catch(ArrayIndexOutOfBoundsException e) { // not sure if necessary. should only happen if client sends faulty packet or it becomes corrupted
				System.out.println("Server error");
				return false;
			}
		}

		return true;
    }
    

    boolean isErroneous(short opCode) {
        return opCode == TFTPServer.OP_ERR;

    }
}