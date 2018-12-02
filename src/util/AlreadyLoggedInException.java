package util;

public class AlreadyLoggedInException extends Exception {
    public AlreadyLoggedInException(String s) {
        super(s + " is already logged in. Rejecting this new instance!");
    }
}
