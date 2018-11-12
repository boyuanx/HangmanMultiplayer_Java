package util;

public class WrongPasswordException extends Exception {
    public WrongPasswordException() {
        super("The password you've entered does not match our records.");
    }
}