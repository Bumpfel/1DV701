package Assignment1;

public class NetworkLayer { //TODO remove static?
	protected static final boolean VERBOSE_MODE = true; // prints information about every packet
	
	protected static void validateArgs(String[] args, int expectedArgs, String usage, String packet, int MAX_PACKET_SIZE) {
		try {
			if(args.length != expectedArgs)
				throw new IllegalArgumentException(usage);
			else if(Integer.parseInt(args[2]) < 0)
				throw new IllegalArgumentException("Message transfer rate cannot be less than 0");
			else if(packet.length() > MAX_PACKET_SIZE)
				throw new IllegalArgumentException("Maximum TCP packet size exceeded");
			else if(packet.length() == 0)
				throw new IllegalArgumentException("Message cannot be empty");
	
			validateIP(args[0]);
		}
		catch(NumberFormatException e) {
			System.err.println("Arguments in the wrong format");
		}
		catch(IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}
	}

	private static void validateIP(String IPAddress) throws IllegalArgumentException {
		final String MSG = "Invalid IP Address";

		String[] Segments = IPAddress.split("\\.");
		if(Segments.length != 4)
			throw new IllegalArgumentException(MSG);
		
		for(int i = 0; i < 4; i ++) {
			int IPSegment = Integer.parseInt(Segments[i]);
			if((IPSegment < 0 || IPSegment > 255) || (i == 3 && (IPSegment <= 0 || IPSegment >= 255))) {
				throw new IllegalArgumentException(MSG);
			}
		}
	}
	
	protected static String createPacket(int size) {
		String text = "The first assignment is dedicated to UDP/TCP socket programming with Java and testing your programs in a virtual networking environment. You will use provided starter code for UDP echo server and client, improve it and test your implementation in a setting where server and client programs are executed on different machines connected in a network.";
		return text.substring(0, size);
	}
}
