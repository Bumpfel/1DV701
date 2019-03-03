package assignment2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import assignment2.HTTPRequest.RequestMethod;

public class RequestHandler {
    private InputStream inputStream;
    private boolean containsMultiPartData = false;

    public RequestHandler(InputStream in) throws IOException {
        inputStream = in;
    }

    public HTTPRequest readRequest() throws IOException, ServerException, RequestException {
        ArrayList<String> headers = readHeaders();

        // read data if input stream still contains data (in case of an html post)
        byte[] data = null;
        if(inputStream.available() > 0)
            data = readData();

        return parseRequest(headers, data);
    }
    
    private ArrayList<String> readHeaders() throws IOException {
        ArrayList<String> headers = new ArrayList<>();
        boolean emptyLineFound = false;
        StringBuilder line = new StringBuilder();

        do { // blocks until input stream contains data
            char readChar = (char) inputStream.read();
            // checks for end of line 
            if(readChar != '\n')
                line.append(readChar);
            else {
                headers.add(line.toString()); // add line to headers

                if(line.toString().startsWith("Content-Type: multipart/form-data"))
                    containsMultiPartData = true;
                
                // looks for the second empty line if input stream contains multipart/form-data (both headers and binary data)
                // if not, looks for first empty line
                if(line.toString().trim().isEmpty()) {
                    if(emptyLineFound || !containsMultiPartData)
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
     * To read the binary data for POST and PUT requests
     */
    public byte[] readData() throws IOException {
        // reading to array list to avoid manual re-size
        ArrayList<Byte> tempBytes = new ArrayList<>();
        int foundEOFBytes = 0;
        int[] eof = { 13, 10, 45, 45 }; // \n--
        
        do { // blocks until input stream contains data
            int i = inputStream.read();
            tempBytes.add((byte) i);
            
            //searches for the 4 end of file bytes
            if(i == eof[foundEOFBytes])
                foundEOFBytes ++;
            else
                foundEOFBytes = 0;
            
            // breaks read if eof is found
            if(foundEOFBytes == 4)
                break;
        }
        while(inputStream.available() > 0); // still need to break if request contains an Expect: 100-Continue. Then there is no EoF

        // remove potential eof bytes
        int size = tempBytes.size();
        for(int i = size - 1; i + foundEOFBytes >= size; i --) {
            tempBytes.remove(i);
        }

        // convert to primitive type array since that is what the fileoutputstream will want
        byte[] bytes = new byte[tempBytes.size()];
        for(int i = 0; i < tempBytes.size(); i ++) {
            bytes[i] = tempBytes.get(i);
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