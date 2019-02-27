package assignment2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import assignment2.HTTPRequest.RequestMethod;

public class RequestHandler {
    private InputStream inputStream;

    public RequestHandler(InputStream inStream) {
        inputStream = inStream;
    }

    /**
     * Reads the headers of the request
     */
    public ArrayList<String> readHeaders() throws IOException {
        ArrayList<String> headers = new ArrayList<>();
        boolean emptyLineFound = false;
        StringBuilder line = new StringBuilder();
        do {
            int read = inputStream.read();
            line.append((char) read);
            
            // checks for end of line 
            if((char) read == '\n') { 
                headers.add(line.toString()); // add line to headers

                //looks for the second empty line. that is the boundary between headers and binary data under a post request 
                if(line.toString().trim().isEmpty()) {
                    if(emptyLineFound)
                        break;
                    emptyLineFound = true;
                }
                line = new StringBuilder();
            }
        }
        while(inputStream.available() > 0);

		return headers;
    }

    /**
     * To read the binary data for POST requests
     */
    public byte[] readBytes() throws IOException {
        // reading to array list to avoid manual re-size
        ArrayList<Byte> tmpBytes = new ArrayList<>();
        while(inputStream.available() > 0) {
            tmpBytes.add((byte) inputStream.read());
        }
        
        // // convert to primitive type array
        byte[] bytes = new byte[tmpBytes.size()];
        for(int i = 0; i < tmpBytes.size(); i ++) {
            bytes[i] = tmpBytes.get(i);
        }
        return bytes; //TODO remove last line. go to eof
    }

	public HTTPRequest parseRequest(ArrayList<String> headers, byte[] binaryData) throws HTTPException, ServerException {
        if(headers.isEmpty())
            throw new HTTPException("Empty request");
        else {
            // Checks that requestLine contains 3 segments
            String[] requestLine = headers.get(0).split(" ");
            if(requestLine.length != 3)
                throw new HTTPException("Invalid request");

            if(!requestLine[2].contains("HTTP/1.1"))
                throw new HTTPException("HTTP version of request not supported");

            // check if method is supported
            RequestMethod method;
            try {
                method = RequestMethod.valueOf(requestLine[0]);
            }
            catch(IllegalArgumentException e) {
                throw new ServerException("Method not supported");
            }

            String URI = requestLine[1];
            
            return new HTTPRequest(headers.toArray(new String[headers.size()]), URI, method, binaryData);
        }
    }
    
}