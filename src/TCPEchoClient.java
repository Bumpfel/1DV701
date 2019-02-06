import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class TCPEchoClient {
	public static final int MYPORT = 0;
	private static final int MAX_TCP_PACKET_SIZE = 65535;
	public static String MSG = "An Echo Message! An Echo Message! An Echo Message! An Echo Message! An Echo Message! An Echo Message! An Echo Message!";
	private static final int MSG_SIZE = 45;
//	public static String MSG = "An Echo Message!";
	
	public static void main(String[] args) throws IOException {
		try {
			// handles program args
			String destinationIP = args[0];
			int port = Integer.valueOf(args[1]);
			int msgTransferRate = Integer.valueOf(args[2]); // messages per second
			int clientBufferSize = Integer.valueOf(args[3]); // bytes

			// check validity of program args
			validateIP(destinationIP);
			validateMsgTransferRate(msgTransferRate);
			validatePacketSize(MSG.length());
			
			// Msg size
			createPacket(MSG_SIZE);

			if (args.length != 4) {
				System.err.printf("usage: %s server_name port\n message_transfer_rate client_buffer_size", args[1]);
				System.exit(1);
			}

			Socket socket = new Socket(destinationIP, port);
			
			InputStream in = new DataInputStream(socket.getInputStream());
			OutputStream out = new DataOutputStream(socket.getOutputStream());

			for(int i = 0; i < msgTransferRate; i ++) {
				out.write(MSG.getBytes()); // send as byte[]

				int bytesReceived = 0;
				String receivedString = new String();
				System.out.println("# Msg length is " + MSG.length() + ", and buffer size is " + clientBufferSize);
				do {
					byte[] buf = new byte[clientBufferSize];
					bytesReceived = in.read(buf); // receive echo
					receivedString += new String(buf, 0, bytesReceived);
//					System.out.print(new String(buf)); // DEBUG -------------------------------------------------------------------------------------
//					System.out.println(" - received " + bytesReceived + " bytes"); // DEBUG ---------------------------------------------------------
				}
				while(receivedString.length() < MSG.length());
				
				System.out.println(MSG.getBytes().length + " bytes sent and " + receivedString.length() + " bytes received");
				if (receivedString.compareTo(MSG) != 0)
					System.out.println("Sent and received msg not equal!");

				int sleepTime = 1000;
				if(msgTransferRate > 0)
					sleepTime /= msgTransferRate;
				try {
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e) {
				}
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
		if(packetSz > MAX_TCP_PACKET_SIZE)
			throw new IllegalArgumentException("Maximum TCP packet size exceeded");
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
	
	private static void createPacket(int size) {
		String text = "The first assignment is dedicated to UDP/TCP socket programming with Java and testing your programs in a virtual networking environment. You will use provided starter code for UDP echo server and client, improve it and test your implementation in a setting where server and client programs are executed on different machines connected in a network.";
		MSG = text.substring(0, size);
	}
}