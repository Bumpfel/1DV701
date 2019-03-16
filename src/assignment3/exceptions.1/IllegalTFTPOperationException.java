package assignment3.exceptions;

@SuppressWarnings("serial")
public class IllegalTFTPOperationException extends Exception {

    public IllegalTFTPOperationException(String message) {
        super(message);
    }
}