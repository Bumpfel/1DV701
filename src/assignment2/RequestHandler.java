package assignment2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import assignment2.HTTPRequest.RequestMethod;

public class RequestHandler {
    private InputStream inputStream;

    public RequestHandler(InputStream in) throws IOException {
        inputStream = in;
    }

    public HTTPRequest readRequest() throws IOException, ServerException, RequestException {
        return parseRequest(readHeaders(), readBytes());
    }

    /**
     * Reads the headers of the request
     */
    private ArrayList<String> readHeaders() throws IOException {
        ArrayList<String> headers = new ArrayList<>();
        boolean emptyLineFound = false;
        StringBuilder line = new StringBuilder();
        do {
            int read = inputStream.read();
            line.append((char) read);
            
            // checks for end of line 
            if((char) read == '\n') { 
                headers.add(line.toString()); // add line to headers

                // looks for the second empty line. that is the boundary between headers and binary data under a post request 
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
    private byte[] readBytes() throws IOException {
        // reading to array list to avoid manual re-size
        ArrayList<Byte> tmpBytes = new ArrayList<>();
        int foundEOFBytes = 0;
        int[] eof = { 13, 10, 45, 45 };

        while(inputStream.available() > 0) {
            int i = inputStream.read();
            tmpBytes.add((byte) i);

            //TODO fullösning
            //searches for the 4 end of file bytes
            if(i == eof[foundEOFBytes])
                foundEOFBytes ++;
            else
                foundEOFBytes = 0;
            // breaks read if eof is found
            if(foundEOFBytes == 4)
                break;
        }
        //... and removes them
        int n = tmpBytes.size();
        if(n >= 4) {
            for(int i = 0; i < 4; i ++)
                tmpBytes.remove(-- n);
        }

        // convert to primitive type array since that is what the fileoutputstream will want
        byte[] bytes = new byte[tmpBytes.size()];
        for(int i = 0; i < tmpBytes.size(); i ++) {
            bytes[i] = tmpBytes.get(i);
        }
        return bytes;
    }

	private HTTPRequest parseRequest(ArrayList<String> headers, byte[] binaryData) throws RequestException, ServerException {
        if(headers.isEmpty())
            throw new RequestException("400: Bad Request - Request empty");
        
        // Checks that requestLine contains 3 segments
        String[] requestLine = headers.get(0).split(" ");
        if(requestLine.length != 3)
            throw new RequestException("400: Bad Request");

        if(!requestLine[2].trim().equals("HTTP/1.1"))
            throw new ServerException("505: HTTP Version Not Supported");

        // check if method is supported. case-sensitive
        RequestMethod method;
        try {
            method = RequestMethod.valueOf(requestLine[0]);
        }
        catch(IllegalArgumentException e) {
            throw new RequestException("405: Method Not Allowed");
        }

        String URI = requestLine[1];
        
        return new HTTPRequest(headers, URI, method, binaryData);
    }
        
}