package Assignment1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPEchoClient extends NetworkLayer {
	private static final boolean VERBOSE_MODE = false; // if true, prints information about every packet segment
	
	public TCPEchoClient(String[] args) {
		super(args, 100, "TCP");
	}
	
	public static void main(String[] args) {
		TCPEchoClient tcpClient = new TCPEchoClient(args);

		// Set up connection
		try(Socket socket = new Socket(tcpClient.DESTINATION_IP, tcpClient.DESTINATION_PORT)) {
			InputStream in = new DataInputStream(socket.getInputStream());
			OutputStream out = new DataOutputStream(socket.getOutputStream());

			System.out.println("Msg size is " + tcpClient.MSG.length() + ", and buffer size is " + tcpClient.CLIENT_BUFFER_SIZE + ". Verbose mode is " + (VERBOSE_MODE ? "on" : "off"));
			
			long timestamp = System.currentTimeMillis();
			for(int i = 0; i < tcpClient.MSG_TRANSFER_RATE; i ++) { // loop for each message (transfer rate)
				tcpClient.checkMaxTime(timestamp, i, tcpClient.MSG_TRANSFER_RATE);
				
				// Send
				out.write(tcpClient.MSG.getBytes()); // sends message as byte array
				if(VERBOSE_MODE)
					System.out.println(tcpClient.MSG.getBytes().length + " byte(s) sent");
				
				// Receive echo
				int bytesReceived = 0;
				String receivedString = new String();
				byte[] buf = new byte[tcpClient.CLIENT_BUFFER_SIZE];
				do {
					bytesReceived = in.read(buf); // receive echo, put it in a buffer
					receivedString += new String(buf, 0, bytesReceived); // piece together the message by appending the buffer to a string. do not add empty slots from the buffer array
					if(VERBOSE_MODE)
						System.out.println(bytesReceived + " byte(s) received");
				}
				while(receivedString.getBytes().length < tcpClient.MSG.getBytes().length); // loop until the received echo message matches the original
				
				// Done
				tcpClient.validatePacketIntegrityAndPrintResults(tcpClient.MSG, receivedString);

			}
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

}