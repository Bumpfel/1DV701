package assignment3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

class ErrorHandler {
    void sendError(DatagramSocket socket, short errCode, String errMsg) {
        if (errCode < 0 || errCode > 7)
            throw new IllegalArgumentException("Error code is not a valid TFTP code");

        int packetLength = 4 + errMsg.length() + 1;
        ByteBuffer bb = ByteBuffer.allocate(packetLength);

        bb.putShort(TFTPServer.OP_ERR);
        bb.putShort(errCode);
        bb.put(errMsg.getBytes());
        bb.put((byte) 0);

        DatagramPacket errPacket = new DatagramPacket(bb.array(), packetLength);
        try {
            socket.send(errPacket);
        } catch (IOException e) {
            System.out.println("Could not send error msg to client: " + e.getMessage());
        }
    }
}