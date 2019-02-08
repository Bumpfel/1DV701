package Assignment1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

public class UDPEchoClient extends NetworkLayer {
	private final int MYPORT = 0;

	public UDPEchoClient(String[] args) {
		super(args, 100, "UDP");
	}
	
	public static void main(String[] args) {
		UDPEchoClient udpClient = new UDPEchoClient(args);

		byte[] buf = new byte[udpClient.clientBufferSize];

		/* Create socket */
		try(DatagramSocket socket = new DatagramSocket(null)) {

			/* Create local endpoint using bind() */
			SocketAddress localBindPoint = new InetSocketAddress(udpClient.MYPORT);
			socket.bind(localBindPoint);

			/* Create remote endpoint */
			SocketAddress remoteBindPoint = new InetSocketAddress(args[0], Integer.valueOf(args[1]));

			/* Create datagram packet for sending message */
			DatagramPacket sendPacket = new DatagramPacket(udpClient.MSG.getBytes(), udpClient.MSG.length(), remoteBindPoint);

			/* Create datagram packet for receiving echoed message */
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

			System.out.println("Msg size is " + udpClient.MSG.length() + ", and buffer size is " + udpClient.clientBufferSize);

			long timestamp = System.currentTimeMillis();
			// loop for each message
			for(int i = 0; i < udpClient.msgTransferRate; i ++) {
				udpClient.checkMaxTime(timestamp,i, udpClient.msgTransferRate);

				/* Send and receive message*/
				socket.send(sendPacket);
				socket.receive(receivePacket);

				/* Compare sent and received message */
				String receivedString =
						new String(receivePacket.getData(),
								receivePacket.getOffset(),
								receivePacket.getLength());

				udpClient.validatePacketIntegrityAndPrintResults(udpClient.MSG, receivedString);

			}
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}
}