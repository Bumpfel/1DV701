package assignment3.exceptions;

@SuppressWarnings("serial")
public class TransferTimedOutException extends Exception {

    public TransferTimedOutException(String msg) {
        super(msg);
    }
}