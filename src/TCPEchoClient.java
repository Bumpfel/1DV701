import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class TCPEchoClient {
	public static final int BUFSIZE= 1024;
	public static final int MYPORT= 0;
	public static final String MSG= "An Echo Message!";
	
	public static void main(String[] args) throws IOException, InterruptedException {
		try {
			// handles program args
			String destinationIP = args[0];
			int port = Integer.valueOf(args[1]);
			int msgTransferRate = Integer.valueOf(args[2]); // messages per second
			int clientBufferSize = Integer.valueOf(args[3]); // bytes
			
			
			// check validity of program args
			validateIP(destinationIP);
			validateMsgTransferRate(msgTransferRate);
//			validatePacketSize(MSG.length());

			SocketAddress localBindPoint = new InetSocketAddress(MYPORT);
			SocketAddress remoteBindPoint = new InetSocketAddress(destinationIP, port);

			byte[] buf= new byte[clientBufferSize];
			if (args.length != 4) {
				System.err.printf("usage: %s server_name port\n message_transfer_rate client_buffer_size", args[1]);
				System.exit(1);
			}

			Socket socket = new Socket(destinationIP, port);
			do {
				
			}
			while(true);
//			socket.close();
		}
		catch(NumberFormatException e) {
			System.err.println("Arguments in the wrong format");
		}
		catch(IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}
		catch(SocketException e) {
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
	
	private static void validateIP(String IP) throws IllegalArgumentException { // DEBUG IS ON ERR MESSAGES
		final String MSG = "Invalid IP Address";

		String[] IPGroups = IP.split("\\.");
		if(IPGroups.length != 4)
			throw new IllegalArgumentException(MSG + 1);
		for(int i = 0; i < 4; i ++) {
			try {
				int IPint = Integer.parseInt(IPGroups[i]);
				if((IPint < 0 || IPint > 255) || (i == 3 && (IPint <= 0 || IPint >= 255))) {
					throw new IllegalArgumentException(MSG + 2);
				}
			}
			catch(NumberFormatException e) {
				throw new IllegalArgumentException(MSG + 3);
			}
		}
	}
}