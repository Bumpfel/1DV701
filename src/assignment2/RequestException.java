package assignment2;

public class RequestException extends Exception {
	private static final long serialVersionUID = 828972198;

	
	public RequestException(String msg) {
		super(msg);
	}

	public RequestException() {
		super();
	}
}
