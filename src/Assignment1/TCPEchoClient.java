package Assignment1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class TCPEchoClient extends NetworkLayer {
	public static final int MYPORT = 0;
	private static final int MAX_TCP_PACKET_SIZE = 65535;
	private static final int MSG_SIZE = 59;
	private static final boolean VERBOSE_MODE = false; // prints information about every packet
	
	public static void main(String[] args) throws IOException {
		try {
			// handles program args
			String destinationIP = args[0];
			int port = Integer.valueOf(args[1]);
			int msgTransferRate = Integer.valueOf(args[2]); // messages per second
			int clientBufferSize = Integer.valueOf(args[3]); // bytes

			// Create message of specific size
			final String MSG = createPacket(MSG_SIZE);
			
			// check validity of program args
			validateIP(destinationIP);
			validateMsgTransferRate(msgTransferRate);
			validatePacketSize(MSG.length(), MAX_TCP_PACKET_SIZE);
			validateMessageSize(MSG.length());

			if (args.length != 4) {
				System.err.println("Incorrect launch commands. Usage: server_name port message_transfer_rate client_buffer_size");
				System.exit(1);
			}

			// Set up connection
//			while(true) { // uncomment to run forever, using a new socket
			Socket socket = new Socket(destinationIP, port);
			InputStream in = new DataInputStream(socket.getInputStream());
			OutputStream out = new DataOutputStream(socket.getOutputStream());

			System.out.println("Msg size is " + MSG.length() + ", and buffer size is " + clientBufferSize);
			for(int i = 0; i < msgTransferRate; i ++) { // loop for transfer rate
				// Send
				out.write(MSG.getBytes()); // sends message as byte array
				if(VERBOSE_MODE)
					System.out.println(MSG.getBytes().length + " bytes sent");
				
				// Receive echo
				int bytesReceived = 0;
				String receivedString = new String();
				byte[] buf = new byte[clientBufferSize];
				do {
					bytesReceived = in.read(buf); // receive echo, put it in a buffer
					receivedString += new String(buf, 0, bytesReceived); // piece together the message by appending the buffer to a string. do not add empty slots from the buffer array
					if(VERBOSE_MODE)
						System.out.println(bytesReceived + " bytes received");
				}
				while(receivedString.getBytes().length < MSG.getBytes().length); // loop until the received echo message matches the original
				
				// Done
				System.out.println("In total " + MSG.getBytes().length + " bytes sent and " + receivedString.getBytes().length + " bytes received");
				if (receivedString.compareTo(MSG) != 0)
					System.out.println("Sent and received msg not equal!");

				// Delay
				int sleepTime = 1000;
				if(msgTransferRate > 0)
					sleepTime /= msgTransferRate;
				try {
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e) {
				}
//			}
			socket.close();
			}
		}
		catch(NumberFormatException e) { // failing to parse an argument as an integer
			System.err.println("Arguments in the wrong format");
		}
		catch(IllegalArgumentException | SocketException e) {
			System.err.println(e.getMessage());
		}
	}


}