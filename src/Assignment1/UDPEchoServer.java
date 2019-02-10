package Assignment1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class UDPEchoServer {
	public final int BUFSIZE = 64;
	public final int MYPORT = 4950;

	public static void main(String[] args) {
		UDPEchoServer udpServer = new UDPEchoServer();
		
		udpServer.startServer(args);
	}

	public void startServer(String[] args) {
		byte[] buf = new byte[BUFSIZE];
		
		try(DatagramSocket socket = new DatagramSocket(null)) {
			/* Create socket */
			System.out.print("Server started on port " + MYPORT + ". ");
			System.out.println("Buffer size is " + buf.length + " bytes. ");

			/* Create local bind point */
			SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
			socket.bind(localBindPoint);


			while(true) {
				/* Create datagram packet for receiving message */
				DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

				/* Receiving message */
				socket.receive(receivePacket);

				/* Create datagram packet for sending message */
				DatagramPacket sendPacket =
						new DatagramPacket(receivePacket.getData(),
								receivePacket.getLength(),
								receivePacket.getAddress(),
								receivePacket.getPort());

				/* Send message*/
				socket.send(sendPacket);
				System.out.print("UDP echo request from " + receivePacket.getAddress().getHostAddress() + ". " + receivePacket.getLength() + " bytes received and sent");
				System.out.println(" using port " + receivePacket.getPort());
			}

		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}
}