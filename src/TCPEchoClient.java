import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class TCPEchoClient {
//	public static final int BUFSIZE = 1024;
	public static final int MYPORT= 0;
	private static final int MAX_TCP_PACKET_SIZE = 65535;
	public static final String MSG= "An Echo Message! An Echo Message! An Echo Message! An Echo Message! An Echo Message! An Echo Message! An Echo Message!";

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

//			SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
//			SocketAddress remoteBindPoint = new InetSocketAddress(destinationIP, port);

			byte[] buf = new byte[clientBufferSize];
			if (args.length != 4) {
				System.err.printf("usage: %s server_name port\n message_transfer_rate client_buffer_size", args[1]);
				System.exit(1);
			}

			Socket socket = new Socket(destinationIP, port);
//			Socket socket = new Socket();
//			socket.bind(localBindPoint);
//			socket.connect(remoteBindPoint, 1000);

			InputStream in = new DataInputStream(socket.getInputStream());
			OutputStream out = new DataOutputStream(socket.getOutputStream());

			for(int i = 0; i < msgTransferRate; i ++) {
				out.write(MSG.getBytes()); // send as byte[]

				String receivedString = new String();
				do {
					in.read(buf); // receive echo
					receivedString += new String(buf).trim();

				}
				while(receivedString.length() < MSG.length());
				
				System.out.println(MSG.getBytes().length + " bytes sent and " + receivedString.length() + " bytes received");
				if (receivedString.compareTo(MSG) != 0)
					System.out.println("Sent and received msg not equal!: ");

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
}