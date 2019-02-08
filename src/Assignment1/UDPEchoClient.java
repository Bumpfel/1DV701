package Assignment1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

public class UDPEchoClient extends NetworkLayer {
	public static final int MYPORT = 0;
	private static final int MAX_UDP_PACKET_SIZE = 65507;
	private static final int MSG_SIZE = 100;

	public static void main(String[] args) {
		// Create message of specific size
		final String MSG = createPacket(MSG_SIZE);

		validateArgs(args, MSG.getBytes(), MAX_UDP_PACKET_SIZE);

		int msgTransferRate = Integer.valueOf(args[2]); // messages per second
		int clientBufferSize = Integer.valueOf(args[3]); // bytes

		byte[] buf = new byte[clientBufferSize];

		/* Create socket */
		try(DatagramSocket socket = new DatagramSocket(null)) {

			/* Create local endpoint using bind() */
			SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
			socket.bind(localBindPoint);

			/* Create remote endpoint */
			SocketAddress remoteBindPoint = new InetSocketAddress(args[0], Integer.valueOf(args[1]));

			/* Create datagram packet for sending message */
			DatagramPacket sendPacket = new DatagramPacket(MSG.getBytes(), MSG.length(), remoteBindPoint);

			/* Create datagram packet for receiving echoed message */
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

			System.out.println("Msg size is " + MSG.length() + ", and buffer size is " + clientBufferSize);

			long timestamp = System.currentTimeMillis();
			// loop for each message
			for(int i = 0; i < msgTransferRate; i ++) {
				checkMaxTime(timestamp,i, msgTransferRate);

				/* Send and receive message*/
				socket.send(sendPacket);
				socket.receive(receivePacket);

				/* Compare sent and received message */
				String receivedString =
						new String(receivePacket.getData(),
								receivePacket.getOffset(),
								receivePacket.getLength());

				validatePacketIntegrityAndPrintResults(MSG, receivedString);

			}
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}
}