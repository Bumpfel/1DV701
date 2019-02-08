package Assignment1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TCPEchoClient extends NetworkLayer {
	private static final int MAX_TCP_PACKET_SIZE = 65535;
	private static final int MSG_SIZE = 150;
	
	public static void main(String[] args) {
		// Create message of specific size
		final String MSG = createPacket(MSG_SIZE);

		validateArgs(args, MSG.getBytes(), MAX_TCP_PACKET_SIZE);

		// handles program args
		String destinationIP = args[0];
		int port = Integer.valueOf(args[1]);
		int msgTransferRate = Integer.valueOf(args[2]); // messages per second
		int clientBufferSize = Integer.valueOf(args[3]); // bytes

		// Set up connection
		try {
			Socket socket = new Socket(destinationIP, port);
			InputStream in = new DataInputStream(socket.getInputStream());
			OutputStream out = new DataOutputStream(socket.getOutputStream());

			System.out.println("Msg size is " + MSG.length() + ", and buffer size is " + clientBufferSize);
			
			long timestamp = System.currentTimeMillis();
			for(int i = 0; i < msgTransferRate; i ++) { // loop for each message (transfer rate)
				checkMaxTime(timestamp, i, msgTransferRate);
				
				// Send
				out.write(MSG.getBytes()); // sends message as byte array
				if(VERBOSE_MODE)
					System.out.println(MSG.getBytes().length + " byte(s) sent");
				
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
				validatePacketIntegrityAndPrintResults(MSG, receivedString);

			}
			socket.close();
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

}