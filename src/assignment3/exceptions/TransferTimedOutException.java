package assignment3.exceptions;

@SuppressWarnings("serial")
public class TransferTimedOutException extends Exception {

    private String msg;

    public TransferTimedOutException(String errMsg) {
        msg = errMsg;
    }

    @Override
    public String getMessage() {
        return msg;
    }
}