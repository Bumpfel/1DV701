package Assignment1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class UDPEchoClient extends NetworkLayer {
	private static final int MYPORT = 0;

	public UDPEchoClient(String[] args) {
		super(args, "UDP");
	}
	
	public static void main(String[] args) {
		NetworkLayer client = new UDPEchoClient(args);
		
		String msg = client.createPacket(50);
		client.send(msg);
	}
	
	public void send(String packet) {
		byte[] buf = new byte[clientBufferSize];

		try(DatagramSocket socket = new DatagramSocket(null)) {

			SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
			socket.bind(localBindPoint);
			SocketAddress remoteBindPoint = new InetSocketAddress(destinationIP, destinationPort);

			DatagramPacket sendPacket = new DatagramPacket(packet.getBytes(), packet.length(), remoteBindPoint);
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

			System.out.println("--Msg size is " + packet.length() + ", and buffer size is " + clientBufferSize);

			long timestamp = System.currentTimeMillis();
			// loop for each message
			for(int i = 0; i < msgTransferRate; i ++) {
				checkMaxTime(timestamp,i, msgTransferRate);

				/* Send and receive message*/
				socket.send(sendPacket);
				socket.receive(receivePacket);

				String receivedString =
						new String(receivePacket.getData(),
								receivePacket.getOffset(),
								receivePacket.getLength());

				/* Compare sent and received message */
				validatePacketIntegrityAndPrintResults(packet, receivedString, i);

			}
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}
}