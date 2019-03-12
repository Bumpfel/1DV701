package assignment3.exceptions;

@SuppressWarnings("serial")
public class AllocationExceededException extends Exception {

    private String msg;

    public AllocationExceededException(String errMsg) {
        msg = errMsg;
    }

    @Override
    public String getMessage() {
        return msg;
    }
}