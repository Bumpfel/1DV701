package Assignment1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPEchoClient extends NetworkLayer {
	private final boolean VERBOSE_MODE = true; // prints information about every packet
	private Socket socket;
	private InputStream in;
	private OutputStream out;
		
	public TCPEchoClient(String[] args) {
		super(args, "TCP");
	}
	
	public static void main(String[] args) {
		NetworkLayer client = new TCPEchoClient(args);
		
		String msg = client.createPacket(100); // creates a msg of length [arg]
		client.send(msg);
	}
	
	public void send(String packet) {
		// Set up connection
		try {
			socket = new Socket(destinationIP, destinationPort);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			
			validatePacket(packet);
			System.out.println("--Msg size is " + packet.length() + ", and buffer size is " + clientBufferSize + ". Verbose mode is " + (VERBOSE_MODE ? "on" : "off"));
			
			long timestamp = System.currentTimeMillis();
			for(int i = 0; i < msgTransferRate; i ++) { // loop for each message (transfer rate)
				checkMaxTime(timestamp, i, msgTransferRate);
				
				// Send
				out.write(packet.getBytes()); // sends message as byte array
				if(VERBOSE_MODE)
					System.out.println("Packet #" + (i + 1)+ ": " + packet.length() + " byte(s) sent");
				
				// Receive echo
				int bytesReceived = 0;
				String receivedString = new String();
				byte[] buf = new byte[clientBufferSize];
//				do {
				while(receivedString.length() < packet.length()) { // loop until the received echo message matches the original
					bytesReceived = in.read(buf); // receive echo, put it in a buffer
					receivedString += new String(buf, 0, bytesReceived); // pieces together the message by appending the buffer to a string. does not add empty or old data from the buffer array
					if(VERBOSE_MODE)
						System.out.println(" " + bytesReceived + " bytes received");
				}
				
				// Done
				validatePacketIntegrityAndPrintResults(packet, receivedString, i);
				
			}
			socket.close();
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

}