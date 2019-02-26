package assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import assignment2.HTTPRequest.RequestMethod;

public class RequestHandler {
    private InputStream inputStream;

    public RequestHandler(InputStream socketStream) {
        inputStream = socketStream;
    }

     // Currently used
     public String readChars() throws IOException {
        StringBuilder smt = new StringBuilder();
        do {
            int read = inputStream.read();
            smt.append((char) read);
        }
        while(inputStream.available() > 0);

		return smt.toString();
    }

    public String readChars2() throws IOException {
        ArrayList<Byte> smt = new ArrayList<>();
        do {
            int read = inputStream.read();
            smt.add((byte) read);
        }
        while(inputStream.available() > 0);
        
        return smt.toString();
    }   

    public String readInputStream() throws IOException {
        byte[] buffer = new byte[1500];
		StringBuilder builder = new StringBuilder();		
		do {
            int read = inputStream.read(buffer, 0, buffer.length);
            String str = new String(buffer, 0, read, "ISO-8859-1"); //ISO-8859-15
            builder.append(str);
        }
        while(inputStream.available() > 0);
        
		return builder.toString().trim();
    }
    
    public String readScanner() {
        Scanner in = new Scanner(inputStream);

        String ret = new String();
        int contentlength = 0;
        in.useDelimiter("");
        while(in.hasNext()) {
            String read = in.next();
            ret += read;
            if(read.isEmpty() || read == null) {
                break;
            }
            // if(read.startsWith("Content-Length")) {
            //     contentlength = Integer.parseInt(read.substring(16));
            // }
                
        }
        System.out.println(ret);
        return ret;
    }
	
	public String readBufferedLines() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        
        String line, requestString = new String();
        while(true) {
            line = in.readLine();
			requestString += line + "\r\n";
            if (line.isEmpty() || line.equals("\r\n")) {
                break;
            }
        }
		return requestString;
	}

	public HTTPRequest parseRequest(String requestString) throws HTTPException, IllegalArgumentException {
        String[] lines = requestString.split("\n");

        if(requestString.isEmpty())
            throw new HTTPException("Empty request");
        else {
            String[] firstLine = lines[0].split(" ");
            if(firstLine.length != 3)
                throw new HTTPException("Request has malformed header");
            if(!firstLine[2].contains("HTTP/1.1"))
                throw new HTTPException("HTTP version of request not supported");
          
            String[] headers = Arrays.copyOfRange(lines, 1, lines.length);

            String path = firstLine[1];
            if(path.endsWith("/")) { // set default file if path ends with a slash
                path += "index.html";
            }
            
            RequestMethod method = RequestMethod.valueOf(firstLine[0]);

            return new HTTPRequest(requestString, headers, path, method);
        }
    }
    
}