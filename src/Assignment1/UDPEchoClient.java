package Assignment1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class UDPEchoClient extends NetworkLayer {
	private static final int MYPORT = 0;

	public UDPEchoClient(String[] args) {
		super(args, 64, "UDP");
	}
	
	public static void main(String[] args) {
		UDPEchoClient udpClient = new UDPEchoClient(args);

		byte[] buf = new byte[udpClient.CLIENT_BUFFER_SIZE];

		/* Create socket */
		try(DatagramSocket socket = new DatagramSocket(null)) {

			SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
			socket.bind(localBindPoint);
			SocketAddress remoteBindPoint = new InetSocketAddress(args[0], Integer.valueOf(args[1]));

			DatagramPacket sendPacket = new DatagramPacket(udpClient.MSG.getBytes(), udpClient.MSG.length(), remoteBindPoint);
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

			System.out.println("Msg size is " + udpClient.MSG.length() + ", and buffer size is " + udpClient.CLIENT_BUFFER_SIZE);

			long timestamp = System.currentTimeMillis();
			// loop for each message
			for(int i = 0; i < udpClient.MSG_TRANSFER_RATE; i ++) {
				udpClient.checkMaxTime(timestamp,i, udpClient.MSG_TRANSFER_RATE);

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