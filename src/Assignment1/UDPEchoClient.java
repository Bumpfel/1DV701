package Assignment1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

public class UDPEchoClient {
	public static final int MYPORT = 0;
	private static final int MAX_UDP_PACKET_SIZE = 65507;
	public static final String MSG = "An Echo Message! An Echo Message! An Echo Message! An Echo Message!";

	public static void main(String[] args) throws IOException, InterruptedException {
		try {
			validateIP(args[0]);

			int msgTransferRate = Integer.valueOf(args[2]); // messages per second
			int clientBufferSize = Integer.valueOf(args[3]); // bytes

			validateMsgTransferRate(msgTransferRate);
			validatePacketSize(MSG.length());

			byte[] buf = new byte[clientBufferSize];
			if (args.length != 4) {
				System.err.printf("usage: %s server_name port\n message_transfer_rate client_buffer_size", args[1]);
				System.exit(1);
			}

			/* Create socket */
			DatagramSocket socket = new DatagramSocket(null);

			/* Create local endpoint using bind() */
			SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
			socket.bind(localBindPoint);

			/* Create remote endpoint */
			SocketAddress remoteBindPoint = new InetSocketAddress(args[0], Integer.valueOf(args[1]));

			/* Create datagram packet for sending message */
			DatagramPacket sendPacket = new DatagramPacket(MSG.getBytes(), MSG.length(), remoteBindPoint);

			/* Create datagram packet for receiving echoed message */
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

			// loop for each message
			for(int i = 0; i < msgTransferRate; i ++) {				
				/* Send and receive message*/
				socket.send(sendPacket);
				socket.receive(receivePacket);

				/* Compare sent and received message */
				String receivedString =
						new String(receivePacket.getData(),
								receivePacket.getOffset(),
								receivePacket.getLength());

//				System.out.printf("%d bytes sent and received\n", receivePacket.getLength());
				System.out.println(MSG.getBytes().length + " bytes sent and " + receivedString.length() + " bytes received");
				if (receivedString.compareTo(MSG) != 0) {
					System.out.printf("Sent and received msg not equal!\n");
//					break;
				}

				// Delay
				int sleepTime = 1000;
				if(msgTransferRate > 0)
					sleepTime = 1000 / msgTransferRate;
				Thread.sleep(sleepTime);
			}
			socket.close();
		}
		catch(NumberFormatException e) {
			System.err.println("Arguments in the wrong format");
		}
		catch(IllegalArgumentException | SocketException e) {
			System.err.println(e.getMessage());
		}
	}

	private static void validateMsgTransferRate(int transferRate) {	
		if(transferRate < 0)
			throw new IllegalArgumentException("Message transfer rate cannot be less than 0");
	}

	private static void validatePacketSize(int packetSz) {
		if(packetSz > MAX_UDP_PACKET_SIZE)
			throw new IllegalArgumentException("Maximum UDP packet size exceeded");
	}

	private static void validateIP(String IP) throws IllegalArgumentException {
		final String MSG = "Invalid IP Address";

		String[] IPGroups = IP.split("\\.");
		if(IPGroups.length != 4)
			throw new IllegalArgumentException(MSG);
		for(int i = 0; i < 4; i ++) {
			try {
				int IPint = Integer.parseInt(IPGroups[i]);
				if((IPint < 0 || IPint > 255) || (i == 3 && (IPint <= 0 || IPint >= 255))) {
					throw new IllegalArgumentException(MSG);
				}
			}
			catch(NumberFormatException e) {
				throw new IllegalArgumentException(MSG);
			}
		}
	}
}