package Assignment1;

public class NetworkLayer {
	protected static void validateMsgTransferRate(int transferRate) {	
		if(transferRate < 0)
			throw new IllegalArgumentException("Message transfer rate cannot be less than 0");
	}

	protected static void validatePacketSize(int packetSz, final int MAX_PACKET_SIZE) {
		if(packetSz > MAX_PACKET_SIZE)
			throw new IllegalArgumentException("Maximum TCP packet size exceeded");
	}

	protected static void validateMessageSize(int msgSz) {
		if(msgSz == 0)
			throw new IllegalArgumentException("Message cannot be empty");
	}

	protected static void validateIP(String IP) throws IllegalArgumentException {
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
	
	protected static String createPacket(int size) {
		String text = "The first assignment is dedicated to UDP/TCP socket programming with Java and testing your programs in a virtual networking environment. You will use provided starter code for UDP echo server and client, improve it and test your implementation in a setting where server and client programs are executed on different machines connected in a network.";
		return text.substring(0, size);
	}
}
