package Assignment1;

public abstract class NetworkLayer {
	protected String destinationIP;
	protected int destinationPort;
	protected int msgTransferRate; // messages per second
	protected int clientBufferSize; // bytes
	protected final int MAX_PACKET_SIZE;
	protected final String TYPE; // for printing purposes

	public NetworkLayer(String[] args, String type) {
		if(type.equals("UDP")) {
			MAX_PACKET_SIZE = 65507;
			TYPE = type;
		}
		else if(type.equals("TCP")) {
			MAX_PACKET_SIZE = 65535;
			TYPE = type;
		}
		else
			throw new IllegalArgumentException("Protocol type not supported");

		// validates all arguments before binding them to variables
		validateArgs(args);

		destinationIP = args[0];
		destinationPort = Integer.valueOf(args[1]);
		msgTransferRate = Integer.valueOf(args[2]);
		clientBufferSize = Integer.valueOf(args[3]);
	}
	
	public abstract void send(String packet);

	protected void validateArgs(String[] args) {
		int expectedArgs = 4;
		
		try {
			if(args.length != expectedArgs)
				throw new IllegalArgumentException("Incorrect launch commands.\nUsage: server_name port message_transfer_rate client_buffer_size");
			else if(Integer.parseInt(args[2]) < 0)
				throw new IllegalArgumentException("Message transfer rate cannot be less than 0");

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
			if((IPSegment < 0 || IPSegment > 255)) { // || (i == 3 && (IPSegment <= 0 || IPSegment >= 255))) {
				throw new IllegalArgumentException(MSG);
			}
		}
	}

	protected void validatePacket(String packet) {
		try {
			if(packet.length() == 0)
				throw new IllegalArgumentException("Message cannot be empty");
			else if(packet.length() > MAX_PACKET_SIZE)
				throw new IllegalArgumentException("Maximum " + TYPE + " packet size exceeded. Is " + packet.length() + ", maximum packet size is: " + MAX_PACKET_SIZE);
		}
		catch(IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}

	/**
	 * Creates a packet with specified length taken from a random text
	 * @param size - length of the msg
	 * @return msg string
	 */
	protected String createPacket(int size) {
		String text = "The first assignment is dedicated to UDP/TCP socket programming with Java and testing your programs in a virtual networking environment. You will use provided starter code for UDP echo server and client, improve it and test your implementation in a setting where server and client programs are executed on different machines connected in a network.";
		return text.substring(0, size);
	}

	protected void validatePacketIntegrityAndPrintResults(String sentPacket, String receivedPacket, int packetNumber) {
		System.out.println("Packet #" + packetNumber + ": In total " + sentPacket.length() + " byte(s) sent and " + receivedPacket.length() + " byte(s) received");

		if (receivedPacket.compareTo(sentPacket) != 0) {
			System.out.printf("Sent and received msg not equal!\n");
//			System.exit(-1);
		}
	}

	/**
	 * Makes sure max time is not exceeded
	 * @param timestamp - time when started (static value)
	 * @param iteration - how many messages has been sent (dynamic value)
	 * @param transferRate - decides how many messages are attemped to be sent
	 */
	protected void checkMaxTime(long timestamp, int iteration, int transferRate) {
		final int MAX_DURATION = 1000;
		if(System.currentTimeMillis() > timestamp + MAX_DURATION) {
			System.err.println("Maximum time exceeded");
			System.err.println(transferRate - iteration + " messages was not sent");
			System.exit(-1);
		}
	}

}
