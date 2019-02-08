package Assignment1;

public abstract class NetworkLayer {
	protected String destinationIP;
	protected int destinationPort;
	protected int msgTransferRate; // messages per second
	protected int clientBufferSize; // bytes
	protected final String MSG;
	protected final int MAX_PACKET_SIZE;
	
	public NetworkLayer(String[] args, int msgSize, String type) {
		MSG = createPacket(msgSize);
		if(type.equals("UDP"))
			MAX_PACKET_SIZE = 65507;
		else 
			MAX_PACKET_SIZE = 65535;
		
		validateArgs(args, MSG, MAX_PACKET_SIZE);
		
		destinationIP = args[0];
		destinationPort = Integer.valueOf(args[1]);
		msgTransferRate = Integer.valueOf(args[2]);
		clientBufferSize = Integer.valueOf(args[3]);		
		
	}
	
	protected void validateArgs(String[] args, String packet, int MAX_PACKET_SIZE) {
		int expectedArgs = 4;
		try {
			if(args.length != expectedArgs)
				throw new IllegalArgumentException("Incorrect launch commands.\nUsage: server_name port message_transfer_rate client_buffer_size");
			else if(Integer.parseInt(args[2]) < 0)
				throw new IllegalArgumentException("Message transfer rate cannot be less than 0");
			else if(packet.length() > MAX_PACKET_SIZE)
				throw new IllegalArgumentException("Maximum TCP packet size exceeded");
			else if(packet.length() == 0)
				throw new IllegalArgumentException("Message cannot be empty");
	
			validateIP(args[0]);
		}
		catch(NumberFormatException e) {
			System.err.println("Program arguments in the wrong format");
			System.exit(-1);
		}
		catch(IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}

	private void validateIP(String IPAddress) throws IllegalArgumentException {
		final String MSG = "Invalid IP Address";

		// check if address has 4 segments, separated by a dot
		String[] Segments = IPAddress.split("\\.");
		if(Segments.length != 4)
			throw new IllegalArgumentException(MSG);
		
		// check IP segment ranges
		for(int i = 0; i < 4; i ++) {
			int IPSegment = Integer.parseInt(Segments[i]);
			if((IPSegment < 0 || IPSegment > 255) || (i == 3 && (IPSegment <= 0 || IPSegment >= 255))) {
				throw new IllegalArgumentException(MSG);
			}
		}
	}
	
	/**
	 * Creates a packet with specified length taken from a random text
	 * @param size
	 * @return
	 */
	protected String createPacket(int size) {
		String text = "The first assignment is dedicated to UDP/TCP socket programming with Java and testing your programs in a virtual networking environment. You will use provided starter code for UDP echo server and client, improve it and test your implementation in a setting where server and client programs are executed on different machines connected in a network.";
		return text.substring(0, size);
	}
	
	protected void validatePacketIntegrityAndPrintResults(String sentPacket, String receivedPacket) {
		System.out.println("In total " + sentPacket.length() + " byte(s) sent and " + receivedPacket.length() + " byte(s) received");
		
		if (receivedPacket.compareTo(sentPacket) != 0) {
			System.out.printf("Sent and received msg not equal!\n");
			System.exit(-1);
		}
	}
	
	/**
	 * Makes sure max time is not exceeded
	 * @param timestamp
	 * @param iteration
	 * @param transferRate
	 */
	protected void checkMaxTime(long timestamp, int iteration, int transferRate) {
		final int MAX_DURATION = 1000;
		if(System.currentTimeMillis() > timestamp + MAX_DURATION) {
			System.err.println(transferRate - iteration + " messages was not sent");
			System.exit(-1);
		}
	}
	
}
