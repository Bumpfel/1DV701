package assignment2;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class ClientThread extends Thread {

	private BufferedReader in;
    private InputStream in2; //TODO remove one of these. This is used for readInData()
    private Scanner in3;
	private DataOutputStream out;
    private Socket socket;
    
    private final String DIR_PATH = "src/assignment2/content/";

	public ClientThread(Socket socket) {
		this.socket = socket;

		try {
			socket.setSoTimeout(10000);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));			
			out = new DataOutputStream(socket.getOutputStream());

            in2 = socket.getInputStream();
            in3 = new Scanner(socket.getInputStream());
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

	public void run() {
		try {
            // String inData = readInput();// TODO use one
            String inData = readInData();
            // String inData = readWithScanner();
            // System.out.print(inData); // TODO remove debug
            
            HTTPRequest request = HTTPRequest.parseRequest(inData);
            // System.out.println(request.METHOD + " request from " + socket.getInetAddress().toString().substring(1) + " for " + request.PATH.substring(1));
            if(request != null) {

                HTTPResponse response = HTTPResponse.createResponse(request, DIR_PATH);
                if(response != null)
                    writeResponse(response);
                
                // if(response.getFile() != null)
                // System.out.println("Sent " + response.getFile().getName() + " (" +  response.getFile().length() / 1000.000 + " kB) to " + socket.getInetAddress().toString().substring(1) + " using port " + socket.getPort());
                
            }
            socket.close();
        }
        catch(SocketTimeoutException e) {
        }
		catch(HTTPException | IOException e) {
            System.err.println(e.getMessage());
			try {
				socket.close();
			}
			catch(IOException e2) {
			}
        }
	}

	private String readInData() throws IOException {
		byte[] buffer = new byte[1500];
		StringBuilder builder = new StringBuilder();		
		// do {
        while (in2.available() > 0) {
            int read = in2.read(buffer, 0, buffer.length);
            String str = new String(buffer, "ISO-8859-15").substring(0, read);
            builder.append(str);
        }
		return builder.toString().trim();
    }
    
    private String readWithScanner() {
        String ret = new String();
        int contentlength = 0;
        in3.useDelimiter("\r\n");
        while(in3.hasNext()) {
            String read = in3.next();
            ret += read + "\r\n";
            if(read.isEmpty() || read == null) {
                break;
            }
            System.out.println(read);
            // if(read.startsWith("Content-Length")) {
            //     contentlength = Integer.parseInt(read.substring(16));
            // }

        }
        return ret;// + readBody(contentlength);
    }
	
	private String readInput() throws IOException {
        String line, requestString = new String();
        int contentlength = 0; 
        while(true) {
            line = in.readLine();
			requestString += line + "\r\n";
            if (line == null || line.equals("") || line.equals("\r\n")) {
                break;
            }
            if(line.startsWith("Content-Length")) {
                contentlength = Integer.parseInt(line.substring(16));
            }
        }
        // System.out.println(requestString);
		return requestString;
	}

    //TODO from toll
    private String readBody(int contentlength) throws IOException {
		StringBuilder data = new StringBuilder();
		for (int i = 0; i < contentlength; i++) {
			data.append((char)in.read());
        }
		System.out.println(data.toString());
		return data.toString();
    }
    
	/**
	 * Writes header, and then either file if requested and response is 200, or a defined html body
	 * @param response
	 */
	private void writeResponse(HTTPResponse response) {
		try {
			out.write(response.getHeader().getBytes());
			File file = response.getFile();
			if(file != null) {
				writeFile(file);
			}
			else {
				out.write(response.getBody().getBytes());
			}
		}
		catch(IOException e) {
			System.out.print(e.getMessage());
		}

	}
	
	private void writeFile(File file) {
		byte[] buf = new byte[1500];
		
		try(FileInputStream fileIn = new FileInputStream(file)) {
			int bytesRead;
			while((bytesRead = fileIn.read(buf)) > 0) {
				out.write(buf, 0, bytesRead);
			}
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
}